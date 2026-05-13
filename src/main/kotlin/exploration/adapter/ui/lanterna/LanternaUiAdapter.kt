package exploration.adapter.ui.lanterna

import com.googlecode.lanterna.SGR
import com.googlecode.lanterna.TerminalPosition
import com.googlecode.lanterna.TextColor
import com.googlecode.lanterna.graphics.TextGraphics
import com.googlecode.lanterna.input.KeyType
import com.googlecode.lanterna.screen.Screen
import com.googlecode.lanterna.screen.TerminalScreen
import com.googlecode.lanterna.terminal.DefaultTerminalFactory
import exploration.model.StatusRange
import exploration.port.GameEngine
import exploration.port.InputEvent
import exploration.port.ItemView
import exploration.port.ViewData

class LanternaUiAdapter(private val engine: GameEngine) {

    data class HistoryEntry(val text: String, val isTrigger: Boolean)

    companion object {
        private const val MAX_MESSAGES = 10
        private const val MIN_WIDTH = 64
        private const val MIN_HEIGHT = 23
    }

    private enum class SelectionTarget { TAKE, DROP, EQUIP, UNEQUIP }

    private data class SelectionState(val target: SelectionTarget, val items: List<ItemView>)

    fun run(scenarioId: String) {
        val screen = TerminalScreen(DefaultTerminalFactory().createTerminal())
        screen.startScreen()
        screen.cursorPosition = null

        val history = mutableListOf<HistoryEntry>()
        var shownTriggers = 0
        var selectionState: SelectionState? = null

        var state = engine.start(scenarioId)
        addMessage(history, "=== Exploration Engine (Lanterna) ===", false)
        addMessage(
            history,
            "arrows/wasd: move | l: look | u: activate | g: grab | p: drop | e: equip | r: uneq | h: help | q/esc: quit",
            false
        )
        addMessage(
            history,
            "Goal: explore all areas and activate all devices. Don't run out of health!",
            false
        )

        render(screen, engine.view(state), history)

        while (!engine.view(state).gameOver) {
            screen.doResizeIfNecessary()
            val viewBefore = engine.view(state)
            val result = readInputKey(screen, viewBefore, selectionState)
            selectionState = result.selectionState

            when (result.action) {
                is KeyAction.Quit -> break
                is KeyAction.Help -> {
                    addMessage(history, "arrows/wasd: move | l: look | u: activate | g: grab | p: drop | e: equip | r: uneq | h: help | q/esc: quit", false)
                    render(screen, engine.view(state), history)
                    continue
                }
                is KeyAction.WaitSelection -> {
                    val ss = selectionState!!
                    addMessage(history, buildSelectionPrompt(ss.target, ss.items), false)
                    render(screen, viewBefore, history)
                    continue
                }
                is KeyAction.Event -> {
                    state = engine.tick(state, result.action.event)
                    val viewAfter = engine.view(state)
                    if (viewAfter.commandText.isNotBlank()) addMessage(history, viewAfter.commandText, false)
                    val newTriggerCount = viewAfter.triggerTexts.size - shownTriggers
                    for (i in 0 until newTriggerCount) {
                        val triggerText = viewAfter.triggerTexts[shownTriggers + i]
                        if (triggerText.isNotBlank()) addMessage(history, triggerText, true)
                    }
                    shownTriggers = viewAfter.triggerTexts.size
                    render(screen, viewAfter, history)
                }
            }
        }

        val finalView = engine.view(state)
        for (i in shownTriggers until finalView.triggerTexts.size) {
            if (finalView.triggerTexts[i].isNotBlank()) addMessage(history, finalView.triggerTexts[i], true)
        }
        addMessage(
            history,
            if (finalView.win == true) "=== YOU WIN! ===" else "=== GAME OVER ===",
            false
        )
        screen.doResizeIfNecessary()
        render(screen, finalView, history)

        screen.stopScreen()
        printEndSummary(finalView, history)
    }

    private sealed class KeyAction {
        object Quit : KeyAction()
        object Help : KeyAction()
        object WaitSelection : KeyAction()
        data class Event(val event: InputEvent) : KeyAction()
    }

    private data class ReadInputResult(val action: KeyAction, val selectionState: SelectionState?)

