package exploration.state

import exploration.model.AreaId
import exploration.model.Direction
import exploration.model.ExitId
import exploration.model.ExitStateData
import exploration.model.DeviceId
import exploration.model.Item

import exploration.model.Player
import exploration.model.World

import exploration.model.StatusRange
import exploration.model.Trigger

data class GameState(
    val world: World,
    val player: Player,
    val exploredAreas: Set<AreaId> = emptySet(),
    val activatedDevices: Set<DeviceId> = emptySet(),
    val commandOutput: String = "",
    val triggerTexts: List<String> = emptyList(),
    val isOver: Boolean = false,
    val win: Boolean? = null,
    val statusBounds: Map<String, StatusRange> = emptyMap(),
    val turn: Int = 0,
    val triggers: List<Trigger> = emptyList(),
    val items: List<Item> = emptyList(),
    val storyMessages: List<String> = emptyList(),
    val exitStates: Map<ExitId, ExitStateData> = emptyMap()
) {
    fun allDeviceIds(): Set<DeviceId> {
        val devices = mutableSetOf<DeviceId>()
        for (area in world.areas.values) {
            area.device?.let { devices.add(it.id) }
        }
        return devices
    }

    fun checkOutcome(): GameState = if (!isOver) {
        val allExplored = exploredAreas == world.areas.keys
        val allActivated = activatedDevices.containsAll(allDeviceIds())
        when {
            player.health <= 0 -> copy(isOver = true, win = false, commandOutput = "You collapse from exhaustion... Game Over.")
            allExplored && allActivated -> copy(isOver = true, win = true, commandOutput = "You've explored every corner and activated every device. Victory!")
            else -> this
        }
    } else this

    fun visibleExits(currentArea: AreaId): List<Direction> {
        val exitsByDir = world.getArea(currentArea).exits.associateBy { it.direction.index }
        return Direction.values().filter { dir ->
            val exit = exitsByDir[dir.index] ?: return@filter false
            val id = ExitId(currentArea, exit.targetArea)
            val data = exitStates[id] ?: ExitStateData() // default: not hidden
            !data.hidden
        }
    }

    fun isExitBlocked(from: AreaId, to: AreaId): Boolean {
        val id = ExitId(from, to)
        val data = exitStates[id] ?: return false
        return data.state != exploration.model.ExitState.OPEN || data.hidden
    }
}
