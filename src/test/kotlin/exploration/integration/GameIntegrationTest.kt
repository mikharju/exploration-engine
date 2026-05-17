package exploration.integration

import exploration.command.Command
import exploration.command.processCommand
import exploration.model.*
import exploration.state.GameState
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GameIntegrationTest {

    private fun buildWorld(): World = World(
        areas = mapOf(
            AreaId("Forest") to Area(AreaId("Forest"), "A dense forest.", setOf(Exit(AreaId("Cave"), Direction.East)), Device(DeviceId("Crystal"), "glowing crystal", "+10 health", 10)),
            AreaId("Cave") to Area(AreaId("Cave"), "A dim cavern.", setOf(Exit(AreaId("Forest"), Direction.West), Exit(AreaId("Ruins"), Direction.South), Exit(AreaId("Tower"), Direction.North)), Device(DeviceId("Glyph Wall"), "glyphs", "message", 0)),
            AreaId("Ruins") to Area(AreaId("Ruins"), "Crumbled ruins.", setOf(Exit(AreaId("Cave"), Direction.North), Exit(AreaId("Sanctuary"), Direction.East)), Device(DeviceId("Machine"), "machine", "-5 health", -5)),
            AreaId("Tower") to Area(AreaId("Tower"), "A tall tower.", setOf(Exit(AreaId("Cave"), Direction.South)), Device(DeviceId("Orb"), "orb", "+3 health", 3)),
            AreaId("Sanctuary") to Area(AreaId("Sanctuary"), "A pristine chamber.", setOf(Exit(AreaId("Ruins"), Direction.West)))
        ),
        startArea = AreaId("Forest")
    )

    @Test
    fun `full winning playthrough with endGame trigger`() {
        val world = buildWorld()
        var state = GameState(world, Player(10, 20, world.startArea), triggers = listOf(Trigger("sanctum", OwnerType.AREA, "Sanctuary", emptyList(), listOf(Effect.EndGame("You found the sanctum and its secrets.")))))

        // Forest: explore + heal
        state = processCommand(state, Command.Look)
        assertTrue(AreaId("Forest") in state.exploredAreas)
        state = processCommand(state, Command.Activate)
        assertEquals(20, state.player.health)

        // Cave: explore + activate neutral device
        state = processCommand(state, Command.Move("Cave"))
        state = processCommand(state, Command.Look)
        state = processCommand(state, Command.Activate)

        // Tower: explore + heal (clamped to max), also opens the Sanctuary door
        state = processCommand(state, Command.Move("Tower"))
        state = processCommand(state, Command.Look)
        assertTrue(AreaId("Tower") in state.exploredAreas)
        state = processCommand(state, Command.Activate)

        // Ruins: explore + damage, also reveals the Sanctuary passage
        state = processCommand(state, Command.Move("Cave"))
        state = processCommand(state, Command.Move("Ruins"))
        state = processCommand(state, Command.Look)
        assertTrue(AreaId("Ruins") in state.exploredAreas)
        state = processCommand(state, Command.Activate)

        // Move to Sanctuary (now visible and open) -> triggers endGame
        state = processCommand(state, Command.Move("Sanctuary"))
        state = processCommand(state, Command.Look)
        assertTrue(AreaId("Sanctuary") in state.exploredAreas, "Player should be in Sanctuary, explored: ${state.exploredAreas}, playerArea: ${state.player.currentArea}")
        assertNotNull(state.endGameMessage)
    }

    @Test
    fun `health reaches zero does not end the game`() {
        val world = buildWorld()
        var state = GameState(world, Player(3, 20, world.startArea))

        state = processCommand(state, Command.Move("Cave"))
        state = processCommand(state, Command.Move("Ruins"))
        state = processCommand(state, Command.Look)
        state = processCommand(state, Command.Activate) // -5 health -> 0

        assertTrue(state.player.health <= 0)
        assertNull(state.endGameMessage)
    }

    @Test
    fun `partial exploration does not end the game`() {
        val world = buildWorld()
        var state = GameState(world, Player(10, 20, world.startArea))

        state = processCommand(state, Command.Look) // Only Forest explored
        assertTrue(AreaId("Forest") in state.exploredAreas)
        assertNull(state.endGameMessage)

        state = processCommand(state, Command.Activate)
        state = processCommand(state, Command.Move("Cave"))
        assertNull(state.endGameMessage)
    }
}
