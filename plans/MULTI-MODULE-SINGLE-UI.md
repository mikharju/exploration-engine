# Multi-Module: Single UI Per Build (VERIFIED)

## Structure

```
core/                          -- exploration-engine-core (library)
  src/main/kotlin/exploration/core/model/       -- Area, Device, Player, World, Trigger, Item, Direction, StatusDef, ItemLocation
  src/main/kotlin/exploration/core/state/       -- GameState
  src/main/kotlin/exploration/core/command/     -- Command, CommandProcessor
  src/main/kotlin/exploration/core/engine/      -- GameEngineImpl, TriggerEngine
  src/main/kotlin/exploration/port/             -- GameEngine, InputEvent, ViewData, GameStateStore, ScenarioRepository, GameRef
  src/main/kotlin/exploration/scenario/         -- ScenarioFile, ScenarioLoader

adapter-ui-text/               -- exploration-engine-ui-text (library)
  adapter/ui/text/TextUiAdapter.kt
  adapter/ui/text/TextCommandParser.kt

adapter-ui-key/                -- exploration-engine-ui-key (library)
  adapter/ui/key/KeyUiAdapter.kt

adapter-ui-lanterna/           -- exploration-engine-ui-lanterna (library)
  adapter/ui/lanterna/LanternaUiAdapter.kt, InputMapper.kt, UiUtils.kt, Overlay types

app/                           -- exploration-engine (application)
  src-text/main/kotlin/exploration/cli/Main.kt       -- TextUiAdapter entry point
  src-key/main/kotlin/exploration/cli/Main.kt        -- KeyUiAdapter entry point
  src-lanterna/main/kotlin/exploration/cli/Main.kt   -- LanternaUiAdapter entry point
  src/main/kotlin/exploration/adapter/storage/*
  src/main/kotlin/exploration/adapter/jsonloader/*
  src/test/resources/scenarios/test-default/         -- test scenario JSON files
```

## Build-time Selection

Gradle property `defaultUI` selects the UI at build time (defaults to "text"):

```sh
./gradlew -PdefaultUI=text       installDist   # text, no extra deps
./gradlew -PdefaultUI=key        installDist   # key mode, jansi only
./gradlew -PdefaultUI=lanterna   installDist   # TUI mode, lanterna + jansi
```

Run: `./app/build/install/exploration-engine/bin/exploration-engine <scenario-file>`

App build.gradle.kts configures sourceSets to use the correct UI source dir based on `defaultUi` property. Three Main.kt files exist in separate source directories — Gradle compiles only one per variant.

## Build-time Fix (Critical)

The app/build.gradle.kts uses absolute paths via `rootProject.projectDir` for source dirs:
```kotlin
val uiSourceDir = when (defaultUi.lowercase()) {
    "key" -> file("${rootProject.projectDir}/src-key/main/kotlin")
    "lanterna" -> file("${rootProject.projectDir}/src-lanterna/main/kotlin")
    else -> file("${rootProject.projectDir}/src-text/main/kotlin")
}
```

This ensures Gradle finds the Main.kt files when `sourceSets.main.kotlin.srcDir(uiSourceDir)` is called. Relative paths via `layout.projectDirectory` don't work for source directories in subprojects.

## JVM Toolchain

Set to Java 21 (environment has OpenJDK 21, not 25 as originally planned).

## Tests

All tests pass:
- core: GameStateTest, GameEngineImplTest, CommandProcessorTest, ScenarioLoaderTest
- adapter-ui-text: TextCommandParserTest
- adapter-ui-lanterna: InputMapperTest, UiUtilsTest
- app (via `:`): InMemoryGameStateStoreTest

ScenarioLoaderTest stays in app module because it depends on JsonScenarioRepository.

## Verified ✅

- [x] All modules compile independently
- [x] Tests pass across all 5 modules
- [x] Text UI builds and runs with scenarios/default/default.json
- [x] Key UI builds successfully
- [x] Lanterna UI builds successfully
- [x] AGENTS.md updated with correct build instructions
