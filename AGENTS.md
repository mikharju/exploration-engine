# Agent Guide — exploration-engine

## Build & Test

```sh
./gradlew build          # compile + test + jar
./gradlew test           # tests only
./gradlew installDist    # create dist with launch scripts
./build/install/exploration-engine/bin/exploration-engine  # run the CLI game
```

No lint/typecheck step separate from `compileKotlin`. JVM 25 required (jvmToolchain(25) in build.gradle.kts).

## Project Structure

Single-module Gradle project (Kotlin 2.3.21, JUnit 5).

| Directory | Purpose |
|---|---|
| `src/main/kotlin/exploration/model/` | Data classes: Area, AreaId, Device, DeviceId, Player, World |
| `src/main/kotlin/exploration/state/` | GameState — immutable snapshot, win/lose check |
| `src/main/kotlin/exploration/command/` | Sealed class Command (Look/Move/Activate), parseInput, processCommand |
| `src/main/kotlin/exploration/cli/` | Main.kt — imperative read-eval loop |

**Entry point**: `exploration.cli.MainKt` (function `main()`)

## Architecture

- **Functional core**: `processCommand(state: GameState, command: Command): GameState` — pure, no side effects.
- **Imperative shell**: Main.kt loop reads input, prints output.
- State is immutable `data class GameState` with `copy(...)` for transitions.

## Commands

Look, move `<area>`, activate — case-insensitive. `use` is an alias for `activate`.

## Default World (4 rooms)

```
Forest -- Cave -- Ruins
             |
           Tower
```

- Player starts in Forest, health=3, maxHealth=20.
- Each room has exactly one device: Crystal (+10 heal), Glyph Wall (0), Ancient Machine (-5), Orb (+3).
- Win: explore all areas + activate all devices. Lose: health ≤ 0.

## Tests

Three test files under `src/test/kotlin/exploration/`:
- `model/ModelTest.kt` — data class invariants, adjustHealth clamping, world validation
- `command/CommandProcessorTest.kt` — move/look/activate rules, win/lose conditions
- `integration/GameIntegrationTest.kt` — full winning/losing playthroughs with default world

Tests use `kotlin.test.*` assertions and JUnit 5 (no Kotest or other framework).

## Conventions

- No generated code, migrations, or build artifacts to manage.
- Gradle wrapper (`gradlew`) is the canonical build invocation.
- Health bar printed via `buildString` loop in `printStatus`.
- Area names are case-insensitive in move parsing (`lowercase()` comparison).
