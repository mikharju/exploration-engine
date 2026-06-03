package exploration.adapter.ui.libgdx

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import exploration.port.ItemView

object UiUtils {

    fun wrapText(text: String, maxCharsPerLine: Int): List<String> {
        return text.split("\n").flatMap { line ->
            if (line.length <= maxCharsPerLine) {
                listOf(line)
            } else {
                val words = line.split(" ")
                val lines = mutableListOf<String>()
                var currentLine = ""
                
                for (word in words) {
                    if ((currentLine + " " + word).length > maxCharsPerLine && currentLine.isNotEmpty()) {
                        lines.add(currentLine.trim())
                        currentLine = word
                    } else {
                        currentLine = if (currentLine.isEmpty()) word else "$currentLine $word"
                    }
                }
                
                if (currentLine.isNotEmpty()) {
                    lines.add(currentLine.trim())
                }
                
                lines
            }
        }
    }

    fun countWrappedDisplayLines(text: String, maxCharsPerLine: Int): Int = wrapText(text, maxCharsPerLine).size

    fun buildHpBarString(hp: Int, maxHp: Int): String {
        val filledWidth = if (maxHp > 0) (hp * 20 / maxHp) else 0
        return "[HP:${"|".repeat(filledWidth)}${" ".repeat(20 - filledWidth)}$hp/$maxHp]"
    }

    fun formatStatus(status: String, value: Int, bounds: Map<String, ClosedRange<Int>> = emptyMap()): String {
        val range = bounds[status]
        return if (range != null) {
            when {
                value < range.start -> "$status: ${value} [LOW]"
                value > range.endInclusive -> "$status: ${value} [HIGH]"
                else -> "$status: $value"
            }
        } else {
            "$status: $value"
        }
    }

    fun buildExitLabel(exitName: String?, blocked: Boolean): String = when {
        exitName == null || blocked -> "[BLOCKED]"
        else -> "[$exitName]"
    }

    fun buildSelectionPrompt(target: SelectionTarget, items: List<ItemView>): String {
        val actionVerb = when (target) {
            SelectionTarget.TAKE -> "grab"
            SelectionTarget.DROP -> "drop"
            SelectionTarget.EQUIP -> "equip"
            SelectionTarget.UNEQUIP -> "unequip"
        }
        
        return buildString {
            append("Which ")
            append(actionVerb)
            append("? Enter number (1-")
            append(items.size)
            append("): \n")
            
            for ((index, item) in items.withIndex()) {
                append("${index + 1}. ${item.name}")
                if (item.description.isNotEmpty()) {
                    append(" - ")
                    append(item.description.take(30))
                }
                append("\n")
            }
        }
    }

    fun formatItemDisplay(item: ItemView, equipped: Boolean = false): String {
        val prefix = if (equipped) "★" else "•"
        return "$prefix ${item.name}" + if (item.locked) " [LOCKED]" else ""
    }

    fun buildStatusPanelContent(health: Int, maxHealth: Int, exploredCount: Int, totalAreas: Int,
                               activatedCount: Int, totalDevices: Int, statuses: Map<String, Int>,
                               statusBounds: Map<String, ClosedRange<Int>>,
                               areaItems: List<ItemView>, carriedItems: List<ItemView>, equippedItems: List<ItemView>): String {
        val sb = StringBuilder()
        
        // Health bar
        sb.append(buildHpBarString(health, maxHealth))
        sb.append("\n\n")
        
        // Stats
        sb.append("Areas: $exploredCount/$totalAreas | Devices: $activatedCount/$totalDevices\n\n")
        
        // Statuses
        if (statuses.isNotEmpty()) {
            sb.append("Statuses:\n")
            for ((status, value) in statuses.entries.sortedBy { it.key }) {
                sb.append("  ").append(formatStatus(status, value, statusBounds)).append("\n")
            }
            sb.append("\n")
        }
        
        // Items hint
        if (equippedItems.isNotEmpty()) {
            sb.append("Equipped: ${equippedItems.joinToString(", ") { it.name }}\n")
        }
        if (carriedItems.isNotEmpty() || equippedItems.isNotEmpty()) {
            sb.append("\nEnter 'p' to drop, 'e' to equip\n")
        } else if (areaItems.isNotEmpty()) {
            sb.append("Enter 'g' to grab items here\n")
        }
        
        return sb.toString()
    }

    fun formatItemDisplayWithColor(item: ItemView): Color = when {
        "weapon" in item.name.lowercase() -> Color(0.9f, 0.3f, 0.3f, 1f) // Red for weapons
        "armor" in item.name.lowercase() || "shield" in item.name.lowercase() -> Color(0.2f, 0.4f, 0.9f, 1f) // Blue for armor
        else -> Color.WHITE.cpy().let { it.a = 0.9f; it }
    }
}
