package exploration.adapter.ui.libgdx

import com.badlogic.gdx.Input as GdxInput
import exploration.model.Direction
import exploration.port.ItemView
import exploration.port.InputEvent
import exploration.port.ViewData
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class InputMapperTest {

    private fun viewData(
        areaItems: List<ItemView> = emptyList(),
        carriedItems: List<ItemView> = emptyList(),
        equippedItems: List<ItemView> = emptyList()
    ): ViewData = ViewData(
        messageHistory = listOf("You are in a room."), commandText = "", triggerTexts = emptyList(),
        health = 10, maxHealth = 20, currentAreaName = "Room",
        exploredCount = 1, totalAreas = 1, activatedCount = 0, totalDevices = 1,
        exits = mapOf(
            Direction.North to null,
            Direction.West to null,
            Direction.South to null,
            Direction.East to null
        ), statuses = emptyMap(), statusBounds = emptyMap(),
        areaItems = areaItems, carriedItems = carriedItems, equippedItems = equippedItems,
        storyMessages = listOf("Story message")
    )

    // Movement key mapping

    @Test
    fun `arrow keys map to directions`() {
        val v = viewData()
        assertEquals(InputEvent.MoveDirection(Direction.North), InputMapper.mapKey(GdxInput.Keys.UP, v))
        assertEquals(InputEvent.MoveDirection(Direction.South), InputMapper.mapKey(GdxInput.Keys.DOWN, v))
        assertEquals(InputEvent.MoveDirection(Direction.West), InputMapper.mapKey(GdxInput.Keys.LEFT, v))
        assertEquals(InputEvent.MoveDirection(Direction.East), InputMapper.mapKey(GdxInput.Keys.RIGHT, v))
    }

    @Test
    fun `wasd keys map to directions`() {
        val v = viewData()
        assertEquals(InputEvent.MoveDirection(Direction.North), InputMapper.mapKey(GdxInput.Keys.W, v))
        assertEquals(InputEvent.MoveDirection(Direction.South), InputMapper.mapKey(GdxInput.Keys.S, v))
        assertEquals(InputEvent.MoveDirection(Direction.West), InputMapper.mapKey(GdxInput.Keys.A, v))
        assertEquals(InputEvent.MoveDirection(Direction.East), InputMapper.mapKey(GdxInput.Keys.D, v))
    }

    // Action key mapping

    @Test
    fun `action keys trigger correct events`() {
        val v = viewData()
        assertEquals(InputEvent.Look, InputMapper.mapKey(GdxInput.Keys.L, v))
        assertEquals(InputEvent.Activate, InputMapper.mapKey(GdxInput.Keys.U, v))
        assertEquals(InputEvent.Inventory, InputMapper.mapKey(GdxInput.Keys.I, v))
    }

    @Test
    fun `unknown keys return null`() {
        val v = viewData()
        assertNull(InputMapper.mapKey(GdxInput.Keys.Q, v))
        assertNull(InputMapper.mapKey(999, v))
    }

    // Item actions - single item auto-execute

    @Test
    fun `single area item take executes directly`() {
        val result = InputMapper.handleItemKey('g', viewData(areaItems = listOf(ItemView("key", ""))))
        assertEquals(InputEvent.TakeItem("key"), (result as InputMapper.ItemAction.Event).event)
    }

    @Test
    fun `single carried item drop executes directly`() {
        val result = InputMapper.handleItemKey('p', viewData(carriedItems = listOf(ItemView("sword", ""))))
        assertEquals(InputEvent.DropItem("sword"), (result as InputMapper.ItemAction.Event).event)
    }

    @Test
    fun `single carried item equip executes directly`() {
        val result = InputMapper.handleItemKey('e', viewData(carriedItems = listOf(ItemView("helmet", ""))))
        assertEquals(InputEvent.EquipItem("helmet"), (result as InputMapper.ItemAction.Event).event)
    }

    @Test
    fun `single equipped item unequip executes directly`() {
        val result = InputMapper.handleItemKey('r', viewData(equippedItems = listOf(ItemView("ring", ""))))
        assertEquals(InputEvent.UnequipItem("ring"), (result as InputMapper.ItemAction.Event).event)
    }

    // Item actions - no items shows message

    @Test
    fun `no area items shows nothing to grab message`() {
        val result = InputMapper.handleItemKey('g', viewData())
        assertTrue(result is InputMapper.ItemAction.Message)
        assertEquals("Nothing to grab here.", (result as InputMapper.ItemAction.Message).text)
    }

    @Test
    fun `no carried items shows nothing to drop message`() {
        val result = InputMapper.handleItemKey('p', viewData())
        assertTrue(result is InputMapper.ItemAction.Message)
        assertEquals("Nothing to drop.", (result as InputMapper.ItemAction.Message).text)
    }

    @Test
    fun `no carried items shows nothing to equip message`() {
        val result = InputMapper.handleItemKey('e', viewData())
        assertTrue(result is InputMapper.ItemAction.Message)
        assertEquals("Nothing to equip.", (result as InputMapper.ItemAction.Message).text)
    }

    @Test
    fun `no equipped items shows nothing equipped message`() {
        val result = InputMapper.handleItemKey('r', viewData())
        assertTrue(result is InputMapper.ItemAction.Message)
        assertEquals("Nothing equipped.", (result as InputMapper.ItemAction.Message).text)
    }

    // Item actions - multiple items triggers selection

    @Test
    fun `multiple area items trigger take selection`() {
        val result = InputMapper.handleItemKey('g', viewData(areaItems = listOf(ItemView("x", ""), ItemView("y", ""))))
        assertTrue(result is InputMapper.ItemAction.Selection)
        assertEquals(SelectionTarget.TAKE, (result as InputMapper.ItemAction.Selection).target)
    }

    @Test
    fun `multiple carried items trigger drop selection`() {
        val result = InputMapper.handleItemKey('p', viewData(carriedItems = listOf(ItemView("a", ""), ItemView("b", ""))))
        assertTrue(result is InputMapper.ItemAction.Selection)
        assertEquals(SelectionTarget.DROP, (result as InputMapper.ItemAction.Selection).target)
    }

    @Test
    fun `multiple carried items trigger equip selection`() {
        val result = InputMapper.handleItemKey('e', viewData(carriedItems = listOf(ItemView("a", ""), ItemView("b", ""))))
        assertTrue(result is InputMapper.ItemAction.Selection)
        assertEquals(SelectionTarget.EQUIP, (result as InputMapper.ItemAction.Selection).target)
    }

    @Test
    fun `multiple equipped items trigger unequip selection`() {
        val result = InputMapper.handleItemKey('r', viewData(equippedItems = listOf(ItemView("a", ""), ItemView("b", ""))))
        assertTrue(result is InputMapper.ItemAction.Selection)
        assertEquals(SelectionTarget.UNEQUIP, (result as InputMapper.ItemAction.Selection).target)
    }

    // Case insensitive item keys

    @Test
    fun `item action keys are case insensitive`() {
        val v = viewData(areaItems = listOf(ItemView("key", "")))
        assertEquals(InputEvent.TakeItem("key"), (InputMapper.handleItemKey('G', v) as InputMapper.ItemAction.Event).event)
        assertEquals(InputEvent.TakeItem("key"), (InputMapper.handleItemKey('g', v) as InputMapper.ItemAction.Event).event)
    }

    // Unknown item keys return null

    @Test
    fun `unknown item keys return null`() {
        val v = viewData()
        assertNull(InputMapper.handleItemKey('z', v))
        assertNull(InputMapper.handleItemKey('1', v))
    }
}
