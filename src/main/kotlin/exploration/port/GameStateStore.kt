package exploration.port

import exploration.core.state.GameState

/**
 * Port for persisting [GameState] instances identified by [GameRef].
 */
interface GameStateStore {
    /** Creates a new game from a scenario and stores it, returning the reference. */
    fun createGame(scenarioId: String): GameRef

    /** Loads a stored game state. Throws if not found. */
    fun loadGame(ref: GameRef): GameState

    /** Saves an updated game state. */
    fun saveGame(ref: GameRef, state: GameState)
}
