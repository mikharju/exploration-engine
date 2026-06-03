package exploration.adapter.ui.libgdx.components

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer

/** Renders the progressive world map. */
class WorldMap {

    companion object {
        const val NODE_RADIUS = 14f
        const val KNOWN_ALPHA = 0.3f // Dimmed alpha for known-but-unexplored areas
    }

    /** Draw area nodes and connections on the map. */
    fun render(
        batch: SpriteBatch,
        shapeRenderer: ShapeRenderer,
        font: BitmapFont,
        startX: Float,
        startY: Float,
        areas: List<String>, // Simplified - list of area names/IDs
        currentAreaId: String?
    ) {
        if (areas.isEmpty()) return

        // Layout calculation: place nodes in a grid-like pattern
        val cols = 4 // Columns for layout
        val spacingX = 100f
        val spacingY = 80f

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
        shapeRenderer.color = Color.WHITE.cpy().let { it.a = 0.15f; it }

        // Draw connections between areas (simplified: connect adjacent in list for now)
        for ((i, _) in areas.withIndex()) {
            val x = startX + (i % cols) * spacingX
            val y = startY - (i / cols) * spacingY

            // Connect to next area if exists and has connection info (simplified approach)
            if (i < areas.size - 1) {
                val nextX = startX + ((i + 1) % cols) * spacingX
                val nextY = startY - ((i + 1) / cols) * spacingY
                shapeRenderer.line(x, y, nextX, nextY)
            }

            // Draw connection lines from current area to its exits (if we had that data)
        }
        shapeRenderer.end()

        // Draw area nodes
        for ((i, areaId) in areas.withIndex()) {
            val x = startX + (i % cols) * spacingX
            val y = startY - (i / cols) * spacingY

            val isCurrentArea = currentAreaId != null && areaId == currentAreaId
            val isExplored = true // All areas shown are explored or known; this would track exploration state in real implementation

            // Node background circle
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
            if (isCurrentArea) {
                shapeRenderer.color = Color.GREEN.cpy().let { it.a = 0.5f; it }
            } else {
                shapeRenderer.color = Color.GRAY.cpy().let { it.a = KNOWN_ALPHA; it }
            }
            shapeRenderer.circle(x, y, NODE_RADIUS)
            shapeRenderer.end()

            // Node border
            shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
            if (isCurrentArea) {
                shapeRenderer.color = Color.GREEN
            } else {
                shapeRenderer.color = Color.WHITE.cpy().let { it.a = 0.4f; it }
            }
            shapeRenderer.circle(x, y, NODE_RADIUS + 2f, 16)
            shapeRenderer.end()

            // Node label (area name or ID)
            batch.begin()
            val displayLabel = if (i == 0 && areas.firstOrNull()?.length ?: 0 > 10) {
                // Truncate long names
                areaId.take(7) + "..."
            } else {
                areaId.substringAfterLast('_').take(8)
            }

            font.color = if (isCurrentArea) Color.GREEN else Color.WHITE.cpy().let { it.a = 0.6f; it }
            // Approximate text width for centering using character count
            val approxWidth = displayLabel.length * 6f
            font.draw(batch, displayLabel, x - approxWidth / 2f, y + NODE_RADIUS + 14f)

            // "Exploring" indicator for current area
            if (isCurrentArea) {
                font.color = Color.GREEN.cpy().let { it.a = 0.8f; it }
                val youApproxWidth = "[YOU]".length * 6f
                font.draw(batch, "[YOU]", x - youApproxWidth / 2f, y + NODE_RADIUS + 26f)
            }

            batch.end()
        }
    }
}
