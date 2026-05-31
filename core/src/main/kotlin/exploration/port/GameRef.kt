package exploration.port

/**
 * Opaque handle for a game instance stored by [GameStateStore].
 * UI adapters hold this instead of GameState.
 */
data class GameRef(val id: String)
