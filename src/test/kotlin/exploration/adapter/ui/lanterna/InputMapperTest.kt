package exploration.adapter.ui.lanterna

import com.googlecode.lanterna.input.KeyStroke
import com.googlecode.lanterna.input.KeyType
import exploration.port.InputEvent
import exploration.port.ItemView
import exploration.port.ViewData
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class InputMapperTest {

    private lateinit var mapper: LanternaKeyMapper

    @BeforeEach fun setup() = Unit.apply { mapper = LanternaKeyMapper() }

    // Movement — arrows and WASD map to directions, case insensitive

    @Test
    fun `movement keys map to directions`() {
        val v = viewData()
        assertEquals(InputEvent.Direction.North, moveDir(keyOf(KeyType.ArrowUp), v))
        assertEquals(InputEvent.Direction.South, moveDir(keyOf(KeyType.ArrowDown), v))
        assertEquals(InputEvent.Direction.West, moveDir(keyOf(KeyType.ArrowLeft), v))
        assertEquals(InputEvent.Direction.East, moveDir(keyOf(KeyType.ArrowRight), v))
        assertEquals(InputEvent.Direction.North, moveDir(keyOf('w'), v))
        assertEquals(InputEvent.Direction.South, moveDir(keyOf('s'), v))
        assertEquals(InputEvent.Direction.West, moveDir(keyOf('a'), v))
        assertEquals(InputEvent.Direction.East, moveDir(keyOf('d'), v))
        // Uppercase also works
        assertEquals(InputEvent.Direction.North, moveDir(keyOf('W'), v))
    }

    private fun moveDir(key: KeyStroke, v: ViewData) = (mapper.mapKey(key, v, null, Overlay.None).action as KeyAction.Event).event.let {
        require(it is InputEvent.MoveDirection) { it }
        it.direction
    }

    // Action keys

    @Test
    fun `action keys trigger correct events`() {
        val v = viewData()
        assertEquals(InputEvent.Look, event(keyOf('l'), v))
        assertEquals(InputEvent.Activate, event(keyOf('u'), v))
        assertEquals(InputEvent.Inventory, event(keyOf('i'), v))
    }

    @Test
    fun `q and escape quit`() {
        val v = viewData()
        assertEquals(KeyAction.Quit, mapper.mapKey(keyOf('q'), v, null, Overlay.None).action)
        assertEquals(KeyAction.Quit, mapper.mapKey(keyOf(KeyType.Escape), v, null, Overlay.None).action)
    }

    @Test
    fun `unknown keys show help and h returns NoOp`() {
        val v = viewData()
        assertEquals(KeyAction.Help, mapper.mapKey(keyOf('z'), v, null, Overlay.None).action)
        assertEquals(KeyAction.Help, mapper.mapKey(keyOf(KeyType.Unknown), v, null, Overlay.None).action)
        assertEquals(KeyAction.NoOp, mapper.mapKey(keyOf('h'), v, null, Overlay.None).action)
    }

    // Take/drop/equip/unequip — single item auto-execute, multiple triggers selection

    @Test
    fun `single item actions execute directly`() {
        val g = mapper.mapKey(keyOf('g'), viewData(areaItems = listOf(ItemView("key", ""))), null, Overlay.None)
        assertEquals(KeyAction.Event(InputEvent.TakeItem("key")), g.action)

        val p = mapper.mapKey(keyOf('p'), viewData(carriedItems = listOf(ItemView("sword", ""))), null, Overlay.None)
        assertEquals(KeyAction.Event(InputEvent.DropItem("sword")), p.action)

        val e = mapper.mapKey(keyOf('e'), viewData(carriedItems = listOf(ItemView("helmet", ""))), null, Overlay.None)
        assertEquals(KeyAction.Event(InputEvent.EquipItem("helmet")), e.action)

        val r = mapper.mapKey(keyOf('r'), viewData(equippedItems = listOf(ItemView("ring", ""))), null, Overlay.None)
        assertEquals(KeyAction.Event(InputEvent.UnequipItem("ring")), r.action)
    }

    @Test
    fun `no items shows help and multiple triggers selection`() {
        // No area items → help
        val gEmpty = mapper.mapKey(keyOf('g'), viewData(), null, Overlay.None)
        assertEquals(KeyAction.Help, gEmpty.action)

        // Selection target is set correctly for each verb
        val gTake = mapper.mapKey(keyOf('g'), viewData(areaItems = listOf(ItemView("x", ""), ItemView("y", ""))), null, Overlay.None)
        assertEquals(SelectionTarget.TAKE, gTake.selectionState!!.target)

        val pDrop = mapper.mapKey(keyOf('p'), viewData(carriedItems = listOf(ItemView("a", ""), ItemView("b", ""))), null, Overlay.None)
        assertEquals(KeyAction.WaitSelection, pDrop.action)
        assertNotNull(pDrop.selectionState)
        assertEquals(SelectionTarget.DROP, pDrop.selectionState.target)

        val eEquip = mapper.mapKey(keyOf('e'), viewData(carriedItems = listOf(ItemView("a", ""), ItemView("b", ""))), null, Overlay.None)
        assertEquals(SelectionTarget.EQUIP, eEquip.selectionState!!.target)

        val rUnequip = mapper.mapKey(keyOf('r'), viewData(equippedItems = listOf(ItemView("a", ""), ItemView("b", ""))), null, Overlay.None)
        assertEquals(SelectionTarget.UNEQUIP, rUnequip.selectionState!!.target)
    }

    @Test
    fun `active selection overrides g key`() {
        val sel = SelectionState(SelectionTarget.TAKE, listOf(ItemView("a", "")))
        val result = mapper.mapKey(keyOf('g'), viewData(), sel, Overlay.None)
        assertEquals(KeyAction.WaitSelection, result.action)
    }

    // Story viewer

    @Test
    fun `j key opens story viewer or shows help`() {
        val withMsgs = mapper.mapKey(keyOf('j'), viewData(storyMessages = listOf("A", "B")), null, Overlay.None)
        assertEquals(KeyAction.NoOp, withMsgs.action)
        assertTrue(withMsgs.overlay is Overlay.MessageViewer)

        // Filters blank messages
        val filtered = mapper.mapKey(keyOf('j'), viewData(storyMessages = listOf("First", "", "Last")), null, Overlay.None)
        assertTrue(filtered.overlay is Overlay.MessageViewer)
        assertEquals(2, filtered.overlay.messages.size)

        // Empty list → help
        val empty = mapper.mapKey(keyOf('j'), viewData(storyMessages = emptyList()), null, Overlay.None)
        assertEquals(KeyAction.Help, empty.action)
    }

    // Selection digit handling — 1-indexed, out-of-range/0/non-digit → help or keep waiting

    @Test
    fun `selection digits pick items by target type`() {
        // TAKE
        val take = select(SelectionTarget.TAKE, listOf(ItemView("sword", "")), '1')
        assertEquals(KeyAction.Event(InputEvent.TakeItem("sword")), take.action)
        assertNull(take.selectionState)

        // DROP — second item
        val drop = select(SelectionTarget.DROP, listOf(ItemView("a", ""), ItemView("b", "")), '2')
        assertEquals(KeyAction.Event(InputEvent.DropItem("b")), drop.action)

        // UNEQUIP
        val unequip = select(SelectionTarget.UNEQUIP, listOf(ItemView("ring", "")), '1')
        assertEquals(KeyAction.Event(InputEvent.UnequipItem("ring")), unequip.action)
    }

    @Test
    fun `selection rejects invalid input`() {
        val sel = SelectionState(SelectionTarget.TAKE, listOf(ItemView("a", ""), ItemView("b", "")))

        // '0' → Help (1-indexed)
        assertEquals(KeyAction.Help, mapper.mapKey(keyOf('0'), viewData(), sel, Overlay.None).action)
        // Out of range
        assertEquals(KeyAction.Help, mapper.mapKey(keyOf('3'), viewData(), sel, Overlay.None).action)
        // Non-digit arrow key → stays in selection
        assertEquals(KeyAction.WaitSelection, mapper.mapKey(keyOf(KeyType.ArrowUp), viewData(), sel, Overlay.None).action)
        // Escape during selection → Help
        assertEquals(KeyAction.Help, mapper.mapKey(keyOf(KeyType.Escape), viewData(), sel, Overlay.None).action)
    }

    // Message viewer overlay navigation — scroll, focused index, close on normal key or escape

    @Test
    fun `message viewer scrolls and changes focused index`() {
        val ov = Overlay.MessageViewer(listOf("L1", "L2", "L3"), scrollOffset = 0, maxHeight = 3)

        // Arrow down increments scroll
        val result = mapper.mapKey(keyOf(KeyType.ArrowDown), viewData(), null, ov, 80, 40)
        assertTrue(result.overlay is Overlay.MessageViewer)
        assertEquals(1, result.overlay.scrollOffset)

        // Arrow up decrements (uses previous overlay state)
        val scrollUpResult = mapper.mapKey(keyOf(KeyType.ArrowUp), viewData(), null, result.overlay, 80, 40)
        assertTrue(scrollUpResult.overlay is Overlay.MessageViewer)
        assertEquals(0, scrollUpResult.overlay.scrollOffset)

        // Page down pages by max height - 2
        val bigOv = Overlay.MessageViewer((1..20).map { "L$it" }, scrollOffset = 0, maxHeight = 15)
        val pageDownResult = mapper.mapKey(keyOf(KeyType.PageDown), viewData(), null, bigOv, 80, 40)
        assertTrue(pageDownResult.overlay is Overlay.MessageViewer)

        // Arrow left/right change focused index (resets scroll)
        val idxOv = Overlay.MessageViewer(listOf("A", "B", "C"), focusedIndex = 2, scrollOffset = 0)
        val leftResult = mapper.mapKey(keyOf(KeyType.ArrowLeft), viewData(), null, idxOv, 80, 40)
        assertTrue(leftResult.overlay is Overlay.MessageViewer)
        assertEquals(1, leftResult.overlay.focusedIndex)

        // Boundary: left at start and right at end stay put
        val startOv = Overlay.MessageViewer(listOf("A", "B"), focusedIndex = 0)
        val startResult = mapper.mapKey(keyOf(KeyType.ArrowLeft), viewData(), null, startOv, 80, 40)
        assertTrue(startResult.overlay is Overlay.MessageViewer)
        assertEquals(0, startResult.overlay.focusedIndex)

        val endOv = Overlay.MessageViewer(listOf("A", "B"), focusedIndex = 1)
        val endResult = mapper.mapKey(keyOf(KeyType.ArrowRight), viewData(), null, endOv, 80, 40)
        assertTrue(endResult.overlay is Overlay.MessageViewer)
        assertEquals(1, endResult.overlay.focusedIndex)
    }

    @Test
    fun `message viewer closes on normal key or escape`() {
        val ov = Overlay.MessageViewer(listOf("Line"))
        assertEquals(Overlay.None, mapper.mapKey(keyOf('w'), viewData(), null, ov, 80, 40).overlay)
        assertEquals(Overlay.None, mapper.mapKey(keyOf(KeyType.Escape), viewData(), null, ov, 80, 40).overlay)
    }

    // Helpers

    private fun keyOf(char: Char): KeyStroke = KeyStroke(char, false, false)
    private fun keyOf(type: KeyType): KeyStroke = KeyStroke(type)

    private fun select(target: SelectionTarget, items: List<ItemView>, digit: Char) =
        mapper.mapKey(keyOf(digit), viewData(), SelectionState(target, items), Overlay.None)

    private fun event(key: KeyStroke, v: ViewData) = (mapper.mapKey(key, v, null, Overlay.None).action as KeyAction.Event).event

    private fun viewData(
        areaItems: List<ItemView> = emptyList(),
        carriedItems: List<ItemView> = emptyList(),
        equippedItems: List<ItemView> = emptyList(),
        storyMessages: List<String> = listOf("Story message")
    ): ViewData = ViewData(
        outputLine = "You are in a room.", commandText = "", triggerTexts = emptyList(),
        health = 10, maxHealth = 20, currentAreaName = "Room",
        exploredCount = 1, totalAreas = 1, activatedCount = 0, totalDevices = 1,
        exits = emptyList(), statuses = emptyMap(), statusBounds = emptyMap(),
        areaItems = areaItems, carriedItems = carriedItems, equippedItems = equippedItems,
        storyMessages = storyMessages
    )
}
