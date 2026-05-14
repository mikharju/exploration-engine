package exploration.adapter.ui.text

import exploration.port.InputEvent
import exploration.port.InputEvent.Direction.*

fun parseText(input: String): InputEvent? {
    val trimmed = input.trim()
    val parts = trimmed.split(" ", limit = 2)
    val verb = parts.firstOrNull()?.lowercase() ?: return null
    val arg = parts.getOrNull(1)?.trim()

    return when (verb) {
        "look", "l" -> InputEvent.Look
        "activate", "use", "u" -> InputEvent.Activate
        "inv", "inventory", "i" -> InputEvent.Inventory
        "take", "get", "grab" -> arg?.let { InputEvent.TakeItem(it) } ?: InputEvent.TakeItem("")
        "drop", "discard" -> arg?.let { InputEvent.DropItem(it) } ?: InputEvent.DropItem("")
        "equip", "wear" -> arg?.let { InputEvent.EquipItem(it) } ?: InputEvent.EquipItem("")
        "unequip", "remove" -> arg?.let { InputEvent.UnequipItem(it) } ?: InputEvent.UnequipItem("")
        "move", "m" -> parseDirection(arg)
        "w", "a", "s", "d" -> parseDirection(verb)
        else -> null
    }
}

private fun parseDirection(arg: String?): InputEvent? {
    val dir = when (arg) {
        "w" -> North; "a" -> West; "s" -> South; "d" -> East
        else -> return null
    }
    return InputEvent.MoveDirection(dir)
}
