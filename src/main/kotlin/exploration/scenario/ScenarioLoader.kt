package exploration.scenario

import exploration.model.*
import exploration.state.GameState

fun assembleGame(config: ScenarioConfig, areaEntries: List<AreaEntry>, deviceEntries: List<DeviceEntry>): GameState {
    val statusInitial = mutableMapOf<String, Int>()
    val statusBounds = mutableMapOf<String, StatusRange>()
    for ((name, entry) in config.statuses) {
        statusInitial[name] = entry.initial
        statusBounds[name] = StatusRange(entry.min, entry.max)
    }

    val deviceMap = mutableMapOf<String, Device>()
    for (entry in deviceEntries) {
        deviceMap[entry.id] = Device(
            id = DeviceId(entry.id),
            lookDescription = entry.lookDescription,
            activateDescription = entry.activateDescription,
            healthEffect = entry.healthEffect,
            statusEffects = entry.statusEffects
        )
    }

    val areaIds = mutableSetOf<String>()
    for (entry in areaEntries) {
        areaIds.add(entry.id)
    }

    val areasMap = mutableMapOf<AreaId, Area>()
    for (entry in areaEntries) {
        if (entry.deviceId != null && entry.deviceId !in deviceMap) {
            throw IllegalArgumentException("Area '${entry.id}' references unknown device: ${entry.deviceId}")
        }

        val connections = mutableSetOf<AreaId>()
        for (connName in entry.connections) {
            if (connName !in areaIds) {
                throw IllegalArgumentException(
                    "Area '${entry.id}' connects to unknown area: $connName"
                )
            }
            connections.add(AreaId(connName))
        }

        areasMap[AreaId(entry.id)] = Area(
            id = AreaId(entry.id),
            description = entry.description,
            connections = connections,
            device = entry.deviceId?.let { deviceMap[it]!! }
        )
    }

    val startId = AreaId(config.playerStart.startArea)
    if (startId !in areasMap.keys) {
        throw IllegalArgumentException("Start area '${config.playerStart.startArea}' not found in areas")
    }

    val world = World(areas = areasMap, startArea = startId)

    val player = Player(
        health = config.playerStart.health,
        maxHealth = config.playerStart.maxHealth,
        currentArea = startId,
        statuses = statusInitial
    )

    return GameState(world = world, player = player, statusBounds = statusBounds)
}
