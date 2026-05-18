package exploration.adapter.storage

import exploration.port.GameRef
import exploration.port.GameStateStore
import exploration.port.ScenarioRepository
import exploration.core.state.GameState
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

class InMemoryGameStateStore(
    private val scenarioRepo: ScenarioRepository,
    private val store: ConcurrentHashMap<String, GameState> = ConcurrentHashMap(),
    private val counter: AtomicLong = AtomicLong()
) : GameStateStore {

    override fun createGame(scenarioId: String): GameRef {
        val id = "game-${counter.incrementAndGet()}"
        val state = scenarioRepo.load(scenarioId)
        store[id] = state
        return GameRef(id)
    }

    override fun loadGame(ref: GameRef): GameState {
        return store[ref.id] ?: throw IllegalArgumentException("Game not found: ${ref.id}")
    }

    override fun saveGame(ref: GameRef, state: GameState) {
        store[ref.id] = state
    }
}
