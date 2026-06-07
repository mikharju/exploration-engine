package exploration.adapter.ui.libgdx

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import exploration.model.Direction

/** libGDX implementation of RenderBackend. Wraps SpriteBatch/ShapeRenderer/Camera for rendering. */
class LibgdxRenderBackend(
    private val batch: SpriteBatch,
    private val shapeRenderer: ShapeRenderer,
    private val camera: OrthographicCamera,
    private val font: BitmapFont,
    private val layout: LayoutConfig
) : RenderBackend {

    private val glyphLayout = GlyphLayout()

    override fun textWidth(text: String): Float { glyphLayout.setText(font, text); return glyphLayout.width }
    override val fontLineHeight: Float get() = font.lineHeight

    override fun render(viewData: exploration.port.ViewData, selectionState: SelectionState) {
        batch.setProjectionMatrix(camera.combined)
        shapeRenderer.setProjectionMatrix(camera.combined)
        drawBackgroundPass()
        drawMessagePanelPass(viewData.triggerTexts, viewData.storyMessages, viewData.currentAreaName, viewData.areaDescription, viewData.messageHistory)
        drawStatusPanelPass(viewData.health, viewData.maxHealth, viewData.exploredCount,
            viewData.totalAreas, viewData.activatedCount, viewData.totalDevices,
            viewData.statuses, viewData.areaItems, viewData.carriedItems, viewData.equippedItems)
        drawBottomBarPass(viewData.exits, viewData.areaItems, viewData.carriedItems, viewData.equippedItems)

        batch.begin()
        font.draw(batch, "WASD/Arrows: Move | L: Look | U: Activate | I: Inventory", 20f, layout.viewportH - 15f)
        if (viewData.areaItems.isNotEmpty()) {
            val itemsStr = viewData.areaItems.joinToString(", ") { it.name }
            font.draw(batch, "Items here: $itemsStr", 20f, layout.viewportH / 2 + 60f)
        }

        if (selectionState.active && selectionState.target != null) {
            val prompt = when (selectionState.target) {
                SelectionTarget.UNEQUIP -> "Unequip which item? (1-${selectionState.items.size})"
                SelectionTarget.DROP -> "Drop which item? (1-${selectionState.items.size})"
                SelectionTarget.TAKE -> "Take which item? (1-${selectionState.items.size})"
                SelectionTarget.EQUIP -> "Equip which item? (1-${selectionState.items.size})"
            }
            font.color = TEXT_HIGHLIGHT
            font.draw(batch, prompt, layout.viewportW / 2f - textWidth(prompt) / 2f, layout.viewportH * 0.65f)

            val startY = layout.viewportH * 0.55f
            for ((i, item) in selectionState.items.withIndex()) {
                val y = startY - i * font.lineHeight * 1.4f
                if (y < layout.bottomBarHeight + 20f) break
                val label = "${i + 1}. ${item.name}"
                font.color = TEXT_WHITE
                font.draw(batch, label, layout.viewportW / 2f - textWidth(label) / 2f, y)
            }
        }

        batch.end()

        viewData.endGameMessage?.let { drawGameOverOverlay(it) }
    }

    override fun renderWithOverlay(viewData: exploration.port.ViewData, overlayName: String, selectionState: SelectionState) {
        batch.setProjectionMatrix(camera.combined)
        shapeRenderer.setProjectionMatrix(camera.combined)
        val withTrigger = viewData.copy(triggerTexts = listOf("[${overlayName}]"))
        render(withTrigger, selectionState)
    }

    override fun renderStoryViewer(
        viewData: exploration.port.ViewData,
        stories: List<String>,
        selectionState: SelectionState,
        scrollLines: Int,
        maxVisibleLines: Int,
        currentPage: Int,
        totalPages: Int
    ) {
        batch.setProjectionMatrix(camera.combined)
        shapeRenderer.setProjectionMatrix(camera.combined)
        drawBackgroundPass()

        val boxW = layout.storyBoxW; val boxH = layout.storyBoxH
        val x = (layout.viewportW - boxW) / 2; val y = (layout.viewportH - boxH) / 2

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        shapeRenderer.setColor(Color(PANEL_BG.r, PANEL_BG.g, PANEL_BG.b, 1f)); shapeRenderer.rect(x, y, boxW, boxH)
        shapeRenderer.end()

        batch.begin()
        font.color = TEXT_HIGHLIGHT
        val title = " Story Messages"
        font.draw(batch, title, x + (boxW - textWidth(title)) / 2f, y + boxH - RenderBackend.MARGIN)

        val maxWidthPx = (boxW - RenderBackend.MARGIN * 2).toInt()
        val maxCharsPerLine = if (maxWidthPx > 0) {
            val testStr = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
            val testWidth = textWidth(testStr)
            ((testStr.length.toFloat() / testWidth) * maxWidthPx).toInt().coerceIn(1, MAX_WRAP_CHARS)
        } else DEFAULT_MAX_WRAP_CHARS

        data class StoryLine(val storyIdx: Int, val lineNum: Int, val text: String, val isBlank: Boolean)

        val allLines = mutableListOf<StoryLine>()
        for ((storyIdx, story) in stories.withIndex()) {
            if (story.isBlank()) continue
            for (segment in story.split("\n")) {
                if (segment.isBlank()) {
                    allLines.add(StoryLine(storyIdx, -1, "", true))
                } else {
                    val wrapped = splitText(segment, maxCharsPerLine)
                    for ((lineNum, text) in wrapped.withIndex()) {
                        allLines.add(StoryLine(storyIdx, lineNum, text, false))
                    }
                }
            }
        }

        val clampedScroll = scrollLines.coerceAtLeast(0).coerceAtMost(maxOf(allLines.size - maxVisibleLines, 0))
        val visibleLines = allLines.slice(clampedScroll until minOf(clampedScroll + maxVisibleLines, allLines.size))

        var storyY = y + boxH - RenderBackend.MARGIN - font.lineHeight * 1.5f

        for (entry in visibleLines) {
            if (storyY < y + RenderBackend.MARGIN + font.lineHeight * 1.5f) break

            if (entry.isBlank) {
                storyY -= font.lineHeight * 0.5f
            } else {
                font.draw(batch, entry.text, x + RenderBackend.MARGIN, storyY)
                storyY -= font.lineHeight * 1.3f
            }
        }

        font.color = TEXT_DIM
        val hint = "Press ESC to close | ${currentPage} / $totalPages"
        font.draw(batch, hint, x + (boxW - textWidth(hint)) / 2f, y + RenderBackend.MARGIN)
        batch.end()
    }

    override fun renderGameOver(message: String) {
        drawBackgroundPass()

        val boxW = layout.gameOverBoxW; val boxH = layout.gameOverBoxH
        val x = (layout.viewportW - boxW) / 2; val y = (layout.viewportH - boxH) / 2

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        shapeRenderer.setColor(PANEL_BG); shapeRenderer.rect(x, y, boxW, boxH)
        shapeRenderer.end()

        batch.begin()
        font.color = TEXT_HIGHLIGHT
        font.draw(batch, "Game Over", x + (boxW - textWidth("Game Over")) / 2f, y + boxH - RenderBackend.MARGIN)
        font.color = TEXT_WHITE
        val lines = splitText(message, (layout.viewportW * 0.8f).toInt())
        for ((i, line) in lines.withIndex()) {
            val ly = y + boxH * 0.55f - (lines.size - 1 - i) * font.lineHeight
            font.draw(batch, line, x + RenderBackend.MARGIN, ly)
        }
        batch.end()
    }

    override fun renderQuitConfirm() {
        drawBackgroundPass()

        val boxW = layout.quitConfirmBoxW; val boxH = layout.quitConfirmBoxH
        val x = (layout.viewportW - boxW) / 2; val y = (layout.viewportH - boxH) / 2

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        shapeRenderer.setColor(Color(0f, 0f, 0f, 0.6f)); shapeRenderer.rect(0f, 0f, layout.viewportW, layout.viewportH)
        shapeRenderer.end()

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        shapeRenderer.setColor(PANEL_BG); shapeRenderer.rect(x, y, boxW, boxH)
        shapeRenderer.end()

        batch.begin()
        font.color = TEXT_HIGHLIGHT
        val title = "Are you sure you want to quit?"
        font.draw(batch, title, x + (boxW - textWidth(title)) / 2f, y + boxH - RenderBackend.MARGIN)

        font.color = TEXT_WHITE
        val hint = "Press Y to confirm or N to cancel"
        font.draw(batch, hint, x + (boxW - textWidth(hint)) / 2f, y + boxH * 0.5f)
        batch.end()
    }

    private fun drawBackgroundPass() {
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        shapeRenderer.setColor(BG_COLOR); shapeRenderer.rect(0f, 0f, layout.viewportW, layout.viewportH)
        shapeRenderer.end()
    }

    private fun drawMessagePanelPass(triggers: List<String>, stories: List<String>, areaName: String?, areaDescription: String?, messageHistory: List<String>) {
        val filteredTriggers = if (areaDescription != null && triggers.contains(areaDescription)) {
            triggers.filterNot { it == areaDescription }
        } else {
            triggers
        }

        val panelX = RenderBackend.MARGIN; val panelBottomY = layout.viewportH - layout.bottomBarHeight - layout.panelPadding * 2
        val panelW = layout.viewportW / 2.5f; val panelH = layout.viewportH - layout.bottomBarHeight - RenderBackend.MARGIN * 2 - layout.panelPadding * 4
        val panelTopY = panelBottomY - panelH

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        shapeRenderer.setColor(PANEL_BG); shapeRenderer.rect(panelX, panelBottomY - panelH + layout.panelPadding * 2, panelW, panelH)
        shapeRenderer.end()

        batch.begin()

        val allItems = mutableListOf<String>()
        for (trigger in filteredTriggers) {
            if (!allItems.contains(trigger)) allItems.add(trigger)
        }
        for (msg in messageHistory) {
            if (!allItems.contains(msg)) allItems.add(msg)
        }
        if (allItems.size > layout.maxMessages) {
            allItems.subList(0, allItems.size - layout.maxMessages).clear()
        }
        allItems.reverse()

        drawAreaLabel(areaName, panelX, panelBottomY)

        var y = panelBottomY - font.lineHeight * 1.3f
        val minY = panelTopY + layout.panelPadding

        val maxCharsForTriggers = (panelW - layout.panelPadding * 2).toInt() / 6.coerceAtLeast(1)
        drawMessageLines(allItems, y, panelX, minY, maxCharsForTriggers)
        
        batch.end()
    }

    private fun drawAreaLabel(areaName: String?, panelX: Float, panelBottomY: Float) {
        val areaLabelY = panelBottomY - font.lineHeight / 2
        if (areaName != null && areaName.isNotBlank()) {
            font.color = TEXT_HIGHLIGHT; font.draw(batch, "Area: $areaName", panelX + layout.panelPadding, areaLabelY)
        }
    }

    private fun drawMessageLines(allItems: List<String>, startY: Float, panelX: Float, minY: Float, maxCharsPerLine: Int) {
        var y = startY
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
                val wrappedLines = splitText(segment, maxCharsPerLine)
                for ((lineIdx, line) in wrappedLines.withIndex()) {
                    if (y < minY) break
                    font.draw(batch, line, panelX + layout.panelPadding, y)
                    y -= font.lineHeight * 1.3f
                }
                isFirstSegment = false
            }
        }

        if (allItems.isEmpty()) {
            val hintY = startY - font.lineHeight * 2
            if (hintY > minY) { font.color = TEXT_DIM; font.draw(batch, "Press L to look around", panelX + layout.panelPadding, hintY) }
        }
    }

    private fun drawStatusPanelPass(health: Int, maxHealth: Int, exploredCount: Int, totalAreas: Int, activatedCount: Int, totalDevices: Int, statuses: Map<String, Int>, areaItems: List<exploration.port.ItemView>, carriedItems: List<exploration.port.ItemView>, equippedItems: List<exploration.port.ItemView>) {
        val panelX = RenderBackend.MARGIN + layout.viewportW / 2.5f + RenderBackend.MARGIN * 0.8f
        val panelBottomY = layout.viewportH - layout.bottomBarHeight - layout.panelPadding * 2
        val panelW = layout.viewportW / 3f; val panelH = layout.viewportH - layout.bottomBarHeight - RenderBackend.MARGIN * 2 - layout.panelPadding * 4
        val panelTopY = panelBottomY - panelH

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        shapeRenderer.setColor(PANEL_BG); shapeRenderer.rect(panelX, panelBottomY - panelH + layout.panelPadding * 2, panelW, panelH)
        shapeRenderer.end()

        var y = panelBottomY - font.lineHeight / 2
        val minY = panelTopY + layout.panelPadding
        
        // Draw health bar text and bar
        val healthPct = if (maxHealth > 0) health.toFloat() / maxHealth else 0f
        val hpColor = when { healthPct > 0.6f -> HP_GREEN; healthPct > 0.3f -> HP_YELLOW; else -> HP_RED }
        batch.begin()
        font.color = TEXT_WHITE
        val hpLabel = "HP: $health/$maxHealth"
        font.draw(batch, hpLabel, panelX + layout.panelPadding, y)
        batch.end()
        
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        val barY = y - font.lineHeight * 0.7f
        val barX = panelX + layout.panelPadding + textWidth(hpLabel) + 10
        shapeRenderer.setColor(Color(0.15f, 0.15f, 0.2f, 1f)); shapeRenderer.rect(barX, barY, layout.statusBarWidth, 8f)
        shapeRenderer.setColor(hpColor); shapeRenderer.rect(barX, barY, layout.statusBarWidth * healthPct, 8f)
        shapeRenderer.end()

        y -= font.lineHeight * 2f

        batch.begin()
        font.color = TEXT_DIM; font.draw(batch, "Explored: $exploredCount/$totalAreas | Devices: $activatedCount/$totalDevices", panelX + layout.panelPadding, y)

        if (carriedItems.isNotEmpty()) {
            y -= font.lineHeight * 2.5f
            drawItemSection(carriedItems, "Carried:", panelX, y, minY)
        }

        if (equippedItems.isNotEmpty()) {
            y -= font.lineHeight * 2f
            drawEquippedSection(equippedItems, panelX, y, minY)
        }
        batch.end()
    }

    private fun drawItemSection(items: List<exploration.port.ItemView>, label: String, panelX: Float, startY: Float, minY: Float) {
        var y = startY
        font.color = TEXT_HIGHLIGHT
        font.draw(batch, label, panelX + layout.panelPadding, y)

        for ((i, item) in items.withIndex()) {
            y -= font.lineHeight * 1.3f
            if (y < minY) break
            font.color = if (item.locked) TEXT_DIM else TEXT_WHITE
            val lockedStr = if (item.locked) " [locked]" else ""
            font.draw(batch, "${i + 1}. ${item.name}$lockedStr", panelX + layout.panelPadding, y)
        }
    }

    private fun drawEquippedSection(items: List<exploration.port.ItemView>, panelX: Float, startY: Float, minY: Float) {
        var y = startY
        font.color = TEXT_HIGHLIGHT; font.draw(batch, "Equipped:", panelX + layout.panelPadding, y)

        for ((i, item) in items.withIndex()) {
            y -= font.lineHeight * 1.3f
            if (y < minY) break
            font.color = TEXT_HIGHLIGHT; val lockedStr = if (item.locked) " [locked]" else ""
            font.draw(batch, "${i + 1}. ${item.name}$lockedStr", panelX + layout.panelPadding, y)
        }
    }

    private fun drawBottomBarPass(exits: Map<Direction, exploration.port.ExitInfo?>, areaItems: List<exploration.port.ItemView>, carriedItems: List<exploration.port.ItemView>, equippedItems: List<exploration.port.ItemView>) {
        val barY = 0f; val barH = layout.bottomBarHeight

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        shapeRenderer.setColor(PANEL_BG); shapeRenderer.rect(0f, barY, layout.viewportW, barH)

        val centerX = layout.viewportW / 2; val buttonY = barY + layout.directionBtnSize / 2 + 10f
        val radius = layout.directionBtnSize / 2; val spacing = layout.directionBtnSize * BUTTON_SPACING_MULTIPLIER

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
        font.draw(batch, hintText, layout.viewportW / 2f - textWidth(hintText) / 2f, barH - 15f)

        for ((dir, pos) in buttonPositions) {
            val exitInfo = exits[dir]; val locked = exitInfo == null || exitInfo.blocked
            font.color = if (locked) Color(0.3f, 0.25f, 0.25f, 1f) else TEXT_WHITE
            val arrow = when(dir) { Direction.North -> "^"; Direction.South -> "v"; Direction.West -> "<"; Direction.East -> ">" }
            val lockText = if (locked && exitInfo != null) "$arrow [LOCKED]" else arrow
            font.draw(batch, lockText, pos.first - textWidth(lockText) / 2f, pos.second + font.lineHeight * 0.3f)
            @Suppress("UNNECESSARY_SAFE_CALL")
            if (!locked && exitInfo?.name?.isNotBlank() == true) {
                val infoName = exitInfo.name ?: ""; val name = infoName.take(12); font.color = TEXT_DIM; font.draw(batch, name, pos.first - textWidth(name) / 2f, pos.second + radius + 5f)
            }
        }

        if (areaItems.isNotEmpty()) {
            var itemX = RenderBackend.MARGIN; val itemY = barH * 0.5f; font.color = TEXT_HIGHLIGHT
            for ((i, item) in areaItems.withIndex()) if (!item.locked) {
                val label = "${i + 1}. ${item.name}"; font.draw(batch, label, itemX, itemY); itemX += textWidth(label) + ITEM_SPACING
            }
        }
        batch.end()
    }

    private fun drawGameOverOverlay(message: String) {
        val overlayW = layout.viewportW * 0.7f; val overlayH = layout.viewportH * 0.5f
        val x = (layout.viewportW - overlayW) / 2; val y = (layout.viewportH - overlayH) / 2

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        shapeRenderer.setColor(Color(0f, 0f, 0f, 0.6f)); shapeRenderer.rect(0f, 0f, layout.viewportW, layout.viewportH)
        shapeRenderer.end()

        batch.begin()
        font.color = TEXT_HIGHLIGHT; font.draw(batch, "Game Over", x + overlayW / 2f - textWidth("Game Over") / 2f, y + overlayH - RenderBackend.MARGIN)
        batch.end()
    }

    override fun splitText(text: String, maxChars: Int): List<String> {
        if (maxChars <= 0) return listOf(text)
        val lines = mutableListOf<String>(); var remaining = text
        while (remaining.isNotEmpty()) {
            if (remaining.length <= maxChars) { lines.add(remaining); break }
            val cutOff = remaining.substring(0, maxChars).lastIndexOf(' ')
            val end = if (cutOff > 0) cutOff else maxChars; lines.add(remaining.take(end)); remaining = remaining.substring(end).trimStart()
        }
        return lines
    }

    fun linesVisibleInHeight(height: Float): Int = (height / font.lineHeight).toInt().coerceAtLeast(1)

    fun dispose() { font.dispose() }

    companion object {
        const val BUTTON_SPACING_MULTIPLIER = 1.3f
        const val ITEM_SPACING = 20f
        const val MAX_WRAP_CHARS = 200
        const val DEFAULT_MAX_WRAP_CHARS = 40

        val PANEL_BG = Color(0.05f, 0.06f, 0.09f, 0.85f)
        val BG_COLOR = Color(0.1f, 0.12f, 0.18f, 1f)
        val BORDER_COLOR = Color(0.25f, 0.3f, 0.4f, 1f)
        val TEXT_WHITE = Color(0.9f, 0.9f, 0.9f, 1f)
        val TEXT_DIM = Color(0.4f, 0.45f, 0.55f, 1f)
        val TEXT_HIGHLIGHT = Color(0.3f, 0.7f, 1f, 1f)
        val HP_GREEN = Color(0.2f, 0.8f, 0.3f, 1f)
        val HP_YELLOW = Color(0.9f, 0.7f, 0.1f, 1f)
        val HP_RED = Color(0.85f, 0.2f, 0.2f, 1f)
        val BUTTON_BG = Color(0.15f, 0.2f, 0.3f, 0.9f)
        val BUTTON_LOCKED = Color(0.12f, 0.12f, 0.18f, 0.7f)
    }
}
