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
- Keep tests concise.
- Prove that complex or important logic works.
- Coverage is not a priority.
 
## Build & Run

```sh
./gradlew build          # compile + test + jar
./build/install/exploration-engine/bin/exploration-engine  # run CLI game
```

JVM 25 required. No separate lint/typecheck — `compileKotlin` covers it.

## Structure

Single-module Gradle (Kotlin 2.3.21, JUnit 5). 
Entry: `exploration.cli.MainKt.main()`

| Path | Contents |
|---|---|
| `model/` | Area, Device, Player, World data classes |
| `state/` | Immutable GameState with win/lose check |
| `command/` | Sealed Command (Look/Move/Activate), parseInput, processCommand |
| `cli/Main.kt` | Read-eval-print loop |

## Architecture

Pure functional core: `processCommand(GameState, Command) -> GameState`. Imperative shell in Main.kt. State transitions via `copy(...)`.

## Game Rules

- Commands: `look`, `move <area>`, `activate` (alias: `use`) — case-insensitive
- Default world: Forest → Cave ←→ Ruins, Tower (4 rooms)
- Win: visit all areas + activate all devices. Lose: health ≤ 0.

---

## Maybe Keep (review → keep or delete)

- No generated code, migrations, or build artifacts to manage
- Gradle wrapper (`gradlew`) is the canonical build invocation
- Health bar printed via `buildString` loop in `printStatus`
- Area names use `lowercase()` comparison in move parsing
- Player starts health=3, maxHealth=20; devices: Crystal(+10), Glyph Wall(0), Ancient Machine(-5), Orb(+3)
