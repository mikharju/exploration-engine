package exploration.adapter.ui.libgdx.components

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.glutils.ShapeRenderer

/** Renders the health bar in the status panel. */
class HealthBar {

    companion object {
        const val BAR_HEIGHT = 18f
        const val BORDER_THICKNESS = 2f
    }

    /** Simple data class for HP bar info */
    data class HpInfo(val current: Int, val max: Int) {
        val percent: Float get() = if (max > 0) current.toFloat() / max else 0f
    }

    fun render(
        batch: com.badlogic.gdx.graphics.g2d.SpriteBatch,
        shapeRenderer: ShapeRenderer,
        font: BitmapFont,
        x: Float,
        y: Float,
        barWidth: Float,
        hpInfo: HpInfo
    ) {
        val fillColor = when (hpInfo.percent) {
            in 0f..0.3f -> Color.RED.cpy().let { it.a = 0.8f; it }
            in 0.3f..0.6f -> Color.YELLOW.cpy().let { it.a = 0.7f; it }
            else -> Color(0.2f, 0.6f, 0.2f, 1f)
        }

        // Background bar
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        shapeRenderer.color = Color.GRAY.cpy().let { it.a = 0.3f; it }
        shapeRenderer.rect(x - BORDER_THICKNESS, y - BAR_HEIGHT / 2 - BORDER_THICKNESS, barWidth + BORDER_THICKNESS * 2, BAR_HEIGHT + BORDER_THICKNESS * 2)
        shapeRenderer.end()

        // HP fill
        val fillWidth = barWidth * hpInfo.percent.coerceIn(0f, 1f)
        if (fillWidth > 0) {
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
            shapeRenderer.color = fillColor
            shapeRenderer.rect(x, y - BAR_HEIGHT / 2, fillWidth, BAR_HEIGHT)
            shapeRenderer.end()

            // Fill border
            shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
            shapeRenderer.color = Color.WHITE.cpy().let { it.a = 0.7f; it }
            shapeRenderer.rect(x, y - BAR_HEIGHT / 2, fillWidth, BAR_HEIGHT)
            shapeRenderer.end()
        }

        // Border outline (full bar)
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
        shapeRenderer.color = Color.WHITE.cpy().let { it.a = 0.8f; it }
        shapeRenderer.rect(x - BORDER_THICKNESS, y - BAR_HEIGHT / 2 - BORDER_THICKNESS, barWidth + BORDER_THICKNESS * 2, BAR_HEIGHT + BORDER_THICKNESS * 2)
        shapeRenderer.end()

        // HP text in center of bar
        batch.begin()
        font.color = Color.WHITE.cpy().let { it.a = 0.9f; it }
        val hpText = "${hpInfo.current} / ${hpInfo.max}"
        val approxWidth = hpText.length * 8f // Approximate character width for default font
        font.draw(batch, hpText, x + barWidth * (1 - hpInfo.percent) / 2f - approxWidth / 2f, y + 5f)
        batch.end()
    }
}
