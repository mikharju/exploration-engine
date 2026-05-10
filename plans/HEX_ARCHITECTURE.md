# Hex Architecture + Key UI

**Goal:** Separate functional core behind interfaces. Add a second key-driven UI alongside the existing text command prompt.

## Ports (interfaces — `port/`)

| Port | Purpose |
|---|---|
| `InputEvent` | Sealed class for all input: typed text, WASD direction slots, Look, Activate, Exit |
| `ViewData` | Read-only projection of GameState — output line, health bar data, exits (alphabetically sorted), progress counts, game-over flag |
| `GameEngine` | Core interface: `start(id)`, `tick(state, event) -> state`, `view(state) -> ViewData` |
| `ScenarioRepository` | Loads initial GameState from an opaque identifier string |

## Engine (`core/engine/GameEngineImpl`)

Implements `GameEngine`. Depends only on ports and core packages. Maps WASD direction slots to the alphabetically sorted connection list of the current area — empty slots produce a no-op message. Owns `tick()`, the turn boundary and game-over detection.

**Game loop lives in the core.** UI renders, gathers input, calls `engine.tick()`. Core owns win/lose state so the UI can't allow invalid turns.

## Scenario Loading Separated

`loadScenario(Path)` stays as-is in `scenario/`. A thin adapter (`adapter/jsonloader/JsonScenarioRepository`) implements `ScenarioRepository` by delegating to it. Future loaders (procedural, database) add new adapters without touching core logic.

## UI Adapters — Both Implement the Same Pattern

```
loop:
  render(viewData)
  if gameOver → farewell screen, break
  event = waitForInput()
  state = engine.tick(state, event)
```

### Text UI (`adapter/ui/text/TextUiAdapter`)

Current `cli/Main.kt` logic refactored. Prompt-based, free-form command typing.

### Key UI (`adapter/ui/key/KeyUiAdapter`)

Single-char input via `System.in.read()`:

| Key | Action |
|---|---|
| w/a/s/d | Move — maps to 1st–4th alphabetically sorted exit |
| l | Look |
| u | Activate device |
| q / esc | Exit game |

**WASD layout** rendered as a diamond matching keyboard geometry:

```
                        [w] AreaName
       
[a] AreaName                          [d] AreaName
 
                        [s] 
```

Empty slots render `[s]` with no area name — visually distinct from filled positions, keeping the layout stable for muscle memory. Exits always appear in the same alphabetical order across playthroughs of the same scenario.

**Game-over**: No screen clear. Previous text stays as a log. Farewell message prints inline, waits for any key to exit.

## UI Selection

CLI flag: `exploration-engine [--ui TEXT|KEY] <scenario-file>`. Default is `TEXT`. No config file — selecting between 2 UIs doesn't warrant persistence.

## File Structure After Refactor

```
src/main/kotlin/exploration/
├── port/                    # Boundary interfaces only
│   ├── GameEngine.kt
│   ├── InputEvent.kt
│   ├── ViewData.kt
│   └── ScenarioRepository.kt
├── core/                    # Domain logic, depends on ports only
│   ├── model/               # Area, Device, Player, World — unchanged
│   ├── state/               # GameState + viewData() extension
│   ├── command/             # Command sealed class, processCommand — unchanged
│   └── engine/              # GameEngineImpl
└── adapter/                 # External-facing, depends on core + ports
    ├── ui/text/             # TextUiAdapter
    ├── ui/key/              # KeyUiAdapter
    └── jsonloader/          # JsonScenarioRepository (wraps existing scenario/)
```

`scenario/` stays in place as-is — `adapter/jsonloader/JsonScenarioRepository` delegates to it. No code moves within `scenario/`.

`Main.kt` becomes a thin wiring layer (~15 lines).

## Tests

- `CommandProcessorTest` — unchanged (pure command logic)
- `GameIntegrationTest` — unchanged (uses processCommand directly)
- New: `core/engine/GameEngineImplTest` — verifies direction slot mapping and view projection. Marked for review on necessity.
