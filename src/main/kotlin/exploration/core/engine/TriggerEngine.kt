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
    val locationUpdates = mutableMapOf<String, Location>()
    val lockedUpdates = mutableMapOf<String, Boolean>()

    for (trigger in candidates) {
        if (trigger.remainingActivations <= 0) continue

        fun effectiveLocation(itemName: String): Location? =
            locationUpdates[itemName] ?: state.items.find { it.id.name == itemName }?.location

        fun isLocked(itemName: String): Boolean =
            lockedUpdates[itemName] ?: state.items.find { it.id.name == itemName }?.locked ?: false

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
                is Effect.SetLocation -> {
                    if (state.items.any { it.id.name == effect.itemId }) {
                        locationUpdates[effect.itemId] = Location(effect.locationType, effect.locationId)
                    }
                }
                is Effect.LockItem -> lockedUpdates[effect.itemId] = true
                is Effect.UnlockItem -> lockedUpdates[effect.itemId] = false
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

    val newItems = state.items.map { item ->
        var updated = item
        if (item.id.name in locationUpdates) {
            updated = updated.copy(location = locationUpdates[item.id.name]!!)
        }
        if (item.id.name in lockedUpdates) {
            updated = updated.copy(locked = lockedUpdates[item.id.name]!!)
        }
        updated
    }

    val filteredTexts = newTexts.filter { it.isNotBlank() }

    return current.copy(
        player = player,
        triggerTexts = if (filteredTexts.isNotEmpty()) state.triggerTexts + filteredTexts else state.triggerTexts,
        triggers = allTriggers,
        items = newItems
    )
}
