package exploration.scenario

import exploration.model.ItemLocationType
import exploration.model.LocationTarget
import exploration.model.AreaId
import exploration.model.DeviceId
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
data class ExitEntry(
    val areaId: String,
    val direction: String
)

@Serializable
data class AreaEntry(
    val id: String,
    val description: String,
    val connections: List<String>? = null,
    val exits: List<ExitEntry>? = null,
    val deviceId: String? = null
)

@Serializable
data class ItemEntry(
    val id: String,
    val description: String,
    val initialLocationType: String = "AREA",
    val initialLocationId: String? = null
) {
    fun toLocation(): exploration.model.Location {
        val type = when (initialLocationType.uppercase()) {
            "AREA" -> ItemLocationType.AREA
            "DEVICE" -> ItemLocationType.DEVICE
            "CARRIED" -> ItemLocationType.CARRIED
            "EQUIPPED" -> ItemLocationType.EQUIPPED
            else -> error("Unknown location type: $initialLocationType")
        }
        val target = when (type) {
            ItemLocationType.AREA -> LocationTarget.InArea(AreaId(checkNotNull(initialLocationId)))
            ItemLocationType.DEVICE -> LocationTarget.OnDevice(DeviceId(checkNotNull(initialLocationId)))
            else -> null
        }
        return exploration.model.Location(type, target)
    }
}

@Serializable
data class TriggerCondition(
    val checkType: String? = null,
    val statusName: String? = null,
    val op: String? = null,
    val threshold: Int? = null,
    val itemId: String? = null
)

@Serializable
data class EffectEntry(
    val type: String,
    val text: String? = null,
    val amount: Int? = null,
    val statusName: String? = null,
    val value: Int? = null,
    val itemId: String? = null,
    val locationType: String? = null,
    val locationId: String? = null
)

@Serializable
data class TriggerEntry(
    val id: String,
    val ownerType: String,
    val ownerId: String,
    val conditions: List<TriggerCondition> = emptyList(),
    val effects: List<EffectEntry>,
    val singleUse: Boolean = true,
    val intervalTurns: Int? = null
)

@Serializable
data class ScenarioConfig(
    val playerStart: PlayerStart,
    val areasFile: String,
    val devicesFile: String,
    val statuses: Map<String, StatusEntry> = emptyMap(),
    val triggersFile: String? = null,
    val itemsFile: String? = null
)
