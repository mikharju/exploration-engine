package exploration.core.engine

import exploration.model.*
import exploration.state.GameState

fun fireAreaTriggers(state: GameState, areaName: String): GameState {
    val candidates = state.triggers.filter { it.ownerType == OwnerType.AREA && it.ownerId == areaName }
    return fireMatchingTriggers(state, candidates)
}

fun fireDeviceTriggers(state: GameState, deviceId: String): GameState {
    val candidates = state.triggers.filter { it.ownerType == OwnerType.DEVICE && it.ownerId == deviceId }
    return fireMatchingTriggers(state, candidates)
}

fun fireStatusTriggers(state: GameState): GameState {
    val candidates = state.triggers.filter { it.ownerType == OwnerType.STATUS }
    return fireMatchingTriggers(state, candidates)
}

private fun fireMatchingTriggers(
    state: GameState,
    candidates: List<Trigger>
): GameState {
    var current = state
    val newTexts = mutableListOf<String>()
    var player = state.player
    val updatedTriggers = mutableListOf<Trigger>()

    for (trigger in candidates) {
        if (trigger.remainingActivations <= 0) continue

        val conditionsMet = trigger.conditions.all { cond ->
            val statusValue = player.statuses.getOrElse(cond.statusName) { 0 }
            when (cond.op) {
                ComparisonOp.GT -> statusValue > cond.threshold
                ComparisonOp.LT -> statusValue < cond.threshold
            }
        }

        if (!conditionsMet) continue

        if (trigger.ownerType == OwnerType.STATUS && trigger.intervalTurns != null && trigger.lastFireTurn != null) {
            val turnsSince = current.turn - trigger.lastFireTurn
            if (turnsSince < trigger.intervalTurns) continue
        }

        var newText = ""
        for (effect in trigger.effects) {
            when (effect) {
                is Effect.DisplayText -> newText += effect.text
                is Effect.ChangeHealth -> player = player.adjustHealth(effect.amount)
                is Effect.AdjustStatus -> player = player.adjustStatus(
                    effect.statusName, effect.amount, current.statusBounds[effect.statusName]
                )
                is Effect.SetStatus -> player = player.setStatus(
                    effect.statusName, effect.value, current.statusBounds[effect.statusName]
                )
            }
        }

        newTexts.add(newText)
        updatedTriggers.add(trigger.copy(
            remainingActivations = trigger.remainingActivations - 1,
            lastFireTurn = current.turn
        ))
    }

    val allTriggers = if (updatedTriggers.isNotEmpty()) {
        val updatedMap = updatedTriggers.associateBy { it.id }
        current.triggers.map { t -> updatedMap[t.id] ?: t }
    } else {
        current.triggers
    }

    val filteredTexts = newTexts.filter { it.isNotBlank() }

    return current.copy(
        player = player,
        triggerTexts = if (filteredTexts.isNotEmpty()) state.triggerTexts + filteredTexts else state.triggerTexts,
        triggers = allTriggers
    )
}
