package exploration.port

sealed class InputEvent {
    object Look : InputEvent()
    object Activate : InputEvent()
    data class MoveDirection(val direction: Direction) : InputEvent()
    data class TakeItem(val itemName: String) : InputEvent()
    data class DropItem(val itemName: String) : InputEvent()
    data class EquipItem(val itemName: String) : InputEvent()
    data class UnequipItem(val itemName: String) : InputEvent()
    object Inventory : InputEvent()

    enum class Direction(val index: Int, val key: String, val displayName: String) {
        North(0, "w", "North"),
        West(1, "a", "West"),
        South(2, "s", "South"),
        East(3, "d", "East");

        companion object {
            fun fromIndex(index: Int): Direction? = values().getOrNull(index)
        }
    }
}
