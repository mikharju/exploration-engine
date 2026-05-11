# Lanterna UI Adapter

**Goal:** Add a third screen-buffered TUI using Lanterna's Screen layer. Single-key input, fixed-region layout, no stdout pollution.

## Layer Choice: Screen (not GUI)

GUI layer gives widgets but feels foreign for a game. Screen layer provides double-buffered rendering + `TextGraphics` — closest to ncurses semantics. Blocking single-key input via `screen.readInput()`.

## Layout (fixed regions, redraws each turn)

```
┌──────────────────────────────────────┐  ← Title row (bold cyan)
│   Exploration Engine                 │
├──────────────────────────────────────┤
│                                      │
│   You move to the Cave.              │  ← Message area (10-msg ring buffer,
│   The air is damp and cold...        │     word-wrapped, scrolls upward)
│                                      │
├──────────────────────────────────────┤
│ ████░░ 4/8  Exp:2/4  Dev:1/3        │  ← Status bar (green/red HP bar)
│ [w] Cave  [a] Forest  [s] Ruins     │  ← Exit bindings (yellow bold keys)
└──────────────────────────────────────┘
```

## Regions

| Region | Row(s) | Content |
|---|---|---|
| Title | 0 | Centered, bold cyan "Exploration Engine" |
| Messages | 1 .. h-4 | Ring buffer of last 10 messages, word-wrapped to `w-2`. Oldest scroll off top. |
| Separator | h-3 | Horizontal line (`─`) |
| Status | h-2 | HP bar (█ green / ░ red), health numbers, explored count, device count |
| Exits | h-1 | `[w]`, `[a]`, `[s]`, `[d]` bindings mapped to alphabetically sorted exits |

## Key Bindings (same as KeyUiAdapter)

| Key | Action |
|---|---|
| w/a/s/d | Move — 1st–4th exit in alphabetical order |
| l | Look |
| u | Activate device |
| q / Esc | Quit |

## Game Loop

```
startScreen → hide cursor
render(initialState)
while !gameOver:
    doResizeIfNecessary()
    key = readInput()       // blocking, single-key
    event = mapKey(key, exits)
    if Exit → break
    state = tick(state, event)
    addMessage(history, outputLine)
    render(state)
stopScreen
```

No async needed — turn-based gameplay means blocking input is fine.

## Message History

10-message ring buffer (`MAX_MESSAGES = 10`). New messages push oldest off. Intro text (title, help, goal) pre-populates the buffer. Game-over / win appears as a plain message line in the area — no overlay dialog.

## Word Wrap

Simple greedy wrap at `w-2` columns. Long words without spaces hard-break. Empty lines preserved for readability between messages.

## File Changes

| File | Change |
|---|---|
| `build.gradle.kts` | Add `lanterna:3.1.2` dependency |
| `adapter/ui/lanterna/LanternaUiAdapter.kt` | New — single class, ~200 lines, all rendering + input |
| `cli/Main.kt` | Add `UiMode.LANTERNA`, wire adapter into `when` |

## CLI Usage

```sh
exploration-engine --ui LANTERNA <scenario-file>
```

All three modes coexist: `TEXT` (default), `KEY`, `LANTERNA`.
