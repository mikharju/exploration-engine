# Agent Guide — exploration-engine

**Conciseness is paramount.** Code and docs must be scannable, 
containing only what's needed. If you're unsure whether something 
belongs here, put it in "Maybe Keep" below so it can be reviewed 
or deleted easily.

## Priorities:

1. Code simple, easy to understand
2. Code and docs concise, quick to read
3. Functional, avoid mutability, where it makes sense
4. Hexagonal architecture, functional core behind interfaces, UIs, scenario loading, such in adapters

## Tests

- JUnit 5 + `kotlin.test.*`.
- Keep tests concise. Prove that complex or important logic works. Coverage is not a priority.

## Build & Run

Each UI variant is an independent Gradle module with its own `installDist`:

```sh
./gradlew :exploration-engine-ui-text:installDist   # TEXT mode (no extra deps)
./gradlew :exploration-engine-ui-key:installDist    # KEY mode (jansi only)
./gradlew :exploration-engine-ui-lanterna:installDist  # TUI mode (lanterna + jansi)
./gradlew :adapter-ui-libgdx:assemble               # Desktop GUI mode (libGDX + LWJGL3)
```

Run: `./adapter-ui-text/build/install/exploration-engine-ui-text/bin/exploration-engine-ui-text <scenario-file>`

LibGDX desktop app: `./adapter-ui-libgdx/build/install/exploration-engine-ui-libgdx/bin/exploration-engine-ui-libgdx <scenario-file>` (1800×1100 windowed mode)

Run graphical game with default scenario: `./default-scenario.sh`

JVM 25 required. No separate lint/typecheck — `compileKotlin` covers it.

## Structure

Multi-module Gradle (Kotlin 2.3.21, JUnit 5). Each UI variant has its own `Main.kt`.

| Path | Contents |
|---|---|
| `core/model/` | Area, Device, Player, World, Trigger, Item data classes |
| `core/state/` | Immutable GameState with win/lose check |
| `core/command/` | Sealed Command (Look/Move/Activate/TakeItem/DropItem/EquipItem/UnequipItem/Inventory), processCommand |
| `core/engine/` | GameEngineImpl, TriggerEngine |
| `core/port/` | Interfaces: GameEngine, ScenarioRepository; types: InputEvent, ViewData |
| `core/adapter/jsonloader/` | JsonScenarioRepository, JsonFileReader (scenario loading) |
| `core/adapter/storage/` | InMemoryGameStateStore |
| `core/scenario/` | `ScenarioFile.kt` (JSON entries), `ScenarioLoader.kt` (assembleGame) |
| `adapter-ui-text/src/main/kotlin/exploration/cli/Main.kt` | TextUiAdapter entry point |
| `adapter-ui-key/src/main/kotlin/exploration/cli/Main.kt` | KeyUiAdapter entry point |
| `adapter-ui-lanterna/src/main/kotlin/exploration/cli/Main.kt` | LanternaUiAdapter entry point |
| `adapter-ui-libgdx/` | Desktop GUI (libGDX + LWJGL3, 1800×1100) — `LibgdxUiAdapter`, `GameScreen`, `InputMapper`, `Renderer`, components |

App module (`app/`) contains shared utilities (InMemoryGameStateStore, JsonScenarioRepository moved to core) and test resources. Each adapter-ui-* module is independently buildable with its own `MainKt`.

## Scenario Format

See `scenarios/SCENARIO_FORMAT.md` for full JSON file format spec. Read it into context before making scenario-related changes.

## Architecture

- Pure functional core: `processCommand(GameState, Command) -> GameState`. Imperative shell in Main.kt. State transitions via `copy(...)`.
- Put logic and decision making into core, keep UIs and adapters thin when it makes sense (example: command validation logic in core)

## Agent behavior

- Prefer Grep and Glob when finding files to read instead of find and other bash commands when possible
- Do not spawn multiple sub agents in parallel, one at a time only
- If there is need to use temporary files, put them inside project directory under temp directory. These include extracted library sources when finding out how a library works.

## Game Rules

- Commands: `look`, `move w/a/s/d` (indexed directions), `activate/use`, `take <item>`, `drop <item>`, `equip <item>`, `unequip <item>`, `inv` — case-insensitive
- Game end: defined by triggers, no semantics for winning or losing, trigger may give farewell message which indicates either win or loss

## Screenshots

- Save screenshot by pressing **F12** (built-in)
- Screenshots are saved to `screenshots/` under the working directory
- Window resolution: **1800×1100** (windowed mode)
- Default scenario: `scenarios/default/default.json`
- Visual testing inside sbx requires Xvfb — see SKILL.md for headless setup
---

## Maybe Keep (review → keep or delete)

- No generated code, migrations, or build artifacts to manage
- Gradle wrapper (`gradlew`) is the canonical build invocation
