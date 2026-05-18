package exploration.adapter.ui.text

import exploration.core.model.Direction
import exploration.port.*

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

        val exitStr = if (v.exits.values.all { it == null }) {
            "(none)"
        } else {
            Direction.values().mapNotNull { dir ->
                v.exits[dir]?.let { info ->
                    val suffix = if (info.blocked) " (B)" else ""
                    "[${dir.name[0]}] ${info.name}$suffix"
                }
            }.joinToString("  ").ifEmpty { "(none)" }
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
