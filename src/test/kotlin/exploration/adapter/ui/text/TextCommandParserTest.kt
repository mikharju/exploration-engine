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
    fun `parseText handles direction via move verb`() {
        assertEquals(InputEvent.MoveDirection(InputEvent.Direction.North), parseText("move w"))
        assertEquals(InputEvent.MoveDirection(InputEvent.Direction.West), parseText("move a"))
        assertEquals(InputEvent.MoveDirection(InputEvent.Direction.South), parseText("move s"))
        assertEquals(InputEvent.MoveDirection(InputEvent.Direction.East), parseText("move d"))

        assertEquals(InputEvent.MoveDirection(InputEvent.Direction.North), parseText("w"))
        assertEquals(InputEvent.MoveDirection(InputEvent.Direction.West), parseText("a"))
        assertEquals(InputEvent.MoveDirection(InputEvent.Direction.South), parseText("s"))
        assertEquals(InputEvent.MoveDirection(InputEvent.Direction.East), parseText("d"))
    }

    @Test
    fun `parseText returns null for unknown command`() {
        assertNull(parseText("jump"))
        assertNull(parseText(""))
        assertNull(parseText("Cave"))
        assertNull(parseText("move Cave"))
    }
}
