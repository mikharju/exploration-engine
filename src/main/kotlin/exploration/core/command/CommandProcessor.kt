package exploration.command

import exploration.core.engine.fireAreaTriggers
import exploration.core.engine.fireDeviceTriggers
import exploration.core.engine.fireStatusTriggers
import exploration.model.*
import exploration.state.GameState

fun processCommand(state: GameState, command: Command): GameState {
    if (state.endGameMessage != null && command !is Command.Look) {
        return state.copy(commandOutput = "The game is over. Restart to play again.")
    }

    val cmdResult = when (command) {
        is Command.Look -> CmdResult(processLook(state), null, null, true)
        is Command.Move -> runMove(state, command.areaName)
        is Command.Activate -> runActivate(state)
        is Command.TakeItem -> runTake(state, command.itemName)
        is Command.DropItem -> runDrop(state, command.itemName)
        is Command.EquipItem -> runEquip(state, command.itemName)
        is Command.UnequipItem -> runUnequip(state, command.itemName)
        is Command.Inventory -> CmdResult(processInventory(state), null, null, true)
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

    return withTriggers
}

private data class CmdResult(
    val state: GameState,
    val movedToArea: AreaId?,
    val activatedDeviceId: DeviceId?,
    val isLook: Boolean
)

private fun runMove(state: GameState, targetName: String): CmdResult {
    val currentArea = state.world.getArea(state.player.currentArea)
    val targetId = findAreaId(state, targetName) ?: return CmdResult(
        state.copy(commandOutput = "There's no place called '$targetName' here."), null, null, false)

    if (targetId !in currentArea.connections) {
        val valid = currentArea.connections.joinToString(", ")
        return CmdResult(state.copy(commandOutput = "You can't go there directly. Connected areas: $valid"), null, null, false)
    }

    if (state.isExitBlocked(state.player.currentArea, targetId)) {
        return CmdResult(
            state.copy(commandOutput = "That way is blocked."),
            null, null, false
        )
    }

    return CmdResult(
        state.copy(
            player = state.player.copy(currentArea = targetId),
            commandOutput = "You move to the ${targetId.name}."
        ),
        targetId,
        null,
        false
    )
}

private fun runActivate(state: GameState): CmdResult {
    val area = state.world.getArea(state.player.currentArea)
    val device = area.device ?: return CmdResult(
        state.copy(commandOutput = "There's nothing here to activate."), null, null, false)

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
        device.id,
        false
    )
}

private fun processLook(state: GameState): GameState {
    val areaId = state.player.currentArea
    val area = state.world.getArea(areaId)

    val deviceText = area.device?.let { d ->
        if (d.id in state.activatedDevices) {
            "\n${d.id} has been activated."
        } else {
            "\n${d.lookDescription}"
        }
    } ?: ""

    val roomItems = state.items.filter { it.location.type == ItemLocationType.AREA && (it.location.target as? LocationTarget.InArea)?.areaId == areaId }
    val itemText = if (roomItems.isNotEmpty()) {
        "\nYou see: ${roomItems.joinToString(", ") { it.id.name }}"
    } else ""

    val equipped = state.items.filter { it.location.type == ItemLocationType.EQUIPPED }
    val equippedText = if (equipped.isNotEmpty()) {
        "\nEquipped: ${equipped.joinToString(", ") { it.id.name }}"
    } else ""

    return state.copy(
        exploredAreas = state.exploredAreas + areaId,
        commandOutput = "${area.description}$deviceText$itemText$equippedText"
    )
}

private fun runTake(state: GameState, itemName: String): CmdResult {
    val item = findItem(state, itemName)
        ?: return CmdResult(state.copy(commandOutput = "There's no item called '$itemName' here."), null, null, false)

    if (item.location.type != ItemLocationType.AREA || (item.location.target as? LocationTarget.InArea)?.areaId != state.player.currentArea) {
        return CmdResult(state.copy(commandOutput = "That item isn't here."), null, null, false)
    }

    val newItems = state.items.map { i ->
        if (i.id == item.id) i.copy(location = Location(ItemLocationType.CARRIED)) else i
    }

    return CmdResult(
        state.copy(
            commandOutput = "You pick up the ${item.id.name}.",
            items = newItems
        ),
        null, null, false
    )
}

