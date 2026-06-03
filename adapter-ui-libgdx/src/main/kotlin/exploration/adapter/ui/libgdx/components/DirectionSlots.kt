package exploration.adapter.ui.libgdx.components

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import exploration.model.Direction
import exploration.port.ExitInfo

/** Renders the four directional buttons in the bottom bar. */
class DirectionSlots {

    companion object {
        const val BUTTON_RADIUS = 22f
        const val GAP = 60f
        // Color constants matching Renderer
        val TEXT_GREEN = Color(0.2f, 0.7f, 0.3f, 1f)
        val BORDER_GRAY = Color(0.3f, 0.3f, 0.35f, 0.9f)
        val TEXT_DEFAULT = Color(0.85f, 0.85f, 0.85f, 1f)
    }

    fun render(
        batch: SpriteBatch,
        shapeRenderer: ShapeRenderer,
        font: BitmapFont,
        centerX: Float,
        centerY: Float,
        exits: Map<Direction, ExitInfo?>
    ) {
        for ((dir, exitInfo) in exits.entries.filterNotNull()) {
            var bx = centerX
            var by = centerY

            when (dir) {
                Direction.North -> by -= GAP
                Direction.West -> bx -= GAP * 2f
                Direction.East -> bx += GAP * 2f
                Direction.South -> {}
            }

            val active = exitInfo != null && !exitInfo.blocked

            // Filled circle background
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
            shapeRenderer.color = if (active) Color(0.3f, 0.7f, 0.3f, 0.4f) else Color.GRAY.cpy().let { it.a = 0.2f; it }
            shapeRenderer.circle(bx, by, BUTTON_RADIUS)
            shapeRenderer.end()

            // Circle border
            shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
            shapeRenderer.color = if (active) TEXT_GREEN else BORDER_GRAY
            shapeRenderer.circle(bx, by, BUTTON_RADIUS, 16)
            shapeRenderer.end()

            batch.begin()
            font.color = if (active) TEXT_GREEN else TEXT_DEFAULT.cpy().let { it.a = 0.5f; it }
            val keyChar = when (dir) { Direction.North -> 'W'; Direction.West -> 'A'; Direction.South -> 'S'; Direction.East -> 'D' }
            font.draw(batch, "[$keyChar]", bx - 16f, by + 6f)

            if (active && exitInfo.name != null) {
                val label = "-> ${exitInfo.name}"
                val lx = when (dir) { Direction.North -> bx - 45f; Direction.South -> bx - 38f; Direction.West -> bx + 28f; Direction.East -> bx - 95f }
                font.draw(batch, label, lx, by + 6f)
            }

            batch.end()

            // Arrow indicator (except for south which is at center position)
            if (active && dir != Direction.South) {
                drawArrow(shapeRenderer, bx, by, dir)
            }
        }
    }

    private fun drawArrow(sr: ShapeRenderer, cx: Float, cy: Float, dir: Direction) {
        sr.begin(ShapeRenderer.ShapeType.Filled)
        val s = 7f
        when (dir) {
            Direction.North -> sr.triangle(cx - 3.5f, cy - 10f, cx + 3.5f, cy - 10f, cx, cy - 16f)
            Direction.West -> sr.triangle(cx + 10f, cy - 3.5f, cx + 10f, cy + 3.5f, cx + 16f, cy)
            Direction.East -> sr.triangle(cx - 10f, cy - 3.5f, cx - 10f, cy + 3.5f, cx - 16f, cy)
            else -> {}
        }
        sr.end()
    }
}
