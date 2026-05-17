# Directional Exits — What Was Done

## Problem

Area connections were a `Set<AreaId>` with no direction metadata. Pressing `w`/`a`/`s`/`d` mapped to exits by **alphabetical index** of target area names, making directional movement arbitrary and preventing intentional map design.

## Solution

Every exit now has an explicit direction. The engine sorts exits by Direction index (`w=0, a=1, s=2, d=3`) instead of alphabetically.

## Changes

### Core model
- **New:** `core/model/Direction.kt` — Direction enum moved to domain layer from port layer; added `parse(name)` for JSON deserialization
- **Changed:** `Area.kt` — new `Exit(targetArea, direction)` data class; field changed from `connections: Set<AreaId>` → `exits: Set<Exit>` with computed `connections` property for backward compat

### Scenario loading
- **New:** `ExitEntry(areaId, direction)` in `ScenarioFile.kt`; both `connections` and `exits` nullable in `AreaEntry`
- **Changed:** `ScenarioLoader.kt` — new format uses explicit directions; legacy `connections` only auto-assigns by alphabetical order (preserves existing behavior)

### Engine & UI
- **Changed:** `GameEngineImpl.sortedExits()` — direction-index sort instead of alphabetical; returns `List<String?>` indexed by direction slot
- **Changed:** `ViewData.exits` type to `List<String?>`; all three UI adapters handle nullable exit slots

### Scenario files migrated
**Before:** `{"id": "Forest", "connections": ["Cave"]}`

**After:**
```json
{
  "id": "Forest",
  "exits": [
    { "areaId": "Cave", "direction": "East" }
  ]
}
```

Updated: `scenarios/default/areas.json`, test default scenario.

### Docs
- `SCENARIO_FORMAT.md` — new `exits` format table, Exit object fields documented, legacy backward-compat noted, bidirectional note removed

## Key Decisions

- **Backward compat:** old `connections` arrays still load; directions auto-assigned alphabetically
- **Computed `connections`:** derives from exits, single source of truth — World.kt/CommandProcessor.kt validation unchanged
- **Direction ownership:** lives in core/model per architecture rules; InputEvent uses typealias
