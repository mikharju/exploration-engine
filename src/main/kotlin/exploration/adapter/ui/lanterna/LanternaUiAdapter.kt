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
import exploration.port.ExitInfo
import exploration.port.GameEngine
import exploration.port.GameRef
import exploration.port.InputEvent
import exploration.port.ItemView
import exploration.port.ViewData

class LanternaUiAdapter(private val engine: GameEngine) {

    data class HistoryEntry(val text: String, val isTrigger: Boolean)

    sealed interface Overlay {
        object None : Overlay
        data class MessageViewer(
            val messages: List<String>,
            var scrollOffset: Int = 0,
            val maxHeight: Int = 20,
            var focusedIndex: Int = 0
        ) : Overlay {

            fun overlayTextAreaMaxWidth(screen: Screen): Int =
                minOf(170, maxOf(40, screen.terminalSize.columns - 10)) - 2

            fun overlayTextAreaMaxHeight(screen: Screen): Int =
                (this.maxHeight + 3).coerceAtMost(screen.terminalSize.rows - 6)
        }

    }

    companion object {
        private const val MAX_MESSAGES = 10
        private const val MIN_WIDTH = 64
        private const val MIN_HEIGHT = 23
    }

    private enum class SelectionTarget { TAKE, DROP, EQUIP, UNEQUIP }

    private data class SelectionState(val target: SelectionTarget, val items: List<ItemView>)

    fun run(scenarioId: String) {
        var ref: GameRef
        try {
            ref = engine.start(scenarioId)
        } catch (e: Exception) {
            println("Error loading scenario '$scenarioId': ${e.message}")
            e.printStackTrace()
            return
        }

        val screen = TerminalScreen(DefaultTerminalFactory().createTerminal())
        screen.stopScreen()
        screen.startScreen()
        screen.cursorPosition = null

        val history = mutableListOf<HistoryEntry>()
        var shownTriggers = 0
        var selectionState: SelectionState? = null
        var overlay: Overlay = Overlay.None
        var storedStoryCount = 0

        addMessage(history, "=== Exploration Engine (Lanterna) ===", false)
        addMessage(
            history,
            "arrows/wasd: move | l: look | u: activate | g: grab | p: drop | e: equip | r: uneq | j: stories | h: help | q/esc: quit",
            false
        )

        var currentView = engine.tick(ref, InputEvent.Look)
        render(screen, currentView, history, overlay)

        while (currentView.endGameMessage == null) {
            screen.doResizeIfNecessary()
            val result = readInputKey(screen, currentView, selectionState, overlay)
            selectionState = result.selectionState
            overlay = result.overlay

            when (result.action) {
                is KeyAction.Quit -> break
                is KeyAction.Help -> {
                    addMessage(history, "arrows/wasd: move | l: look | u: activate | g: grab | p: drop | e: equip | r: uneq | j: stories | h: help | q/esc: quit", false)
                    render(screen, currentView, history, overlay)
                    continue
                }
                is KeyAction.NoOp -> {
                    render(screen, currentView, history, overlay)
                    continue
                }
                is KeyAction.WaitSelection -> {
                    val sel = selectionState!!
                    addMessage(history, buildSelectionPrompt(sel.target, sel.items), false)
                    render(screen, currentView, history, overlay)
                    continue
                }
                is KeyAction.Event -> {
                    if (overlay != Overlay.None) overlay = Overlay.None
                    val next = engine.tick(ref, result.action.event)

                    currentView = next
                    if (next.commandText.isNotBlank()) addMessage(history, next.commandText, false)
                    val newTriggerCount = next.triggerTexts.size - shownTriggers
                    for (i in 0 until newTriggerCount) {
                        val triggerText = next.triggerTexts[shownTriggers + i]
                        if (triggerText.isNotBlank()) addMessage(history, triggerText, true)
                    }

                    // Show the newest story message(s) as overlay
                    if (next.storyMessages.size > storedStoryCount) {
                        val subList = next.storyMessages.subList(storedStoryCount, next.storyMessages.size)
                        overlay = Overlay.MessageViewer(subList, focusedIndex = subList.lastIndex)
                        storedStoryCount = next.storyMessages.size
                    } else {
                        storedStoryCount = next.storyMessages.size
                    }

                    shownTriggers = next.triggerTexts.size
                    render(screen, currentView, history, overlay)
                }
            }
        }

        for (i in shownTriggers until currentView.triggerTexts.size) {
            if (currentView.triggerTexts[i].isNotBlank()) addMessage(history, currentView.triggerTexts[i], true)
        }
        storedStoryCount = currentView.storyMessages.size
        if (currentView.endGameMessage != null) {
            addMessage(history, "", false)
            addMessage(history, currentView.endGameMessage, false)
        } else if (currentView.outputLine.isNotBlank()) {
            addMessage(history, currentView.outputLine, false)
        }
        screen.doResizeIfNecessary()
        render(screen, currentView, history, overlay)

        screen.stopScreen()
        printEndSummary(currentView, history)
    }

