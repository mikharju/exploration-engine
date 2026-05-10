package exploration.adapter.jsonloader

import exploration.port.ScenarioRepository
import exploration.scenario.loadScenario
import exploration.state.GameState
import java.nio.file.Path

class JsonScenarioRepository : ScenarioRepository {
    override fun load(id: String): GameState = loadScenario(Path.of(id))
}