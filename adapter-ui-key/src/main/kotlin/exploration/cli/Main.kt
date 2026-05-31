package exploration.cli

import exploration.adapter.jsonloader.JsonScenarioRepository
import exploration.adapter.storage.InMemoryGameStateStore
import exploration.adapter.ui.key.KeyUiAdapter
import exploration.engine.GameEngineImpl

fun main(args: Array<String>) {
    var path: String? = null
    for (arg in args) {
        if (!arg.startsWith("-")) path = arg
    }
    path ?: error("Usage: exploration-engine <scenario-file>")

    val repo = JsonScenarioRepository()
    val engine = GameEngineImpl(InMemoryGameStateStore(repo))

    KeyUiAdapter(engine).run(path)
}
