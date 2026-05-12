package exploration.adapter.ui.text

import exploration.port.GameEngine
import exploration.port.ViewData

class TextUiAdapter(private val engine: GameEngine) {

    fun run(scenarioId: String) {
        var state = engine.start(scenarioId)
        printIntro()
        render(state, engine)

        while (!engine.view(state).gameOver) {
            print("> ")
            val line = readLine()?.trim() ?: break
            if (line.isEmpty()) continue

            val event = parseText(line)
                ?: run {
                    state = state.copy(commandOutput = "Unknown command. Try: look, move <area>, activate")
                    render(state, engine)
                    continue
                }
            state = engine.tick(state, event)
            render(state, engine)
        }

        showEnd(engine.view(state))
    }

    private fun render(state: exploration.state.GameState, eng: GameEngine) {
        val v = eng.view(state)
        if (v.outputLine.isNotBlank()) println(v.outputLine)
        printStatus(v)
    }

    private fun printStatus(v: ViewData) {
        val bar = buildString {
            for (i in 1..v.maxHealth / 2) {
                append(if (i <= v.health / 2) "#" else ".")
            }
        }
        val statusStr = if (v.statuses.isNotEmpty()) {
            " | ${v.statuses.entries.joinToString(" | ") { (k, n) -> "${k.replaceFirstChar { c -> c.uppercaseChar() }}: $n" }}"
        } else ""
        val exits = if (v.exits.isEmpty()) "(none)" else v.exits.joinToString(", ")
        println("[HP: ${v.health}/${v.maxHealth} [$bar] | Explored: ${v.exploredCount}/${v.totalAreas} | Devices: ${v.activatedCount}/${v.totalDevices} | Exits: $exits]$statusStr")
    }

    private fun printIntro() {
        println("=== Exploration Engine ===")
        println("Commands: look, move <area>, activate")
        println("Goal: explore all areas and activate all devices. Don't run out of health!")
        println()
    }

    private fun showEnd(v: ViewData) {
        println()
        if (v.win == true) {
            println("CONGRATULATIONS! You completed the exploration!")
        } else if (v.gameOver && v.win != true) {
            println("GAME OVER - Better luck next time.")
        }
    }
}