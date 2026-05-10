package exploration.port

import exploration.state.GameState

interface GameEngine {
    fun start(scenarioId: String): GameState
    fun tick(state: GameState, event: InputEvent): GameState
    fun view(state: GameState): ViewData
}