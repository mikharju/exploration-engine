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
        private const val MIN_WIDTH = 60
        private const val MIN_HEIGHT = 20
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
            "arrows/wasd: move | l: look | u: activate | q/esc: quit"
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
            KeyType.ArrowUp -> exitMove(exits, 0)
            KeyType.ArrowDown -> exitMove(exits, 2)
            KeyType.ArrowLeft -> exitMove(exits, 1)
            KeyType.ArrowRight -> exitMove(exits, 3)
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

    private fun exitMove(exits: List<String>, idx: Int): InputEvent =
        exits.getOrNull(idx)?.let { InputEvent.Move(it) } ?: InputEvent.Look

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

        if (h < MIN_HEIGHT || w < MIN_WIDTH) {
            drawBorder(g, w, h)
            drawTooSmallMessage(g, w, h)
            screen.refresh()
            return
        }

        drawBorder(g, w, h)
        drawTitle(g, w)
        drawMessages(g, history, TerminalPosition(1, 1), w - 2, h - 6)
        g.drawLine(0, h - 4, w - 1, h - 4, '─')
        drawDirectionPad(g, view.exits, w - 2, h - 3)
        drawStatusRight(g, view, TerminalPosition(1, h - 2), w - 2)

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

    private fun drawStatusRight(g: TextGraphics, v: ViewData, pos: TerminalPosition, width: Int = 0) {
        val barLen = 10
        val filled = ((v.health.toDouble() / v.maxHealth) * barLen).toInt().coerceIn(0, barLen)
        val empty = barLen - filled

        val text = " ${v.health}/${v.maxHealth}  Exp:${v.exploredCount}/${v.totalAreas}  Dev:${v.activatedCount}/${v.totalDevices}"
        val totalWidth = barLen + 1 + text.length
        var startCol = pos.column
        if (width > 0) {
            startCol = (pos.column + width - totalWidth).coerceAtLeast(pos.column)
        }

        g.foregroundColor = TextColor.ANSI.GREEN
        repeat(filled) { g.setCharacter(startCol + it, pos.row, '█') }
        g.foregroundColor = TextColor.ANSI.RED
        repeat(empty) { g.setCharacter(startCol + barLen + it, pos.row, '░') }

        g.foregroundColor = TextColor.ANSI.WHITE
        g.setCharacter(startCol + barLen, pos.row, ' ')
        g.putString(TerminalPosition(startCol + barLen + 1, pos.row), text)
    }

    private fun drawDirectionPad(
        g: TextGraphics,
        exits: List<String>,
        width: Int,
        topRow: Int
    ) {
        val wLabel = exitLabel('w', exits.getOrNull(0))
        val aLabel = exitLabel('a', exits.getOrNull(1))
        val sLabel = exitLabel('s', exits.getOrNull(2))
        val dLabel = exitLabel('d', exits.getOrNull(3))

        val gap = 2
        val contentWidth = aLabel.length + gap + sLabel.length + gap + dLabel.length
        val leftPad = (width - contentWidth) / 2

        val colA = leftPad
        val colS = colA + aLabel.length + gap
        val colD = colS + sLabel.length + gap
        val colW = colS - wLabel.length / 2

        drawSlot(g, wLabel, TerminalPosition(colW, topRow), exits.size > 0)
        drawSlot(g, aLabel, TerminalPosition(colA, topRow + 1), exits.size > 1)
        drawSlot(g, sLabel, TerminalPosition(colS, topRow + 1), exits.size > 2)
        drawSlot(g, dLabel, TerminalPosition(colD, topRow + 1), exits.size > 3)
    }

    private fun exitLabel(key: Char, name: String?): String =
        if (name != null) "[$key] $name" else "[$key]  ."

    private fun drawSlot(g: TextGraphics, label: String, pos: TerminalPosition, active: Boolean) {
        g.foregroundColor = if (active) TextColor.ANSI.YELLOW else TextColor.ANSI.DEFAULT
        g.enableModifiers(SGR.BOLD)
        val keyPart = label.substringBefore(']') + "]"
        g.putString(pos.column, pos.row, keyPart)
        g.disableModifiers(SGR.BOLD)

        val nameStart = pos.column + keyPart.length
        val namePart = if (label.contains("] ")) label.substringAfter("] ") else ""
        g.foregroundColor = if (active) TextColor.ANSI.WHITE else TextColor.ANSI.DEFAULT
        g.putString(nameStart, pos.row, namePart)

        g.disableModifiers(SGR.BOLD)
    }

    private fun drawTooSmallMessage(g: TextGraphics, w: Int, h: Int) {
        val lines = listOf(
            "Terminal too small.",
            "Resize to at least $MIN_WIDTH columns, $MIN_HEIGHT rows."
        )
        val startRow = (h - lines.size) / 2
        g.foregroundColor = TextColor.ANSI.YELLOW
        g.enableModifiers(SGR.BOLD)
        for ((i, line) in lines.withIndex()) {
            val col = (w - line.length) / 2
            g.putString(col, startRow + i, line)
        }
        g.disableModifiers(SGR.BOLD)
    }
}
