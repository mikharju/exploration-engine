package exploration.command

sealed class Command {
    object Look : Command()
    data class Move(val areaName: String) : Command()
    object Activate : Command()
    data class TakeItem(val itemName: String) : Command()
    data class DropItem(val itemName: String) : Command()
    data class EquipItem(val itemName: String) : Command()
    data class UnequipItem(val itemName: String) : Command()
    object Inventory : Command()
}
