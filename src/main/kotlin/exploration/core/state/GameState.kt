package exploration.state

import exploration.model.AreaId
import exploration.model.DeviceId
import exploration.model.Player
import exploration.model.World

import exploration.model.StatusRange

data class GameState(
    val world: World,
    val player: Player,
    val exploredAreas: Set<AreaId> = emptySet(),
    val activatedDevices: Set<DeviceId> = emptySet(),
    val output: String = "",
    val isOver: Boolean = false,
    val win: Boolean? = null,
    val statusBounds: Map<String, StatusRange> = emptyMap()
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
            player.health <= 0 -> copy(isOver = true, win = false, output = "You collapse from exhaustion... Game Over.")
            allExplored && allActivated -> copy(isOver = true, win = true, output = "You've explored every corner and activated every device. Victory!")
            else -> this
        }
    } else this
}
