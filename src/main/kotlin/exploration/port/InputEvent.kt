package exploration.port

sealed class InputEvent {
    data class Text(val text: String) : InputEvent()
    object Look : InputEvent()
    object Activate : InputEvent()
    data class MoveDirection(val slot: DirectionSlot) : InputEvent()
    object Exit : InputEvent()
}

enum class DirectionSlot { W, A, S, D }