package exploration.adapter.ui.lanterna

import exploration.core.model.StatusRange
import exploration.port.ExitInfo
import exploration.port.ItemView
import exploration.port.ViewData

object UiUtils {

    fun wrapText(text: String, width: Int): List<String> {
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

    fun countWrappedDisplayLines(messages: List<String>, lineWidth: Int): Int {
        var total = 0
        for (msg in messages) {
            for (line in msg.split("\n")) {
                val wrappedLines = wrapText(line, lineWidth)
                total += wrappedLines.size
            }
        }
        return total
    }

    fun buildHpBar(v: ViewData, width: Int): String {
        val barLen = 10
        val filled = ((v.health.toDouble() / v.maxHealth) * barLen).toInt().coerceIn(0, barLen)
        val empty = barLen - filled
        val bar = "█".repeat(filled) + "░".repeat(empty)
        val textPart = "${v.health}/${v.maxHealth}"
        return "$bar $textPart"
    }

    fun formatStatus(name: String, value: Int, range: StatusRange?, width: Int): String {
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

    fun exitLabel(key: Char, info: ExitInfo?): String =
        if (info == null || info.name.isNullOrEmpty()) "[${key}]  ."
        else {
            val suffix = if (info.blocked) " (B)" else ""
            "[${key}] ${info.name}$suffix"
        }

    fun buildSelectionPrompt(target: SelectionTarget, items: List<ItemView>): String {
        val verb = when (target) {
            SelectionTarget.TAKE -> "Take"
            SelectionTarget.DROP -> "Drop"
            SelectionTarget.EQUIP -> "Equip"
            SelectionTarget.UNEQUIP -> "Unequip"
        }
        val options = items.mapIndexed { i, it -> "[${i + 1}] ${it.name}" }.joinToString(" ")
        return "$verb what? $options"
    }

    private const val MIN_WIDTH = 64
    private const val MIN_HEIGHT = 23

    fun isTerminalTooSmall(w: Int, h: Int): Boolean = h < MIN_HEIGHT || w < MIN_WIDTH

    fun calcOverlayWidth(terminalColumns: Int): Int = minOf(170, maxOf(40, terminalColumns - 10)) - 2
    fun calcOverlayMaxHeight(maxHeight: Int, terminalRows: Int): Int = (maxHeight + 3).coerceAtMost(terminalRows - 6)
}