    private sealed class KeyAction {
        object Quit : KeyAction()
        object Help : KeyAction()
        object WaitSelection : KeyAction()
        object NoOp : KeyAction()
        data class Event(val event: InputEvent) : KeyAction()
    }

    private data class ReadInputResult(val action: KeyAction, val selectionState: SelectionState?, val overlay: Overlay)

    private fun readInputKey(
        screen: Screen,
        view: ViewData,
        selState: SelectionState?,
        currentOverlay: Overlay
    ): ReadInputResult {
        val key = screen.readInput() ?: return ReadInputResult(KeyAction.Quit, null, currentOverlay)

        if (selState != null) {
            return handleSelectionDigit(key, selState, currentOverlay)
        }

        var nextOverlay = currentOverlay
        var handledInOverlay: Boolean

        when (currentOverlay) {
            is Overlay.MessageViewer -> {
                val ov = currentOverlay
                val totalWrappedLines = countWrappedDisplayLines(ov.messages, ov.overlayTextAreaMaxWidth(screen))
                val visibleRows = ov.overlayTextAreaMaxHeight(screen) - 3
                val maxScroll = maxOf(0, totalWrappedLines - visibleRows + 3)
                handledInOverlay = when (key.keyType) {
                    KeyType.PageDown -> {
                        nextOverlay = ov.copy(scrollOffset = minOf(maxScroll, ov.scrollOffset + maxOf(1, ov.overlayTextAreaMaxHeight(screen) - 2)))
                        true
                    }
                    KeyType.ArrowDown -> {
                        nextOverlay = ov.copy(scrollOffset = minOf(maxScroll, ov.scrollOffset + 1))
                        true
                    }
                    KeyType.PageUp -> {
                        nextOverlay = ov.copy(scrollOffset = maxOf(0, ov.scrollOffset - maxOf(1, ov.maxHeight - 2)))
                        true
                    }
                    KeyType.ArrowUp -> {
                        nextOverlay = ov.copy(scrollOffset = maxOf(0, ov.scrollOffset - 1))
                        true
                    }
                    KeyType.ArrowLeft -> {
                        if (ov.focusedIndex > 0)
                            nextOverlay = ov.copy(focusedIndex = ov.focusedIndex - 1, scrollOffset = 0)
                        true
                    }
                    KeyType.ArrowRight -> {
                        if (ov.focusedIndex < ov.messages.size - 1)
                            nextOverlay = ov.copy(focusedIndex = ov.focusedIndex + 1, scrollOffset = 0)
                        true
                    }
                    KeyType.Escape -> {
                        nextOverlay = Overlay.None
                        true
                    }
                    else -> false
                }
            }
            Overlay.None -> handledInOverlay = false
        }

        if (handledInOverlay) return ReadInputResult(KeyAction.NoOp, null, nextOverlay)

        val displayOverlay = if (currentOverlay !is Overlay.None && key.keyType != KeyType.Escape) Overlay.None else currentOverlay

        when (key.keyType) {
            KeyType.Escape -> return if (currentOverlay !is Overlay.None) {
                ReadInputResult(KeyAction.NoOp, null, Overlay.None)
            } else {
                ReadInputResult(KeyAction.Quit, null, currentOverlay)
            }
            KeyType.ArrowUp -> return ReadInputResult(KeyAction.Event(InputEvent.MoveDirection(InputEvent.Direction.North)), null, displayOverlay)
            KeyType.ArrowDown -> return ReadInputResult(KeyAction.Event(InputEvent.MoveDirection(InputEvent.Direction.South)), null, displayOverlay)
            KeyType.ArrowLeft -> return ReadInputResult(KeyAction.Event(InputEvent.MoveDirection(InputEvent.Direction.West)), null, displayOverlay)
            KeyType.ArrowRight -> return ReadInputResult(KeyAction.Event(InputEvent.MoveDirection(InputEvent.Direction.East)), null, displayOverlay)
            KeyType.Character -> {
                when (key.character.lowercaseChar()) {
                    'w' -> return ReadInputResult(KeyAction.Event(InputEvent.MoveDirection(InputEvent.Direction.North)), null, displayOverlay)
                    'a' -> return ReadInputResult(KeyAction.Event(InputEvent.MoveDirection(InputEvent.Direction.West)), null, displayOverlay)
                    's' -> return ReadInputResult(KeyAction.Event(InputEvent.MoveDirection(InputEvent.Direction.South)), null, displayOverlay)
                    'd' -> return ReadInputResult(KeyAction.Event(InputEvent.MoveDirection(InputEvent.Direction.East)), null, displayOverlay)
                    'l' -> return ReadInputResult(KeyAction.Event(InputEvent.Look), null, displayOverlay)
                    'u' -> return ReadInputResult(KeyAction.Event(InputEvent.Activate), null, displayOverlay)
                    'g' -> {
                        val candidates = view.areaItems
                        return if (candidates.isEmpty()) ReadInputResult(KeyAction.Help, null, displayOverlay)
                        else if (candidates.size == 1) ReadInputResult(KeyAction.Event(InputEvent.TakeItem(candidates[0].name)), null, displayOverlay)
                        else ReadInputResult(KeyAction.WaitSelection, SelectionState(SelectionTarget.TAKE, candidates), displayOverlay)
                    }
                    'p' -> {
                        val candidates = view.carriedItems + view.equippedItems
                        return if (candidates.isEmpty()) ReadInputResult(KeyAction.Help, null, displayOverlay)
                        else if (candidates.size == 1) ReadInputResult(KeyAction.Event(InputEvent.DropItem(candidates[0].name)), null, displayOverlay)
                        else ReadInputResult(KeyAction.WaitSelection, SelectionState(SelectionTarget.DROP, candidates), displayOverlay)
                    }
                    'e' -> {
                        val candidates = view.carriedItems
                        return if (candidates.isEmpty()) ReadInputResult(KeyAction.Help, null, displayOverlay)
                        else if (candidates.size == 1) ReadInputResult(KeyAction.Event(InputEvent.EquipItem(candidates[0].name)), null, displayOverlay)
                        else ReadInputResult(KeyAction.WaitSelection, SelectionState(SelectionTarget.EQUIP, candidates), displayOverlay)
                    }
                    'r' -> {
                        val candidates = view.equippedItems
                        return if (candidates.isEmpty()) ReadInputResult(KeyAction.Help, null, displayOverlay)
                        else if (candidates.size == 1) ReadInputResult(KeyAction.Event(InputEvent.UnequipItem(candidates[0].name)), null, displayOverlay)
                        else ReadInputResult(KeyAction.WaitSelection, SelectionState(SelectionTarget.UNEQUIP, candidates), displayOverlay)
                    }
                    'i' -> return ReadInputResult(KeyAction.Event(InputEvent.Inventory), null, displayOverlay)
                    'j' -> {
                        val msgs = view.storyMessages.filter { it.isNotBlank() }
                        return if (msgs.isEmpty()) ReadInputResult(KeyAction.Help, null, displayOverlay)
                        else ReadInputResult(KeyAction.NoOp, null, Overlay.MessageViewer(msgs, focusedIndex = msgs.lastIndex))
                    }
                    'h' -> return ReadInputResult(KeyAction.NoOp, null, displayOverlay)
                    'q' -> return ReadInputResult(KeyAction.Quit, null, displayOverlay)
                }
            }
            else -> {}
        }
        return ReadInputResult(KeyAction.Help, null, currentOverlay)
    }

