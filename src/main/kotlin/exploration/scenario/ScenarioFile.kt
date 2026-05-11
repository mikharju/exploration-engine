package exploration.scenario

import kotlinx.serialization.Serializable

@Serializable
data class PlayerStart(
    val health: Int,
    val maxHealth: Int,
    val startArea: String
) {
    init {
        require(health >= 0) { "Player health must be non-negative" }
        require(maxHealth > 0) { "maxHealth must be positive" }
        require(maxHealth >= health) { "maxHealth must be >= health" }
    }
}

@Serializable
data class StatusEntry(
    val initial: Int,
    val min: Int = 0,
    val max: Int = Int.MAX_VALUE
)

@Serializable
data class DeviceEntry(
    val id: String,
    val lookDescription: String,
    val activateDescription: String,
    val healthEffect: Int = 0,
    val statusEffects: Map<String, Int> = emptyMap()
)

@Serializable
data class AreaEntry(
    val id: String,
    val description: String,
    val connections: List<String>,
    val deviceId: String? = null
)

@Serializable
data class ScenarioConfig(
    val playerStart: PlayerStart,
    val areasFile: String,
    val devicesFile: String,
    val statuses: Map<String, StatusEntry> = emptyMap()
)
