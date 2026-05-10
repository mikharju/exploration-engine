package exploration.core.engine

import exploration.command.Command
import exploration.command.parseInput
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
        if (state.isOver) return state.copy(output = "Game is over. Restart to play again.")

        val command: Command? = when (event) {
            is InputEvent.Text -> parseInput(event.text)
            is InputEvent.Look -> Command.Look
            is InputEvent.Activate -> Command.Activate
            is InputEvent.MoveDirection -> moveBySlot(state, event.slot)
            is InputEvent.Exit -> return state.copy(isOver = true, output = "Goodbye.", win = null)
        }

        if (command == null) {
            return state.copy(output = "Nothing there.")
        }

        return processCommand(state, command)
    }

    override fun view(state: GameState): ViewData {
        val area = state.world.getArea(state.player.currentArea)
        return ViewData(
            outputLine = state.output,
            health = state.player.health,
            maxHealth = state.player.maxHealth,
            currentAreaName = area.id.name,
            exploredCount = state.exploredAreas.size,
            totalAreas = state.world.areas.size,
            activatedCount = state.activatedDevices.size,
            totalDevices = state.allDeviceIds().size,
            exits = sortedExits(state),
            gameOver = state.isOver,
            win = state.win
        )
    }

    private fun moveBySlot(state: GameState, slot: DirectionSlot): Command? {
        val exits = sortedExitAreaIds(state)
        val idx = slot.ordinal
        return if (idx < exits.size) Command.Move(exits[idx].name) else null
    }

    private fun sortedExits(state: GameState): List<String> =
        sortedExitAreaIds(state).map { it.name }

    private fun sortedExitAreaIds(state: GameState): List<AreaId> =
        state.world.getArea(state.player.currentArea).connections.toList()
            .sortedBy { it.name.lowercase() }
}