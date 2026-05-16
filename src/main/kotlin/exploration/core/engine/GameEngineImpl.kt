package exploration.core.engine

import exploration.command.Command
import exploration.command.processCommand
import exploration.model.ItemLocationType
import exploration.model.LocationTarget
import exploration.port.*
import exploration.state.GameState

class GameEngineImpl(
    private val scenarioRepo: exploration.port.ScenarioRepository,
    private val store: GameStateStore
) : GameEngine {

    override fun start(scenarioId: String): GameRef = store.createGame(scenarioId)

    override fun tick(ref: GameRef, event: InputEvent): ViewData {
        var state = store.loadGame(ref)
        if (state.isOver) return makeGameOverView(state, "Game is over. Restart to play again.")

        val command: Command? = when (event) {
            is InputEvent.Look -> Command.Look
            is InputEvent.Activate -> Command.Activate
            is InputEvent.MoveDirection -> resolveDirection(state, event.direction.index)
            is InputEvent.TakeItem -> Command.TakeItem(event.itemName)
            is InputEvent.DropItem -> Command.DropItem(event.itemName)
            is InputEvent.EquipItem -> Command.EquipItem(event.itemName)
            is InputEvent.UnequipItem -> Command.UnequipItem(event.itemName)
            is InputEvent.Inventory -> Command.Inventory
        }

        state = if (command != null) processCommand(state, command) else state.copy(commandOutput = "Can't move that way.")
        store.saveGame(ref, state)
        return makeView(state)
    }

    private fun resolveDirection(state: GameState, index: Int): Command? {
        val exitName = sortedExits(state).getOrNull(index) ?: return null
        return Command.Move(exitName)
    }

    private fun makeView(state: GameState): ViewData {
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
            win = state.win,
            areaItems = state.items
                .filter { it.location.type == ItemLocationType.AREA && (it.location.target as? LocationTarget.InArea)?.areaId == area.id }
                .map { ItemView(it.id.name, it.description) },
            carriedItems = buildItemViews(state, ItemLocationType.CARRIED),
            equippedItems = buildItemViews(state, ItemLocationType.EQUIPPED)
        )
    }

    private fun makeGameOverView(state: GameState, commandOutput: String): ViewData {
        val area = state.world.getArea(state.player.currentArea)
        return ViewData(
            commandText = commandOutput,
            triggerTexts = emptyList(),
            outputLine = commandOutput,
            health = state.player.health,
            maxHealth = state.player.maxHealth,
            currentAreaName = area.id.name,
            exploredCount = state.exploredAreas.size,
            totalAreas = state.world.areas.size,
            activatedCount = state.activatedDevices.size,
            totalDevices = state.allDeviceIds().size,
            exits = sortedExits(state),
            statuses = emptyMap(),
            statusBounds = emptyMap(),
            gameOver = true,
            win = false,
            areaItems = emptyList(),
            carriedItems = emptyList(),
            equippedItems = emptyList()
        )
    }

    private fun buildItemViews(state: GameState, type: ItemLocationType): List<ItemView> {
        return state.items
            .filter { it.location.type == type }
            .map { ItemView(it.id.name, it.description) }
    }

    private fun sortedExits(state: GameState): List<String> =
        state.world.getArea(state.player.currentArea).connections
            .map { it.name }
            .sortedBy { it.lowercase() }
}
