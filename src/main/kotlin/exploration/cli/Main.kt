package exploration.cli

import exploration.command.Command
import exploration.command.parseInput
import exploration.command.processCommand
import exploration.model.buildDefaultWorld
import exploration.model.Player
import exploration.state.GameState

fun playGame() {
    val world = buildDefaultWorld()
    var state = GameState(
        world = world,
        player = Player(health = 3, maxHealth = 20, currentArea = world.startArea)
    )

    println("=== Exploration Engine ===")
    println("Commands: look, move <area>, activate")
    println("Goal: explore all areas and activate all devices. Don't run out of health!")
    println()
    printStatus(state)

    while (!state.isOver) {
        print("> ")
        val line = readLine()?.trim() ?: break

        val command = parseInput(line)
        if (command == null) {
            state = state.copy(output = "Unknown command. Try: look, move <area>, activate")
            println(state.output)
        } else {
            state = processCommand(state, command)
            println(state.output)
        }

        printStatus(state)
    }

    println()
    if (state.win == true) {
        println("CONGRATULATIONS! You completed the exploration!")
    } else {
        println("GAME OVER - Better luck next time.")
    }
}

private fun printStatus(state: GameState) {
    val healthBar = buildString {
        for (i in 1..state.player.maxHealth / 2) {
            append(if (i <= state.player.health / 2) "#" else ".")
        }
    }
    val exits = state.world.getArea(state.player.currentArea).connections.joinToString(", ")
    println("[HP: ${state.player.health}/${state.player.maxHealth} [$healthBar] | Explored: ${state.exploredAreas.size}/${state.world.areas.size} | Exits: $exits]")
}

fun main() = playGame()
