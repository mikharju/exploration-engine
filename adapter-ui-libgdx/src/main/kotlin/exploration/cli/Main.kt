package exploration.cli

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import exploration.adapter.jsonloader.JsonScenarioRepository
import exploration.adapter.storage.InMemoryGameStateStore
import exploration.adapter.ui.libgdx.LibgdxUiAdapter
import exploration.engine.GameEngineImpl

fun main(args: Array<String>) {
    var path: String? = null
    for (arg in args) {
        if (!arg.startsWith("-")) path = arg
    }
    path ?: error("Usage: exploration-engine <scenario-file>")

    val repo = JsonScenarioRepository()
    val engine = GameEngineImpl(InMemoryGameStateStore(repo))

    val config = Lwjgl3ApplicationConfiguration()
    config.setTitle("Exploration Engine")
    Lwjgl3Application(LibgdxUiAdapter(engine, path), config)
}
