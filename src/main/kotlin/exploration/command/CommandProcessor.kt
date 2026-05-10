package exploration.command

import exploration.model.AreaId
import exploration.state.GameState

fun processCommand(state: GameState, command: Command): GameState {
    if (state.isOver) {
        return state.copy(output = "The game is over. Restart to play again.")
    }

    val result = when (command) {
        is Command.Look -> processLook(state)
        is Command.Move -> processMove(state, command.areaName)
        is Command.Activate -> processActivate(state)
    }

    return result.checkOutcome()
}

private fun processLook(state: GameState): GameState {
    val areaId = state.player.currentArea
    val area = state.world.getArea(areaId)

    val deviceText = area.device?.let { d ->
        if (d.id in state.activatedDevices) {
            "\nYou see a deactivated ${d.id} here."
        } else {
            "\n${d.lookDescription}"
        }
    } ?: ""

    return state.copy(
        exploredAreas = state.exploredAreas + areaId,
        output = "${area.description}$deviceText"
    )
}

private fun findAreaId(world: GameState, name: String): AreaId? {
    val lower = name.lowercase()
    return world.world.areas.keys.find { it.name.lowercase() == lower }
}

private fun processMove(state: GameState, targetName: String): GameState {
    val currentArea = state.world.getArea(state.player.currentArea)
    val targetId = findAreaId(state, targetName)

    if (targetId == null) {
        return state.copy(output = "There's no place called '$targetName' here.")
    }

    if (targetId !in currentArea.connections) {
        val valid = currentArea.connections.joinToString(", ")
        return state.copy(output = "You can't go there directly. Connected areas: $valid")
    }

    return state.copy(
        player = state.player.copy(currentArea = targetId),
        output = "You move to the ${targetId.name}."
    )
}

private fun processActivate(state: GameState): GameState {
    val area = state.world.getArea(state.player.currentArea)
    val device = area.device

    if (device == null) {
        return state.copy(output = "There's nothing here to activate.")
    }

    if (device.id in state.activatedDevices) {
        return state.copy(output = "${device.id} has already been activated.")
    }

    val newPlayer = state.player.adjustHealth(device.healthEffect)
    val healthMsg = when (device.healthEffect) {
        0 -> ""
        in 1..Int.MAX_VALUE -> " (+${device.healthEffect} health)"
        else -> " (${device.healthEffect} health)"
    }

    return state.copy(
        player = newPlayer,
        activatedDevices = state.activatedDevices + device.id,
        output = "${device.activateDescription}$healthMsg"
    )
}
