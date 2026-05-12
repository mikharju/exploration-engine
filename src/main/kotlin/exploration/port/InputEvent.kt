package exploration.port

sealed class InputEvent {
    object Look : InputEvent()
    object Activate : InputEvent()
    data class MoveDirection(val index: Int) : InputEvent()
}
