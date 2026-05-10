package exploration.adapter.ui.key

import exploration.port.*

class KeyUiAdapter(private val engine: GameEngine) {

    fun run(scenarioId: String) {
        var state = engine.start(scenarioId)
        printIntro()
        render(state, engine)

        while (!engine.view(state).gameOver) {
            state = engine.tick(state, waitForInput())
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
        println("[HP: ${v.health}/${v.maxHealth} [$bar] | Explored: ${v.exploredCount}/${v.totalAreas} | Devices: ${v.activatedCount}/${v.totalDevices}]")
        printWasdLayout(v.exits)
    }

    private fun printWasdLayout(exits: List<String>) {
        val w = exits.getOrElse(0) { "" }
        val a = exits.getOrElse(1) { "" }
        val s = exits.getOrElse(2) { "" }
        val d = exits.getOrElse(3) { "" }

        println()
        val padW = if (w.isEmpty()) "  [w]       " else "  [w] $w    "
        println(padW.padEnd(40))
        println()

        val leftPad = (26 - a.length).coerceAtLeast(3)
        println("[a] ${a.ifEmpty { "  " }}${" ".repeat(leftPad)}[d] ${d.ifEmpty { "  " }}")
        println()

        val padS = if (s.isEmpty()) "  [s]       " else "  [s] $s    "
        println(padS.padEnd(40))
        println("[l]ook  [u]se  [q]uit | Press a key:")
    }

    private fun waitForInput(): InputEvent {
        while (true) {
            val r = System.`in`.read()
            if (r < 0) return InputEvent.Exit

            val ch = r.toChar().lowercaseChar()
            when (ch) {
                'w' -> return InputEvent.MoveDirection(DirectionSlot.W)
                'a' -> return InputEvent.MoveDirection(DirectionSlot.A)
                's' -> return InputEvent.MoveDirection(DirectionSlot.S)
                'd' -> return InputEvent.MoveDirection(DirectionSlot.D)
                'l' -> return InputEvent.Look
                'u' -> return InputEvent.Activate
                'q', '\u001b' -> return InputEvent.Exit
            }
        }
    }

    private fun printIntro() {
        println("=== Exploration Engine (Key Mode) ===")
        println("w/a/s/d: move | l: look | u: activate | q: quit")
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
        System.out.print("Press any key to exit... ")
        System.`in`.read()
    }
}