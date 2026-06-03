package exploration.adapter.ui.libgdx.components

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import exploration.port.ItemView

/** Renders an item icon (procedural) with optional label. */
class ItemSprite {

    companion object {
        const val SIZE = 36f
        const val ICON_BORDER = 1.5f
    }

    /** Procedural color based on item name for now; can be replaced with textures later. */
    private fun itemColorByName(item: ItemView): Color = when {
        "weapon" in item.name.lowercase() -> Color(0.8f, 0.2f, 0.2f, 1f) // Red
        "armor" in item.name.lowercase() || "shield" in item.name.lowercase() -> Color(0.2f, 0.4f, 0.8f, 1f) // Blue
        "key" in item.name.lowercase() -> Color.YELLOW.cpy().let { it.a = 0.9f; it }
        else -> Color(0.7f, 0.5f, 0.2f, 1f) // Orange/brown default
    }

    /** Render a simple procedural icon (circle with item type indicator). */
    fun render(
        batch: SpriteBatch,
        shapeRenderer: ShapeRenderer,
        font: BitmapFont,
        x: Float,
        y: Float,
        size: Float = SIZE,
        item: ItemView?,
        label: String? = null,
        equipped: Boolean = false
    ) {
        if (item == null) return

        val color = itemColorByName(item)

        // Background shape (circle)
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        shapeRenderer.color = Color.GRAY.cpy().let { it.a = 0.3f; it }
        shapeRenderer.circle(x, y, size / 2)
        shapeRenderer.end()

        // Border
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
        shapeRenderer.color = if (equipped) Color.GREEN else color
        shapeRenderer.circle(x, y, size / 2 + ICON_BORDER, 16)
        shapeRenderer.end()

        // Item type indicator letter/emoji based on name
        batch.begin()
        font.color = Color.WHITE.cpy().let { it.a = 0.9f; it }
        val displayChar = when {
            "weapon" in item.name.lowercase() -> "⚔"
            "armor" in item.name.lowercase() || "shield" in item.name.lowercase() -> "🛡"
            "key" in item.name.lowercase() -> "🔑"
            else -> "•"
        }
        font.draw(batch, displayChar, x - 8f, y + 5f)

        if (label != null) {
            val labelHeight = 16f // Approximate line height for default font
            val offsetY = if (equipped) -size / 2 - labelHeight else size / 2 + 2f
            val displayLabel = if (label.length > 12) label.take(9) + "..." else label
            font.color = Color.WHITE.cpy().let { it.a = 0.85f; it }
            // Approximate text width for centering
            val approxWidth = displayLabel.length * 6f
            font.draw(batch, displayLabel, x - approxWidth / 2f, y + offsetY)
        }

        batch.end()
    }
}
