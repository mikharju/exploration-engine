package exploration.core.engine

import exploration.core.model.*
import exploration.core.state.GameState

fun fireAreaTriggers(state: GameState, areaId: AreaId): GameState {
    val candidates = state.triggers.filter { it.owner is TriggerOwner.Area && it.areaId() == areaId }
    return fireMatchingTriggers(state, candidates)
}

fun fireDeviceTriggers(state: GameState, deviceId: DeviceId): GameState {
    val candidates = state.triggers.filter { it.owner is TriggerOwner.Device && it.deviceId() == deviceId }
    return fireMatchingTriggers(state, candidates)
}

fun fireStatusTriggers(state: GameState): GameState {
    val candidates = state.triggers.filter { it.owner is TriggerOwner.Status }
    return fireMatchingTriggers(state, candidates)
}

private fun fireMatchingTriggers(
    state: GameState,
    candidates: List<Trigger>
): GameState {
    data class EffectsAccumulator(
        val player: Player = state.player,
        val newTexts: List<String> = emptyList(),
        val storyMessages: List<String> = emptyList(),
        val locationUpdates: Map<ItemId, Location> = emptyMap(),
        val lockedUpdates: Map<ItemId, Boolean> = emptyMap(),
        val exitStateUpdates: Map<ExitId, ExitState> = emptyMap(),
        val exitHiddenUpdates: Map<ExitId, Boolean> = emptyMap(),
        val endGameText: String? = null
    )
    data class MutationAccumulator(
        val items: List<Item>,
        val turn: Int,
        val updatedTriggers: Map<String, Trigger> = emptyMap(),
        val effectsAcc: EffectsAccumulator = EffectsAccumulator()
    )

    fun effectiveLocation(accum: MutationAccumulator, itemId: ItemId): Location? =
        accum.effectsAcc.locationUpdates[itemId] ?: state.items.find { it.id == itemId }?.location

    val finalAcc = candidates.fold(MutationAccumulator(state.items, state.turn)) { acc, trigger ->
        if (trigger.remainingActivations <= 0) return@fold acc

        val conditionsMet = trigger.conditions.all { cond ->
            when (cond.checkType) {
                CheckType.STATUS -> {
                    val statusValue = if (cond.statusName == "health") acc.effectsAcc.player.health
                    else cond.statusName?.let { acc.effectsAcc.player.statuses.getOrElse(it) { 0 } } ?: 0
                    when (cond.op) {
                        ComparisonOp.GT -> statusValue > cond.threshold
                        ComparisonOp.LT -> statusValue < cond.threshold
                        ComparisonOp.LTE -> statusValue <= cond.threshold
                        ComparisonOp.GTE -> statusValue >= cond.threshold
                        ComparisonOp.EQ -> statusValue == cond.threshold
                    }
                }
                CheckType.ITEM_CARRIED -> cond.itemId?.let {
                    effectiveLocation(acc, it)?.type == ItemLocationType.CARRIED
                } ?: false
                CheckType.ITEM_EQUIPPED -> cond.itemId?.let {
                    effectiveLocation(acc, it)?.type == ItemLocationType.EQUIPPED
                } ?: false
            }
        }

        if (!conditionsMet) return@fold acc

        // Interval check for STATUS triggers
        if (trigger.owner is TriggerOwner.Status && trigger.intervalTurns != null && trigger.lastFireTurn != null) {
            val turnsSince = state.turn - trigger.lastFireTurn
            if (turnsSince < trigger.intervalTurns) return@fold acc
        }

        // Apply effects within this trigger's scope
        val newEffects: EffectsAccumulator = trigger.effects.fold(acc.effectsAcc) { effAcc, effect ->
            when (effect) {
                is Effect.DisplayText -> if (effect.text.isNotBlank()) effAcc.copy(newTexts = effAcc.newTexts + effect.text) else effAcc
                is Effect.ChangeHealth -> effAcc.copy(player = effAcc.player.adjustHealth(effect.amount))
                is Effect.AdjustStatus -> effAcc.copy(player = effAcc.player.adjustStatus(
                    effect.statusName, effect.amount, state.statusBounds[effect.statusName]))
                is Effect.SetStatus -> effAcc.copy(player = effAcc.player.setStatus(
                    effect.statusName, effect.value, state.statusBounds[effect.statusName]))
                is Effect.SetLocation ->
                    if (state.items.any { it.id == effect.itemId }) effAcc.copy(locationUpdates = effAcc.locationUpdates + (effect.itemId to Location.fromTarget(effect.target)))
                    else effAcc
                is Effect.LockItem ->  effAcc.copy(lockedUpdates = effAcc.lockedUpdates + (effect.itemId to true))
                is Effect.UnlockItem ->  effAcc.copy(lockedUpdates = effAcc.lockedUpdates + (effect.itemId to false))
                is Effect.StoryMessage -> if (effect.text.isNotBlank()) effAcc.copy(storyMessages = effAcc.storyMessages + effect.text) else effAcc
                is Effect.SetExitBlocked -> {
                    val id = ExitId(effect.from, effect.to)
                    val newState = if (effect.blocked) ExitState.BLOCKED else ExitState.OPEN
                    effAcc.copy(exitStateUpdates = effAcc.exitStateUpdates + (id to newState))
                }
                is Effect.HideExit -> {
                    val id = ExitId(effect.from, effect.to)
                    effAcc.copy(exitHiddenUpdates = effAcc.exitHiddenUpdates + (id to true))
                }
                is Effect.ShowExit -> {
                    val id = ExitId(effect.from, effect.to)
                    effAcc.copy(exitHiddenUpdates = effAcc.exitHiddenUpdates + (id to false))
                }
                is Effect.EndGame -> if (effAcc.endGameText == null) effAcc.copy(endGameText = effect.text) else effAcc
            }
        }

        acc.copy(
            effectsAcc = newEffects,
            updatedTriggers = acc.updatedTriggers + (trigger.id to trigger.copy(
                remainingActivations = trigger.remainingActivations - 1,
                lastFireTurn = state.turn
            ))
        )
    }

    val allTriggers = if (finalAcc.updatedTriggers.isNotEmpty()) {
        state.triggers.map { t -> finalAcc.updatedTriggers[t.id] ?: t }
    } else {
        state.triggers
    }

    val newItems = state.items.map { item ->
        val itemElsewhere = if (item.id in finalAcc.effectsAcc.locationUpdates) item.copy(location = finalAcc.effectsAcc.locationUpdates[item.id]!!) else item
        if (item.id in finalAcc.effectsAcc.lockedUpdates) itemElsewhere.copy(locked = finalAcc.effectsAcc.lockedUpdates[item.id]!!) else itemElsewhere
    }

    // Build merged exit states: merge new updates with existing state
    val mergedExitStates = mutableMapOf<ExitId, ExitStateData>()
    for (exitId in state.exitStates.keys + finalAcc.effectsAcc.exitStateUpdates.keys + finalAcc.effectsAcc.exitHiddenUpdates.keys) {
        val current = state.exitStates[exitId] ?: ExitStateData()
        val updatedState = finalAcc.effectsAcc.exitStateUpdates[exitId] ?: current.state
        val updatedHidden = finalAcc.effectsAcc.exitHiddenUpdates[exitId] ?: current.hidden
        mergedExitStates[exitId] = ExitStateData(updatedState, updatedHidden)
    }

    val finalEndGame = if (state.endGameMessage == null && finalAcc.effectsAcc.endGameText != null) {
        finalAcc.effectsAcc.endGameText
    } else state.endGameMessage

    return state.copy(
        player = finalAcc.effectsAcc.player,
        triggerTexts = if (finalAcc.effectsAcc.newTexts.isNotEmpty()) state.triggerTexts + finalAcc.effectsAcc.newTexts else state.triggerTexts,
        storyMessages = if (finalAcc.effectsAcc.storyMessages.isNotEmpty()) state.storyMessages + finalAcc.effectsAcc.storyMessages else state.storyMessages,
        triggers = allTriggers,
        items = newItems,
        exitStates = mergedExitStates,
        endGameMessage = finalEndGame
    )
}
