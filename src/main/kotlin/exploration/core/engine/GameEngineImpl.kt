package exploration.core.engine

import exploration.command.Command
import exploration.command.processCommand
import exploration.model.AreaId
import exploration.port.*
import exploration.state.GameState

class GameEngineImpl(
    private val scenarioRepo: ScenarioRepository
) : GameEngine {

    override fun start(scenarioId: String): GameState =
        scenarioRepo.load(scenarioId)

    override fun tick(state: GameState, event: InputEvent): GameState {
        if (state.isOver) return state.copy(commandOutput = "Game is over. Restart to play again.")

        val command: Command = when (event) {
            is InputEvent.Look -> Command.Look
            is InputEvent.Activate -> Command.Activate
            is InputEvent.Move -> Command.Move(event.areaName)
            is InputEvent.InvalidMove -> return state.copy(commandOutput = "Can't move that way.")
            is InputEvent.Exit -> return state.copy(isOver = true, commandOutput = "Goodbye.", win = null)
        }

        return processCommand(state, command)
    }

    override fun view(state: GameState): ViewData {
        val area = state.world.getArea(state.player.currentArea)
        return ViewData(
            commandText = state.commandOutput,
            triggerTexts = state.triggerTexts,
            outputLine = if (state.triggerTexts.isEmpty()) state.commandOutput else "${state.commandOutput}\n${state.triggerTexts.joinToString("\n")}",
            health = state.player.health,
            maxHealth = state.player.maxHealth,
            currentAreaName = area.id.name,
            exploredCount = state.exploredAreas.size,
            totalAreas = state.world.areas.size,
            activatedCount = state.activatedDevices.size,
            totalDevices = state.allDeviceIds().size,
            exits = sortedExits(state),
            statuses = state.player.statuses.filterValues { it != 0 },
            statusBounds = state.statusBounds,
            gameOver = state.isOver,
            win = state.win
        )
    }

    private fun sortedExits(state: GameState): List<String> =
        state.world.getArea(state.player.currentArea).connections
            .map { it.name }
            .sortedBy { it.lowercase() }
}