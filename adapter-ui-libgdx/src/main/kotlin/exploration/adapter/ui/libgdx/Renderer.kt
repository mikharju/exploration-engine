package exploration.adapter.ui.libgdx

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import exploration.model.Direction
import exploration.port.ItemView
import exploration.port.ViewData

/** Renders the game UI using SpriteBatch and ShapeRenderer. */
class Renderer(
    private val batch: SpriteBatch,
    private val shapeRenderer: ShapeRenderer,
    private val camera: OrthographicCamera,
    private val viewportW: Float,
    private val viewportH: Float
) {

    companion object {
        val BG_COLOR = Color(0.1f, 0.12f, 0.18f, 1f)
        val PANEL_BG = Color(0.05f, 0.06f, 0.09f, 0.85f)
        val BORDER_COLOR = Color(0.25f, 0.3f, 0.4f, 1f)
        val TEXT_WHITE = Color(0.9f, 0.9f, 0.9f, 1f)
        val TEXT_DIM = Color(0.4f, 0.45f, 0.55f, 1f)
        val TEXT_HIGHLIGHT = Color(0.3f, 0.7f, 1f, 1f)
        val HP_GREEN = Color(0.2f, 0.8f, 0.3f, 1f)
        val HP_YELLOW = Color(0.9f, 0.7f, 0.1f, 1f)
        val HP_RED = Color(0.85f, 0.2f, 0.2f, 1f)
        val BUTTON_BG = Color(0.15f, 0.2f, 0.3f, 0.9f)
        val BUTTON_LOCKED = Color(0.12f, 0.12f, 0.18f, 0.7f)

        const val MARGIN = 40f
        const val PANEL_PADDING = 20f
        const val STATUS_BAR_WIDTH = 180f
        const val BOTTOM_BAR_HEIGHT = 180f
        const val DIRECTION_BTN_SIZE = 70f
    }

    private val font: BitmapFont = try {
        BitmapFont(Gdx.files.internal("com/badlogic/gdx/utils/lsans-15.fnt"))
    } catch (e: Exception) {
        BitmapFont()
    }
    init { font.color = TEXT_WHITE }

    private val layout = GlyphLayout()
    private fun textWidth(text: String): Float { layout.setText(font, text); return layout.width }

    fun render(viewData: ViewData, selectionState: SelectionState = SelectionState.inactive()) {
        batch.setProjectionMatrix(camera.combined)
        shapeRenderer.setProjectionMatrix(camera.combined)
        drawBackgroundPass()
        drawMessagePanelPass(viewData.triggerTexts, viewData.storyMessages, viewData.currentAreaName, viewData.areaDescription, viewData.messageHistory)
        drawStatusPanelPass(viewData.health, viewData.maxHealth, viewData.exploredCount,
            viewData.totalAreas, viewData.activatedCount, viewData.totalDevices,
            viewData.statuses, viewData.areaItems, viewData.carriedItems, viewData.equippedItems)
        drawBottomBarPass(viewData.exits, viewData.areaItems, viewData.carriedItems, viewData.equippedItems)

        batch.begin()
        font.draw(batch, "WASD/Arrows: Move | L: Look | U: Activate | I: Inventory", 20f, viewportH - 15f)
        if (viewData.areaItems.isNotEmpty()) {
            val itemsStr = viewData.areaItems.joinToString(", ") { it.name }
            font.draw(batch, "Items here: $itemsStr", 20f, viewportH / 2 + 60f)
        }

        if (selectionState.active && selectionState.target != null) {
            val prompt = when (selectionState.target) {
                SelectionTarget.UNEQUIP -> "Unequip which item? (1-${selectionState.items.size})"
                SelectionTarget.DROP -> "Drop which item? (1-${selectionState.items.size})"
                SelectionTarget.TAKE -> "Take which item? (1-${selectionState.items.size})"
                SelectionTarget.EQUIP -> "Equip which item? (1-${selectionState.items.size})"
            }
            font.color = TEXT_HIGHLIGHT
            font.draw(batch, prompt, viewportW / 2f - textWidth(prompt) / 2f, viewportH * 0.65f)

            val startY = viewportH * 0.55f
            for ((i, item) in selectionState.items.withIndex()) {
                val y = startY - i * font.lineHeight * 1.4f
                if (y < BOTTOM_BAR_HEIGHT + 20f) break
                val label = "${i + 1}. ${item.name}"
                font.color = TEXT_WHITE
                font.draw(batch, label, viewportW / 2f - textWidth(label) / 2f, y)
            }
        }

        batch.end()

        viewData.endGameMessage?.let { drawGameOverOverlay(it) }
    }

    fun renderWithOverlay(viewData: ViewData, overlayName: String, selectionState: SelectionState = SelectionState.inactive()) {
        batch.setProjectionMatrix(camera.combined)
        shapeRenderer.setProjectionMatrix(camera.combined)
        val withTrigger = viewData.copy(triggerTexts = listOf("[${overlayName}]"))
        render(withTrigger, selectionState)
    }

    fun renderStoryViewer(viewData: ViewData, stories: List<String>, selectionState: SelectionState = SelectionState.inactive()) {
        batch.setProjectionMatrix(camera.combined)
        shapeRenderer.setProjectionMatrix(camera.combined)
        drawBackgroundPass()

        val boxW = 700f; val boxH = 500f
        val x = (viewportW - boxW) / 2; val y = (viewportH - boxH) / 2

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        shapeRenderer.setColor(Color(PANEL_BG.r, PANEL_BG.g, PANEL_BG.b, 1f)); shapeRenderer.rect(x, y, boxW, boxH)
        shapeRenderer.end()

        batch.begin()
        font.color = TEXT_HIGHLIGHT
        val title = " Story Messages"
        font.draw(batch, title, x + (boxW - textWidth(title)) / 2f, y + boxH - MARGIN)

        var storyY = y + boxH * 0.75f
        val maxWidthPx = (boxW - MARGIN * 2).toInt()
        val maxCharsPerLine = if (maxWidthPx > 0) {
            val testStr = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
            val testWidth = textWidth(testStr)
            ((testStr.length.toFloat() / testWidth) * maxWidthPx).toInt().coerceIn(1, 200)
        } else 40

        val maxStoriesToShow = 50
        val storiesToRender = if (stories.size > maxStoriesToShow) stories.takeLast(maxStoriesToShow) else stories
        for ((i, story) in storiesToRender.withIndex()) {
            if (story.isBlank()) continue
            font.color = TEXT_WHITE
            val segments = story.split("\n")
            var isFirstSegment = true
            for (segment in segments) {
                if (segment.isBlank()) {
                    storyY -= font.lineHeight * 0.5f
                    isFirstSegment = false
                    continue
                }
                val wrappedLines = splitText(segment, maxCharsPerLine)
                for ((lineIdx, line) in wrappedLines.withIndex()) {
                    if (storyY < y + MARGIN + font.lineHeight) break
                    val displayLine = if (isFirstSegment && lineIdx == 0) "${i + 1}. $line" else line
                    font.draw(batch, displayLine, x + MARGIN, storyY)
                    storyY -= font.lineHeight * 1.3f
                }
                isFirstSegment = false
            }
            storyY -= font.lineHeight * 0.5f
        }

        font.color = TEXT_DIM
        val hint = "Press ESC to close"
        font.draw(batch, hint, x + (boxW - textWidth(hint)) / 2f, y + MARGIN)
        batch.end()
    }

    fun renderGameOver(message: String) {
        drawBackgroundPass()

        val boxW = 600f; val boxH = 300f
        val x = (viewportW - boxW) / 2; val y = (viewportH - boxH) / 2

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        shapeRenderer.setColor(PANEL_BG); shapeRenderer.rect(x, y, boxW, boxH)
        shapeRenderer.end()

        batch.begin()
        font.color = TEXT_HIGHLIGHT
        font.draw(batch, "Game Over", x + (boxW - textWidth("Game Over")) / 2f, y + boxH - MARGIN)
        font.color = TEXT_WHITE
        val lines = splitText(message, (viewportW * 0.8f).toInt())
        for ((i, line) in lines.withIndex()) {
            val ly = y + boxH * 0.55f - (lines.size - 1 - i) * font.lineHeight
            font.draw(batch, line, x + MARGIN, ly)
        }
        batch.end()
    }

    private fun drawBackgroundPass() {
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        shapeRenderer.setColor(BG_COLOR); shapeRenderer.rect(0f, 0f, viewportW, viewportH)
        shapeRenderer.end()
    }

    private fun drawMessagePanelPass(triggers: List<String>, stories: List<String>, areaName: String?, areaDescription: String?, messageHistory: List<String>) {
        val filteredTriggers = if (areaDescription != null && triggers.contains(areaDescription)) {
            triggers.filterNot { it == areaDescription }
        } else {
            triggers
        }

        val panelX = MARGIN; val panelBottomY = viewportH - BOTTOM_BAR_HEIGHT - PANEL_PADDING * 2
        val panelW = viewportW / 2.5f; val panelH = viewportH - BOTTOM_BAR_HEIGHT - MARGIN * 2 - PANEL_PADDING * 4
        val panelTopY = panelBottomY - panelH

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        shapeRenderer.setColor(PANEL_BG); shapeRenderer.rect(panelX, panelBottomY - panelH + PANEL_PADDING * 2, panelW, panelH)
        shapeRenderer.end()

        batch.begin()

        val maxMessages = 50

        val allItems = mutableListOf<String>()
        for (trigger in filteredTriggers) {
            allItems.add(trigger)
        }
        for (msg in messageHistory) {
            allItems.add(msg)
        }
        if (allItems.size > maxMessages) {
            allItems.subList(0, allItems.size - maxMessages).clear()
        }
        allItems.reverse()

        val areaLabelY = panelBottomY - font.lineHeight / 2
        if (areaName != null && areaName.isNotBlank()) {
            font.color = TEXT_HIGHLIGHT; font.draw(batch, "Area: $areaName", panelX + PANEL_PADDING, areaLabelY)
        }

        var y = areaLabelY - font.lineHeight * 1.3f
        val minY = panelTopY + PANEL_PADDING

        val maxCharsForTriggers = (panelW - PANEL_PADDING * 2).toInt() / 6.coerceAtLeast(1)
        for (text in allItems) {
            if (y < minY) break
            font.color = TEXT_WHITE
            val segments = text.split("\n")
            var isFirstSegment = true
            for (segment in segments) {
                if (segment.isBlank()) {
                    y -= font.lineHeight * 0.5f
                    isFirstSegment = false
                    continue
                }
                val wrappedLines = splitText(segment, maxCharsForTriggers)
                for ((lineIdx, line) in wrappedLines.withIndex()) {
                    if (y < minY) break
                    font.draw(batch, line, panelX + PANEL_PADDING, y)
                    y -= font.lineHeight * 1.3f
                }
                isFirstSegment = false
            }
        }

        if (allItems.isEmpty()) {
            val hintY = areaLabelY - font.lineHeight * 2
            if (hintY > minY) { font.color = TEXT_DIM; font.draw(batch, "Press L to look around", panelX + PANEL_PADDING, hintY) }
        }
        batch.end()
    }

    private fun drawStatusPanelPass(health: Int, maxHealth: Int, exploredCount: Int, totalAreas: Int, activatedCount: Int, totalDevices: Int, statuses: Map<String, Int>, areaItems: List<ItemView>, carriedItems: List<ItemView>, equippedItems: List<ItemView>) {
        val panelX = MARGIN + viewportW / 2.5f + MARGIN * 0.8f
        val panelBottomY = viewportH - BOTTOM_BAR_HEIGHT - PANEL_PADDING * 2
        val panelW = viewportW / 3f; val panelH = viewportH - BOTTOM_BAR_HEIGHT - MARGIN * 2 - PANEL_PADDING * 4
        val panelTopY = panelBottomY - panelH

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        shapeRenderer.setColor(PANEL_BG); shapeRenderer.rect(panelX, panelBottomY - panelH + PANEL_PADDING * 2, panelW, panelH)
        shapeRenderer.end()

        var y = panelBottomY - font.lineHeight / 2
        val minY = panelTopY + PANEL_PADDING
        val healthPct = if (maxHealth > 0) health.toFloat() / maxHealth else 0f
        val hpColor = when { healthPct > 0.6f -> HP_GREEN; healthPct > 0.3f -> HP_YELLOW; else -> HP_RED }

        batch.begin()
        font.color = TEXT_WHITE
        val hpLabel = "HP: $health/$maxHealth"
        font.draw(batch, hpLabel, panelX + PANEL_PADDING, y)
        batch.end()
        y -= font.lineHeight * 0.7f

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        val barX = panelX + PANEL_PADDING + textWidth(hpLabel) + 10
        shapeRenderer.setColor(Color(0.15f, 0.15f, 0.2f, 1f)); shapeRenderer.rect(barX, y - font.lineHeight * 0.7f, STATUS_BAR_WIDTH, 8f)
        shapeRenderer.setColor(hpColor); shapeRenderer.rect(barX, y - font.lineHeight * 0.7f, STATUS_BAR_WIDTH * healthPct, 8f)
        shapeRenderer.end()

        y -= font.lineHeight * 2f

        batch.begin()
        font.color = TEXT_DIM; font.draw(batch, "Explored: $exploredCount/$totalAreas | Devices: $activatedCount/$totalDevices", panelX + PANEL_PADDING, y)

        if (carriedItems.isNotEmpty()) {
            font.color = TEXT_HIGHLIGHT
            y -= font.lineHeight * 2.5f
            font.draw(batch, "Carried:", panelX + PANEL_PADDING, y)
            for ((i, item) in carriedItems.withIndex()) {
                y -= font.lineHeight * 1.3f
                if (y < minY) break
                font.color = if (item.locked) TEXT_DIM else TEXT_WHITE
                val lockedStr = if (item.locked) " [locked]" else ""
                font.draw(batch, "${i + 1}. ${item.name}$lockedStr", panelX + PANEL_PADDING, y)
            }
        }

        if (equippedItems.isNotEmpty()) {
            y -= font.lineHeight * 2f
            font.color = TEXT_HIGHLIGHT; font.draw(batch, "Equipped:", panelX + PANEL_PADDING, y)
            for ((i, item) in equippedItems.withIndex()) {
                y -= font.lineHeight * 1.3f
                if (y < minY) break
                font.color = TEXT_HIGHLIGHT; val lockedStr = if (item.locked) " [locked]" else ""
                font.draw(batch, "${i + 1}. ${item.name}$lockedStr", panelX + PANEL_PADDING, y)
            }
        }
        batch.end()
    }

    private fun drawBottomBarPass(exits: Map<Direction, exploration.port.ExitInfo?>, areaItems: List<ItemView>, carriedItems: List<ItemView>, equippedItems: List<ItemView>) {
        val barY = 0f; val barH = BOTTOM_BAR_HEIGHT

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        shapeRenderer.setColor(PANEL_BG); shapeRenderer.rect(0f, barY, viewportW, barH)

        val centerX = viewportW / 2; val buttonY = barY + DIRECTION_BTN_SIZE / 2 + 10f
        val radius = DIRECTION_BTN_SIZE / 2; val spacing = DIRECTION_BTN_SIZE * 1.3f

        // WASD-style layout: W above, S/A/D on same row
        val southRowY = buttonY
        val northY = buttonY + spacing * 0.9f
        val buttonPositions = listOf(
            Direction.North to (centerX to (northY)),
            Direction.West to ((centerX - spacing) to (southRowY)),
            Direction.South to (centerX to (southRowY)),
            Direction.East to ((centerX + spacing) to (southRowY))
        )

        for ((dir, pos) in buttonPositions) {
            val exitInfo = exits[dir]; val locked = exitInfo == null || exitInfo.blocked
            shapeRenderer.setColor(if (locked) BUTTON_LOCKED else BUTTON_BG); shapeRenderer.circle(pos.first, pos.second, radius)
            if (!locked) {
                shapeRenderer.end(); shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
                shapeRenderer.setColor(BORDER_COLOR); shapeRenderer.circle(pos.first, pos.second, radius)
                shapeRenderer.end(); shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
            }
        }
        shapeRenderer.end()

        batch.begin()
        font.color = TEXT_DIM
        val hintText = "g: Take | p: Drop | e: Equip | r: Unequip"
        font.draw(batch, hintText, viewportW / 2f - textWidth(hintText) / 2f, barH - 15f)

        for ((dir, pos) in buttonPositions) {
            val exitInfo = exits[dir]; val locked = exitInfo == null || exitInfo.blocked
            font.color = if (locked) Color(0.3f, 0.25f, 0.25f, 1f) else TEXT_WHITE
            val arrow = when(dir) { Direction.North -> "^"; Direction.South -> "v"; Direction.West -> "<"; Direction.East -> ">" }
            val lockText = if (locked && exitInfo != null) "$arrow [LOCKED]" else arrow
            font.draw(batch, lockText, pos.first - textWidth(lockText) / 2f, pos.second + font.lineHeight * 0.3f)
            if (!locked && exitInfo?.name?.isNotBlank() == true) {
                val name = exitInfo.name!!.take(12); font.color = TEXT_DIM; font.draw(batch, name, pos.first - textWidth(name) / 2f, pos.second + radius + 5f)
            }
        }

        if (areaItems.isNotEmpty()) {
            var itemX = MARGIN; val itemY = barH * 0.5f; font.color = TEXT_HIGHLIGHT
            for ((i, item) in areaItems.withIndex()) if (!item.locked) {
                val label = "${i + 1}. ${item.name}"; font.draw(batch, label, itemX.toFloat(), itemY); itemX += textWidth(label) + 20
            }
        }
        batch.end()
    }

    private fun drawGameOverOverlay(message: String) {
        val overlayW = viewportW * 0.7f; val overlayH = viewportH * 0.5f
        val x = (viewportW - overlayW) / 2; val y = (viewportH - overlayH) / 2

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        shapeRenderer.setColor(Color(0f, 0f, 0f, 0.6f)); shapeRenderer.rect(0f, 0f, viewportW, viewportH)
        shapeRenderer.end()

        batch.begin()
        font.color = TEXT_HIGHLIGHT; font.draw(batch, "Game Over", x + overlayW / 2f - textWidth("Game Over") / 2f, y + overlayH - MARGIN)
        batch.end()
    }

    fun renderQuitConfirm() {
        drawBackgroundPass()

        val boxW = 500f; val boxH = 200f
        val x = (viewportW - boxW) / 2; val y = (viewportH - boxH) / 2

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        shapeRenderer.setColor(Color(0f, 0f, 0f, 0.6f)); shapeRenderer.rect(0f, 0f, viewportW, viewportH)
        shapeRenderer.end()

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        shapeRenderer.setColor(PANEL_BG); shapeRenderer.rect(x, y, boxW, boxH)
        shapeRenderer.end()

        batch.begin()
        font.color = TEXT_HIGHLIGHT
        val title = "Are you sure you want to quit?"
        font.draw(batch, title, x + (boxW - textWidth(title)) / 2f, y + boxH - MARGIN)

        font.color = TEXT_WHITE
        val hint = "Press Y to confirm or N to cancel"
        font.draw(batch, hint, x + (boxW - textWidth(hint)) / 2f, y + boxH * 0.5f)
        batch.end()
    }

    private fun splitText(text: String, maxChars: Int): List<String> {
        if (maxChars <= 0) return listOf(text)
        val lines = mutableListOf<String>(); var remaining = text
        while (remaining.isNotEmpty()) {
            if (remaining.length <= maxChars) { lines.add(remaining); break }
            val cutOff = remaining.substring(0, maxChars).lastIndexOf(' ')
            val end = if (cutOff > 0) cutOff else maxChars; lines.add(remaining.take(end)); remaining = remaining.substring(end).trimStart()
        }
        return lines
    }

    fun dispose() { font.dispose() }
}