    private fun handleSelectionDigit(key: com.googlecode.lanterna.input.KeyStroke, selState: SelectionState, overlay: Overlay): ReadInputResult {
        if (key.keyType == KeyType.Escape) return ReadInputResult(KeyAction.Help, null, overlay)
        if (key.keyType != KeyType.Character) return ReadInputResult(KeyAction.WaitSelection, selState, overlay)
        val idx = key.character.digitToIntOrNull() ?: return ReadInputResult(KeyAction.WaitSelection, selState, overlay)
        val itemIdx = idx - 1
        if (itemIdx < 0 || itemIdx >= selState.items.size) return ReadInputResult(KeyAction.Help, null, overlay)
        val item = selState.items[itemIdx]
        val event = when (selState.target) {
            SelectionTarget.TAKE -> InputEvent.TakeItem(item.name)
            SelectionTarget.DROP -> InputEvent.DropItem(item.name)
            SelectionTarget.EQUIP -> InputEvent.EquipItem(item.name)
            SelectionTarget.UNEQUIP -> InputEvent.UnequipItem(item.name)
        }
        return ReadInputResult(KeyAction.Event(event), null, overlay)
    }

    private fun countWrappedDisplayLines(messages: List<String>, lineWidth: Int): Int {
        var total = 0
        for (msg in messages) {
            for (line in msg.split("\n")) {
                val wrappedLines = wrapText(line, lineWidth)
                total += wrappedLines.size
            }
        }
        return total
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

    private fun render(screen: Screen, view: ViewData, history: List<HistoryEntry>, overlay: Overlay) {
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

        // Draw overlay if active
        when (overlay) {
            is Overlay.MessageViewer -> {
                drawMessageOverlay(screen, g, view, overlay, size)
                screen.refresh()
                return
            }
            else -> {}
        }

        val leftWidth = ((w - 3) * 0.6).toInt()
        val rightWidth = (w - 3) - leftWidth
        val splitCol = 1 + leftWidth
        val panelStartRow = 3

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

    private fun drawMessageOverlay(
        screen: Screen,
        g: TextGraphics,
        view: ViewData,
        overlay: Overlay.MessageViewer,
        size: com.googlecode.lanterna.TerminalSize
    ) {
        val w = size.columns
        val h = size.rows
        val panelW = minOf(170, maxOf(40, w - 10))
        val panelH = overlay.maxHeight.coerceAtMost(h - 6)
        val startCol = (w - panelW) / 2 + 1
        val startRow = (h - panelH) / 2
        val textAreaWidth = overlay.overlayTextAreaMaxWidth(screen)

        drawBorder(g, startCol - 1, startRow - 1, startCol + panelW, startRow + panelH + 1)

        val title = " Story Message"
        g.foregroundColor = TextColor.ANSI.CYAN
        g.enableModifiers(SGR.BOLD)
        g.putString(startCol, startRow, title)
        g.disableModifiers(SGR.BOLD)
        g.foregroundColor = TextColor.ANSI.DEFAULT

        val contentTop = startRow + 2
        val focusedMsg = overlay.messages.getOrElse(overlay.focusedIndex) { "" }
        var rowIdx = 0
        for (paragraph in focusedMsg.split("\n")) {
            if (paragraph.isBlank()) {
                if (rowIdx < overlay.scrollOffset) { rowIdx++; continue }
                if (rowIdx >= overlay.scrollOffset + panelH - 3) break
                val drawRow = contentTop + (rowIdx - overlay.scrollOffset)
                if (drawRow < startRow + panelH - 1) {
                    g.putString(startCol, drawRow, "".padEnd(textAreaWidth))
                }
                rowIdx++
            } else {
                for (line in wrapText(paragraph, textAreaWidth)) {
                    if (rowIdx < overlay.scrollOffset) { rowIdx++; continue }
                    if (rowIdx >= overlay.scrollOffset + panelH - 3) break
                    val drawRow = contentTop + (rowIdx - overlay.scrollOffset)
                    if (drawRow < startRow + panelH - 1) {
                        g.putString(startCol, drawRow, line.padEnd(textAreaWidth))
                    }
                    rowIdx++
                }
            }
        }

        val totalCount = countWrappedDisplayLines(listOf(focusedMsg), textAreaWidth)
        val canScrollUp = overlay.scrollOffset > 0
        val canScrollDown = (panelH - 3 > 0) && (overlay.scrollOffset + panelH - 3 < totalCount)
        g.putString(startCol, startRow + panelH - 1, buildString {
            append(" [PgUp:up PgDn:down ")
            if (canScrollUp || canScrollDown) append(" | ")
            if (canScrollUp) append("scroll↑ ")
            if (canScrollDown) append("scroll↓ ")
            append("| ${overlay.focusedIndex + 1} / ${overlay.messages.size} esc:close]")
        })

        g.foregroundColor = TextColor.ANSI.DEFAULT
    }


    private fun drawBorder(g: TextGraphics, left: Int, top: Int, right: Int, bottom: Int) {
        g.setCharacter(left, top, '┌')
        g.setCharacter(right - 1, top, '┐')
        g.setCharacter(left, bottom - 1, '└')
        g.setCharacter(right - 1, bottom - 1, '┘')
        for (col in left + 1 until right - 1) {
            g.setCharacter(col, top, '─')
            g.setCharacter(col, bottom - 1, '─')
        }
        for (row in top + 1 until bottom - 1) {
            g.setCharacter(left, row, '│')
            g.setCharacter(right - 1, row, '│')
        }
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
            val label = "@ ${item.name}" + if (item.locked) " (Locked)" else ""
            g.foregroundColor = if (item.locked) TextColor.ANSI.RED else TextColor.ANSI.MAGENTA
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
        val wInfo = exits.getOrNull(0)
        val aInfo = exits.getOrNull(1)
        val sInfo = exits.getOrNull(2)
        val dInfo = exits.getOrNull(3)

        val wLabel = exitLabel('w', wInfo)
        val aLabel = exitLabel('a', aInfo)
        val sLabel = exitLabel('s', sInfo)
        val dLabel = exitLabel('d', dInfo)

        val gap = 2
        val contentWidth = aLabel.length + gap + sLabel.length + gap + dLabel.length
        val leftPad = (width - contentWidth) / 2

        val colA = leftPad
        val colS = colA + aLabel.length + gap
        val colD = colS + sLabel.length + gap
        val colW = colS - wLabel.length / 2

        drawSlot(g, wLabel, TerminalPosition(colW, topRow), wInfo != null, wInfo?.blocked ?: false)
        drawInvHint(g, width, topRow)
        drawSlot(g, aLabel, TerminalPosition(colA, topRow + 1), aInfo != null, aInfo?.blocked ?: false)
        drawSlot(g, sLabel, TerminalPosition(colS, topRow + 1), sInfo != null, sInfo?.blocked ?: false)
        drawSlot(g, dLabel, TerminalPosition(colD, topRow + 1), dInfo != null, dInfo?.blocked ?: false)
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

        val gLabel = "[g] grab"
        val pLabel = "[p] drop"
        val eLabel = "[e] equip"
        val rLabel = "[r] uneq"

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

    private fun exitLabel(key: Char, info: ExitInfo?): String =
        if (info == null || info.name.isNullOrEmpty()) "[${key}]  ."
        else {
            val suffix = if (info.blocked) " (B)" else ""
            "[${key}] ${info.name}$suffix"
        }

    private fun drawSlot(g: TextGraphics, label: String, pos: TerminalPosition, active: Boolean, blocked: Boolean) {
        g.foregroundColor = when {
            !active -> TextColor.ANSI.DEFAULT
            blocked -> TextColor.ANSI.DEFAULT
            else -> TextColor.ANSI.GREEN
        }
        g.enableModifiers(SGR.BOLD)
        val keyPart = label.substringBefore(']') + "]"
        g.putString(pos.column, pos.row, keyPart)
        g.disableModifiers(SGR.BOLD)

        val nameStart = pos.column + keyPart.length
        val namePart = if (label.contains("] ")) label.substringAfter("] ") else ""
        g.foregroundColor = when {
            !active -> TextColor.ANSI.DEFAULT
            blocked -> TextColor.ANSI.DEFAULT
            else -> TextColor.ANSI.GREEN
        }
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
        println()
        if (view.endGameMessage != null) {
            println("===== ${view.endGameMessage} =====")
        } else {
            println("===== Game Over =====")
        }
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
