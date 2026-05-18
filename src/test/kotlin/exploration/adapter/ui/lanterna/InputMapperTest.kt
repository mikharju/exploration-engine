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

    @BeforeEach
    fun setup() {
        mapper = LanternaKeyMapper()
    }

    // Movement keys

    @Test
    fun `arrow up maps to North movement`() {
        val result = mapper.mapKey(keyOf(KeyType.ArrowUp), viewData(), null, Overlay.None)
        assertEquals(KeyAction.Event(InputEvent.MoveDirection(InputEvent.Direction.North)), result.action)
    }

    @Test
    fun `arrow down maps to South movement`() {
        val result = mapper.mapKey(keyOf(KeyType.ArrowDown), viewData(), null, Overlay.None)
        assertEquals(KeyAction.Event(InputEvent.MoveDirection(InputEvent.Direction.South)), result.action)
    }

    @Test
    fun `arrow left maps to West movement`() {
        val result = mapper.mapKey(keyOf(KeyType.ArrowLeft), viewData(), null, Overlay.None)
        assertEquals(KeyAction.Event(InputEvent.MoveDirection(InputEvent.Direction.West)), result.action)
    }

    @Test
    fun `arrow right maps to East movement`() {
        val result = mapper.mapKey(keyOf(KeyType.ArrowRight), viewData(), null, Overlay.None)
        assertEquals(KeyAction.Event(InputEvent.MoveDirection(InputEvent.Direction.East)), result.action)
    }

    // WASD keys

    @Test
    fun `w key maps to North movement`() {
        val result = mapper.mapKey(keyOf('w'), viewData(), null, Overlay.None)
        assertEquals(KeyAction.Event(InputEvent.MoveDirection(InputEvent.Direction.North)), result.action)
    }

    @Test
    fun `a key maps to West movement`() {
        val result = mapper.mapKey(keyOf('a'), viewData(), null, Overlay.None)
        assertEquals(KeyAction.Event(InputEvent.MoveDirection(InputEvent.Direction.West)), result.action)
    }

    @Test
    fun `s key maps to South movement`() {
        val result = mapper.mapKey(keyOf('s'), viewData(), null, Overlay.None)
        assertEquals(KeyAction.Event(InputEvent.MoveDirection(InputEvent.Direction.South)), result.action)
    }

    @Test
    fun `d key maps to East movement`() {
        val result = mapper.mapKey(keyOf('d'), viewData(), null, Overlay.None)
        assertEquals(KeyAction.Event(InputEvent.MoveDirection(InputEvent.Direction.East)), result.action)
    }

    // Action keys

    @Test
    fun `l key triggers look`() {
        val result = mapper.mapKey(keyOf('l'), viewData(), null, Overlay.None)
        assertEquals(KeyAction.Event(InputEvent.Look), result.action)
    }

    @Test
    fun `u key triggers activate`() {
        val result = mapper.mapKey(keyOf('u'), viewData(), null, Overlay.None)
        assertEquals(KeyAction.Event(InputEvent.Activate), result.action)
    }

    @Test
    fun `i key triggers inventory`() {
        val result = mapper.mapKey(keyOf('i'), viewData(), null, Overlay.None)
        assertEquals(KeyAction.Event(InputEvent.Inventory), result.action)
    }

    @Test
    fun `q key triggers quit`() {
        val result = mapper.mapKey(keyOf('q'), viewData(), null, Overlay.None)
        assertEquals(KeyAction.Quit, result.action)
    }

    @Test
    fun `escape triggers quit when no overlay`() {
        val result = mapper.mapKey(keyOf(KeyType.Escape), viewData(), null, Overlay.None)
        assertEquals(KeyAction.Quit, result.action)
    }

    @Test
    fun `h key triggers help display with unknown char`() {
        val result = mapper.mapKey(keyOf('z'), viewData(), null, Overlay.None)
        assertEquals(KeyAction.Help, result.action)
    }

    // Take/drop/equip/unequip — single item auto-take

    @Test
    fun `g key takes single area item`() {
        val view = viewData(areaItems = listOf(ItemView("key", "A small key")))
        val result = mapper.mapKey(keyOf('g'), view, null, Overlay.None)
        assertEquals(KeyAction.Event(InputEvent.TakeItem("key")), result.action)
    }

    @Test
    fun `g key shows help when no area items`() {
        val result = mapper.mapKey(keyOf('g'), viewData(), null, Overlay.None)
        assertEquals(KeyAction.Help, result.action)
    }

    @Test
    fun `p key drops single carried item`() {
        val view = viewData(carriedItems = listOf(ItemView("sword", "A sword")))
        val result = mapper.mapKey(keyOf('p'), view, null, Overlay.None)
        assertEquals(KeyAction.Event(InputEvent.DropItem("sword")), result.action)
    }

    @Test
    fun `e key equips single carried item`() {
        val view = viewData(carriedItems = listOf(ItemView("helmet", "A helmet")))
        val result = mapper.mapKey(keyOf('e'), view, null, Overlay.None)
        assertEquals(KeyAction.Event(InputEvent.EquipItem("helmet")), result.action)
    }

    @Test
    fun `r key unequips single equipped item`() {
        val view = viewData(equippedItems = listOf(ItemView("ring", "A ring")))
        val result = mapper.mapKey(keyOf('r'), view, null, Overlay.None)
        assertEquals(KeyAction.Event(InputEvent.UnequipItem("ring")), result.action)
    }

    // Selection triggers

    @Test
    fun `g key waits selection when multiple area items`() {
        val view = viewData(
            areaItems = listOf(
                ItemView("key", "A small key"),
                ItemView("lock", "A rusty lock")
            )
        )
        val result = mapper.mapKey(keyOf('g'), view, null, Overlay.None)
        assertEquals(KeyAction.WaitSelection, result.action)
        assertNotNull(result.selectionState)
        assertEquals(SelectionTarget.TAKE, result.selectionState.target)
    }

    @Test
    fun `p key waits selection when multiple carried items`() {
        val view = viewData(carriedItems = listOf(ItemView("a", ""), ItemView("b", "")))
        val result = mapper.mapKey(keyOf('p'), view, null, Overlay.None)
        assertEquals(KeyAction.WaitSelection, result.action)
        assertNotNull(result.selectionState)
        assertEquals(SelectionTarget.DROP, result.selectionState.target)
    }

    @Test
    fun `e key waits selection when multiple carried items`() {
        val view = viewData(carriedItems = listOf(ItemView("a", ""), ItemView("b", "")))
        val result = mapper.mapKey(keyOf('e'), view, null, Overlay.None)
        assertEquals(KeyAction.WaitSelection, result.action)
        assertNotNull(result.selectionState)
        assertEquals(SelectionTarget.EQUIP, result.selectionState.target)
    }

    @Test
    fun `r key waits selection when multiple equipped items`() {
        val view = viewData(equippedItems = listOf(ItemView("a", ""), ItemView("b", "")))
        val result = mapper.mapKey(keyOf('r'), view, null, Overlay.None)
        assertEquals(KeyAction.WaitSelection, result.action)
        assertNotNull(result.selectionState)
        assertEquals(SelectionTarget.UNEQUIP, result.selectionState.target)
    }

    @Test
    fun `g key waits selection when no area items but selection active`() {
        val sel = SelectionState(SelectionTarget.TAKE, listOf(ItemView("a", "")))
        val result = mapper.mapKey(keyOf('g'), viewData(), sel, Overlay.None)
        assertEquals(KeyAction.WaitSelection, result.action)
    }

    // Story viewer

    @Test
    fun `j key opens story viewer with messages`() {
        val view = viewData(storyMessages = listOf("Once upon a time", "The end"))
        val result = mapper.mapKey(keyOf('j'), view, null, Overlay.None)
        assertEquals(KeyAction.NoOp, result.action)
        assertTrue(result.overlay is Overlay.MessageViewer)
    }

    @Test
    fun `j key shows help when no story messages`() {
        val view = viewData(storyMessages = emptyList())
        val result = mapper.mapKey(keyOf('j'), view, null, Overlay.None)
        assertEquals(KeyAction.Help, result.action)
    }

    @Test
    fun `j key filters blank story messages`() {
        val view = viewData(storyMessages = listOf("First", "", "Last"))
        val result = mapper.mapKey(keyOf('j'), view, null, Overlay.None)
        assertTrue(result.overlay is Overlay.MessageViewer)
        val viewer: Overlay.MessageViewer = result.overlay
        assertEquals(2, viewer.messages.size)
    }

    // Selection digit handling

    @Test
    fun `selection digit 1 picks first item`() {
        val items = listOf(ItemView("sword", ""), ItemView("shield", ""))
        val sel = SelectionState(SelectionTarget.TAKE, items)
        val result = mapper.mapKey(keyOf('1'), viewData(), sel, Overlay.None)
        assertEquals(KeyAction.Event(InputEvent.TakeItem("sword")), result.action)
    }

    @Test
    fun `selection digit 2 picks second item`() {
        val items = listOf(ItemView("a", ""), ItemView("b", ""))
        val sel = SelectionState(SelectionTarget.DROP, items)
        val result = mapper.mapKey(keyOf('2'), viewData(), sel, Overlay.None)
        assertEquals(KeyAction.Event(InputEvent.DropItem("b")), result.action)
    }

    @Test
    fun `selection digit 3 picks third item`() {
        val items = listOf(ItemView("a", ""), ItemView("b", ""), ItemView("c", ""))
        val sel = SelectionState(SelectionTarget.EQUIP, items)
        val result = mapper.mapKey(keyOf('3'), viewData(), sel, Overlay.None)
        assertEquals(KeyAction.Event(InputEvent.EquipItem("c")), result.action)
    }

    @Test
    fun `selection digit handles unequip`() {
        val items = listOf(ItemView("ring", ""))
        val sel = SelectionState(SelectionTarget.UNEQUIP, items)
        val result = mapper.mapKey(keyOf('1'), viewData(), sel, Overlay.None)
        assertEquals(KeyAction.Event(InputEvent.UnequipItem("ring")), result.action)
    }

    @Test
    fun `selection digit 0 picks first item`() {
        // Digits are 1-indexed but '0' gives idx=0-1=-1 which is < 0 so Help
        val items = listOf(ItemView("a", ""), ItemView("b", ""))
        val sel = SelectionState(SelectionTarget.TAKE, items)
        val result = mapper.mapKey(keyOf('0'), viewData(), sel, Overlay.None)
        assertEquals(KeyAction.Help, result.action)
    }

    @Test
    fun `selection digit out of range shows help`() {
        val items = listOf(ItemView("a", ""))
        val sel = SelectionState(SelectionTarget.TAKE, items)
        val result = mapper.mapKey(keyOf('2'), viewData(), sel, Overlay.None)
        assertEquals(KeyAction.Help, result.action)
    }

    @Test
    fun `selection ignores non-digit keys`() {
        val items = listOf(ItemView("a", ""), ItemView("b", ""))
        val sel = SelectionState(SelectionTarget.TAKE, items)
        val result = mapper.mapKey(keyOf(KeyType.Escape), viewData(), sel, Overlay.None)
        assertEquals(KeyAction.Help, result.action)
    }

    @Test
    fun `selection ignores arrow keys`() {
        val items = listOf(ItemView("a", ""), ItemView("b", ""))
        val sel = SelectionState(SelectionTarget.TAKE, items)
        val result = mapper.mapKey(keyOf(KeyType.ArrowUp), viewData(), sel, Overlay.None)
        assertEquals(KeyAction.WaitSelection, result.action)
    }

    // Message viewer overlay navigation

    @Test
    fun `arrow down scrolls message viewer`() {
        val msgs = listOf("Line 1", "Line 2", "Line 3")
        val ov = Overlay.MessageViewer(msgs, scrollOffset = 0, maxHeight = 3)
        val result = mapper.mapKey(keyOf(KeyType.ArrowDown), viewData(), null, ov, 80, 40)
        assertTrue(result.overlay is Overlay.MessageViewer)
        assertEquals(1, result.overlay.scrollOffset)
    }

    @Test
    fun `arrow up scrolls message viewer backwards`() {
        val msgs = listOf("Line 1", "Line 2", "Line 3")
        val ov = Overlay.MessageViewer(msgs, scrollOffset = 2, maxHeight = 3)
        val result = mapper.mapKey(keyOf(KeyType.ArrowUp), viewData(), null, ov, 80, 40)
        assertTrue(result.overlay is Overlay.MessageViewer)
        assertEquals(1, result.overlay.scrollOffset)
    }

    @Test
    fun `page down scrolls message viewer by page`() {
        val msgs = buildList { for (i in 1..20) add("Line $i") }
        val ov = Overlay.MessageViewer(msgs, scrollOffset = 0, maxHeight = 15)
        val result = mapper.mapKey(keyOf(KeyType.PageDown), viewData(), null, ov, 80, 40)
        assertTrue(result.overlay is Overlay.MessageViewer)
    }

    @Test
    fun `page up scrolls message viewer by page backwards`() {
        val msgs = buildList { for (i in 1..20) add("Line $i") }
        val ov = Overlay.MessageViewer(msgs, scrollOffset = 5, maxHeight = 15)
        val result = mapper.mapKey(keyOf(KeyType.PageUp), viewData(), null, ov, 80, 40)
        assertTrue(result.overlay is Overlay.MessageViewer)
    }

    @Test
    fun `escape closes message viewer`() {
        val msgs = listOf("Line 1")
        val ov = Overlay.MessageViewer(msgs, scrollOffset = 0)
        val result = mapper.mapKey(keyOf(KeyType.Escape), viewData(), null, ov, 80, 40)
        assertEquals(Overlay.None, result.overlay)
    }

    @Test
    fun `arrow left moves focused index in message viewer`() {
        val msgs = listOf("Line 1", "Line 2", "Line 3")
        val ov = Overlay.MessageViewer(msgs, scrollOffset = 0, focusedIndex = 2)
        val result = mapper.mapKey(keyOf(KeyType.ArrowLeft), viewData(), null, ov, 80, 40)
        assertTrue(result.overlay is Overlay.MessageViewer)
        assertEquals(1, result.overlay.focusedIndex)
    }

    @Test
    fun `arrow right moves focused index in message viewer`() {
        val msgs = listOf("Line 1", "Line 2", "Line 3")
        val ov = Overlay.MessageViewer(msgs, scrollOffset = 0, focusedIndex = 0)
        val result = mapper.mapKey(keyOf(KeyType.ArrowRight), viewData(), null, ov, 80, 40)
        assertTrue(result.overlay is Overlay.MessageViewer)
        assertEquals(1, result.overlay.focusedIndex)
    }

    @Test
    fun `arrow left at start keeps focused index 0`() {
        val msgs = listOf("Line 1", "Line 2")
        val ov = Overlay.MessageViewer(msgs, scrollOffset = 0, focusedIndex = 0)
        val result = mapper.mapKey(keyOf(KeyType.ArrowLeft), viewData(), null, ov, 80, 40)
        assertTrue(result.overlay is Overlay.MessageViewer)
        assertEquals(0, result.overlay.focusedIndex)
    }

    @Test
    fun `arrow right at end keeps focused index at last`() {
        val msgs = listOf("Line 1", "Line 2")
        val ov = Overlay.MessageViewer(msgs, scrollOffset = 0, focusedIndex = 1)
        val result = mapper.mapKey(keyOf(KeyType.ArrowRight), viewData(), null, ov, 80, 40)
        assertTrue(result.overlay is Overlay.MessageViewer)
        assertEquals(1, result.overlay.focusedIndex)
    }

    // Overlay closing on normal key press

    @Test
    fun `normal key in message viewer closes overlay`() {
        val msgs = listOf("Line 1")
        val ov = Overlay.MessageViewer(msgs)
        val result = mapper.mapKey(keyOf('w'), viewData(), null, ov, 80, 40)
        assertEquals(Overlay.None, result.overlay)
    }

    @Test
    fun `escape in message viewer closes overlay`() {
        val msgs = listOf("Line 1")
        val ov = Overlay.MessageViewer(msgs)
        val result = mapper.mapKey(keyOf(KeyType.Escape), viewData(), null, ov, 80, 40)
        assertEquals(Overlay.None, result.overlay)
    }

    // Escape with overlay still active — should close it

    @Test
    fun `escape closes overlay when escape pressed normally`() {
        val msgs = listOf("Line 1")
        val ov = Overlay.MessageViewer(msgs)
        val result = mapper.mapKey(keyOf(KeyType.Escape), viewData(), null, ov, 80, 40)
        assertEquals(Overlay.None, result.overlay)
    }

    // Case insensitivity

    @Test
    fun `W (uppercase) maps to North movement`() {
        val result = mapper.mapKey(keyOf('W'), viewData(), null, Overlay.None)
        assertEquals(KeyAction.Event(InputEvent.MoveDirection(InputEvent.Direction.North)), result.action)
    }

    @Test
    fun `L (uppercase) triggers look`() {
        val result = mapper.mapKey(keyOf('L'), viewData(), null, Overlay.None)
        assertEquals(KeyAction.Event(InputEvent.Look), result.action)
    }

    // Result contains overlay state

    @Test
    fun `mapResult returns unchanged overlay for normal keypress`() {
        val msgs = buildList { for (i in 1..30) add("Message $i") }
        val ov = Overlay.MessageViewer(msgs, scrollOffset = 5, maxHeight = 3)
        val result = mapper.mapKey(keyOf(KeyType.ArrowDown), viewData(), null, ov, 80, 40)
        assertTrue(result.overlay is Overlay.MessageViewer)
        assertEquals(6, result.overlay.scrollOffset)
    }

    // Selection state cleared after selection

    @Test
    fun `selection result clears selectionState`() {
        val items = listOf(ItemView("key", ""))
        val sel = SelectionState(SelectionTarget.TAKE, items)
        val result = mapper.mapKey(keyOf('1'), viewData(), sel, Overlay.None)
        assertNull(result.selectionState)
    }

    // Unknown keys show help

    @Test
    fun `unknown key type shows help`() {
        val result = mapper.mapKey(keyOf(KeyType.Unknown), viewData(), null, Overlay.None)
        assertEquals(KeyAction.Help, result.action)
    }

    // Edge cases

    @Test
    fun `h key returns NoOp (not Help)`() {
        val result = mapper.mapKey(keyOf('h'), viewData(), null, Overlay.None)
        assertEquals(KeyAction.NoOp, result.action)
    }

    @Test
    fun `escape during selection shows help`() {
        val items = listOf(ItemView("a", ""), ItemView("b", ""))
        val sel = SelectionState(SelectionTarget.TAKE, items)
        val result = mapper.mapKey(keyOf(KeyType.Escape), viewData(), sel, Overlay.None)
        assertEquals(KeyAction.Help, result.action)
    }

    // Helpers

    private fun keyOf(char: Char): KeyStroke =
        KeyStroke(char, false, false)

    private fun keyOf(type: KeyType): KeyStroke =
        KeyStroke(type)

    private fun viewData(
        areaItems: List<ItemView> = emptyList(),
        carriedItems: List<ItemView> = emptyList(),
        equippedItems: List<ItemView> = emptyList(),
        storyMessages: List<String> = listOf("Story message")
    ): ViewData = ViewData(
        outputLine = "You are in a room.",
        commandText = "",
        triggerTexts = emptyList(),
        health = 10,
        maxHealth = 20,
        currentAreaName = "Room",
        exploredCount = 1,
        totalAreas = 1,
        activatedCount = 0,
        totalDevices = 1,
        exits = emptyList(),
        statuses = emptyMap(),
        statusBounds = emptyMap(),
        areaItems = areaItems,
        carriedItems = carriedItems,
        equippedItems = equippedItems,
        storyMessages = storyMessages
    )
}
