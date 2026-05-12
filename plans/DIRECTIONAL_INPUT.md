# Directional Input + Core-Owned Move Validation

## Problem
- Lanterna UI maps wasd/arrows to area names then checks if exit exists — validation leaking into adapter
- Text UI sends `InputEvent.Move(areaName)` with freeform area names, but core should own validation
- `Help` and `Quit/Exit` are UI concerns being sent through the core

## Decision
All UIs use directional input only. Core resolves direction index against current area's sorted exits. Help and quit stay in adapters.

---

## Changes

### 1. `InputEvent.kt` — Directional + Look/Activate only
- Remove `data class Move(areaName)` (no more named-area moves)
- Remove `object InvalidMove`
- Remove `object Exit` (quit is UI concern, handled locally in each adapter)
- Keep: `Look`, `Activate`
- Add: `data class MoveDirection(val index: Int) : InputEvent()` — index 0-3 maps to sorted-exits position

### 2. `GameEngineImpl.kt` — Core resolves direction against exits
| Event | Behavior |
|---|---|
| `MoveDirection(idx)` | Look up exit at `idx` from current area's sorted connections. Valid → `processCommand(state, Command.Move(exitName))`. Invalid (no exit) → return old state with `"Can't move that way."`, **no turn increment**, no area change |
| `Look`, `Activate` | No change — pass to `processCommand` as before |

### 3. LanternaUiAdapter.kt
- Remove `exitMove()` helper entirely
- wasd/arrows → unconditional `InputEvent.MoveDirection(idx)` (0=w/up, 1=a/left, 2=s/down, 3=d/right)
- 'h' or unrecognized key → show contextual help in message history **without calling tick**, then re-render
- 'q', Esc → break loop & shutdown immediately (**no event sent to core**)

### 4. KeyUiAdapter.kt
- wasd → unconditional `InputEvent.MoveDirection(idx)` (remove `exits.getOrNull` lookup, remove fallback to `Look`)
- 'l'/'u' → same as before
- 'q', Esc → break loop immediately (**no event sent to core**)

### 5. TextCommandParser.kt — Parse directions + bare keys
```kotlin
"look", "l"              → Look
"activate", "use", "u"   → Activate
"w","a","s","d"          → MoveDirection(0/1/2/3)
"move w/a/s/d"           → MoveDirection(idx)
→ null for anything else (including area names, unknown commands)
```

### 6. TextUiAdapter.kt — Help + quit locally
- Intro: `"Commands: l, u, w/a/s/d, help, quit"`
- Status bar exits: `[w] Forest [a] Cave ...` format instead of comma-separated list (from `view.exits`)
- parseText returns null → show error + help with exit labels, no tick
- "quit" typed by user → break loop immediately (**no event sent to core**)

### 7. TextCommandParserTest.kt — Update tests
- Remove old `Move(areaName)` test
- Add: bare direction keys ("w","a","s","d") → `MoveDirection(0/1/2/3)`
- Add: "move w" etc. → same as above

---

## Key Principles
- Core owns move validation — resolves directional index against exits, decides valid/invalid
- UIs send direction indices unconditionally — no validation logic in adapters
- Help and quit are adapter-only concerns, never sent to core
