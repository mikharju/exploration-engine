package exploration.core.engine

import exploration.model.*
import exploration.port.*
import exploration.state.GameState
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class GameEngineImplTest {

    private val world = World(
        areas = mapOf(
            AreaId("Forest") to Area(AreaId("Forest"), "A forest.", setOf(Exit(AreaId("Cave"), Direction.East))),
            AreaId("Cave") to Area(AreaId("Cave"), "A cave.", setOf(Exit(AreaId("Forest"), Direction.West)))
        ),
        startArea = AreaId("Forest")
    )

    private val scenarioRepo: ScenarioRepository = object : ScenarioRepository {
        override fun load(id: String): GameState = GameState(world, Player(10, 10, world.startArea))
    }

    @Test
    fun `start creates game via store`() {
        val store = object : GameStateStore {
            override fun createGame(scenarioId: String): GameRef {
                return GameRef(scenarioId)
            }

            override fun loadGame(ref: GameRef): GameState = GameState(world, Player(10, 10, world.startArea))
            override fun saveGame(ref: GameRef, state: GameState) {}
        }

        val engine = GameEngineImpl(scenarioRepo, store)
        val ref = engine.start("test")

        assertEquals(GameRef("test"), ref)
    }

    @Test
    fun `tick with Look returns view`() {
        val store = InMemoryStore(world)
        val engine = GameEngineImpl(scenarioRepo, store)
        val ref = engine.start("test")

        val view = engine.tick(ref, InputEvent.Look)

        assertEquals("A forest.", view.commandText)
        assertEquals(1, view.exploredCount)
        assertEquals(2, view.totalAreas)
    }

    @Test
    fun `view shows exits`() {
        val store = InMemoryStore(world)
        val engine = GameEngineImpl(scenarioRepo, store)
        val ref = engine.start("test")

        val view = engine.tick(ref, InputEvent.Look)

        assertEquals(4, view.exits.size)
        assertEquals(null, view.exits[0]) // North has no exit from Forest
        assertEquals(null, view.exits[1]) // West has no exit from Forest
    }

    @Test
    fun `tick with direction to non-exit returns error`() {
        val store = InMemoryStore(world)
        val engine = GameEngineImpl(scenarioRepo, store)
        val ref = engine.start("test")

        val view = engine.tick(ref, InputEvent.MoveDirection(Direction.South))

        assertEquals("Can't move that way.", view.outputLine)
    }

    @Test
    fun `tick with valid direction moves`() {
        var savedState: GameState? = null
        val store = InMemoryStore(world) { savedState = it }
        val engine = GameEngineImpl(scenarioRepo, store)
        val ref = engine.start("test")

        engine.tick(ref, InputEvent.MoveDirection(Direction.East))

        assertEquals(AreaId("Cave"), savedState!!.player.currentArea)
    }

    @Test
    fun `tick blocks when game already ended`() {
        val store = object : GameStateStore {
            override fun createGame(scenarioId: String): GameRef = GameRef(scenarioId)
            override fun loadGame(ref: GameRef): GameState = GameState(world, Player(10, 10, world.startArea), endGameMessage = "You won!")
            override fun saveGame(ref: GameRef, state: GameState) {}
        }

        val engine = GameEngineImpl(scenarioRepo, store)
        val ref = engine.start("test")

        // loadGame always returns ended state
        val view = engine.tick(ref, InputEvent.Look)

        assertEquals("You won!", view.endGameMessage)
    }

    private class InMemoryStore(
        world: World,
        private val onSave: ((GameState) -> Unit)? = null
    ) : GameStateStore {

        private var _state: GameState = GameState(world, Player(10, 10, world.startArea))

        override fun createGame(scenarioId: String): GameRef = GameRef(scenarioId)

        override fun loadGame(ref: GameRef): GameState = _state

        override fun saveGame(ref: GameRef, state: GameState) {
            _state = state
            onSave?.invoke(state)
        }
    }
}