    private fun readInputKey(
        screen: Screen,
        view: ViewData,
        selState: SelectionState?
    ): ReadInputResult {
        val key = screen.readInput() ?: return ReadInputResult(KeyAction.Quit, null)

        if (selState != null) {
            return handleSelectionDigit(key, selState)
        }

        when (key.keyType) {
            KeyType.Escape -> return ReadInputResult(KeyAction.Quit, null)
            KeyType.ArrowUp -> return ReadInputResult(KeyAction.Event(InputEvent.MoveDirection(0)), null)
            KeyType.ArrowDown -> return ReadInputResult(KeyAction.Event(InputEvent.MoveDirection(2)), null)
            KeyType.ArrowLeft -> return ReadInputResult(KeyAction.Event(InputEvent.MoveDirection(1)), null)
            KeyType.ArrowRight -> return ReadInputResult(KeyAction.Event(InputEvent.MoveDirection(3)), null)
            KeyType.Character -> {
                when (key.character.lowercaseChar()) {
                    'w' -> return ReadInputResult(KeyAction.Event(InputEvent.MoveDirection(0)), null)
                    'a' -> return ReadInputResult(KeyAction.Event(InputEvent.MoveDirection(1)), null)
                    's' -> return ReadInputResult(KeyAction.Event(InputEvent.MoveDirection(2)), null)
                    'd' -> return ReadInputResult(KeyAction.Event(InputEvent.MoveDirection(3)), null)
                    'l' -> return ReadInputResult(KeyAction.Event(InputEvent.Look), null)
                    'u' -> return ReadInputResult(KeyAction.Event(InputEvent.Activate), null)
                    'g' -> {
                        val candidates = view.areaItems
                        return if (candidates.isEmpty()) ReadInputResult(KeyAction.Help, null)
                        else if (candidates.size == 1) ReadInputResult(KeyAction.Event(InputEvent.TakeItem(candidates[0].name)), null)
                        else ReadInputResult(KeyAction.WaitSelection, SelectionState(SelectionTarget.TAKE, candidates))
                    }
                    'p' -> {
                        val candidates = view.carriedItems + view.equippedItems
                        return if (candidates.isEmpty()) ReadInputResult(KeyAction.Help, null)
                        else if (candidates.size == 1) ReadInputResult(KeyAction.Event(InputEvent.DropItem(candidates[0].name)), null)
                        else ReadInputResult(KeyAction.WaitSelection, SelectionState(SelectionTarget.DROP, candidates))
                    }
                    'e' -> {
                        val candidates = view.carriedItems
                        return if (candidates.isEmpty()) ReadInputResult(KeyAction.Help, null)
                        else if (candidates.size == 1) ReadInputResult(KeyAction.Event(InputEvent.EquipItem(candidates[0].name)), null)
                        else ReadInputResult(KeyAction.WaitSelection, SelectionState(SelectionTarget.EQUIP, candidates))
                    }
                    'r' -> {
                        val candidates = view.equippedItems
                        return if (candidates.isEmpty()) ReadInputResult(KeyAction.Help, null)
                        else if (candidates.size == 1) ReadInputResult(KeyAction.Event(InputEvent.UnequipItem(candidates[0].name)), null)
                        else ReadInputResult(KeyAction.WaitSelection, SelectionState(SelectionTarget.UNEQUIP, candidates))
                    }
                    'i' -> return ReadInputResult(KeyAction.Event(InputEvent.Inventory), null)
                    'h' -> return ReadInputResult(KeyAction.Help, null)
                    'q' -> return ReadInputResult(KeyAction.Quit, null)
                }
            }
            else -> {}
        }
        return ReadInputResult(KeyAction.Help, null)
    }

    private fun handleSelectionDigit(key: com.googlecode.lanterna.input.KeyStroke, selState: SelectionState): ReadInputResult {
        if (key.keyType == KeyType.Escape) return ReadInputResult(KeyAction.Help, null)
        if (key.keyType != KeyType.Character) return ReadInputResult(KeyAction.WaitSelection, selState)
        val idx = key.character.digitToIntOrNull() ?: return ReadInputResult(KeyAction.WaitSelection, selState)
        val itemIdx = idx - 1
        if (itemIdx < 0 || itemIdx >= selState.items.size) return ReadInputResult(KeyAction.Help, null)
        val item = selState.items[itemIdx]
        val event = when (selState.target) {
            SelectionTarget.TAKE -> InputEvent.TakeItem(item.name)
            SelectionTarget.DROP -> InputEvent.DropItem(item.name)
            SelectionTarget.EQUIP -> InputEvent.EquipItem(item.name)
            SelectionTarget.UNEQUIP -> InputEvent.UnequipItem(item.name)
        }
        return ReadInputResult(KeyAction.Event(event), null)
    }

    private fun buildSelectionPrompt(target: SelectionTarget, items: List<ItemView>): String {
        val verb = when (target) {
            SelectionTarget.TAKE -> "Take"
            SelectionTarget.DROP -> "Drop"
            SelectionTarget.EQUIP -> "Equip"
            SelectionTarget.UNEQUIP -> "Unequip"
        }
        val options = items.mapIndexed { i, it -> "[${i + 1}] ${it.name}" }.joinToString(" ")
        return "$verb what? $options"
    }

    private fun addMessage(history: MutableList<HistoryEntry>, msg: String, isTrigger: Boolean) {
        if (msg.isBlank()) return
        history.add(HistoryEntry(msg, isTrigger))
        while (history.size > MAX_MESSAGES) history.removeAt(0)
    }

