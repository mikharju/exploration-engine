package exploration.adapter.ui.text

import exploration.port.InputEvent
import org.junit.jupiter.api.Test
import kotlin.test.*

class TextCommandParserTest {

    @Test
    fun `parseText handles look command`() {
        assertEquals(InputEvent.Look, parseText("look"))
        assertEquals(InputEvent.Look, parseText("LOOK"))
        assertEquals(InputEvent.Look, parseText("  Look  "))
    }

    @Test
    fun `parseText handles activate variants`() {
        assertEquals(InputEvent.Activate, parseText("activate"))
        assertEquals(InputEvent.Activate, parseText("use"))
    }

    @Test
    fun `parseText handles move with area name`() {
        val event = parseText("move Cave")
        assertIs<InputEvent.Move>(event)
        assertEquals("Cave", event.areaName)
    }

    @Test
    fun `parseText returns null for unknown command`() {
        assertNull(parseText("jump"))
        assertNull(parseText(""))
    }
}
