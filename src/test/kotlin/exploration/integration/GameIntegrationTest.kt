package exploration.integration

import exploration.command.Command
import exploration.command.processCommand
import exploration.model.*
import exploration.state.GameState
import org.junit.jupiter.api.Test
import kotlin.test.*

class GameIntegrationTest {

    private fun buildDefaultWorld(): World {
        val forestId = AreaId("Forest")
        val caveId = AreaId("Cave")
        val ruinsId = AreaId("Ruins")
        val towerId = AreaId("Tower")

        val crystal = Device(
            id = DeviceId("Crystal"),
            lookDescription = "A glowing crystal pulses with soft blue light.",
            activateDescription = "The crystal flares brightly, warmth flooding through you. (+10 health)",
            healthEffect = 10
        )

        val glyphWall = Device(
            id = DeviceId("Glyph Wall"),
            lookDescription = "Ancient glyphs cover the cave wall, shimmering faintly.",
            activateDescription = "The glyphs rearrange into a clear message: 'All paths lead to understanding.'",
            healthEffect = 0
        )

        val ancientMachine = Device(
            id = DeviceId("Ancient Machine"),
            lookDescription = "A rusted machine hums with unstable energy.",
            activateDescription = "The machine whirs violently, discharging a burst of raw power. (-5 health)",
            healthEffect = -5
        )

        val orb = Device(
            id = DeviceId("Orb"),
            lookDescription = "A smooth obsidian orb sits on a stone pedestal.",
            activateDescription = "The orb cracks open, releasing warm light that strengthens you. (+3 health)",
            healthEffect = 3
        )

        return World(
            areas = mapOf(
                forestId to Area(forestId, "A dense forest with tall pines and dappled sunlight.", setOf(caveId), crystal),
                caveId to Area(caveId, "A dim cavern with stalactites and a cool breeze.", setOf(forestId, ruinsId, towerId), glyphWall),
                ruinsId to Area(ruinsId, "Crumbled stone ruins overgrown with vines.", setOf(caveId), ancientMachine),
                towerId to Area(towerId, "A tall stone tower interior with a spiral staircase.", setOf(caveId), orb)
            ),
            startArea = forestId
        )
    }

    @Test
    fun `full winning playthrough with default world`() {
        val world = buildDefaultWorld()
        var state = GameState(
            world = world,
            player = Player(10, 20, world.startArea)
        )

        // Start in Forest
        state = processCommand(state, Command.Look)
        assertTrue(AreaId("Forest") in state.exploredAreas)
        assertEquals(10, state.player.health)

        // Activate Crystal (+10 health)
        state = processCommand(state, Command.Activate)
        assertEquals(20, state.player.health)

        // Move to Cave
        state = processCommand(state, Command.Move("Cave"))
        assertEquals(AreaId("Cave"), state.player.currentArea)

        // Look and activate Glyph Wall (no health effect)
        state = processCommand(state, Command.Look)
        assertTrue(state.output.contains("glyphs") || state.output.contains("Glyph"))
        state = processCommand(state, Command.Activate)
        assertEquals(20, state.player.health)

        // Move to Tower
        state = processCommand(state, Command.Move("Tower"))
        assertEquals(AreaId("Tower"), state.player.currentArea)

        // Look and activate Orb (+3 health, clamped to max 20)
        state = processCommand(state, Command.Look)
        assertTrue(AreaId("Tower") in state.exploredAreas)
        state = processCommand(state, Command.Activate)
        assertEquals(20, state.player.health)

        // Move back to Cave then to Ruins
        state = processCommand(state, Command.Move("Cave"))
        state = processCommand(state, Command.Move("Ruins"))
        assertEquals(AreaId("Ruins"), state.player.currentArea)

        // Look and activate Ancient Machine (-5 health)
        state = processCommand(state, Command.Look)
        assertTrue(AreaId("Ruins") in state.exploredAreas)
        state = processCommand(state, Command.Activate)

        // Verify win
        assertTrue(state.isOver, "Game should be over after exploring all areas and activating all devices")
        assertEquals(true, state.win)
        assertEquals(15, state.player.health)
    }

    @Test
    fun `losing playthrough - start at 3 health go straight to ruins`() {
        val world = buildDefaultWorld()
        var state = GameState(
            world = world,
            player = Player(3, 20, world.startArea)
        )

        // Move Forest -> Cave -> Ruins (no heals picked up)
        state = processCommand(state, Command.Move("Cave"))
        state = processCommand(state, Command.Move("Ruins"))

        // Look to explore ruins
        state = processCommand(state, Command.Look)

        // Activate Ancient Machine (-5 health, drops from 3 to 0)
        state = processCommand(state, Command.Activate)

        assertTrue(state.isOver, "Game should be over after health reaches 0")
        assertEquals(false, state.win)
    }

    @Test
    fun `partial exploration does not trigger win`() {
        val world = buildDefaultWorld()
        var state = GameState(
            world = world,
            player = Player(10, 20, world.startArea)
        )

        // Only look at Forest (1 of 4 explored)
        state = processCommand(state, Command.Look)
        assertFalse(state.isOver, "Game should not be over with only 1 area explored")

        // Activate Crystal and move to Cave but don't explore further
        state = processCommand(state, Command.Activate)
        state = processCommand(state, Command.Move("Cave"))
        assertFalse(state.isOver, "Game should not be over yet")
    }

    @Test
    fun `cannot activate device in other room`() {
        val world = buildDefaultWorld()
        var state = GameState(
            world = world,
            player = Player(10, 20, world.startArea)
        )

        // Try to activate while in Forest (has Crystal) — succeeds
        state = processCommand(state, Command.Activate)
        assertEquals(20, state.player.health)

        // Move to Cave — Glyph Wall is here, not Crystal
        state = processCommand(state, Command.Move("Cave"))
        state = processCommand(state, Command.Look)

        // Activate in Cave activates Glyph Wall (not Crystal again)
        state = processCommand(state, Command.Activate)
        assertEquals(20, state.player.health)
        assertTrue(state.output.contains("glyph") || state.output.contains("Glyph") || state.output.contains("welcome"))
    }

    @Test
    fun `health recovery sequence - low health find heal then survive danger`() {
        val world = buildDefaultWorld()
        var state = GameState(
            world = world,
            player = Player(3, 20, world.startArea)
        )

        // Start at 3 HP in Forest. Activate Crystal (+10 -> clamped to max... wait, 3+10=13 <= 20)
        state = processCommand(state, Command.Activate)
        assertEquals(13, state.player.health)

        // Now go to Ruins and activate Machine (-5)
        state = processCommand(state, Command.Move("Cave"))
        state = processCommand(state, Command.Move("Ruins"))
        state = processCommand(state, Command.Look)
        state = processCommand(state, Command.Activate)

        assertEquals(8, state.player.health)
        assertFalse(state.isOver)
    }

    @Test
    fun `move command case insensitive parsing`() {
        val world = buildDefaultWorld()
        var state = GameState(
            world = world,
            player = Player(10, 20, world.startArea)
        )

        // Parse and execute lowercase move
        state = processCommand(state, Command.Move("cave"))
        assertEquals(AreaId("Cave"), state.player.currentArea)
    }
}
