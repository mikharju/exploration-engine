package exploration.adapter.ui.lanterna

import com.googlecode.lanterna.SGR
import com.googlecode.lanterna.TerminalPosition
import com.googlecode.lanterna.TextColor
import com.googlecode.lanterna.graphics.TextGraphics
import com.googlecode.lanterna.screen.Screen
import com.googlecode.lanterna.screen.TerminalScreen
import com.googlecode.lanterna.terminal.DefaultTerminalFactory
import exploration.port.*

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
    }
}

enum class SelectionTarget { TAKE, DROP, EQUIP, UNEQUIP }
data class SelectionState(val target: SelectionTarget, val items: List<ItemView>)

class LanternaUiAdapter(private val engine: GameEngine) {
    private val keyMapper = LanternaKeyMapper()

    companion object {
        private const val MAX_MESSAGES = 10
        private const val MIN_WIDTH = 64
        private const val MIN_HEIGHT = 23
    }

    private data class LoopState(
        var history: MutableList<HistoryEntry>,
        var currentView: ViewData,
        var shownTriggers: Int = 0,
        var storedStoryCount: Int = 0,
        var selectionState: SelectionState? = null,
        var overlay: Overlay = Overlay.None
    )

    fun run(scenarioId: String) {
        val ref = startEngine(scenarioId) ?: return
        val screen = setupScreen()

        val state = LoopState(
            history = mutableListOf(),
            currentView = engine.tick(ref, InputEvent.Look),
        )

        addMessage(state.history, "=== Exploration Engine (Lanterna) ===", false)
        addMessage(
            state.history,
            "arrows/wasd: move | l: look | u: activate | g: grab | p: drop | e: equip | r: uneq | j: stories | h: help | q/esc: quit",
            false
        )

        initFirstView(screen, state)
        gameLoop(screen, ref, state)
        endGameScreen(screen, state)

        screen.stopScreen()
        printEndSummary(state.currentView, state.history)
    }

    private fun startEngine(scenarioId: String): GameRef? {
        try {
            return engine.start(scenarioId)
        } catch (e: Exception) {
            println("Error loading scenario '$scenarioId': ${e.message}")
            e.printStackTrace()
            return null
        }
    }

    private fun setupScreen(): Screen {
        val screen = TerminalScreen(DefaultTerminalFactory().createTerminal())
        screen.stopScreen()
        screen.startScreen()
        screen.cursorPosition = null
        return screen
    }

    private fun initFirstView(screen: Screen, state: LoopState) {
        render(screen, state.currentView, state.history, state.overlay)

        val initialStoryCount = state.currentView.storyMessages.size
        state.storedStoryCount = initialStoryCount
        if (initialStoryCount > 0) {
            val subList = state.currentView.storyMessages.subList(0, initialStoryCount)
            state.overlay = Overlay.MessageViewer(subList, focusedIndex = subList.lastIndex)
            render(screen, state.currentView, state.history, state.overlay)
        }
    }

    private fun gameLoop(screen: Screen, ref: GameRef, state: LoopState) {
        while (state.currentView.endGameMessage == null) {
            screen.doResizeIfNecessary()
            val key = screen.readInput() ?: break

            val result = keyMapper.mapKey(key, state.currentView, state.selectionState, state.overlay, screen.terminalSize.columns, screen.terminalSize.rows)
            state.selectionState = result.selectionState
            state.overlay = result.overlay

            when (result.action) {
                is KeyAction.Quit -> break
                is KeyAction.Help -> {
                    addMessage(state.history, "arrows/wasd: move | l: look | u: activate | g: grab | p: drop | e: equip | r: uneq | j: stories | h: help | q/esc: quit", false)
                    render(screen, state.currentView, state.history, state.overlay)
                }
                is KeyAction.NoOp -> {
                    render(screen, state.currentView, state.history, state.overlay)
                }
                is KeyAction.WaitSelection -> {
                    val sel = state.selectionState!!
                    addMessage(state.history, UiUtils.buildSelectionPrompt(sel.target, sel.items), false)
                    render(screen, state.currentView, state.history, state.overlay)
                }
                is KeyAction.Event -> {
                    if (state.overlay != Overlay.None) state.overlay = Overlay.None
                    val next = engine.tick(ref, result.action.event)

                    state.currentView = next
                    if (next.commandText.isNotBlank()) addMessage(state.history, next.commandText, false)
                    appendTriggerMessages(state)

                    if (next.storyMessages.size > state.storedStoryCount) {
                        val subList = next.storyMessages.subList(state.storedStoryCount, next.storyMessages.size)
                        state.overlay = Overlay.MessageViewer(subList, focusedIndex = subList.lastIndex)
                        state.storedStoryCount = next.storyMessages.size
                    } else {
                        state.storedStoryCount = next.storyMessages.size
                    }

                    state.shownTriggers = next.triggerTexts.size
                    render(screen, state.currentView, state.history, state.overlay)
                }
            }
        }
    }