private fun runDrop(state: GameState, itemName: String): CmdResult {
    val item = findItem(state, itemName)
        ?: return CmdResult(state.copy(commandOutput = "You don't have '$itemName'."), null, null, false)

    if (item.location.type == ItemLocationType.EQUIPPED) {
        if (item.locked) {
            return CmdResult(state.copy(commandOutput = "You can't drop the ${item.id.name} — it's locked."), null, null, false)
        }
        val newItems = state.items.map { i ->
            if (i.id == item.id) i.copy(location = Location.area(state.player.currentArea)) else i
        }

        return CmdResult(
            state.copy(
                commandOutput = "You drop the ${item.id.name} (auto-unequipped).",
                items = newItems
            ),
            null, null, false
        )
    }

    if (item.location.type != ItemLocationType.CARRIED) {
        return CmdResult(state.copy(commandOutput = "That item isn't in your inventory."), null, null, false)
    }

    if (item.locked) {
        return CmdResult(state.copy(commandOutput = "You can't drop the ${item.id.name} — it's locked."), null, null, false)
    }

    val newItems = state.items.map { i ->
        if (i.id == item.id) i.copy(location = Location.area(state.player.currentArea)) else i
    }

    return CmdResult(
        state.copy(
            commandOutput = "You drop the ${item.id.name}.",
            items = newItems
        ),
        null, null, false
    )
}

private fun runEquip(state: GameState, itemName: String): CmdResult {
    val item = findItem(state, itemName)
        ?: return CmdResult(state.copy(commandOutput = "You don't have '$itemName'."), null, null, false)

    if (item.location.type != ItemLocationType.CARRIED) {
        return CmdResult(state.copy(commandOutput = "That item isn't in your inventory."), null, null, false)
    }

    if (item.locked) {
        return CmdResult(state.copy(commandOutput = "You can't equip the ${item.id.name} — it's locked."), null, null, false)
    }

    val newItems = state.items.map { i ->
        if (i.id == item.id) i.copy(location = Location(ItemLocationType.EQUIPPED)) else i
    }

    return CmdResult(
        state.copy(
            commandOutput = "You equip the ${item.id.name}.",
            items = newItems
        ),
        null, null, false
    )
}

private fun runUnequip(state: GameState, itemName: String): CmdResult {
    val item = findItem(state, itemName)
        ?: return CmdResult(state.copy(commandOutput = "You don't have '$itemName'."), null, null, false)

    if (item.location.type != ItemLocationType.EQUIPPED) {
        return CmdResult(state.copy(commandOutput = "That item isn't equipped."), null, null, false)
    }

    if (item.locked) {
        return CmdResult(state.copy(commandOutput = "You can't unequip the ${item.id.name} — it's locked."), null, null, false)
    }

    val newItems = state.items.map { i ->
        if (i.id == item.id) i.copy(location = Location(ItemLocationType.CARRIED)) else i
    }

    return CmdResult(
        state.copy(
            commandOutput = "You unequip the ${item.id.name}.",
            items = newItems
        ),
        null, null, false
    )
}

private fun processInventory(state: GameState): GameState {
    val carried = state.items.filter { it.location.type == ItemLocationType.CARRIED }
    val equipped = state.items.filter { it.location.type == ItemLocationType.EQUIPPED }

    val lines = mutableListOf<String>()
    lines.add("Inventory:")

    if (equipped.isNotEmpty()) {
        lines.add("  Equipped:")
        for (item in equipped) {
            lines.add("    [${item.id.name}] ${item.description}")
        }
    }

    if (carried.isNotEmpty()) {
        lines.add("  Carried:")
        for (item in carried) {
            lines.add("    [${item.id.name}] ${item.description}")
        }
    }

    if (equipped.isEmpty() && carried.isEmpty()) {
        lines.add("  (empty)")
    }

    return state.copy(commandOutput = lines.joinToString("\n"))
}

private fun findItem(state: GameState, name: String): Item? {
    val lower = name.lowercase()
    return state.items.find { it.id.name.lowercase() == lower }
}

private fun findAreaId(state: GameState, name: String): AreaId? {
    val lower = name.lowercase()
    return state.world.areas.keys.find { it.name.lowercase() == lower }
}
