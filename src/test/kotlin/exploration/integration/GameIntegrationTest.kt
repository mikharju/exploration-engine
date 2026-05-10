package exploration.integration

import exploration.command.Command
import exploration.command.processCommand
import exploration.model.*
import exploration.state.GameState
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GameIntegrationTest {

    private fun buildWorld(): World = World(
        areas = mapOf(
            AreaId("Forest") to Area(AreaId("Forest"), "A dense forest.", setOf(AreaId("Cave")), Device(DeviceId("Crystal"), "glowing crystal", "+10 health", 10)),
            AreaId("Cave") to Area(AreaId("Cave"), "A dim cavern.", setOf(AreaId("Forest"), AreaId("Ruins"), AreaId("Tower")), Device(DeviceId("Glyph Wall"), "glyphs", "message", 0)),
            AreaId("Ruins") to Area(AreaId("Ruins"), "Crumbled ruins.", setOf(AreaId("Cave")), Device(DeviceId("Machine"), "machine", "-5 health", -5)),
            AreaId("Tower") to Area(AreaId("Tower"), "A tall tower.", setOf(AreaId("Cave")), Device(DeviceId("Orb"), "orb", "+3 health", 3))
        ),
        startArea = AreaId("Forest")
    )

    @Test
    fun `full winning playthrough`() {
        val world = buildWorld()
        var state = GameState(world, Player(10, 20, world.startArea))

        // Forest: explore + heal
        state = processCommand(state, Command.Look)
        assertTrue(AreaId("Forest") in state.exploredAreas)
        state = processCommand(state, Command.Activate)
        assertEquals(20, state.player.health)

        // Cave: explore + activate neutral device
        state = processCommand(state, Command.Move("Cave"))
        state = processCommand(state, Command.Look)
        state = processCommand(state, Command.Activate)

        // Tower: explore + heal (clamped to max)
        state = processCommand(state, Command.Move("Tower"))
        state = processCommand(state, Command.Look)
        assertTrue(AreaId("Tower") in state.exploredAreas)
        state = processCommand(state, Command.Activate)

        // Ruins: explore + damage
        state = processCommand(state, Command.Move("Cave"))
        state = processCommand(state, Command.Move("Ruins"))
        state = processCommand(state, Command.Look)
        assertTrue(AreaId("Ruins") in state.exploredAreas)
        state = processCommand(state, Command.Activate)

        // Win: all areas explored + all devices activated
        assertTrue(state.isOver)
        assertEquals(true, state.win)
    }

    @Test
    fun `losing playthrough - low health damage`() {
        val world = buildWorld()
        var state = GameState(world, Player(3, 20, world.startArea))

        state = processCommand(state, Command.Move("Cave"))
        state = processCommand(state, Command.Move("Ruins"))
        state = processCommand(state, Command.Look)
        state = processCommand(state, Command.Activate) // -5 health -> 0

        assertTrue(state.isOver)
        assertEquals(false, state.win)
    }

    @Test
    fun `partial exploration does not win`() {
        val world = buildWorld()
        var state = GameState(world, Player(10, 20, world.startArea))

        state = processCommand(state, Command.Look) // Only Forest explored
        assertFalse(state.isOver)

        state = processCommand(state, Command.Activate)
        state = processCommand(state, Command.Move("Cave"))
        assertFalse(state.isOver)
    }
}
