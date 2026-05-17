package exploration.port

import exploration.model.Direction

sealed class InputEvent {
    object Look : InputEvent()
    object Activate : InputEvent()
    data class MoveDirection(val direction: Direction) : InputEvent()
    data class TakeItem(val itemName: String) : InputEvent()
    data class DropItem(val itemName: String) : InputEvent()
    data class EquipItem(val itemName: String) : InputEvent()
    data class UnequipItem(val itemName: String) : InputEvent()
    object Inventory : InputEvent()

    typealias Direction = exploration.model.Direction
}
