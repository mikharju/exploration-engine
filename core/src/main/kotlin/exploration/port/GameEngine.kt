package exploration.port

interface GameEngine {
    val tickCount: Int
    fun start(scenarioId: String): GameRef
    fun tick(ref: GameRef, event: InputEvent): ViewData
}