    private fun render(screen: Screen, view: ViewData, history: List<HistoryEntry>) {
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

        val leftWidth = ((w - 3) * 0.6).toInt()
        val rightWidth = (w - 3) - leftWidth
        val splitCol = 1 + leftWidth
        val panelStartRow = 3
        val panelEndRow = h - 6

        drawBorder(g, w, h)
        drawTitle(g, w)
        drawPanelHeaders(g, leftWidth, rightWidth, splitCol)
        drawSeparatorLine(g, w, splitCol, panelStartRow - 1)
        drawMessagesPanel(g, history, TerminalPosition(1, panelStartRow), leftWidth, h - panelStartRow - 4)
        drawStatusesPanel(g, view, TerminalPosition(splitCol + 1, panelStartRow), rightWidth - 1, h - panelStartRow - 4)
        drawVerticalSeparator(g, splitCol, 2, h - 5)
        drawSplitSeparator(g, w, splitCol, h - 5)
        drawBottomBar(g, view, w - 2, h - 5)

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

    private fun drawPanelHeaders(g: TextGraphics, leftWidth: Int, rightWidth: Int, splitCol: Int) {
        val msgHeader = " Messages"
        g.enableModifiers(SGR.BOLD)
        g.foregroundColor = TextColor.ANSI.WHITE
        g.putString(1, 1, msgHeader)

        val statusHeader = " Statuses"
        val rightStart = splitCol + 1
        g.putString(rightStart, 1, statusHeader)
        g.disableModifiers(SGR.BOLD)

        for (col in 1 until splitCol) {
            if (col < 1 + leftWidth && col > 0) {
                g.setCharacter(col, 2, '─')
            }
        }
        g.setCharacter(splitCol, 2, '│')
        for (col in splitCol + 1 until splitCol + rightWidth) {
            if (col < 1 + leftWidth + rightWidth - 1) {
                g.setCharacter(col, 2, '─')
            }
        }
    }

    private fun drawSeparatorLine(g: TextGraphics, w: Int, splitCol: Int, row: Int) {
        for (col in 0 until w) {
            if (col == splitCol) g.setCharacter(col, row, '┼')
            else g.setCharacter(col, row, '─')
        }
    }

    private fun drawVerticalSeparator(g: TextGraphics, col: Int, startRow: Int, count: Int) {
        for (row in startRow until startRow + count) {
            if (row > 0 && row < count + startRow - 1) {
                g.setCharacter(col, row, '│')
            }
        }
    }

    private fun drawSplitSeparator(g: TextGraphics, w: Int, splitCol: Int, row: Int) {
        for (col in 0 until w) {
            if (col == splitCol) g.setCharacter(col, row, '┼')
            else g.setCharacter(col, row, '─')
        }
    }

    private fun drawMessagesPanel(
        g: TextGraphics,
        history: List<HistoryEntry>,
        top: TerminalPosition,
        width: Int,
        maxRows: Int
    ) {
        val lines = mutableListOf<Pair<String, Boolean>>()
        for (entry in history) {
            for (segment in entry.text.split("\n")) {
                wrapText(segment, width).forEach { lines.add(it to entry.isTrigger) }
            }
        }

        val skip = maxOf(0, lines.size - maxRows)
        var row = top.row
        for (i in skip until lines.size) {
            if (row >= top.row + maxRows) break
            val (lineText, isTrigger) = lines[i]
            g.foregroundColor = if (isTrigger) TextColor.ANSI.BLUE else TextColor.ANSI.DEFAULT
            g.putString(top.column, row, lineText.padEnd(width))
            row++
        }
        g.foregroundColor = TextColor.ANSI.DEFAULT
    }

    private fun drawStatusesPanel(
        g: TextGraphics,
        v: ViewData,
        top: TerminalPosition,
        width: Int,
        maxRows: Int
    ) {
        val hpLine = buildHpBar(v, width)
        g.putString(top.column, top.row, hpLine.padEnd(width))

        val progLine = " Exp: ${v.exploredCount}/${v.totalAreas}   Dev: ${v.activatedCount}/${v.totalDevices}"
        g.putString(top.column, top.row + 1, progLine.padEnd(width))

        var row = top.row + 2

        val statuses = v.statuses.filterValues { it != 0 }
        for ((name, value) in statuses) {
            if (row >= top.row + maxRows) break
            val range = v.statusBounds[name]
            g.putString(top.column, row, formatStatus(name, value, range, width).padEnd(width))
            row++
        }

        if (v.equippedItems.isEmpty()) return

        if (row < top.row + maxRows && statuses.isNotEmpty()) {
            g.putString(top.column, row, " ".repeat(width))
            row++
        }

        for ((i, item) in v.equippedItems.withIndex()) {
            if (row >= top.row + maxRows) break
            val label = "@ ${item.name}"
            g.foregroundColor = TextColor.ANSI.MAGENTA
            g.enableModifiers(SGR.BOLD)
            g.putString(top.column, row, label.padEnd(width))
            g.disableModifiers(SGR.BOLD)
            g.foregroundColor = TextColor.ANSI.DEFAULT
            row++
        }

        g.foregroundColor = TextColor.ANSI.DEFAULT
    }

    private fun buildHpBar(v: ViewData, width: Int): String {
        val barLen = 10
        val filled = ((v.health.toDouble() / v.maxHealth) * barLen).toInt().coerceIn(0, barLen)
        val empty = barLen - filled

        val bar = "█".repeat(filled) + "░".repeat(empty)
        val textPart = "${v.health}/${v.maxHealth}"
        return "$bar $textPart"
    }

    private fun formatStatus(name: String, value: Int, range: StatusRange?, width: Int): String {
        val maxLen = width - 10
        val displayValue = if (range != null && range.max != Int.MAX_VALUE) {
            "$value/${range.max}"
        } else {
            "$value"
        }
        val template = ": $displayValue"
        val nameMaxLen = maxOf(4, width - template.length)

        val displayName = if (name.length > nameMaxLen) {
            name.substring(0, nameMaxLen - 3) + "..."
        } else {
            name
        }
        return "$displayName$template"
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

    private fun drawBottomBar(
        g: TextGraphics,
        view: ViewData,
        width: Int,
        topRow: Int
    ) {
        val exits = view.exits
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
        drawInvHint(g, width, topRow)
        drawSlot(g, aLabel, TerminalPosition(colA, topRow + 1), exits.size > 1)
        drawSlot(g, sLabel, TerminalPosition(colS, topRow + 1), exits.size > 2)
        drawSlot(g, dLabel, TerminalPosition(colD, topRow + 1), exits.size > 3)
        drawActionHints(g, view, width, topRow + 2)
    }

    private fun drawInvHint(g: TextGraphics, width: Int, row: Int) {
        val label = "[\$i] inv"
        g.foregroundColor = TextColor.ANSI.CYAN
        g.enableModifiers(SGR.BOLD)
        val keyPart = "[i]"
        g.putString(width - 10, row, keyPart)
        g.disableModifiers(SGR.BOLD)
        g.foregroundColor = TextColor.ANSI.WHITE
        g.putString(width - 7, row, "inv")
    }

    private fun drawActionHints(g: TextGraphics, view: ViewData, width: Int, row: Int) {
        val hasAreaItems = view.areaItems.isNotEmpty()
        val hasCarriedOrEquipped = view.carriedItems.isNotEmpty() || view.equippedItems.isNotEmpty()
        val hasCarried = view.carriedItems.isNotEmpty()
        val hasEquipped = view.equippedItems.isNotEmpty()

        val gLabel = "[\$g] grab"
        val pLabel = "[\$p] drop"
        val eLabel = "[\$e] equip"
        val rLabel = "[\$r] uneq"

        val gap = 2
        val totalWidth = gLabel.length + gap + pLabel.length + gap + eLabel.length + gap + rLabel.length
        val leftPad = (width - totalWidth) / 2

        var col = leftPad
        drawActionSlot(g, gLabel, TerminalPosition(col, row), hasAreaItems)
        col += gLabel.length + gap
        drawActionSlot(g, pLabel, TerminalPosition(col, row), hasCarriedOrEquipped)
        col += pLabel.length + gap
        drawActionSlot(g, eLabel, TerminalPosition(col, row), hasCarried)
        col += eLabel.length + gap
        drawActionSlot(g, rLabel, TerminalPosition(col, row), hasEquipped)
    }

    private fun drawActionSlot(g: TextGraphics, label: String, pos: TerminalPosition, active: Boolean) {
        g.foregroundColor = if (active) TextColor.ANSI.GREEN else TextColor.ANSI.DEFAULT
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

    private fun printEndSummary(view: ViewData, history: List<HistoryEntry>) {
        val result = if (view.win == true) "YOU WIN!" else "GAME OVER"
        println("\n===== $result =====")
        println("Area  : ${view.currentAreaName}")
        println("Health: ${view.health}/${view.maxHealth}")
        println("Explored: ${view.exploredCount}/${view.totalAreas}  |  Devices: ${view.activatedCount}/${view.totalDevices}")
        if (view.statuses.isNotEmpty()) {
            val statusLines = view.statuses.filterValues { it != 0 }.map { (name, value) ->
                "$name=$value"
            }
            println("Statuses: ${statusLines.joinToString(", ")}")
        }
        if (history.isNotEmpty()) {
            println("\n--- Last messages ---")
            for (entry in history) println(entry.text)
        }
    }
}
