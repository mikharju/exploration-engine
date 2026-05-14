package exploration.core.engine

import exploration.model.*
import exploration.state.GameState

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
    val newTexts = mutableListOf<String>()
    var player = state.player
    val updatedTriggers = mutableListOf<Trigger>()
    val locationUpdates = mutableMapOf<ItemId, Location>()
    val lockedUpdates = mutableMapOf<ItemId, Boolean>()

    for (trigger in candidates) {
        if (trigger.remainingActivations <= 0) continue

        fun effectiveLocation(itemId: ItemId): Location? =
            locationUpdates[itemId] ?: state.items.find { it.id == itemId }?.location

        val conditionsMet = trigger.conditions.all { cond ->
            when (cond.checkType) {
                CheckType.STATUS -> {
                    val statusValue = cond.statusName?.let { player.statuses.getOrElse(it) { 0 } } ?: 0
                    when (cond.op) {
                        ComparisonOp.GT -> statusValue > cond.threshold
                        ComparisonOp.LT -> statusValue < cond.threshold
                    }
                }
                CheckType.ITEM_CARRIED -> cond.itemId?.let { effectiveLocation(it)?.type == ItemLocationType.CARRIED } ?: false
                CheckType.ITEM_EQUIPPED -> cond.itemId?.let { effectiveLocation(it)?.type == ItemLocationType.EQUIPPED } ?: false
            }
        }

        if (!conditionsMet) continue

        if (trigger.owner is TriggerOwner.Status && trigger.intervalTurns != null && trigger.lastFireTurn != null) {
            val turnsSince = state.turn - trigger.lastFireTurn
            if (turnsSince < trigger.intervalTurns) continue
        }

        var newText = ""
        for (effect in trigger.effects) {
            when (effect) {
                is Effect.DisplayText -> newText += effect.text
                is Effect.ChangeHealth -> player = player.adjustHealth(effect.amount)
                is Effect.AdjustStatus -> player = player.adjustStatus(
                    effect.statusName, effect.amount, state.statusBounds[effect.statusName]
                )
                is Effect.SetStatus -> player = player.setStatus(
                    effect.statusName, effect.value, state.statusBounds[effect.statusName]
                )
                is Effect.SetLocation -> {
                    if (state.items.any { it.id == effect.itemId }) {
                        locationUpdates[effect.itemId] = Location.fromTarget(effect.target)
                    }
                }
                is Effect.LockItem -> lockedUpdates[effect.itemId] = true
                is Effect.UnlockItem -> lockedUpdates[effect.itemId] = false
            }
        }

        newTexts.add(newText)
        updatedTriggers.add(trigger.copy(
            remainingActivations = trigger.remainingActivations - 1,
            lastFireTurn = state.turn
        ))
    }

    val allTriggers = if (updatedTriggers.isNotEmpty()) {
        val updatedMap = updatedTriggers.associateBy { it.id }
        state.triggers.map { t -> updatedMap[t.id] ?: t }
    } else {
        state.triggers
    }

    val newItems = state.items.map { item ->
        var updated = item
        if (item.id in locationUpdates) {
            updated = updated.copy(location = locationUpdates[item.id]!!)
        }
        if (item.id in lockedUpdates) {
            updated = updated.copy(locked = lockedUpdates[item.id]!!)
        }
        updated
    }

    val filteredTexts = newTexts.filter { it.isNotBlank() }

    return state.copy(
        player = player,
        triggerTexts = if (filteredTexts.isNotEmpty()) state.triggerTexts + filteredTexts else state.triggerTexts,
        triggers = allTriggers,
        items = newItems
    )
}
