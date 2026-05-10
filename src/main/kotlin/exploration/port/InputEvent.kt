package exploration.port

sealed class InputEvent {
    object Look : InputEvent()
    object Activate : InputEvent()
    data class Move(val areaName: String) : InputEvent()
    object Exit : InputEvent()
}