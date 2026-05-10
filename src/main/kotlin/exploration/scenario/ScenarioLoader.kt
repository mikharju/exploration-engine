package exploration.scenario

import exploration.model.*
import exploration.state.GameState
import kotlinx.serialization.json.Json
import java.nio.file.Path

private val json = Json { ignoreUnknownKeys = true }

fun loadScenario(configPath: Path): GameState {
    require(configPath.toFile().exists()) { "Scenario file not found: $configPath" }
    require(configPath.toFile().isFile) { "Not a file: $configPath" }

    val config = json.decodeFromString<ScenarioConfig>(configPath.toFile().readText())
    val baseDir = configPath.parent
        ?: throw IllegalArgumentException("Cannot determine parent directory of: $configPath")

    val devicesPath = baseDir.resolve(config.devicesFile)
    require(devicesPath.toFile().exists()) { "Devices file not found: $devicesPath" }

    val areasPath = baseDir.resolve(config.areasFile)
    require(areasPath.toFile().exists()) { "Areas file not found: $areasPath" }

    val deviceEntries = json.decodeFromString<List<DeviceEntry>>(devicesPath.toFile().readText())
    val areaEntries = json.decodeFromString<List<AreaEntry>>(areasPath.toFile().readText())

    val deviceMap = mutableMapOf<String, Device>()
    for (entry in deviceEntries) {
        deviceMap[entry.id] = Device(
            id = DeviceId(entry.id),
            lookDescription = entry.lookDescription,
            activateDescription = entry.activateDescription,
            healthEffect = entry.healthEffect
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
        currentArea = startId
    )

    return GameState(world = world, player = player)
}
