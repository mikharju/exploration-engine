# Doors — Exit State Management

Exits can be blocked and/or hidden, independently tracked per directional exit pair (Forest→Cave ≠ Cave→Forest).

## Properties

| Property | Values | Meaning |
|----------|--------|---------|
| `state` | OPEN / BLOCKED | Physical passability. Blocked exits show `(B)` in UI; movement fails with "That way is blocked." |
| `hidden` | true / false | Visibility/epistemic fog. Hidden exits are invisible to the player — not shown by look or WASD layout. |

## Computed Rules

```
visible      = !hidden
traversable  = state == OPEN && !hidden
```

Default (no exit state data): `OPEN`, non-hidden — fully passable and visible.

## Trigger Effects

Effects modify exit state at runtime:

| Effect | JSON | What it does |
|--------|------|-------------|
| **setExitBlocked** | `{ "type": "SetExitBlocked", "fromId": "...", "toId": "...", "blocked": true }` | Toggles passability. UI shows/hides `(B)` live. Hidden exits remain hidden regardless of state change. |
| **hideExit** | `{ "type": "HideExit", "fromId": "...", "toId": "..." }` | Conceals the exit from all views. Player must discover it again (e.g., via showExit). State is unchanged — exiting hides only affects visibility. |
| **showExit** | `{ "type": "ShowExit", "fromId": "...", "toId": "..." }` | Reveals a hidden exit. Does not unblock it — the exit may still be blocked and show `(B)`. |

Effects merge: multiple triggers can modify the same exit in one turn. State changes accumulate; hidden flag is overwritten by last-writer-wins across all effects for that exit.

## JSON Schema (exits.json / areas → exits field)

```json
{
  "targetArea": "Sanctuary",
  "direction": "East",
  "initialState": "blocked",
  "hidden": true
}
```

- `initialState`: `"open"` (default) or `"blocked"`. Sets exit state at game start.
- `hidden`: defaults to `false`. Exits are visible unless explicitly hidden.

## JSON Schema (triggers.json → effects field)

```json
{
  "type": "ShowExit",
  "fromId": "Ruins",
  "toId": "Sanctuary"
}
```

- `fromId` / `toId`: area IDs identifying the unidirectional exit. Must reference valid areas in the scenario.
- `blocked` (SetExitBlocked only): `true` to block, `false` to open.

## UI Conventions

| UI | Open visible | Blocked visible | Hidden |
|----|-------------|-----------------|--------|
| Text | `[w] Cave` | `[w] Sanctuary (B)` | not shown |
| Key (raw) | `  [w] Cave     ` | `  [w] Sanctuary (B)    ` | not shown |
| Lanterna | green text, key bold | grey text + `(B)` suffix, key bold | not shown |

## Default Scenario Example

Ruins → Sanctuary is blocked and hidden at start. Two devices interact with it:

- **Ancient Machine** (Ruins): `ShowExit` — reveals the exit so players can see it
- **Orb** (Tower): `SetExitBlocked(false)` — unblocks passage so movement succeeds

Both effects must fire to reach Sanctuary, creating a puzzle chain.
