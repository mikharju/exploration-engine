package exploration.adapter.ui.key

import exploration.port.*
import org.fusesource.jansi.AnsiConsole

class KeyUiAdapter(private val engine: GameEngine) {

    fun run(scenarioId: String) {
        AnsiConsole.systemInstall()
        enableRawInput()
        try {
            var state = engine.start(scenarioId)
            printIntro()
            render(state, engine)

            while (!engine.view(state).gameOver) {
                val exits = engine.view(state).exits
                val event = waitForInput(exits)
                if (event is InputEvent.Exit) break
                state = engine.tick(state, event)
                render(state, engine)
            }

            showEnd(engine.view(state))
        } finally {
            disableRawInput()
            AnsiConsole.systemUninstall()
        }
    }

    private fun runStty(args: String) {
        try {
            ProcessBuilder("stty", "-F", "/dev/tty", *args.split(" ").toTypedArray())
                .redirectError(ProcessBuilder.Redirect.DISCARD)
                .start()
                .waitFor()
        } catch (_: Exception) {}
    }

    private fun enableRawInput() {
        if (!openTty()) {
            println("Warning: /dev/tty not available — falling back to stdin.")
        }
        runStty("-icanon -echo min 1 time 0")
    }

    private fun disableRawInput() {
        ttyStream?.close()
        runStty("icanon echo")
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
    }

    private var ttyStream: java.io.FileInputStream? = null

    private fun openTty(): Boolean {
        return try {
            ttyStream = java.io.FileInputStream("/dev/tty")
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun waitForInput(exits: List<String>): InputEvent {
        val stream = ttyStream ?: System.`in`
        while (true) {
            val r = stream.read()
            if (r < 0) return InputEvent.Exit

            when (val ch = r.toChar()) {
                '\u001b' -> return InputEvent.Exit
                else -> {
                    val lc = ch.lowercaseChar()
                    when (lc) {
                        'w', 'a', 's', 'd' -> {
                            val idx = listOf('w', 'a', 's', 'd').indexOf(lc)
                            return exits.getOrNull(idx)?.let { InputEvent.Move(it) }
                                ?: InputEvent.Look
                        }
                        'l' -> return InputEvent.Look
                        'u' -> return InputEvent.Activate
                        'q' -> return InputEvent.Exit
                    }
                }
            }
        }
    }

    private fun printIntro() {
        println("=== Exploration Engine (Key Mode) ===")
        println("w/a/s/d: move | l: look | u: activate | q/esc: quit")
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
