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
        while (!lastView.gameOver) {
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

        val finalView = engine.tick(ref, InputEvent.Look)
        showEnd(finalView)
    }

    private fun render(v: ViewData) {
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
        val exitStr = if (v.exits.isEmpty()) {
            "(none)"
        } else {
            v.exits.mapIndexed { idx, name ->
                when (idx) {
                    0 -> "[w] $name"
                    1 -> "[a] $name"
                    2 -> "[s] $name"
                    3 -> "[d] $name"
                    else -> "[$idx] $name"
                }
            }.joinToString(" ")
        }

        println("[HP: ${v.health}/${v.maxHealth} [$bar] | Explored: ${v.exploredCount}/${v.totalAreas} | Devices: ${v.activatedCount}/${v.totalDevices} | Exits: $exitStr]$statusStr")
    }

    private fun printIntro() {
        println("=== Exploration Engine ===")
        println("Commands: l, u, w/a/s/d, help, quit")
        println("Goal: explore all areas and activate all devices. Don't run out of health!")
        println()
    }

    private fun showEnd(v: ViewData) {
        println()
        if (v.outputLine.isNotBlank()) println(v.outputLine)
        println()
        if (v.win == true) {
            println("CONGRATULATIONS! You completed the exploration!")
        } else if (v.gameOver && v.win != true) {
            println("GAME OVER - Better luck next time.")
        }
    }
}
