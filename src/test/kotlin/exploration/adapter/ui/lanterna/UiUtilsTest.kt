package exploration.adapter.ui.lanterna

import exploration.model.StatusRange
import exploration.port.ExitInfo
import exploration.port.ItemView
import exploration.port.ViewData
import org.junit.jupiter.api.Test
import kotlin.test.*

class UiUtilsTest {

    @Test
    fun `wrapText returns single line when text fits`() = assertEquals(
        listOf("hello world"),
        UiUtils.wrapText("hello world", 20)
    )

    @Test
    fun `wrapText splits long text into multiple lines`() = assertEquals(
        listOf("hello", "world"),
        UiUtils.wrapText("hello world", 8)
    )

    @Test
    fun `wrapText handles word that exactly fits width`() = assertEquals(
        listOf("hello"),
        UiUtils.wrapText("hello", 5)
    )

    @Test
    fun `wrapText splits word longer than width character by chunk`() = assertEquals(
        listOf("longwor", "dtext"),
        UiUtils.wrapText("longwordtext", 7)
    )

    @Test
    fun `wrapText handles empty string`() = assertEquals(
        listOf(""),
        UiUtils.wrapText("", 10)
    )

    @Test
    fun `wrapText handles single word longer than width`() = assertEquals(
        listOf("superlo", "ng"),
        UiUtils.wrapText("superlong", 7)
    )

    @Test
    fun `wrapText splits empty words from multiple spaces`() = assertEquals(
        listOf("hello  ", "world"),
        UiUtils.wrapText("hello   world", 10)
    )

    @Test
    fun `countWrappedDisplayLines counts lines across multiple messages`() = assertEquals(
        4,
        UiUtils.countWrappedDisplayLines(listOf("hello world", "foo bar baz"), 8)
    )

    @Test
    fun `countWrappedDisplayLines handles empty list`() = assertEquals(
        0,
        UiUtils.countWrappedDisplayLines(emptyList(), 20)
    )

    @Test
    fun `buildHpBar shows full health bar at max health`() {
        val view = fullViewData().copy(health = 10, maxHealth = 10)
        assertEquals("██████████ 10/10", UiUtils.buildHpBar(view, 8))
    }

    @Test
    fun `buildHpBar shows empty health bar at zero health`() {
        val view = fullViewData().copy(health = 0, maxHealth = 10)
        assertEquals("░░░░░░░░░░ 0/10", UiUtils.buildHpBar(view, 8))
    }

    @Test
    fun `buildHpBar shows partial health bar`() {
        val view = fullViewData().copy(health = 5, maxHealth = 10)
        assertEquals("█████░░░░░ 5/10", UiUtils.buildHpBar(view, 8))
    }

    @Test
    fun `buildHpBar clamps health to bar range`() {
        val view = fullViewData().copy(health = 20, maxHealth = 10)
        assertEquals("██████████ 20/10", UiUtils.buildHpBar(view, 8))
    }

    @Test
    fun `formatStatus with no range shows just value`() {
        assertEquals("Energy: 42", UiUtils.formatStatus("Energy", 42, null, 50))
    }

    @Test
    fun `formatStatus with range shows value and max`() {
        assertEquals("Mana: 15/30", UiUtils.formatStatus("Mana", 15, StatusRange(min = 0, max = 30), 50))
    }

    @Test
    fun `formatStatus truncates long name with ellipsis`() {
        val result = UiUtils.formatStatus("ExtremelyLongStatusName", 5, null, 20)
        assertTrue(result.endsWith(": 5"))
    }

    @Test
    fun `formatStatus respects minimum name length of 4 chars`() {
        assertEquals("Ab: 10", UiUtils.formatStatus("Ab", 10, null, 20))
    }

    @Test
    fun `formatStatus with max value range shows just value`() {
        assertEquals("Infinite: 99", UiUtils.formatStatus("Infinite", 99, StatusRange(Int.MAX_VALUE), 50))
    }

    @Test
    fun `exitLabel with null info shows placeholder`() = assertEquals(
        "[w]  .",
        UiUtils.exitLabel('w', null)
    )

    @Test
    fun `exitLabel with empty name shows placeholder`() {
        assertEquals("[w]  .", UiUtils.exitLabel('w', ExitInfo(null, false)))
    }

    @Test
    fun `exitLabel with blocked exit shows (B)`() = assertEquals(
        "[s] Blocked path (B)",
        UiUtils.exitLabel('s', ExitInfo("Blocked path", true))
    )

    @Test
    fun `exitLabel with normal exit`() {
        assertEquals("[n] Open corridor", UiUtils.exitLabel('n', ExitInfo("Open corridor", false)))
    }

    @Test
    fun `buildSelectionPrompt for TAKE target`() {
        val items = listOf(ItemView("sword", "A sword"), ItemView("shield", "A shield"))
        assertEquals("Take what? [1] sword [2] shield", UiUtils.buildSelectionPrompt(SelectionTarget.TAKE, items))
    }

    @Test
    fun `buildSelectionPrompt for DROP target`() {
        val items = listOf(ItemView("potion", "A potion"))
        assertEquals("Drop what? [1] potion", UiUtils.buildSelectionPrompt(SelectionTarget.DROP, items))
    }

    @Test
    fun `isTerminalTooSmall returns true when height below minimum`() = assertTrue(
        UiUtils.isTerminalTooSmall(80, 22)
    )

    @Test
    fun `isTerminalTooSmall returns true when width below minimum`() = assertTrue(
        UiUtils.isTerminalTooSmall(63, 30)
    )

    @Test
    fun `isTerminalTooSmall returns false at minimum dimensions`() = assertFalse(
        UiUtils.isTerminalTooSmall(64, 23)
    )

    @Test
    fun `isTerminalTooSmall returns false above minimum dimensions`() = assertFalse(
        UiUtils.isTerminalTooSmall(100, 30)
    )

    @Test
    fun `calcOverlayWidth clamps to min 40 and max 170`() {
        assertEquals(38, UiUtils.calcOverlayWidth(50)) // 50 - 10 - 2 = 38
        assertEquals(168, UiUtils.calcOverlayWidth(200)) // min(170, 190) - 2 = 168
    }

    @Test
    fun `calcOverlayMaxHeight respects terminal rows limit`() {
        assertEquals(23, UiUtils.calcOverlayMaxHeight(20, 40))
    }

    @Test
    fun `calcOverlayMaxHeight uses maxHeight when terminal is small`() {
        assertEquals(18, UiUtils.calcOverlayMaxHeight(15, 25))
    }

    private fun fullViewData(): ViewData = ViewData(
        outputLine = "test", commandText = "", triggerTexts = emptyList(),
        health = 10, maxHealth = 10, currentAreaName = "Room",
        exploredCount = 1, totalAreas = 1, activatedCount = 0, totalDevices = 1,
        exits = emptyList(), statuses = emptyMap(), statusBounds = emptyMap()
    )
}
