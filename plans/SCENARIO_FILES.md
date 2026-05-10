# Plan: Scenario File Loading System

## Overview

Replace hardcoded world data with JSON scenario files loaded at startup. The game engine requires a scenario file path as its first argument. This enables content authors and LLMs to create new scenarios without modifying Kotlin code.

## Changes Summary

| Action | Path | Purpose |
|--------|------|---------|
| Modify | `build.gradle.kts` | Add kotlinx.serialization plugin + dependency |
| New | `src/main/kotlin/exploration/scenario/ScenarioFile.kt` | Serializable data classes for JSON parsing |
| New | `src/main/kotlin/exploration/scenario/ScenarioLoader.kt` | Load scenario from config file, resolve relative files, build GameState |
| Modify | `src/main/kotlin/exploration/cli/Main.kt` | Parse args[0] as scenario path, call loader instead of hardcoded world |
| Modify | `src/main/kotlin/exploration/model/World.kt` | Remove `buildDefaultWorld()` function |
| New | `scenarios/default/default.json` | Default scenario config file |
| New | `scenarios/default/areas.json` | Default areas data |
| New | `scenarios/default/devices.json` | Default devices data |
| Modify | `src/test/kotlin/exploration/model/ModelTest.kt` | Replace buildDefaultWorld() calls with minimal inline builders |
| Modify | `src/test/kotlin/exploration/integration/GameIntegrationTest.kt` | Use inline world builder (no JSON dependency) |
| New | `src/test/kotlin/exploration/scenario/ScenarioLoaderTest.kt` | Dedicated tests for JSON loading, path resolution, validation |
| New | `src/test/resources/scenarios/test-default/config.json` | Small test scenario for loader tests |
| New | `src/test/resources/scenarios/test-default/areas.json` | Test areas data |
| New | `src/test/resources/scenarios/test-default/devices.json` | Test devices data |
| New | `scenarios/SCENARIO_FORMAT.md` | LLM-optimized reference document for scenario file structure and usage |

## Implementation Details

### 1. build.gradle.kts — add kotlinx.serialization

Apply the serialization plugin and add the JSON dependency to both main and test configurations:
```kotlin
plugins {
    kotlin("jvm") version "2.3.21"
    kotlin("plugin.serialization") version "2.3.21"  // NEW
    application
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")  // NEW
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.12.2")
}
```

### 2. ScenarioFile.kt — serializable data classes

New file in `src/main/kotlin/exploration/scenario/`:
- `@Serializable data class PlayerStart(health: Int, maxHealth: Int, startArea: String)`
- `@Serializable data class DeviceEntry(id: String, lookDescription: String, activateDescription: String, healthEffect: Int = 0)`
- `@Serializable data class AreaEntry(id: String, description: String, connections: List<String>, deviceId: String?)`
- `@Serializable data class ScenarioConfig(playerStart: PlayerStart, areasFile: String, devicesFile: String)`

### 3. ScenarioLoader.kt — load scenario from files

New file in `src/main/kotlin/exploration/scenario/`:
- `fun loadScenario(configPath: Path): GameState`
  - Read config JSON from the given path using Json.decodeFromString<ScenarioConfig>()
  - Resolve areasFile and devicesFile relative to config file's parent directory
  - Deserialize both files into lists of entries
  - Build Device map first, then build Area map (areas reference deviceIds)
  - Construct World(areas, startAreaId) — constructor validates connections exist
  - Construct Player with initial health/maxHealth at start area
  - Return GameState(world, player) with empty exploredAreas and activatedDevices

### 4. Main.kt — require scenario file argument

```kotlin
fun main(args: Array<String>) {
    if (args.isEmpty()) {
        println("Usage: exploration-engine <scenario-file>")
        return
    }
    val configPath = Path.of(args[0])
    var state = loadScenario(configPath)
    // ... rest of game loop unchanged
}
```

### 5. World.kt — remove buildDefaultWorld()

Delete the `buildDefaultWorld()` function entirely. The hardcoded world data moves to JSON files under `scenarios/default/`.

### 6. Default scenario files (`scenarios/default/`)

Three files representing the current default world:
- `default.json` — config pointing to areas/devices files, playerStart with health=3, maxHealth=20, startArea="Forest"
- `areas.json` — array of 4 area entries (Forest→Cave, Cave→Forest|Ruins|Tower, Ruins→Cave, Tower→Cave) with deviceId references
- `devices.json` — array of 4 device entries (Crystal +10, Glyph Wall 0, Ancient Machine -5, Orb +3)

### 7. Existing tests — minimal inline builders

Both ModelTest.kt and GameIntegrationTest.kt currently call `buildDefaultWorld()`. Replace with small inline world constructors:
- ModelTest: build 2-area worlds for each test (no need to replicate full default world)
- GameIntegrationTest: keep existing inline builder pattern, tests verify command processing logic not file loading

### 8. ScenarioLoaderTest.kt — dedicated loader tests

New test file covering:
- Load valid scenario from resources → correct GameState with expected areas/devices/player
- Relative path resolution works correctly
- Invalid connection target throws meaningful error
- Invalid device reference throws meaningful error
- Missing devicesFile throws IOException

### 9. Test resources (`src/test/resources/scenarios/test-default/`)

Small 2-area scenario for loader tests:
```
config.json   → { playerStart: {health=10, maxHealth=20, startArea="A"}, areasFile="areas.json", devicesFile="devices.json" }
areas.json    → [ A→B with device1, B→A with device2 ]
devices.json  → [ device1 (heal +5), device2 (neutral) ]
```

### 10. SCENARIO_FORMAT.md — LLM-optimized documentation

New file under `scenarios/`. Purpose: serve as a reference document that an LLM can read to understand how to create new scenario files. Includes:
- Strict JSON schemas for all three file types with field tables (name, type, required, constraints)
- File resolution rules (relative paths from config location)
- Complete default scenario as copy-pasteable example
- Validation rules and common mistakes section

## Execution Order

1. Add kotlinx.serialization to build.gradle.kts
2. Create ScenarioFile.kt with serializable data classes
3. Create ScenarioLoader.kt with loading logic
4. Move hardcoded world data to scenarios/default/ JSON files
5. Update Main.kt to require scenario file argument
6. Remove buildDefaultWorld() from World.kt
7. Update ModelTest.kt and GameIntegrationTest.kt with inline builders
8. Create test resources and ScenarioLoaderTest.kt
9. Create SCENARIO_FORMAT.md documentation
10. Run `./gradlew build` to verify everything compiles and tests pass
