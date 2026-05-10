package exploration.port

import exploration.state.GameState

interface ScenarioRepository {
    fun load(id: String): GameState
}