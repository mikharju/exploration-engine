package exploration.cli

import exploration.adapter.jsonloader.JsonScenarioRepository
import exploration.adapter.storage.InMemoryGameStateStore
import exploration.adapter.ui.key.KeyUiAdapter
import exploration.adapter.ui.lanterna.LanternaUiAdapter
import exploration.adapter.ui.text.TextUiAdapter
import exploration.core.engine.GameEngineImpl

enum class UiMode { TEXT, KEY, LANTERNA }

fun main(args: Array<String>) {
    val (mode, scenarioPath) = parseArgs(args)
        ?: error("Usage: exploration-engine [--ui TEXT|KEY] <scenario-file>")

    val repo = JsonScenarioRepository()
    val engine = GameEngineImpl(InMemoryGameStateStore(repo))

    when (mode) {
        UiMode.TEXT     -> TextUiAdapter(engine).run(scenarioPath)
        UiMode.KEY      -> KeyUiAdapter(engine).run(scenarioPath)
        UiMode.LANTERNA -> LanternaUiAdapter(engine).run(scenarioPath)
    }
}

private fun parseArgs(args: Array<String>): Pair<UiMode, String>? {
    var mode = UiMode.TEXT
    var path: String? = null

    var i = 0
    while (i < args.size) {
        when {
            args[i] == "--ui" && i + 1 < args.size -> {
                mode = UiMode.valueOf(args[++i].uppercase())
            }
            !args[i].startsWith("-") -> path = args[i]
        }
        i++
    }

    return path?.let { Pair(mode, it) }
}