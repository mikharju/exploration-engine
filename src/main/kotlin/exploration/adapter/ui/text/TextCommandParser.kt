package exploration.adapter.ui.text

import exploration.port.InputEvent

fun parseText(input: String): InputEvent? {
    val trimmed = input.trim()
    val lower = trimmed.lowercase()
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
        "move", "m" -> {
            val dir = when (arg) {
                "w" -> 0; "a" -> 1; "s" -> 2; "d" -> 3
                else -> return null
            }
            InputEvent.MoveDirection(dir)
        }
        "w" -> InputEvent.MoveDirection(0)
        "a" -> InputEvent.MoveDirection(1)
        "s" -> InputEvent.MoveDirection(2)
        "d" -> InputEvent.MoveDirection(3)
        else -> null
    }
}
