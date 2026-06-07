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
    private var currentStoryIndex: Int = 0
    private var storyScrollLines: Int = 0
    private var messagePanelScrollOffset: Int = 0

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

            // Check for new stories while in StoryViewer and append them
            if (overlayState == OverlayState.StoryViewer && pendingStories.isNotEmpty()) {
                val allStories = vd.storyMessages.filter { it.isNotBlank() }
                if (allStories.size > pendingStories.size) {
                    pendingStories = allStories
                    currentStoryIndex = allStories.size - 1
                    storyScrollLines = 0
                }
            }

            // StoryViewer scrolling — Page Up/Down, arrows only (W/S handled in keyTyped)
            if (overlayState == OverlayState.StoryViewer && pendingStories.isNotEmpty()) {
                when (keycode) {
                    com.badlogic.gdx.Input.Keys.PAGE_UP -> storyScrollLines = (storyScrollLines - 5).coerceAtLeast(0)
                    com.badlogic.gdx.Input.Keys.PAGE_DOWN -> storyScrollLines = (storyScrollLines + 5).coerceAtMost(getMaxStoryScroll())
                    com.badlogic.gdx.Input.Keys.UP -> storyScrollLines = (storyScrollLines - 1).coerceAtLeast(0)
                    com.badlogic.gdx.Input.Keys.DOWN -> storyScrollLines = (storyScrollLines + 1).coerceAtMost(getMaxStoryScroll())
                    com.badlogic.gdx.Input.Keys.LEFT -> {
                        currentStoryIndex = (currentStoryIndex - 1).coerceAtLeast(0)
                        storyScrollLines = 0
                    }
                    com.badlogic.gdx.Input.Keys.RIGHT -> {
                        currentStoryIndex = (currentStoryIndex + 1).coerceAtMost(pendingStories.size - 1)
                        storyScrollLines = 0
                    }
                    else -> {}
                }
            }

            // Arrow keys — non-printable, only fire keyDown (but not when StoryViewer is active)
            var event = when {
                overlayState == OverlayState.StoryViewer && pendingStories.isNotEmpty() -> null
                keycode == com.badlogic.gdx.Input.Keys.UP -> InputEvent.MoveDirection(Direction.North)
                keycode == com.badlogic.gdx.Input.Keys.LEFT -> InputEvent.MoveDirection(Direction.West)
                keycode == com.badlogic.gdx.Input.Keys.DOWN -> InputEvent.MoveDirection(Direction.South)
                keycode == com.badlogic.gdx.Input.Keys.RIGHT -> InputEvent.MoveDirection(Direction.East)
                else -> null
            }

            // J — story viewer (non-printable key)
            if (event == null && keycode == com.badlogic.gdx.Input.Keys.J) {
                val allStories = vd.storyMessages.filter { it.isNotBlank() }
                if (allStories.isNotEmpty()) {
                    overlayState = OverlayState.StoryViewer
                    pendingStories = allStories
                    currentStoryIndex = 0
                    storyScrollLines = 0
                }
            }

            // Escape — non-printable key
            if (event == null && keycode == com.badlogic.gdx.Input.Keys.ESCAPE) {
                when (overlayState) {
                    OverlayState.StoryViewer -> {
                        overlayState = OverlayState.Playing
                        pendingStories = emptyList()
                    }
                    OverlayState.Inventory, OverlayState.GameOver -> Gdx.app.exit()
                    OverlayState.QuitConfirm -> overlayState = OverlayState.Playing
                    else -> overlayState = OverlayState.QuitConfirm
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

        override fun keyTyped(character: Char): Boolean {
            val vd = viewData ?: return false

            // Y/N — quit confirmation dialog
            if (overlayState == OverlayState.QuitConfirm) {
                when (character.lowercaseChar()) {
                    'y' -> Gdx.app.exit()
                    'n' -> overlayState = OverlayState.Playing
                }
                return true
            }

            var event: InputEvent? = null

            // Movement — printable chars only fire keyTyped (not keyDown)
            when (character.lowercaseChar()) {
                'w' -> if (overlayState == OverlayState.StoryViewer && pendingStories.isNotEmpty()) {
                    storyScrollLines = (storyScrollLines - 1).coerceAtLeast(0)
                } else {
                    event = InputEvent.MoveDirection(Direction.North)
                }
                'a' -> if (overlayState == OverlayState.StoryViewer && pendingStories.isNotEmpty()) {
                    currentStoryIndex = (currentStoryIndex - 1).coerceAtLeast(0)
                    storyScrollLines = 0
                } else {
                    event = InputEvent.MoveDirection(Direction.West)
                }
                's' -> if (overlayState == OverlayState.StoryViewer && pendingStories.isNotEmpty()) {
                    storyScrollLines = (storyScrollLines + 1).coerceAtMost(getMaxStoryScroll())
                } else {
                    event = InputEvent.MoveDirection(Direction.South)
                }
                'd' -> if (overlayState == OverlayState.StoryViewer && pendingStories.isNotEmpty()) {
                    currentStoryIndex = (currentStoryIndex + 1).coerceAtMost(pendingStories.size - 1)
                    storyScrollLines = 0
                } else {
                    event = InputEvent.MoveDirection(Direction.East)
                }
                'l' -> event = InputEvent.Look
                'u' -> event = InputEvent.Activate
                'i' -> event = InputEvent.Inventory
            }

            // g/p/e/r — item actions (may trigger selection overlay)
            if (event == null) {
                val action = InputMapper.handleItemKey(character, vd)
                when (action) {
                    is InputMapper.ItemAction.Event -> {
                        viewData = engine.tick(gameRef!!, action.event)
                        checkGameOver(viewData!!)
                        updateStoryOverlay()
                    }
                    is InputMapper.ItemAction.Selection -> {
                        selectionState = SelectionState(true, action.target, action.items)
                        overlayState = OverlayState.Inventory
                    }
                    is InputMapper.ItemAction.Message -> {} // no-op for now
                    null -> {}
                }
            }

            // 1-9 — digit selection for multi-item prompts
            if (event == null && character in '0'..'9') {
                event = handleDigitSelection(character, vd)
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

    private fun getMaxStoryScroll(): Int {
        if (pendingStories.isEmpty() || currentStoryIndex < 0 || currentStoryIndex >= pendingStories.size) return 0
        val story = pendingStories[currentStoryIndex]
        val boxW = 700f
        val maxWidthPx = (boxW - Renderer.MARGIN * 2).toInt()
        val maxCharsPerLine = if (maxWidthPx > 0) {
            val testStr = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
            val testWidth = renderer.textWidth(testStr)
            ((testStr.length.toFloat() / testWidth) * maxWidthPx).toInt().coerceIn(1, 200)
        } else 40

        var lineCount = 0
        if (story.isBlank()) return 0
        for (segment in story.split("\n")) {
            if (segment.isBlank()) {
                lineCount += 1
            } else {
                lineCount += renderer.splitText(segment, maxCharsPerLine).size
            }
        }
        val boxH = 500f
        val availableHeight = boxH - Renderer.MARGIN * 2 - renderer.fontLineHeight
        val maxVisibleLines = (availableHeight / (renderer.fontLineHeight * 1.3f)).toInt().coerceAtLeast(1)
        return maxOf(lineCount - maxVisibleLines, 0)
    }

    private fun renderGameContent() {
        val vd = viewData ?: return
        when (overlayState) {
            OverlayState.Playing -> renderer.render(vd, selectionState)
            OverlayState.Inventory ->
                renderer.renderWithOverlay(vd, overlayState.name, selectionState)
            OverlayState.StoryViewer -> {
                val boxH = 500f
                val availableHeight = boxH - Renderer.MARGIN * 2 - renderer.fontLineHeight
                val maxVisibleLines = (availableHeight / (renderer.fontLineHeight * 1.3f)).toInt().coerceAtLeast(1)
                val currentStory = if (currentStoryIndex in pendingStories.indices) pendingStories[currentStoryIndex] else ""
                renderer.renderStoryViewer(viewData!!, listOf(currentStory), selectionState, storyScrollLines, maxVisibleLines, currentStoryIndex + 1, pendingStories.size)
            }
            OverlayState.GameOver -> vd.endGameMessage?.let { msg ->
                renderer.renderGameOver(msg)
            } ?: renderer.render(vd, selectionState)
            OverlayState.QuitConfirm -> renderer.renderQuitConfirm()
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
