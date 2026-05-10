package exploration.adapter.jsonloader

import exploration.port.ScenarioRepository
import exploration.scenario.assembleGame
import exploration.state.GameState
import java.nio.file.Path

class JsonScenarioRepository : ScenarioRepository {
    override fun load(id: String): GameState {
        val files = loadScenarioFiles(Path.of(id))
        return assembleGame(files.config, files.areas, files.devices)
    }
}