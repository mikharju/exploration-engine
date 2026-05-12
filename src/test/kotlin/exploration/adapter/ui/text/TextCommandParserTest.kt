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
        assertEquals(InputEvent.Look, parseText("l"))
    }

    @Test
    fun `parseText handles activate variants`() {
        assertEquals(InputEvent.Activate, parseText("activate"))
        assertEquals(InputEvent.Activate, parseText("use"))
        assertEquals(InputEvent.Activate, parseText("u"))
    }

    @Test
    fun `parseText handles bare direction keys`() {
        assertEquals(InputEvent.MoveDirection(0), parseText("w"))
        assertEquals(InputEvent.MoveDirection(1), parseText("a"))
        assertEquals(InputEvent.MoveDirection(2), parseText("s"))
        assertEquals(InputEvent.MoveDirection(3), parseText("d"))
    }

    @Test
    fun `parseText handles move direction commands`() {
        assertEquals(InputEvent.MoveDirection(0), parseText("move w"))
        assertEquals(InputEvent.MoveDirection(1), parseText("move a"))
        assertEquals(InputEvent.MoveDirection(2), parseText("move s"))
        assertEquals(InputEvent.MoveDirection(3), parseText("move d"))
    }

    @Test
    fun `parseText returns null for unknown command`() {
        assertNull(parseText("jump"))
        assertNull(parseText(""))
        assertNull(parseText("Cave"))
        assertNull(parseText("move Cave"))
    }
}
