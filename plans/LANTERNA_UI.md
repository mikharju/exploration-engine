# Lanterna UI Adapter

**Goal:** Add a third screen-buffered TUI using Lanterna's Screen layer. Single-key input, fixed-region layout, no stdout pollution.

## Layer Choice: Screen (not GUI)

GUI layer gives widgets but feels foreign for a game. Screen layer provides double-buffered rendering + `TextGraphics` — closest to ncurses semantics. Blocking single-key input via `screen.readInput()`.

## Layout (fixed regions, redraws each turn)

```
┌──────────────────────────────────────────────┐  ← Title row (bold cyan)
│     Exploration Engine                       │
│                                              │
│   You move to the Cave.                      │  ← Message area (10-msg ring buffer,
│   The air is damp and cold...                │     word-wrapped, scrolls upward)
│                                              │
├──────────────────────────────────────────────┤
│             [w] Cave                         │  ← Direction pad (arrow-cross layout)
│  [a] Forest  [s] Ruins  [d] Tower            │     centered horizontally
│           ██████░░ 15/20 Exp:2/4 Dev:1/3    │  ← Status bar (right-aligned, HP + stats)
└──────────────────────────────────────────────┘
```

## Regions

| Region | Row(s) | Content |
|---|---|---|
| Title | 0 | Centered, bold cyan "Exploration Engine" |
| Messages | 1 .. h-5 | Ring buffer of last 10 messages, word-wrapped to `w-2`. Oldest scroll off top. |
| Separator | h-4 | Horizontal line (`─`) |
| Direction pad | h-3 | `[W]` centered above — arrow-cross layout matching keyboard arrows |
| Direction pad | h-2 | `[A] [S] [D]` in a row, exit names beside each key. Inactive slots dimmed gray with `.` placeholder. Centered horizontally within border. |
| Status | h-1 | HP bar (█ green / ░ red), health numbers, explored/device count — right-aligned |

### Min Terminal Size

Minimum: **60 columns × 20 rows**. Ensures direction pad labels and status stats fit without complex wrapping logic.

If the terminal is smaller, a centered message replaces the game layout:

```
┌─────────────────────┐
│                     │
│  Terminal too small.│
│  Resize to at least │
│  60 columns,        │
│  20 rows.           │
│                     │
└─────────────────────┘
```

Game loop keeps running — layout resumes automatically when the terminal resizes above minimum via `doResizeIfNecessary()`.

## Key Bindings (same as KeyUiAdapter)

| Key | Action |
|---|---|
| w/a/s/d | Move — 1st–4th exit in alphabetical order |
| ↑/↓/←/→ | Same as w/a/s/d respectively |
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
| `adapter/ui/lanterna/LanternaUiAdapter.kt` | Single class, ~260 lines: direction pad, status, messages, min-size guard, input mapping |
| `cli/Main.kt` | Add `UiMode.LANTERNA`, wire adapter into `when` |

## CLI Usage

```sh
exploration-engine --ui LANTERNA <scenario-file>
```

All three modes coexist: `TEXT` (default), `KEY`, `LANTERNA`.
