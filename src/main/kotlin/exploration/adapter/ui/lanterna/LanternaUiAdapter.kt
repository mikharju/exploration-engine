package exploration.adapter.ui.lanterna

import com.googlecode.lanterna.SGR
import com.googlecode.lanterna.TerminalPosition
import com.googlecode.lanterna.TextColor
import com.googlecode.lanterna.graphics.TextGraphics
import com.googlecode.lanterna.input.KeyType
import com.googlecode.lanterna.screen.Screen
import com.googlecode.lanterna.screen.TerminalScreen
import com.googlecode.lanterna.terminal.DefaultTerminalFactory
import exploration.port.GameEngine
import exploration.port.InputEvent
import exploration.port.ViewData

class LanternaUiAdapter(private val engine: GameEngine) {

    companion object {
        private const val MAX_MESSAGES = 10
    }

    fun run(scenarioId: String) {
        val screen = TerminalScreen(DefaultTerminalFactory().createTerminal())
        screen.startScreen()
        screen.cursorPosition = null

        val history = mutableListOf<String>()

        var state = engine.start(scenarioId)
        addMessage(history, "=== Exploration Engine (Lanterna) ===")
        addMessage(
            history,
            "w/a/s/d: move | l: look | u: activate | q/esc: quit"
        )
        addMessage(
            history,
            "Goal: explore all areas and activate all devices. Don't run out of health!"
        )

        render(screen, engine.view(state), history)

        while (!engine.view(state).gameOver) {
            screen.doResizeIfNecessary()
            val event = readInputKey(screen, engine.view(state).exits)
            if (event is InputEvent.Exit) break
            state = engine.tick(state, event)
            addMessage(history, engine.view(state).outputLine)
            render(screen, engine.view(state), history)
        }

        screen.stopScreen()
    }

    private fun readInputKey(screen: Screen, exits: List<String>): InputEvent {
        val key = screen.readInput() ?: return InputEvent.Exit
        when (key.keyType) {
            KeyType.Escape -> return InputEvent.Exit
            KeyType.Character -> {
                when (key.character.lowercaseChar()) {
                    'w', 'a', 's', 'd' -> {
                        val idx = listOf('w', 'a', 's', 'd').indexOf(key.character.lowercaseChar())
                        return exits.getOrNull(idx)?.let { InputEvent.Move(it) } ?: InputEvent.Look
                    }
                    'l' -> return InputEvent.Look
                    'u' -> return InputEvent.Activate
                    'q' -> return InputEvent.Exit
                }
            }
            else -> {}
        }
        return InputEvent.Exit
    }

    private fun addMessage(history: MutableList<String>, msg: String) {
        if (msg.isBlank()) return
        history.add(msg)
        while (history.size > MAX_MESSAGES) history.removeAt(0)
    }

    private fun render(screen: Screen, view: ViewData, history: List<String>) {
        screen.clear()
        val g = screen.newTextGraphics()
        val size = screen.terminalSize
        val w = size.columns
        val h = size.rows

        if (h < 6 || w < 20) return

        drawBorder(g, w, h)
        drawTitle(g, w)
        drawMessages(g, history, TerminalPosition(1, 1), w - 2, h - 4)
        g.drawLine(0, h - 3, w - 1, h - 3, '─')
        drawStatus(g, view, TerminalPosition(1, h - 2))
        drawExits(g, view.exits, TerminalPosition(1, h - 1), w - 2)

        screen.refresh()
    }

    private fun drawBorder(g: TextGraphics, w: Int, h: Int) {
        g.setCharacter(0, 0, '┌')
        g.setCharacter(w - 1, 0, '┐')
        g.setCharacter(0, h - 1, '└')
        g.setCharacter(w - 1, h - 1, '┘')
        for (col in 1 until w - 1) {
            g.setCharacter(col, 0, '─')
            g.setCharacter(col, h - 1, '─')
        }
        for (row in 1 until h - 1) {
            g.setCharacter(0, row, '│')
            g.setCharacter(w - 1, row, '│')
        }
    }

    private fun drawTitle(g: TextGraphics, width: Int) {
        val title = " Exploration Engine "
        val pad = maxOf(width - title.length, 0) / 2
        g.enableModifiers(SGR.BOLD)
        g.foregroundColor = TextColor.ANSI.CYAN
        g.putString(pad, 0, title)
        g.disableModifiers(SGR.BOLD)
    }

    private fun drawMessages(
        g: TextGraphics,
        history: List<String>,
        top: TerminalPosition,
        width: Int,
        maxRows: Int
    ) {
        val lines = mutableListOf<String>()
        for (msg in history) {
            wrapText(msg, width).forEach { lines.add(it) }
        }

        val skip = maxOf(0, lines.size - maxRows)
        var row = top.row
        for (i in skip until lines.size) {
            if (row >= top.row + maxRows) break
            g.putString(top.column, row, lines[i].padEnd(width))
            row++
        }
    }

    private fun wrapText(text: String, width: Int): List<String> {
        val words = text.split(" ")
        val result = mutableListOf<String>()
        var line = ""

        for (word in words) {
            if (word.length >= width) {
                if (line.isNotEmpty()) result.add(line)
                line = ""
                var remaining = word
                while (remaining.isNotEmpty()) {
                    if (remaining.length <= width) {
                        line += remaining
                        remaining = ""
                    } else {
                        result.add(remaining.substring(0, width))
                        remaining = remaining.substring(width)
                    }
                }
            } else if ((line + " " + word).length <= width) {
                line = if (line.isEmpty()) word else "$line $word"
            } else {
                if (line.isNotEmpty()) result.add(line)
                line = word
            }
        }
        if (line.isNotEmpty()) result.add(line)
        return result.ifEmpty { listOf("") }
    }

    private fun drawStatus(g: TextGraphics, v: ViewData, pos: TerminalPosition) {
        val barLen = 10
        val filled = ((v.health.toDouble() / v.maxHealth) * barLen).toInt().coerceIn(0, barLen)
        val empty = barLen - filled

        g.foregroundColor = TextColor.ANSI.GREEN
        repeat(filled) { g.setCharacter(pos.column + it, pos.row, '█') }
        g.foregroundColor = TextColor.ANSI.RED
        repeat(empty) { g.setCharacter(pos.column + barLen + it, pos.row, '░') }

        val text = " ${v.health}/${v.maxHealth}  Exp:${v.exploredCount}/${v.totalAreas}  Dev:${v.activatedCount}/${v.totalDevices}"
        g.foregroundColor = TextColor.ANSI.WHITE
        g.setCharacter(pos.column + barLen, pos.row, ' ')
        g.putString(TerminalPosition(pos.column + barLen + 1, pos.row), text)
    }

    private fun drawExits(g: TextGraphics, exits: List<String>, pos: TerminalPosition, width: Int) {
        val keys = listOf('w', 'a', 's', 'd')
        var col = pos.column
        g.foregroundColor = TextColor.ANSI.WHITE

        for (i in exits.indices) {
            if (col > pos.column) {
                g.setCharacter(col - 2, pos.row, ' ')
            }
            val label = " [${keys[i]}] ${exits[i]}"
            g.foregroundColor = TextColor.ANSI.YELLOW
            g.enableModifiers(SGR.BOLD)
            g.putString(col, pos.row, "[${keys[i]}]")
            g.disableModifiers(SGR.BOLD)
            g.foregroundColor = TextColor.ANSI.WHITE
            g.putString(col + 3, pos.row, " ${exits[i]}")
            col += label.length
        }
    }
}
