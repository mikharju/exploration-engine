package exploration.adapter.ui.libgdx

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.utils.viewport.FitViewport
import exploration.port.GameEngine
import exploration.port.InputEvent
import exploration.model.Direction

class GameScreen(
    private val engine: GameEngine,
    private val scenarioPath: String
) : Screen {

    companion object {
        const val VIEWPORT_W = 1800f
        const val VIEWPORT_H = 1100f
        const val BOTTOM_BAR_HEIGHT = 130f
        const val MARGIN = 20f
        const val MESSAGE_PANEL_WIDTH_RATIO = 0.58f
        const val PANEL_GAP = 30f
    }

    private var gameRef: exploration.port.GameRef? = null
    private var viewData: exploration.port.ViewData? = null
    private var overlayState: OverlayState.State = OverlayState.State.Playing
    private var selectionState: SelectionState = SelectionState.inactive()

    private val camera: OrthographicCamera = OrthographicCamera()
    private val viewport: FitViewport = FitViewport(VIEWPORT_W, VIEWPORT_H, camera)
    private val stage: Stage = Stage(viewport)
    private val batch: SpriteBatch = SpriteBatch()
    private val shapeRenderer: ShapeRenderer = ShapeRenderer()

    private val inputProcessor = object : InputProcessor {
        override fun keyDown(keycode: Int): Boolean {
            if (keycode == com.badlogic.gdx.Input.Keys.F12) {
                Gdx.app.log("Debug", "=== DEBUG STATE ===")
                viewData?.let { vd ->
                    Gdx.app.log("Debug", "Area: ${vd.currentAreaName}")
                    Gdx.app.log("Debug", "HP: ${vd.health}/${vd.maxHealth}")
                    Gdx.app.log("Debug", "Triggers count: ${vd.triggerTexts.size}")
                    Gdx.app.log("Debug", "Stories count: ${vd.storyMessages.size}")
                    Gdx.app.log("Debug", "Area items: ${vd.areaItems.size} (locked=${vd.areaItems.any { it.locked }})")
                    Gdx.app.log("Debug", "Carried items: ${vd.carriedItems.size}")
                    Gdx.app.log("Debug", "Equipped items: ${vd.equippedItems.size}")
                    Gdx.app.log("Debug", "End game: ${vd.endGameMessage != null}")
                    vd.triggerTexts.take(3).forEachIndexed { i, t ->
                        Gdx.app.log("Debug", "  Trigger[$i]: '$t'")
                    }
                    vd.storyMessages.take(3).forEachIndexed { i, s ->
                        Gdx.app.log("Debug", "  Story[$i]: '$s'")
                    }
                } ?: Gdx.app.log("Debug", "viewData is null")
                return true
            }
            val vd = viewData ?: return false
            var event = when (keycode) {
                com.badlogic.gdx.Input.Keys.UP -> InputEvent.MoveDirection(Direction.North)
                com.badlogic.gdx.Input.Keys.LEFT -> InputEvent.MoveDirection(Direction.West)
                com.badlogic.gdx.Input.Keys.DOWN -> InputEvent.MoveDirection(Direction.South)
                com.badlogic.gdx.Input.Keys.RIGHT -> InputEvent.MoveDirection(Direction.East)
                'W'.code -> InputEvent.MoveDirection(Direction.North)
                'A'.code -> InputEvent.MoveDirection(Direction.West)
                'S'.code -> InputEvent.MoveDirection(Direction.South)
                'D'.code -> InputEvent.MoveDirection(Direction.East)
                com.badlogic.gdx.Input.Keys.L -> InputEvent.Look
                com.badlogic.gdx.Input.Keys.U -> InputEvent.Activate
                com.badlogic.gdx.Input.Keys.I -> InputEvent.Inventory
                else -> null
            }

            if (event == null && keycode >= com.badlogic.gdx.Input.Keys.A && keycode <= com.badlogic.gdx.Input.Keys.Z) {
                val letter = ('a' + (keycode - com.badlogic.gdx.Input.Keys.A)).toChar()
                event = handleItemAction(letter, vd)
            } else if (event == null && keycode >= com.badlogic.gdx.Input.Keys.NUM_0 && keycode <= com.badlogic.gdx.Input.Keys.NUM_9) {
                val digit = (keycode - com.badlogic.gdx.Input.Keys.NUM_0 + 1).toString()[0]
                event = handleDigitSelection(digit, vd)
            } else if (event == null && keycode == com.badlogic.gdx.Input.Keys.ESCAPE && selectionState.active) {
                closeSelection()
            }

            if (event != null && gameRef != null) {
                viewData = engine.tick(gameRef!!, event)
                checkGameOver(viewData!!)
            }
            return true
        }

        override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
            val vd = viewData ?: return false
            val worldPos = camera.unproject(com.badlogic.gdx.math.Vector3(screenX.toFloat(), screenY.toFloat(), 0f))

            var event = InputMapper.mapTouchDirection(worldPos.x, worldPos.y, vd)
            if (event != null && gameRef != null) {
                viewData = engine.tick(gameRef!!, event)
                checkGameOver(viewData!!)
                return true
            }

            val actionResult = InputMapper.mapTouchItem(worldPos.x, worldPos.y, vd)
            actionResult?.let { result ->
                val inputEvent = when (result) {
                    is ItemActionResult.Take -> InputEvent.TakeItem(result.itemName)
                    is ItemActionResult.Drop -> InputEvent.DropItem(result.itemName)
                    is ItemActionResult.Equip -> InputEvent.EquipItem(result.itemName)
                    is ItemActionResult.Unequip -> InputEvent.UnequipItem(result.itemName)
                }
                if (gameRef != null) {
                    viewData = engine.tick(gameRef!!, inputEvent)
                    checkGameOver(viewData!!)
                }
            }

            return true
        }

        override fun keyUp(keycode: Int): Boolean = false
        override fun keyTyped(character: Char): Boolean = false
        override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean = false
        override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean = false
        override fun mouseMoved(screenX: Int, screenY: Int): Boolean = false
        override fun touchCancelled(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean = false
        override fun scrolled(amountX: Float, amountY: Float): Boolean = false
    }

    init {
        viewData = engine.start(scenarioPath).also { gameRef = it }
            .let { engine.tick(it, InputEvent.Look) }
        Gdx.input.setInputProcessor(InputMultiplexer(stage, inputProcessor))
    }

    override fun show() {
        camera.position.set(VIEWPORT_W / 2f, VIEWPORT_H / 2f, 0f)
    }

    private fun handleItemAction(key: Char, vd: exploration.port.ViewData): InputEvent? {
        return when (key.lowercaseChar()) {
            'g' -> handleTake(vd)
            'p' -> handleDrop(vd)
            'e' -> handleEquip(vd)
            'r' -> handleUnequip(vd)
            else -> null
        }
    }

    private fun handleDigitSelection(digit: Char, vd: exploration.port.ViewData): InputEvent? {
        if (!selectionState.active) return null

        val index = (digit - '1').toInt()
        if (index < 0 || index >= selectionState.items.size) {
            closeSelection()
            return null
        }

        val selectedItem = selectionState.items[index]
        val event = when (selectionState.target) {
            SelectionTarget.UNEQUIP -> InputEvent.UnequipItem(selectedItem.name)
            else -> null
        }

        closeSelection()
        return event
    }

    private fun closeSelection() {
        selectionState = SelectionState.inactive()
        overlayState = OverlayState.State.Playing
    }

    private fun handleTake(vd: exploration.port.ViewData): InputEvent? {
        val candidates = vd.areaItems.filterNot { it.locked }
        return when {
            candidates.isEmpty() -> null
            candidates.size == 1 -> InputEvent.TakeItem(candidates[0].name)
            else -> null
        }
    }

    private fun handleDrop(vd: exploration.port.ViewData): InputEvent? {
        val candidates = vd.carriedItems + vd.equippedItems
        return when {
            candidates.isEmpty() -> null
            candidates.size == 1 -> InputEvent.DropItem(candidates[0].name)
            else -> null
        }
    }

    private fun handleEquip(vd: exploration.port.ViewData): InputEvent? {
        val candidates = vd.carriedItems.filterNot { it.locked }
        return when {
            candidates.isEmpty() -> null
            candidates.size == 1 -> InputEvent.EquipItem(candidates[0].name)
            else -> null
        }
    }

    private fun handleUnequip(vd: exploration.port.ViewData): InputEvent? {
        val candidates = vd.equippedItems
        return when {
            candidates.isEmpty() -> null
            candidates.size == 1 -> InputEvent.UnequipItem(candidates[0].name)
            else -> {
                selectionState = SelectionState.forUnequip(candidates)
                overlayState = OverlayState.State.Inventory
                null
            }
        }
    }

    override fun render(delta: Float) {
        Gdx.gl.glDisable(GL20.GL_DEPTH_TEST)
        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.15f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        viewport.apply()

        stage.act(delta)
        stage.draw()

        renderGameContent()
    }

    private fun checkGameOver(vd: exploration.port.ViewData) {
        if (vd.endGameMessage != null) {
            overlayState = OverlayState.State.GameOver
        }
    }

    private fun renderGameContent() {
        val vd = viewData ?: return
        when (overlayState) {
            OverlayState.State.Playing -> renderer.render(vd, selectionState)
            OverlayState.State.Inventory, OverlayState.State.StoryViewer ->
                renderer.renderWithOverlay(vd, overlayState.name, selectionState)
            OverlayState.State.GameOver -> vd.endGameMessage?.let { msg ->
                renderer.renderGameOver(msg)
            } ?: renderer.render(vd, selectionState)
        }
    }

    private val renderer: Renderer by lazy {
        Renderer(batch, shapeRenderer, camera, VIEWPORT_W, VIEWPORT_H)
    }

    override fun resize(width: Int, height: Int) {
        viewport.update(width, height, true)
        camera.position.set(VIEWPORT_W / 2f, VIEWPORT_H / 2f, 0f)
    }

    override fun pause() {}
    override fun resume() {}
    override fun hide() {}

    override fun dispose() {
        batch.dispose()
        shapeRenderer.dispose()
        stage.dispose()
        renderer.dispose()
    }
}
