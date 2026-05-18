package exploration.scenario

import exploration.core.command.Command
import exploration.core.command.processCommand
import exploration.core.engine.fireAreaTriggers
import exploration.core.model.*
import exploration.core.state.GameState
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VisibleExitsTest {

    @Test
    fun `visibleExits returns only non-hidden exits`() {
        val forest = AreaId("Forest")
        val cave = AreaId("Cave")
        val tower = AreaId("Tower")

        val world = World(
            areas = mapOf(
                forest to Area(forest, "Forest.", setOf(Exit(cave, Direction.East), Exit(tower, Direction.North))),
                cave to Area(cave, "Cave.", setOf(Exit(forest, Direction.West))),
                tower to Area(tower, "Tower.", setOf())
            ),
            startArea = forest
        )

        // All open and visible - Direction enum order: North, West, South, East
        var state = GameState(world, Player(10, 20, forest))
        assertEquals(listOf(Direction.North, Direction.East), state.visibleExits(forest))

        // Block east exit (Forest -> Cave) - still visible but blocked for movement
        val blockedState = state.copy(exitStates = mapOf(ExitId(forest, cave) to ExitStateData(ExitState.BLOCKED)))
        assertEquals(listOf(Direction.North, Direction.East), blockedState.visibleExits(forest))

        // Hide north exit (Forest -> Tower) - East should still be visible
        val hiddenState = state.copy(exitStates = mapOf(ExitId(forest, tower) to ExitStateData(ExitState.OPEN, hidden = true)))
        assertEquals(listOf(Direction.East), hiddenState.visibleExits(forest))

        // Block east AND hide north - only east visible (north is hidden even though open)
        val bothState = blockedState.copy(exitStates = blockedState.exitStates + (ExitId(forest, tower) to ExitStateData(ExitState.OPEN, hidden = true)))
        assertEquals(listOf(Direction.East), bothState.visibleExits(forest))
    }

    @Test
    fun `isExitBlocked checks state correctly`() {
        val forest = AreaId("Forest")
        val cave = AreaId("Cave")

        val world = World(
            areas = mapOf(
                forest to Area(forest, "Forest.", setOf(Exit(cave, Direction.East))),
                cave to Area(cave, "Cave.", setOf())
            ),
            startArea = forest
        )

        // No state -> not blocked (defaults to open and visible)
        val state = GameState(world, Player(10, 20, forest))
        assertFalse(state.isExitBlocked(forest, cave))

        // Explicitly open -> not blocked
        val openState = state.copy(exitStates = mapOf(ExitId(forest, cave) to ExitStateData(ExitState.OPEN)))
        assertFalse(openState.isExitBlocked(forest, cave))

        // Blocked -> blocked
        val blockedState = state.copy(exitStates = mapOf(ExitId(forest, cave) to ExitStateData(ExitState.BLOCKED)))
        assertTrue(blockedState.isExitBlocked(forest, cave))

        // Hidden but open -> still counts as "blocked" for movement purposes
        val hiddenState = state.copy(exitStates = mapOf(ExitId(forest, cave) to ExitStateData(ExitState.OPEN, hidden = true)))
        assertTrue(hiddenState.isExitBlocked(forest, cave))
    }

    @Test
    fun `trigger effects combine with existing exit states`() {
        val forest = AreaId("Forest")
        val cave = AreaId("Cave")

        // Start with a blocked exit, then unblock it via trigger
        val state = GameState(
            world = World(mapOf(forest to Area(forest, "F.", setOf(Exit(cave, Direction.East))), cave to Area(cave, "C.", setOf())), forest),
            player = Player(10, 20, forest),
            exitStates = mapOf(ExitId(forest, cave) to ExitStateData(ExitState.BLOCKED)),
            triggers = listOf(Trigger("t1", OwnerType.AREA, "Forest", emptyList(), listOf(Effect.SetExitBlocked(forest, cave, blocked = false))))
        )

        val result = fireAreaTriggers(state, forest)
        val data = result.exitStates[ExitId(forest, cave)]!!
        assertEquals(ExitState.OPEN, data.state)
    }

    @Test
    fun `blocked exit prevents movement via command`() {
        val forest = AreaId("Forest")
        val cave = AreaId("Cave")

        val world = World(
            areas = mapOf(
                forest to Area(forest, "Forest.", setOf(Exit(cave, Direction.East))),
                cave to Area(cave, "Cave.", setOf())
            ),
            startArea = forest
        )

        val state = GameState(world, Player(10, 20, forest), exitStates = mapOf(ExitId(forest, cave) to ExitStateData(ExitState.BLOCKED)))

        val result = processCommand(state, Command.Move("Cave"))
        assertEquals(forest, result.player.currentArea)
        assertTrue(result.commandOutput.contains("blocked"))
    }

    @Test
    fun `game engine filters hidden exits from view`() {
        val forest = AreaId("Forest")
        val cave = AreaId("Cave")
        val tower = AreaId("Tower")

        val world = World(
            areas = mapOf(
                forest to Area(forest, "Forest.", setOf(Exit(cave, Direction.East), Exit(tower, Direction.North))),
                cave to Area(cave, "Cave.", setOf()),
                tower to Area(tower, "Tower.", setOf())
            ),
            startArea = forest
        )

        // With hidden Tower exit, only Cave should show as visible (index 0 for East)
        val state = GameState(
            world, Player(10, 20, forest),
            exitStates = mapOf(ExitId(forest, tower) to ExitStateData(hidden = true))
        )

        val visible = state.visibleExits(forest)
        assertTrue(Direction.East in visible)
        assertFalse(Direction.North in visible)
    }
}
