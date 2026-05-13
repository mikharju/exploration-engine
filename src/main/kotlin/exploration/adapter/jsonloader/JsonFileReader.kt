package exploration.adapter.jsonloader

import kotlinx.serialization.json.Json
import exploration.scenario.AreaEntry
import exploration.scenario.DeviceEntry
import exploration.scenario.ItemEntry
import exploration.scenario.ScenarioConfig
import exploration.scenario.TriggerEntry
import java.nio.file.Path

private val json = Json { ignoreUnknownKeys = true }

data class ScenarioFiles(
    val config: ScenarioConfig,
    val areas: List<AreaEntry>,
    val devices: List<DeviceEntry>,
    val triggers: List<TriggerEntry> = emptyList(),
    val items: List<ItemEntry> = emptyList()
)

fun loadScenarioFiles(configPath: Path): ScenarioFiles {
    require(configPath.toFile().exists()) { "Scenario file not found: $configPath" }
    require(configPath.toFile().isFile) { "Not a file: $configPath" }

    val config = json.decodeFromString<ScenarioConfig>(configPath.toFile().readText())
    val baseDir = configPath.parent
        ?: throw IllegalArgumentException("Cannot determine parent directory of: $configPath")

    val devicesPath = baseDir.resolve(config.devicesFile)
    require(devicesPath.toFile().exists()) { "Devices file not found: $devicesPath" }

    val areasPath = baseDir.resolve(config.areasFile)
    require(areasPath.toFile().exists()) { "Areas file not found: $areasPath" }

    val triggers = config.triggersFile?.let { file ->
        val triggersPath = baseDir.resolve(file)
        require(triggersPath.toFile().exists()) { "Triggers file not found: $triggersPath" }
        json.decodeFromString<List<TriggerEntry>>(triggersPath.toFile().readText())
    } ?: emptyList()

    val items = config.itemsFile?.let { file ->
        val itemsPath = baseDir.resolve(file)
        require(itemsPath.toFile().exists()) { "Items file not found: $itemsPath" }
        json.decodeFromString<List<ItemEntry>>(itemsPath.toFile().readText())
    } ?: emptyList()

    return ScenarioFiles(
        config = config,
        devices = json.decodeFromString<List<DeviceEntry>>(devicesPath.toFile().readText()),
        areas = json.decodeFromString<List<AreaEntry>>(areasPath.toFile().readText()),
        triggers = triggers,
        items = items
    )
}
