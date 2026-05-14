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
        assertEquals("Room A", result.commandOutput.substringBefore("\n"))
    }

    @Test
    fun `look on already explored area still shows description`() {
        val explored = initialState.copy(exploredAreas = setOf(AreaId("A")))
        val result = processCommand(explored, Command.Look)
        assertTrue(result.exploredAreas.contains(AreaId("A")))
        assertTrue(result.commandOutput.startsWith("Room A"))
    }

    @Test
    fun `look shows device description when not activated`() {
        val result = processCommand(initialState, Command.Look)
        assertTrue(result.commandOutput.contains("healing herb"))
    }

    @Test
    fun `look shows deactivated notice after activation`() {
        val activated = initialState.copy(activatedDevices = setOf(DeviceId("Herb")))
        val result = processCommand(activated, Command.Look)
        assertTrue(result.commandOutput.contains("Herb has been activated"))
    }

    @Test
    fun `move to connected area succeeds`() {
        val result = processCommand(initialState, Command.Move("B"))
        assertEquals(AreaId("B"), result.player.currentArea)
        assertTrue(result.commandOutput.contains("move"))
    }

    @Test
    fun `move to non-existent area fails`() {
        val result = processCommand(initialState, Command.Move("Nowhere"))
        assertEquals(AreaId("A"), result.player.currentArea)
        assertTrue(result.commandOutput.contains("no place called"))
    }

    @Test
    fun `move to unconnected area fails`() {
        val result = processCommand(initialState, Command.Move("C"))
        assertEquals(AreaId("A"), result.player.currentArea)
        assertTrue(result.commandOutput.contains("can't go there"))
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
        assertTrue(result.commandOutput.contains("already been activated"))
    }

    @Test
    fun `activate in room with no device shows message`() {
        val emptyWorld = World(
            areas = mapOf(AreaId("X") to Area(AreaId("X"), "Empty.", setOf())),
            startArea = AreaId("X")
        )
        val state = GameState(world = emptyWorld, player = Player(5, 10, AreaId("X")))
        val result = processCommand(state, Command.Activate)
        assertTrue(result.commandOutput.contains("nothing here"))
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
        assertTrue(result.commandOutput.contains("game is over"))
    }

    @Test
    fun `take item in current area succeeds`() {
        val withItem = initialState.copy(
            items = listOf(Item(ItemId("Key"), "A rusty key.", Location.area(AreaId("A"))))
        )
        val result = processCommand(withItem, Command.TakeItem("Key"))
        assertEquals("You pick up the Key.", result.commandOutput)
        assertEquals(1, result.items.size)
        assertEquals(ItemLocationType.CARRIED, result.items[0].location.type)
    }

    @Test
    fun `take item not in current area fails`() {
        val withItem = initialState.copy(
            items = listOf(Item(ItemId("Gem"), "A gem.", Location.device(DeviceId("Orb"))))
        )
        val result = processCommand(withItem, Command.TakeItem("Gem"))
        assertTrue(result.commandOutput.contains("isn't here"))
    }

    @Test
    fun `take non-existent item fails`() {
        val result = processCommand(initialState, Command.TakeItem("Key"))
        assertTrue(result.commandOutput.contains("no item called"))
    }

    @Test
    fun `drop carried item moves to area`() {
        val withCarried = initialState.copy(
            items = listOf(Item(ItemId("Potion"), "A health potion.", Location(ItemLocationType.CARRIED)))
        )
        val result = processCommand(withCarried, Command.DropItem("Potion"))
        assertEquals("You drop the Potion.", result.commandOutput)
        assertEquals(ItemLocationType.AREA, result.items[0].location.type)
    }

    @Test
    fun `drop equipped item auto-unequips to area`() {
        val withEquipped = initialState.copy(
            items = listOf(Item(ItemId("Shield"), "A small shield.", Location(ItemLocationType.EQUIPPED)))
        )
        val result = processCommand(withEquipped, Command.DropItem("Shield"))
        assertEquals("You drop the Shield (auto-unequipped).", result.commandOutput)
        assertEquals(ItemLocationType.AREA, result.items[0].location.type)
    }

    @Test
    fun `drop locked equipped item is rejected`() {
        val withEquipped = initialState.copy(
            items = listOf(Item(ItemId("CursedRing"), "A cursed ring.", Location(ItemLocationType.EQUIPPED), locked = true))
        )
        val result = processCommand(withEquipped, Command.DropItem("CursedRing"))
        assertTrue(result.commandOutput.contains("it's locked"))
    }

    @Test
    fun `drop locked carried item is rejected`() {
        val withCarried = initialState.copy(
            items = listOf(Item(ItemId("Box"), "A sealed box.", Location(ItemLocationType.CARRIED), locked = true))
        )
        val result = processCommand(withCarried, Command.DropItem("Box"))
        assertTrue(result.commandOutput.contains("it's locked"))
    }

    @Test
    fun `drop item not in inventory fails`() {
        val withAreaItem = initialState.copy(
            items = listOf(Item(ItemId("Dagger"), "A dagger.", Location.area(AreaId("B"))))
        )
        val result = processCommand(withAreaItem, Command.DropItem("Dagger"))
        assertTrue(result.commandOutput.contains("isn't in your inventory"))
    }

    @Test
    fun `equip carried item succeeds`() {
        val withCarried = initialState.copy(
            items = listOf(Item(ItemId("Sword"), "An iron sword.", Location(ItemLocationType.CARRIED)))
        )
        val result = processCommand(withCarried, Command.EquipItem("Sword"))
        assertEquals("You equip the Sword.", result.commandOutput)
        assertEquals(ItemLocationType.EQUIPPED, result.items[0].location.type)
    }

    @Test
    fun `equip non-carried item fails`() {
        val withAreaItem = initialState.copy(
            items = listOf(Item(ItemId("Bow"), "A bow.", Location(ItemLocationType.AREA)))
        )
        val result = processCommand(withAreaItem, Command.EquipItem("Bow"))
        assertTrue(result.commandOutput.contains("isn't in your inventory"))
    }

    @Test
    fun `equip locked item is rejected`() {
        val withCarried = initialState.copy(
            items = listOf(Item(ItemId("Amulet"), "An ancient amulet.", Location(ItemLocationType.CARRIED), locked = true))
        )
        val result = processCommand(withCarried, Command.EquipItem("Amulet"))
        assertTrue(result.commandOutput.contains("it's locked"))
    }

    @Test
    fun `unequip equipped item succeeds`() {
        val withEquipped = initialState.copy(
            items = listOf(Item(ItemId("Helmet"), "A steel helmet.", Location(ItemLocationType.EQUIPPED)))
        )
        val result = processCommand(withEquipped, Command.UnequipItem("Helmet"))
        assertEquals("You unequip the Helmet.", result.commandOutput)
        assertEquals(ItemLocationType.CARRIED, result.items[0].location.type)
    }

    @Test
    fun `unequip non-equipped item fails`() {
        val withCarried = initialState.copy(
            items = listOf(Item(ItemId("Gauntlets"), "Iron gauntlets.", Location(ItemLocationType.CARRIED)))
        )
        val result = processCommand(withCarried, Command.UnequipItem("Gauntlets"))
        assertTrue(result.commandOutput.contains("isn't equipped"))
    }

    @Test
    fun `unequip locked item is rejected`() {
        val withEquipped = initialState.copy(
            items = listOf(Item(ItemId("Robe"), "A ceremonial robe.", Location(ItemLocationType.EQUIPPED), locked = true))
        )
        val result = processCommand(withEquipped, Command.UnequipItem("Robe"))
        assertTrue(result.commandOutput.contains("it's locked"))
    }

    @Test
    fun `game over state rejects all item commands`() {
        val gameOver = initialState.copy(isOver = true, win = false)
        val withItems = gameOver.copy(
            items = listOf(Item(ItemId("Key"), "A key.", Location(ItemLocationType.CARRIED)))
        )
        assertTrue(processCommand(withItems, Command.TakeItem("Key")).commandOutput.contains("game is over"))
        assertTrue(processCommand(withItems, Command.DropItem("Key")).commandOutput.contains("game is over"))
        assertTrue(processCommand(withItems, Command.EquipItem("Key")).commandOutput.contains("game is over"))
        assertTrue(processCommand(withItems, Command.UnequipItem("Key")).commandOutput.contains("game is over"))
    }

    @Test
    fun `inventory shows carried and equipped items`() {
        val withItems = initialState.copy(
            items = listOf(
                Item(ItemId("Potion"), "Restores health.", Location(ItemLocationType.CARRIED)),
                Item(ItemId("Sword"), "A sharp blade.", Location(ItemLocationType.EQUIPPED))
            )
        )
        val result = processCommand(withItems, Command.Inventory)
        assertTrue(result.commandOutput.contains("Inventory:"))
        assertTrue(result.commandOutput.contains("Equipped:"))
        assertTrue(result.commandOutput.contains("Sword"))
        assertTrue(result.commandOutput.contains("Carried:"))
        assertTrue(result.commandOutput.contains("Potion"))
    }

    @Test
    fun `inventory shows empty when no items`() {
        val result = processCommand(initialState, Command.Inventory)
        assertTrue(result.commandOutput.contains("Inventory:"))
        assertTrue(result.commandOutput.contains("(empty)"))
    }
}
