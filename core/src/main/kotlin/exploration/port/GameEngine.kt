package exploration.port

interface GameEngine {
    fun start(scenarioId: String): GameRef
    fun tick(ref: GameRef, event: InputEvent): ViewData
}
