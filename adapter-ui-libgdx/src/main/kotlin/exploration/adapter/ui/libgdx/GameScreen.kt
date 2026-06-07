package exploration.adapter.ui.libgdx

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.PixmapIO
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.utils.ScreenUtils
import com.badlogic.gdx.utils.viewport.FitViewport

/** Thin libGDX adapter — translates screen lifecycle and input callbacks to GamePresenter + RenderBackend. */
class GameScreen(
    engine: exploration.port.GameEngine,
    scenarioPath: String
) : Screen {

    private val layout = LayoutConfig()
    private val presenter: GamePresenter = GamePresenter(engine, scenarioPath)
    private val camera: OrthographicCamera = OrthographicCamera()
    private val viewport: FitViewport = FitViewport(layout.viewportW, layout.viewportH, camera)
    private val stage: Stage = Stage(viewport)
    private val batch: SpriteBatch = SpriteBatch()
    private val shapeRenderer: ShapeRenderer = ShapeRenderer()
    private val font: BitmapFont = try {
        BitmapFont(Gdx.files.internal("com/badlogic/gdx/utils/lsans-15.fnt"))
    } catch (e: Exception) {
        BitmapFont()
    }

    private val renderer: LibgdxRenderBackend by lazy {
        LibgdxRenderBackend(batch, shapeRenderer, camera, font, layout)
    }

    private val inputProcessor = object : InputProcessor {
        override fun keyDown(keycode: Int): Boolean {
            if (keycode == com.badlogic.gdx.Input.Keys.F12) {
                saveScreenshot()
                return true
            }
            presenter.handleKeyDown(keycode, renderer)
            return true
        }

        override fun keyTyped(character: Char): Boolean {
            val handled = presenter.handleKeyTyped(character, renderer)
            if (presenter.shouldQuit) {
                presenter.shouldQuit = false
                Gdx.app.exit()
            }
            return handled
        }

        override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
            val worldPos = camera.unproject(com.badlogic.gdx.math.Vector3(screenX.toFloat(), screenY.toFloat(), 0f))
            presenter.handleTouchDown(worldPos.x, worldPos.y)
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
        Gdx.input.setInputProcessor(InputMultiplexer(stage, inputProcessor))
        Gdx.app.log("GameReady", "LIBGDX_GAME_READY")
    }

    override fun show() {
        camera.position.set(layout.viewportW / 2f, layout.viewportH / 2f, 0f)
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

    private fun renderGameContent() {
        val vd = presenter.viewData ?: return
        when (presenter.overlayState) {
            OverlayState.Playing -> renderer.render(vd, presenter.selectionState)
            OverlayState.Inventory -> renderer.renderWithOverlay(vd, "Inventory", presenter.selectionState)
            OverlayState.StoryViewer -> renderStoryViewer()
            OverlayState.GameOver -> vd.endGameMessage?.let { renderer.renderGameOver(it) } ?: renderer.render(vd, presenter.selectionState)
            OverlayState.QuitConfirm -> renderer.renderQuitConfirm()
        }
    }

    private fun renderStoryViewer() {
        val vd = presenter.viewData ?: return
        val boxH = layout.storyBoxH
        val availableHeight = boxH - RenderBackend.MARGIN * 2 - renderer.fontLineHeight
        val maxVisibleLines = presenter.getVisibleLinesInHeight(availableHeight, renderer)
        val currentStory = if (presenter.currentStoryIndex in presenter.pendingStories.indices) {
            presenter.pendingStories[presenter.currentStoryIndex]
        } else ""
        renderer.renderStoryViewer(
            vd, listOf(currentStory), presenter.selectionState,
            presenter.storyScrollLines, maxVisibleLines,
            presenter.currentStoryIndex + 1, presenter.pendingStories.size
        )
    }

    override fun resize(width: Int, height: Int) {
        viewport.update(width, height, true)
        camera.position.set(layout.viewportW / 2f, layout.viewportH / 2f, 0f)
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
