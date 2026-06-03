package exploration.adapter.ui.libgdx

import com.badlogic.gdx.Game
import exploration.port.GameEngine

class LibgdxUiAdapter(
    private val engine: GameEngine,
    private val path: String
) : Game() {

    override fun create() {
        screen = GameScreen(engine, path)
        setScreen(screen)
    }

    override fun resize(width: Int, height: Int) {
        screen?.resize(width, height)
    }

    override fun dispose() {
        super.dispose()
    }
}
