package exploration.adapter.ui.text

import exploration.port.InputEvent

fun parseText(input: String): InputEvent? = when (input.trim().lowercase()) {
    "look", "l" -> InputEvent.Look
    "activate", "use", "u" -> InputEvent.Activate
    "w" -> InputEvent.MoveDirection(0)
    "a" -> InputEvent.MoveDirection(1)
    "s" -> InputEvent.MoveDirection(2)
    "d" -> InputEvent.MoveDirection(3)
    else -> {
        val parts = input.trim().split(" ", limit = 2)
        if (parts.first() == "move" && parts.size > 1) {
            when (parts[1].lowercase()) {
                "w" -> InputEvent.MoveDirection(0)
                "a" -> InputEvent.MoveDirection(1)
                "s" -> InputEvent.MoveDirection(2)
                "d" -> InputEvent.MoveDirection(3)
                else -> null
            }
        } else {
            null
        }
    }
}
