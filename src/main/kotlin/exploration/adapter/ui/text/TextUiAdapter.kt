package exploration.adapter.ui.text

import exploration.port.GameEngine
import exploration.port.GameRef
import exploration.port.InputEvent
import exploration.port.ViewData

class TextUiAdapter(private val engine: GameEngine) {

    fun run(scenarioId: String) {
        var ref: GameRef
        try {
            ref = engine.start(scenarioId)
        } catch (e: Exception) {
            println("Error loading scenario '$scenarioId': ${e.message}")
            e.printStackTrace()
            return
        }

        printIntro()
        val initialView = engine.tick(ref, InputEvent.Look)
        render(initialView)

        var lastView = initialView
        while (lastView.endGameMessage == null) {
            print("> ")
            val line = readLine()?.trim() ?: break
            if (line.isEmpty()) continue
            if (line.lowercase() == "quit") break

            val event = parseText(line)
                ?: run {
                    println("Unknown command.\nCommands: l, u, w/a/s/d, take <item>, drop <item>, equip <item>, unequip <item>, inv, help, quit")
                    printStatus(lastView)
                    continue
                }
            if (line.lowercase() == "help") {
                println("Commands: l(ook), u(se/activate), w/a/s/d(move by direction), help, quit\nExits are listed below with their key bindings.")
                printStatus(lastView)
                continue
            }
            lastView = engine.tick(ref, event)
            render(lastView)
        }

        showEnd(lastView)
    }

    private fun render(v: ViewData) {
        if (v.outputLine.isNotBlank()) println(v.outputLine)
        for (msg in v.storyMessages) {
            if (msg.isNotBlank()) println(msg)
        }
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
        val exitStr = if (v.exits.filterNotNull().isEmpty()) {
            "(none)"
        } else {
            v.exits.mapIndexed { idx, info ->
                val label = when (idx) {
                    0 -> "[w] "
                    1 -> "[a] "
                    2 -> "[s] "
                    3 -> "[d] "
                    else -> "[$idx] "
                }
                if (info == null) label + ".".padStart(8)
                else {
                    val suffix = if (info.blocked) " (B)" else ""
                    "${label}${info.name}$suffix".let { s -> s.padEnd(if (idx < 4) 10 else 6) }
                }
            }.joinToString(" ").trim()
        }

        println("[HP: ${v.health}/${v.maxHealth} [$bar] | Exits: $exitStr]$statusStr")
    }

    private fun printIntro() {
        println("=== Exploration Engine ===")
        println("Commands: l, u, w/a/s/d, help, quit")
        println()
    }

    private fun showEnd(v: ViewData) {
        println()
        if (v.endGameMessage != null) println(v.endGameMessage)
        else if (v.outputLine.isNotBlank()) println(v.outputLine)
        println()
    }
}
