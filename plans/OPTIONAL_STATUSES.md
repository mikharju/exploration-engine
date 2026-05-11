# Optional Player Status Indicators

## Overview

Generic player statuses beyond health. Scenarios declare which exist with name, initial value, and min/max range. Devices apply numeric effects via a map. Win/lose stays health-only.

## Design Decisions

- **Generic `Map<String, Int>`** on Player — any number of statuses without model changes
- **Per-status min/max clamping** defined in scenario config
- **Devices carry `statusEffects: Map<String, Int>`** alongside existing `healthEffect`
- **Backward compatible**: old scenarios omit statuses; other UIs ignore new ViewData field

## Model Changes

| File | Change |
|---|---|
| `model/StatusDef.kt` | New — `StatusRange(min, max)` data class |
| `model/Player.kt` | Added `statuses: Map<String, Int>` + `adjustStatus(name, amount, range)` with clamp |
| `model/Device.kt` | Added `statusEffects: Map<String, Int> = emptyMap()` |
| `state/GameState.kt` | Added `statusBounds: Map<String, StatusRange>` for clamping reference in command processor |

## Scenario Loading

**JSON schema additions:**
```jsonc
// default.json
{ "statuses": { "corruption": { "initial": 0, "min": 0, "max": 100 } } }

// devices.json per device entry
{ "statusEffects": { "corruption": 15 } }
```

**DTOs:** `StatusEntry(initial, min, max)`, added to `ScenarioConfig.statuses` and `DeviceEntry.statusEffects`.

**Loader:** Builds initial status map from config entries into player; builds bounds map for GameState.

## Command Processor

`processActivate` applies health effect first, then iterates `device.statusEffects`. Each status is adjusted via `player.adjustStatus()` with range from `state.statusBounds`. All changes appended to output: `(health delta, name: newValue)`.

## Port / UI

- **ViewData**: Added `statuses: Map<String, Int>` (default empty). Filtered in GameEngineImpl to exclude zero values.
- **TextUiAdapter**: Renders non-zero statuses after the status bar line: `| Corruption: 15`
- **KeyUiAdapter / LanternaUiAdapter**: No changes — default param on ViewData means they compile and ignore new data

## Default Scenario

Corruption (0–100). Crystal heals + reduces corruption (-5). Ancient Machine damages + increases corruption (+15). Orb, Glyph Wall: no status effects.
