package exploration.core.engine

import exploration.core.command.Command
import exploration.core.command.processCommand
import exploration.core.model.Direction
import exploration.core.model.ItemLocationType
import exploration.core.model.LocationTarget
import exploration.port.*
import exploration.core.state.GameState

class GameEngineImpl(
    private val store: GameStateStore
) : GameEngine {

    override fun start(scenarioId: String): GameRef = store.createGame(scenarioId)

    override fun tick(ref: GameRef, event: InputEvent): ViewData {
        var state = store.loadGame(ref)
        if (state.endGameMessage != null) return makeEndView(state)

        val command: Command? = when (event) {
            is InputEvent.Look -> Command.Look
            is InputEvent.Activate -> Command.Activate
            is InputEvent.MoveDirection -> resolveDirection(state, event.direction)
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

    private fun resolveDirection(state: GameState, dir: Direction): Command? {
        val area = state.world.getArea(state.player.currentArea)
        val exit = area.exits.find { it.direction == dir } ?: return null
        if (state.isExitBlocked(state.player.currentArea, exit.targetArea)) return null
        return Command.Move(exit.targetArea.name)
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
            endGameMessage = state.endGameMessage,
            areaItems = state.items
                .filter { it.location.type == ItemLocationType.AREA && (it.location.target as? LocationTarget.InArea)?.areaId == area.id }
                .map { ItemView(it.id.name, it.description, it.locked) },
            carriedItems = buildItemViews(state, ItemLocationType.CARRIED),
            equippedItems = buildItemViews(state, ItemLocationType.EQUIPPED),
            storyMessages = state.storyMessages
        )
    }

    private fun makeEndView(state: GameState): ViewData {
        val area = state.world.getArea(state.player.currentArea)
        return ViewData(
            commandText = "Game over",
            triggerTexts = emptyList(),
            outputLine = state.endGameMessage ?: "The game is over.",
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
            endGameMessage = state.endGameMessage,
            areaItems = emptyList(),
            carriedItems = emptyList(),
            equippedItems = emptyList()
        )
    }

    private fun buildItemViews(state: GameState, type: ItemLocationType): List<ItemView> {
        return state.items
            .filter { it.location.type == type }
            .map { ItemView(it.id.name, it.description, it.locked) }
    }

    private fun sortedExits(state: GameState): Map<Direction, ExitInfo?> {
        val area = state.world.getArea(state.player.currentArea)
        val visibleDirs = state.visibleExits(state.player.currentArea)
        return buildMap {
            for (dir in Direction.values()) {
                if (dir in visibleDirs) {
                    val exit = area.exits.find { it.direction == dir }
                    this[dir] = exit?.let { ExitInfo(it.targetArea.name, state.isExitBlocked(state.player.currentArea, it.targetArea)) }
                } else {
                    this[dir] = null
                }
            }
        }
    }
}
