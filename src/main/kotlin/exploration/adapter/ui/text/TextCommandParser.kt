package exploration.adapter.ui.text

import exploration.port.InputEvent

fun parseText(input: String): InputEvent? = when (input.trim().lowercase()) {
    "look" -> InputEvent.Look
    "activate", "use" -> InputEvent.Activate
    else -> {
        val parts = input.trim().split(" ", limit = 2)
        if (parts.first().lowercase() == "move" && parts.size > 1) {
            InputEvent.Move(parts.subList(1, parts.size).joinToString(" "))
        } else {
            null
        }
    }
}
