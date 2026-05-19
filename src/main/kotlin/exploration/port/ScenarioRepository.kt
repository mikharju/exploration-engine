package exploration.port

import exploration.core.state.GameState

interface ScenarioRepository {
    fun load(id: String): GameState
}