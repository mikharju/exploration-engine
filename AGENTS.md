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

```sh
./gradlew build          # compile + test + jar
./build/install/exploration-engine/bin/exploration-engine <scenario-file>  # TEXT UI
./build/install/exploration-engine/bin/exploration-engine --ui KEY   <scenario-file>  # raw-key mode
./build/install/exploration-engine/bin/exploration-engine --ui LANTERNA <scenario-file>  # TUI
```

JVM 25 required. No separate lint/typecheck — `compileKotlin` covers it.

## Structure

Single-module Gradle (Kotlin 2.3.21, JUnit 5). Entry: `exploration.cli.MainKt.main()`

| Path | Contents |
|---|---|
| `core/model/` | Area, Device, Player, World, Trigger, Item data classes |
| `core/state/` | Immutable GameState with win/lose check |
| `core/command/` | Sealed Command (Look/Move/Activate/TakeItem/DropItem/EquipItem/UnequipItem/Inventory), processCommand |
| `core/engine/` | GameEngineImpl, TriggerEngine |
| `port/` | Interfaces: GameEngine, ScenarioRepository; types: InputEvent, ViewData |
| `adapter/` | JSON loader (`jsonloader/`), UI adapters (`text/`, `key/`, `lanterna/`) |
| `scenario/` | `ScenarioFile.kt` (JSON entries), `ScenarioLoader.kt` (assembleGame) |

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

---

## Maybe Keep (review → keep or delete)

- No generated code, migrations, or build artifacts to manage
- Gradle wrapper (`gradlew`) is the canonical build invocation
