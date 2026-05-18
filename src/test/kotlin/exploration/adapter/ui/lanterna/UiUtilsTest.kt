package exploration.adapter.ui.lanterna

import exploration.model.StatusRange
import exploration.port.ExitInfo
import exploration.port.ItemView
import exploration.port.ViewData
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UiUtilsTest {

    @Test
    fun `wrapText fits splits and chunks`() {
        // Fits in one line
        assertEquals(listOf("hello world"), UiUtils.wrapText("hello world", 20))
        // Splits words at width boundary
        assertEquals(listOf("hello", "world"), UiUtils.wrapText("hello world", 8))
        // Chunks word longer than width
        assertEquals(listOf("longwor", "dtext"), UiUtils.wrapText("longwordtext", 7))
    }

    @Test
    fun `wrapText handles empty string`() = assertEquals(
        listOf(""),
        UiUtils.wrapText("", 10)
    )

    @Test
    fun `countWrappedDisplayLines counts lines across messages and empty list`() {
        assertEquals(4, UiUtils.countWrappedDisplayLines(listOf("hello world", "foo bar baz"), 8))
        assertEquals(0, UiUtils.countWrappedDisplayLines(emptyList(), 20))
    }

    @Test
    fun `buildHpBar renders health bar with clamping`() {
        val full = fullViewData().copy(health = 10, maxHealth = 10)
        assertEquals("██████████ 10/10", UiUtils.buildHpBar(full, 8))

        val half = full.copy(health = 5)
        assertEquals("█████░░░░░ 5/10", UiUtils.buildHpBar(half, 8))

        val over = full.copy(health = 20, maxHealth = 10)
        assertEquals("██████████ 20/10", UiUtils.buildHpBar(over, 8))
    }

    @Test
    fun `formatStatus formats with range truncation and edge cases`() {
        assertEquals("Energy: 42", UiUtils.formatStatus("Energy", 42, null, 50))
        assertEquals("Mana: 15/30", UiUtils.formatStatus("Mana", 15, StatusRange(min = 0, max = 30), 50))
        assertEquals("Infinite: 99", UiUtils.formatStatus("Infinite", 99, StatusRange(Int.MAX_VALUE), 50))

        val truncated = UiUtils.formatStatus("ExtremelyLongStatusName", 5, null, 20)
        assertTrue(truncated.endsWith(": 5"))
    }

    @Test
    fun `exitLabel formats placeholder and normal exits`() {
        assertEquals("[w]  .", UiUtils.exitLabel('w', null))
        assertEquals("[w]  .", UiUtils.exitLabel('w', ExitInfo(null, false)))
        assertEquals("[n] Open corridor", UiUtils.exitLabel('n', ExitInfo("Open corridor", false)))
        assertEquals("[s] Blocked path (B)", UiUtils.exitLabel('s', ExitInfo("Blocked path", true)))
    }

    @Test
    fun `buildSelectionPrompt formats for all targets`() {
        val items = listOf(ItemView("sword", "A sword"), ItemView("shield", "A shield"))
        assertEquals("Take what? [1] sword [2] shield", UiUtils.buildSelectionPrompt(SelectionTarget.TAKE, items))
        assertEquals("Drop what? [1] sword [2] shield", UiUtils.buildSelectionPrompt(SelectionTarget.DROP, items))
        assertEquals("Equip what? [1] sword [2] shield", UiUtils.buildSelectionPrompt(SelectionTarget.EQUIP, items))
        assertEquals("Unequip what? [1] sword [2] shield", UiUtils.buildSelectionPrompt(SelectionTarget.UNEQUIP, items))
    }

    @Test
    fun `isTerminalTooSmall checks minimum dimensions`() {
        assertTrue(UiUtils.isTerminalTooSmall(80, 22))   // height below min
        assertTrue(UiUtils.isTerminalTooSmall(63, 30))   // width below min
        assertFalse(UiUtils.isTerminalTooSmall(64, 23))  // at minimum
        assertFalse(UiUtils.isTerminalTooSmall(100, 30)) // above minimum
    }

    @Test
    fun `calcOverlayWidth and calcOverlayMaxHeight clamp correctly`() {
        assertEquals(38, UiUtils.calcOverlayWidth(50))       // 50 - 10 - 2 = 38
        assertEquals(168, UiUtils.calcOverlayWidth(200))     // min(170, 190) - 2 = 168

        assertEquals(23, UiUtils.calcOverlayMaxHeight(20, 40))   // terminal rows limit
        assertEquals(18, UiUtils.calcOverlayMaxHeight(15, 25))   // maxHeight when small
    }

    private fun fullViewData(): ViewData = ViewData(
        outputLine = "test", commandText = "", triggerTexts = emptyList(),
        health = 10, maxHealth = 10, currentAreaName = "Room",
        exploredCount = 1, totalAreas = 1, activatedCount = 0, totalDevices = 1,
        exits = emptyList(), statuses = emptyMap(), statusBounds = emptyMap()
    )
}
