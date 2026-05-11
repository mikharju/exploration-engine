# Lanterna UI Adapter — Two-Panel Layout

**Goal:** Replace single message area with split layout: messages left, statuses right. WASD direction pad and bottom border unchanged.

## Screen Structure (64×23 min)

```
┌──────────────────────────────────────────────────────┐ r0 title
│               Exploration Engine                      │
│  Messages              │  Statuses                    │
│  ──────────────        │  ─────────                   │
│                        │                              │
│  You enter the         │  ██████░░░░ HP:13/20          │ r3
│  Dark Cave.            │  Exp: 3/4   Dev: 1/3          │ r4
│                        │  Corruption: 30/100           │
│  > activate            │  Fatigue: 8/50                │
│  (+10 health)          │                              │
│                        │                              │
├───────────────┼────────┼──────────────────────────────┤ rh-5
         [w] Forest                               │      rh-3
      [a] Cave    [s] Ruins   [d] Tower                   rh-2
└──────────────────────────────────────────────────────┘ rh-1
```

## Regions

| Region | Row(s) | Content |
|---|---|---|
| Title | 0 | Centered, bold cyan "Exploration Engine" |
| Panels header + underline | 1–2 | `"Messages"` / `"Statuses"` with `─` underline |
| Left panel (messages) | 3..h-6 | Ring buffer of last 10 messages, word-wrapped to left width. Oldest scroll off top. |
| Right panel — HP bar | +0 after header | `██████░░░░ HP:13/20` (green █ / red ░) |
| Right panel — progress | +1 after HP | `Exp: 3/4   Dev: 1/3` |
| Right panel — statuses | remaining rows | Optional non-zero statuses below HP/progress. Name truncated with `...` if too long. `(more...)` suffix if overflow. Always visible even when empty (just shows HP+progress). |
| Separator | h-5 | `├─────┼───────────┤` — horizontal line with split char at panel boundary |
| Direction pad | h-3..h-2 | WASD arrow-cross shape matching keyboard. Unchanged from before. |

## Column Split

```
leftWidth  = (w - 3) * 0.6   // ~60% for messages, word-wrap friendly
rightWidth = (w - 3) * 0.4   // remaining for statuses list
```

Vertical separator at column `1 + leftWidth` drawn through panel area rows `1..h-6`.

## Min Terminal Size

**64 columns × 23 rows**. Right panel needs ~15 usable chars; extra row for HP+progress lines.

If terminal is smaller: centered "Terminal too small" message with current min dimensions. Layout resumes on resize via `doResizeIfNecessary()`.

## Status Display Rules

- Only non-zero statuses shown (filtered in GameEngineImpl)
- Format: `Name: value` or `Name: value/max` when bounds available
- Name truncated to fit right panel width, trailing `...`, minimum 4 visible chars
- If count exceeds available rows → last slot shows `(more...)`

## Input / Loop

Unchanged — same key bindings and blocking game loop. See original LANTERNA_UI.md section "Key Bindings" and "Game Loop".

## Model Changes Needed

| File | Change |
|---|---|
| `port/ViewData.kt` | Add `statusBounds: Map<String, StatusRange> = emptyMap()` + import `exploration.model.StatusRange` |
| `core/engine/GameEngineImpl.kt` | Pass `statusBounds = state.statusBounds` in ViewData constructor |

## LanternaUiAdapter Rewrite

| Function (before) | After |
|---|---|
| `render()` — single area + status right | Dual-panel layout with split column, separator at h-5 |
| `drawMessages(g, history, pos, width, maxRows)` | `drawMessagesPanel` — header + wrapped content in left panel bounds |
| *(none)* | `drawStatusesPanel` — HP bar + progress lines (always visible), optional statuses below with truncation + overflow |
| `drawStatusRight` | Removed — HP logic moved into right panel |
| `drawDirectionPad(g, exits, width, h-3)` | Unchanged |
| `printEndSummary()` | Append statuses line if present |

## File Changes

| File | Change |
|---|---|
| `port/ViewData.kt` | Add `statusBounds` field + import |
| `core/engine/GameEngineImpl.kt` | Pass bounds to ViewData |
| `adapter/ui/lanterna/LanternaUiAdapter.kt` | Layout rewrite: constants, render(), new panel functions, removed drawStatusRight |
