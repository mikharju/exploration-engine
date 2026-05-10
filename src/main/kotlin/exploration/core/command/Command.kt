package exploration.command

sealed class Command {
    object Look : Command()
    data class Move(val areaName: String) : Command()
    object Activate : Command()
}
