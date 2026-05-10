package exploration.command

import exploration.model.*
import exploration.state.GameState
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.*

class CommandProcessorTest {

    private lateinit var world: World
    private lateinit var initialState: GameState

    @BeforeEach
    fun setup() {
        val aId = AreaId("A")
        val bId = AreaId("B")
        val cId = AreaId("C")

        val deviceHeal = Device(DeviceId("Herb"), "A healing herb.", "You consume the herb. (+5)", 5)
        val deviceHurt = Device(DeviceId("Trap"), "A suspicious trap.", "The trap springs! (-4)", -4)
        val deviceNeutral = Device(DeviceId("Sign"), "A wooden sign.", "It reads: welcome.", 0)

        world = World(
            areas = mapOf(
                aId to Area(aId, "Room A", setOf(bId), deviceHeal),
                bId to Area(bId, "Room B", setOf(aId, cId), deviceNeutral),
                cId to Area(cId, "Room C", setOf(bId), deviceHurt)
            ),
            startArea = aId
        )

        initialState = GameState(
            world = world,
            player = Player(10, 20, aId)
        )
    }

    @Test
    fun `look marks area as explored`() {
        val result = processCommand(initialState, Command.Look)
        assertTrue(AreaId("A") in result.exploredAreas)
        assertEquals("Room A", result.output.substringBefore("\n"))
    }

    @Test
    fun `look on already explored area still shows description`() {
        val explored = initialState.copy(exploredAreas = setOf(AreaId("A")))
        val result = processCommand(explored, Command.Look)
        assertTrue(result.exploredAreas.contains(AreaId("A")))
        assertTrue(result.output.startsWith("Room A"))
    }

    @Test
    fun `look shows device description when not activated`() {
        val result = processCommand(initialState, Command.Look)
        assertTrue(result.output.contains("healing herb"))
    }

    @Test
    fun `look shows deactivated notice after activation`() {
        val activated = initialState.copy(activatedDevices = setOf(DeviceId("Herb")))
        val result = processCommand(activated, Command.Look)
        assertTrue(result.output.contains("deactivated Herb"))
    }

    @Test
    fun `move to connected area succeeds`() {
        val result = processCommand(initialState, Command.Move("B"))
        assertEquals(AreaId("B"), result.player.currentArea)
        assertTrue(result.output.contains("move"))
    }

    @Test
    fun `move to non-existent area fails`() {
        val result = processCommand(initialState, Command.Move("Nowhere"))
        assertEquals(AreaId("A"), result.player.currentArea)
        assertTrue(result.output.contains("no place called"))
    }

    @Test
    fun `move to unconnected area fails`() {
        val result = processCommand(initialState, Command.Move("C"))
        assertEquals(AreaId("A"), result.player.currentArea)
        assertTrue(result.output.contains("can't go there"))
    }

    @Test
    fun `activate in room with device applies health effect`() {
        val result = processCommand(initialState, Command.Activate)
        assertEquals(15, result.player.health)
        assertTrue(DeviceId("Herb") in result.activatedDevices)
    }

    @Test
    fun `activate already activated device is rejected`() {
        val activated = initialState.copy(activatedDevices = setOf(DeviceId("Herb")))
        val result = processCommand(activated, Command.Activate)
        assertEquals(10, result.player.health)
        assertTrue(result.output.contains("already been activated"))
    }

    @Test
    fun `activate in room with no device shows message`() {
        val emptyWorld = World(
            areas = mapOf(AreaId("X") to Area(AreaId("X"), "Empty.", setOf())),
            startArea = AreaId("X")
        )
        val state = GameState(world = emptyWorld, player = Player(5, 10, AreaId("X")))
        val result = processCommand(state, Command.Activate)
        assertTrue(result.output.contains("nothing here"))
    }

    @Test
    fun `activate with zero health effect changes nothing`() {
        val movedToB = processCommand(initialState, Command.Move("B"))
        val result = processCommand(movedToB, Command.Activate)
        assertEquals(10, result.player.health)
    }

    @Test
    fun `health drops to zero triggers game over lose`() {
        val lowHealth = initialState.copy(player = Player(3, 20, AreaId("C")))
        val result = processCommand(lowHealth, Command.Activate)
        assertTrue(result.isOver)
        assertEquals(false, result.win)
    }

    @Test
    fun `all explored and activated triggers win`() {
        val allDone = initialState.copy(
            exploredAreas = world.areas.keys,
            activatedDevices = setOf(DeviceId("Herb"), DeviceId("Sign"), DeviceId("Trap"))
        )
        val result = processCommand(allDone, Command.Look)
        assertTrue(result.isOver)
        assertEquals(true, result.win)
    }

    @Test
    fun `command on over state returns message`() {
        val gameOver = initialState.copy(isOver = true, win = false)
        val result = processCommand(gameOver, Command.Look)
        assertTrue(result.output.contains("game is over"))
    }
}
