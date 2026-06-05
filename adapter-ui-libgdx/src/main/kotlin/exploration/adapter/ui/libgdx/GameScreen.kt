package exploration.adapter.ui.libgdx

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.PixmapIO
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.utils.ScreenUtils
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
        const val BOTTOM_BAR_HEIGHT = 180f
        const val MARGIN = 20f
        const val MESSAGE_PANEL_WIDTH_RATIO = 0.58f
        const val PANEL_GAP = 30f
        const val DIRECTION_BTN_SIZE = 70f
    }

    private var gameRef: exploration.port.GameRef? = null
    private var viewData: exploration.port.ViewData? = null
    private var overlayState: OverlayState = OverlayState.Playing
    private var selectionState: SelectionState = SelectionState.inactive()
    private var storedStoryCount: Int = 0
    private var pendingStories: List<String> = emptyList()

    private val camera: OrthographicCamera = OrthographicCamera()
    private val viewport: FitViewport = FitViewport(VIEWPORT_W, VIEWPORT_H, camera)
    private val stage: Stage = Stage(viewport)
    private val batch: SpriteBatch = SpriteBatch()
    private val shapeRenderer: ShapeRenderer = ShapeRenderer()

    private val inputProcessor = object : InputProcessor {
        override fun keyDown(keycode: Int): Boolean {
            if (keycode == com.badlogic.gdx.Input.Keys.F12) {
                saveScreenshot()
                return true
            }
            val vd = viewData ?: return false
            var event = when (keycode) {
                com.badlogic.gdx.Input.Keys.UP -> InputEvent.MoveDirection(Direction.North)
                com.badlogic.gdx.Input.Keys.LEFT -> InputEvent.MoveDirection(Direction.West)
                com.badlogic.gdx.Input.Keys.DOWN -> InputEvent.MoveDirection(Direction.South)
                com.badlogic.gdx.Input.Keys.RIGHT -> InputEvent.MoveDirection(Direction.East)
                com.badlogic.gdx.Input.Keys.W, 'W'.code, 'w'.code -> InputEvent.MoveDirection(Direction.North)
                com.badlogic.gdx.Input.Keys.A, 'A'.code, 'a'.code -> InputEvent.MoveDirection(Direction.West)
                com.badlogic.gdx.Input.Keys.S, 'S'.code, 's'.code -> InputEvent.MoveDirection(Direction.South)
                com.badlogic.gdx.Input.Keys.D, 'D'.code, 'd'.code -> InputEvent.MoveDirection(Direction.East)
                com.badlogic.gdx.Input.Keys.L, 'L'.code, 'l'.code -> InputEvent.Look
                com.badlogic.gdx.Input.Keys.U, 'U'.code, 'u'.code -> InputEvent.Activate
                com.badlogic.gdx.Input.Keys.I, 'I'.code, 'i'.code -> InputEvent.Inventory
                else -> null
            }

            if (event == null && keycode == com.badlogic.gdx.Input.Keys.J) {
                val allStories = vd.storyMessages.filter { it.isNotBlank() }
                if (allStories.isNotEmpty()) {
                    overlayState = OverlayState.StoryViewer
                    pendingStories = allStories
                }
            }

            if (event == null && keycode >= com.badlogic.gdx.Input.Keys.A && keycode <= com.badlogic.gdx.Input.Keys.Z) {
                val letter = ('a' + (keycode - com.badlogic.gdx.Input.Keys.A)).toChar()
                event = handleItemAction(letter, vd)
            } else if (event == null && keycode >= com.badlogic.gdx.Input.Keys.NUM_0 && keycode <= com.badlogic.gdx.Input.Keys.NUM_9) {
                val digit = ('0' + (keycode - com.badlogic.gdx.Input.Keys.NUM_0)).toChar()
                event = handleDigitSelection(digit, vd)
            } else if (event == null && keycode == com.badlogic.gdx.Input.Keys.ESCAPE) {
                when (overlayState) {
                    OverlayState.StoryViewer -> {
                        overlayState = OverlayState.Playing
                        pendingStories = emptyList()
                    }
                    else -> closeSelection()
                }
            }

            if (event != null && gameRef != null) {
                viewData = engine.tick(gameRef!!, event)
                checkGameOver(viewData!!)
                val vd = viewData!!
                if (vd.storyMessages.size > storedStoryCount) {
                    overlayState = OverlayState.StoryViewer
                    pendingStories = vd.storyMessages.drop(storedStoryCount).filter { it.isNotBlank() }
                }
                storedStoryCount = vd.storyMessages.size
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
                updateStoryOverlay()
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
                    updateStoryOverlay()
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
        storedStoryCount = viewData!!.storyMessages.size
        Gdx.input.setInputProcessor(InputMultiplexer(stage, inputProcessor))
        Gdx.app.log("GameReady", "LIBGDX_GAME_READY")
    }

    override fun show() {
        camera.position.set(VIEWPORT_W / 2f, VIEWPORT_H / 2f, 0f)
    }

    private fun handleItemAction(key: Char, vd: exploration.port.ViewData): InputEvent? {
        val action = InputMapper.handleItemKey(key, vd) ?: return null
        return when (action) {
            is InputMapper.ItemAction.Event -> action.event
            is InputMapper.ItemAction.Message -> null
            is InputMapper.ItemAction.Selection -> {
                selectionState = SelectionState(true, action.target, action.items)
                overlayState = OverlayState.Inventory
                null
            }
        }
    }

    private fun handleDigitSelection(digit: Char, vd: exploration.port.ViewData): InputEvent? {
        if (!selectionState.active) return null

        val index = digit - '1'
        if (index < 0 || index >= selectionState.items.size) {
            closeSelection()
            return null
        }

        val selectedItem = selectionState.items[index]
        val event = when (selectionState.target) {
            SelectionTarget.UNEQUIP -> InputEvent.UnequipItem(selectedItem.name)
            SelectionTarget.DROP -> InputEvent.DropItem(selectedItem.name)
            SelectionTarget.TAKE -> InputEvent.TakeItem(selectedItem.name)
            SelectionTarget.EQUIP -> InputEvent.EquipItem(selectedItem.name)
            else -> null
        }

        closeSelection()
        return event
    }

    private fun closeSelection() {
        selectionState = SelectionState.inactive()
        overlayState = OverlayState.Playing
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
            overlayState = OverlayState.GameOver
        }
    }

    private fun updateStoryOverlay() {
        val vd = viewData ?: return
        if (vd.storyMessages.size > storedStoryCount) {
            overlayState = OverlayState.StoryViewer
        }
        storedStoryCount = vd.storyMessages.size
    }

    private fun renderGameContent() {
        val vd = viewData ?: return
        when (overlayState) {
            OverlayState.Playing -> renderer.render(vd, selectionState)
            OverlayState.Inventory ->
                renderer.renderWithOverlay(vd, overlayState.name, selectionState)
            OverlayState.StoryViewer ->
                renderer.renderStoryViewer(viewData!!, pendingStories, selectionState)
            OverlayState.GameOver -> vd.endGameMessage?.let { msg ->
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

    @Suppress("DEPRECATION")
    private fun saveScreenshot() {
        val screenshotDir = Gdx.files.local("screenshots")
        if (!screenshotDir.exists()) screenshotDir.mkdirs()

        val timestamp = System.currentTimeMillis()
        val fileHandle = screenshotDir.child("screenshot_$timestamp.png")

        try {
            Gdx.app.postRunnable {
                try {
                    val srcPixmap = ScreenUtils.getFrameBufferPixmap(0, 0, Gdx.graphics.width, Gdx.graphics.height)
                    val flipped = Pixmap(srcPixmap.getWidth(), srcPixmap.getHeight(), srcPixmap.format)
                    for (y in 0 until srcPixmap.getHeight()) {
                        flipped.drawPixmap(srcPixmap, 0, flipped.getHeight() - 1 - y, 0, y, srcPixmap.getWidth(), 1)
                    }
                    PixmapIO.writePNG(fileHandle, flipped)
                    srcPixmap.dispose()
                    flipped.dispose()
                    Gdx.app.log("Screenshot", "Saved to ${fileHandle.path()}")
                } catch (e: Exception) {
                    Gdx.app.error("Screenshot", "Failed to save screenshot", e)
                }
            }
        } catch (e: Exception) {
            Gdx.app.error("Screenshot", "Failed to queue screenshot", e)
        }
    }
}