    private fun appendTriggerMessages(state: LoopState) {
        val newTriggerCount = state.currentView.triggerTexts.size - state.shownTriggers
        for (i in 0 until newTriggerCount) {
            val triggerText = state.currentView.triggerTexts[state.shownTriggers + i]
            if (triggerText.isNotBlank()) addMessage(state.history, triggerText, true)
        }
    }

    private fun endGameScreen(screen: Screen, state: LoopState) {
        for (i in state.shownTriggers until state.currentView.triggerTexts.size) {
            if (state.currentView.triggerTexts[i].isNotBlank()) addMessage(state.history, state.currentView.triggerTexts[i], true)
        }
        state.storedStoryCount = state.currentView.storyMessages.size
        val endMsg = state.currentView.endGameMessage
        if (endMsg != null) {
            addMessage(state.history, "", false)
            addMessage(state.history, endMsg, false)
        } else if (state.currentView.outputLine.isNotBlank()) {
            addMessage(state.history, state.currentView.outputLine, false)
        }
        screen.doResizeIfNecessary()
        render(screen, state.currentView, state.history, state.overlay)
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
                drawMessageOverlay(screen, g, overlay, size)
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
                for (line in UiUtils.wrapText(paragraph, textAreaWidth)) {
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

        val totalCount = UiUtils.countWrappedDisplayLines(listOf(focusedMsg), textAreaWidth)
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
                UiUtils.wrapText(segment, width).forEach { lines.add(it to entry.isTrigger) }
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
        val hpLine = UiUtils.buildHpBar(v, width)
        g.putString(top.column, top.row, hpLine.padEnd(width))

        val progLine = " Exp: ${v.exploredCount}/${v.totalAreas}   Dev: ${v.activatedCount}/${v.totalDevices}"
        g.putString(top.column, top.row + 1, progLine.padEnd(width))

        var row = top.row + 2

        val statuses = v.statuses.filterValues { it != 0 }
        for ((name, value) in statuses) {
            if (row >= top.row + maxRows) break
            val range = v.statusBounds[name]
            g.putString(top.column, row, UiUtils.formatStatus(name, value, range, width).padEnd(width))
            row++
        }

        if (v.equippedItems.isEmpty()) return

        if (row < top.row + maxRows && statuses.isNotEmpty()) {
            g.putString(top.column, row, " ".repeat(width))
            row++
        }

        for ((_, item) in v.equippedItems.withIndex()) {
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

    private fun drawBottomBar(
        g: TextGraphics,
        view: ViewData,
        width: Int,
        topRow: Int
    ) {
        val exits = view.exits
        val wInfo = exits[InputEvent.Direction.North]
        val aInfo = exits[InputEvent.Direction.West]
        val sInfo = exits[InputEvent.Direction.South]
        val dInfo = exits[InputEvent.Direction.East]

        val wLabel = UiUtils.exitLabel('w', wInfo)
        val aLabel = UiUtils.exitLabel('a', aInfo)
        val sLabel = UiUtils.exitLabel('s', sInfo)
        val dLabel = UiUtils.exitLabel('d', dInfo)

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
