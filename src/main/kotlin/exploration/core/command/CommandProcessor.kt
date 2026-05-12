package exploration.command

import exploration.core.engine.fireAreaTriggers
import exploration.core.engine.fireDeviceTriggers
import exploration.core.engine.fireStatusTriggers
import exploration.model.AreaId
import exploration.state.GameState

fun processCommand(state: GameState, command: Command): GameState {
    if (state.isOver) {
        return state.copy(commandOutput = "The game is over. Restart to play again.")
    }

    val cmdResult = when (command) {
        is Command.Look -> CmdResult(processLook(state), null, null, true)
        is Command.Move -> runMove(state, command.areaName)
        is Command.Activate -> runActivate(state)
    }

    val withTriggers = if (!cmdResult.isLook) {
        var s = cmdResult.state
        if (cmdResult.movedToArea != null) s = fireAreaTriggers(s, cmdResult.movedToArea)
        if (cmdResult.activatedDeviceId != null) s = fireDeviceTriggers(s, cmdResult.activatedDeviceId)
        s = s.copy(turn = s.turn + 1)
        fireStatusTriggers(s)
    } else {
        cmdResult.state
    }

    return withTriggers.checkOutcome()
}

private data class CmdResult(
    val state: GameState,
    val movedToArea: String?,
    val activatedDeviceId: String?,
    val isLook: Boolean
)

private fun runMove(state: GameState, targetName: String): CmdResult {
    val currentArea = state.world.getArea(state.player.currentArea)
    val targetId = findAreaId(state, targetName)

    if (targetId == null) {
        return CmdResult(state.copy(commandOutput = "There's no place called '$targetName' here."), null, null, false)
    }

    if (targetId !in currentArea.connections) {
        val valid = currentArea.connections.joinToString(", ")
        return CmdResult(state.copy(commandOutput = "You can't go there directly. Connected areas: $valid"), null, null, false)
    }

    return CmdResult(
        state.copy(
            player = state.player.copy(currentArea = targetId),
            commandOutput = "You move to the ${targetId.name}."
        ),
        targetId.name,
        null,
        false
    )
}

private fun runActivate(state: GameState): CmdResult {
    val area = state.world.getArea(state.player.currentArea)
    val device = area.device

    if (device == null) {
        return CmdResult(state.copy(commandOutput = "There's nothing here to activate."), null, null, false)
    }

    if (device.id in state.activatedDevices) {
        return CmdResult(state.copy(commandOutput = "${device.id} has already been activated."), null, null, false)
    }

    var player = state.player
    val msgs = mutableListOf<String>()

    player = player.adjustHealth(device.healthEffect)
    when (device.healthEffect) {
        0 -> {}
        in 1..Int.MAX_VALUE -> msgs.add("+${device.healthEffect} health")
        else -> msgs.add("${device.healthEffect} health")
    }

    for ((name, amount) in device.statusEffects) {
        val oldVal = player.statuses.getOrElse(name) { 0 }
        player = player.adjustStatus(name, amount, state.statusBounds[name])
        val newVal = player.statuses.getValue(name)
        if (amount != 0 || newVal != oldVal) {
            msgs.add("$name: $newVal")
        }
    }

    val suffix = if (msgs.isNotEmpty()) " (${msgs.joinToString(", ")})" else ""

    return CmdResult(
        state.copy(
            player = player,
            activatedDevices = state.activatedDevices + device.id,
            commandOutput = "${device.activateDescription}$suffix"
        ),
        null,
        device.id.name,
        false
    )
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
        commandOutput = "${area.description}$deviceText"
    )
}

private fun findAreaId(world: GameState, name: String): AreaId? {
    val lower = name.lowercase()
    return world.world.areas.keys.find { it.name.lowercase() == lower }
}
