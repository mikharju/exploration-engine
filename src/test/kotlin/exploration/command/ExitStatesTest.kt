package exploration.command

import exploration.model.*
import exploration.state.GameState
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.*

class ExitStatesTest {

    private lateinit var forest: AreaId
    private lateinit var cave: AreaId
    private lateinit var tower: AreaId
    private lateinit var world: World
    private lateinit var initialState: GameState

    @BeforeEach
    fun setup() {
        forest = AreaId("Forest")
        cave = AreaId("Cave")
        tower = AreaId("Tower")

        val exitF2C = Exit(cave, Direction.East)
        val exitC2F = Exit(forest, Direction.West)
        val exitC2T = Exit(tower, Direction.North)
        val exitT2C = Exit(cave, Direction.South)

        world = World(
            areas = mapOf(
                forest to Area(forest, "Forest.", setOf(exitF2C)),
                cave to Area(cave, "Cave.", setOf(exitC2F, exitC2T)),
                tower to Area(tower, "Tower.", setOf(exitT2C))
            ),
            startArea = forest
        )

        initialState = GameState(
            world = world,
            player = Player(10, 20, forest)
        )
    }

    @Test
    fun `blocked exit prevents movement`() {
        val withBlockedExit = initialState.copy(exitStates = mapOf(ExitId(forest, cave) to ExitStateData(ExitState.BLOCKED)))
        val result = processCommand(withBlockedExit, Command.Move("Cave"))
        assertEquals(forest, result.player.currentArea)
        assertTrue(result.commandOutput.contains("blocked"))
    }

    @Test
    fun `open exit allows movement`() {
        val withOpenExit = initialState.copy(exitStates = mapOf(ExitId(forest, cave) to ExitStateData(ExitState.OPEN)))
        val result = processCommand(withOpenExit, Command.Move("Cave"))
        assertEquals(cave, result.player.currentArea)
    }

    @Test
    fun `uninitialized exit is open and passable`() {
        val result = processCommand(initialState, Command.Move("Cave"))
        assertEquals(cave, result.player.currentArea)
    }

    @Test
    fun `blocked exit blocks in both directions independently`() {
        // Only block Forest -> Cave, not Cave -> Forest
        val withBlockedExit = initialState.copy(exitStates = mapOf(ExitId(forest, cave) to ExitStateData(ExitState.BLOCKED)))

        // Can't go Forest -> Cave
        var result = processCommand(withBlockedExit, Command.Move("Cave"))
        assertEquals(forest, result.player.currentArea)
        assertTrue(result.commandOutput.contains("blocked"))

        // Move to tower first (no exit state set for that direction), then back to cave, then try Forest -> Cave from cave side
        // Actually, let's test Cave -> Forest is fine since we didn't block it
        val movedToCave = processCommand(initialState, Command.Move("Cave"))
        result = processCommand(movedToCave, Command.Move("Forest"))
        assertEquals(forest, result.player.currentArea)
    }

    @Test
    fun `multiple exits blocked independently`() {
        val states = mapOf(
            ExitId(forest, cave) to ExitStateData(ExitState.BLOCKED),
            ExitId(cave, forest) to ExitStateData(ExitState.OPEN),
            ExitId(cave, tower) to ExitStateData(ExitState.BLOCKED)
        )
        val state = initialState.copy(exitStates = states)

        // Can't go Forest -> Cave (blocked)
        var result = processCommand(state, Command.Move("Cave"))
        assertEquals(forest, result.player.currentArea)
        assertTrue(result.commandOutput.contains("blocked"))

        // Create a different starting position in cave to test one-way
        val stateFromCave = GameState(
            world = world,
            player = Player(10, 20, cave),
            exitStates = states
        )

        // Can go Cave -> Forest (open)
        result = processCommand(stateFromCave, Command.Move("Forest"))
        assertEquals(forest, result.player.currentArea)

        // Can't go Cave -> Tower (blocked)
        result = processCommand(stateFromCave, Command.Move("Tower"))
        assertEquals(cave, result.player.currentArea)
        assertTrue(result.commandOutput.contains("blocked"))
    }

    @Test
    fun `exit state persists across commands`() {
        var state = initialState.copy(exitStates = mapOf(ExitId(forest, cave) to ExitStateData(ExitState.BLOCKED)))

        val r1 = processCommand(state, Command.Move("Cave"))
        assertEquals(forest, r1.player.currentArea)

        // Move still blocked after another command
        val lookResult = processCommand(r1, Command.Look)
        val r2 = processCommand(lookResult, Command.Move("Cave"))
        assertEquals(forest, r2.player.currentArea)
    }
}
