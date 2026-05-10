package exploration.command

sealed class Command {
    object Look : Command()
    data class Move(val areaName: String) : Command()
    object Activate : Command()
}

fun parseInput(input: String): Command? = when (input.trim().lowercase()) {
    "look" -> Command.Look
    "activate", "use" -> Command.Activate
    else -> {
        val parts = input.trim().split(" ", limit = 2)
        if (parts.first().lowercase() == "move" && parts.size > 1) {
            Command.Move(parts.subList(1, parts.size).joinToString(" "))
        } else {
            null
        }
    }
}
