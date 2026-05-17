# Scenario File Format — Exploration Engine

A scenario is one or more JSON files: a **config** referencing data files (**areas**, **devices**, optionally **triggers**, **items**). Pass the config path as the first argument.

```bash
./build/install/exploration-engine/bin/exploration-engine scenarios/default/default.json
```

## Config (`default.json`)

Entry point with player starting state and file references.

| Field | Type | Required | Notes |
|---|---|---|---|
| `playerStart.health` | int | yes | ≥ 0, ≤ maxHealth |
| `playerStart.maxHealth` | int | yes | > 0 |
| `playerStart.startArea` | string | yes | Must match an area `id` |
| `areasFile` | string | yes | Relative to config location |
| `devicesFile` | string | yes | Relative to config location |
| `statuses` | object | no | Optional status indicators. Keys are names, values have `initial`, `min`, `max` |
| `triggersFile` | string | no | Relative to config location |
| `itemsFile` | string | no | Relative to config location |

```json
{
  "playerStart": { "health": 3, "maxHealth": 20, "startArea": "Forest" },
  "areasFile": "areas.json",
  "devicesFile": "devices.json",
  "triggersFile": "triggers.json",
  "itemsFile": "items.json",
  "statuses": {
    "corruption": { "initial": 0, "min": 0, "max": 100 }
  }
}
```

## Areas (`areas.json`)

JSON array of area objects. Each exit specifies a target area and direction (North, West, South, East). Directions are not auto-verified as bidirectional — each side defines its own exits explicitly.

| Field | Type | Required | Notes |
|---|---|---|---|
| `id` | string | yes | Unique. Case-sensitive match, case-insensitive input at runtime |
| `description` | string | yes | Shown on entry/look |
| `exits` | exit[] | no | List of exits with direction. Omit or empty = dead-end |
| `deviceId` | string \| null | no | Must match a device `id`. One per area max |

**Exit object:**

| Field | Type | Required | Notes |
|---|---|---|---|
| `areaId` | string | yes | Target area ID. Must exist in the scenario |
| `direction` | string | yes | `"North"`, `"West"`, `"South"`, or `"East"` |

```json
[
  { "id": "Forest", "description": "...", "exits": [{ "areaId": "Cave", "direction": "East" }], "deviceId": "Crystal" },
  { "id": "Cave", "description": "...", "exits": [
      { "areaId": "Forest", "direction": "West" },
      { "areaId": "Ruins", "direction": "South" },
      { "areaId": "Tower", "direction": "North" }
    ], "deviceId": "Glyph Wall" }
]
```

**Legacy format:** `connections: string[]` is still supported for backward compatibility. When used, directions are auto-assigned by alphabetical order of target area names (preserving previous behavior). New scenarios should use the explicit `exits` format.

## Devices (`devices.json`)

JSON array of device objects. Placed in exactly one area.

| Field | Type | Required | Notes |
|---|---|---|---|
| `id` | string | yes | Unique, referenced by areas via `deviceId` |
| `lookDescription` | string | yes | Shown on "look" for unactivated device |
| `activateDescription` | string | yes | Shown on activation |
| `healthEffect` | int | no | Health change on activate. **Default: 0** |
| `statusEffects` | object | no | Map of status names to integer deltas applied on activation. Values are clamped per-status. **Default: empty** |

```json
[
  { "id": "Crystal", "lookDescription": "...", "activateDescription": "...", "healthEffect": 10, "statusEffects": { "corruption": -5 } },
  { "id": "Glyph Wall", "lookDescription": "...", "activateDescription": "...", "healthEffect": 0 }
]
```

## Statuses (`statuses` in config)

Optional numeric indicators beyond health. Each status has a name, starting value, and clamped range.

| Field | Type | Required | Notes |
|---|---|---|---|
| `<name>.initial` | int | yes | Starting value for the player |
| `<name>.min` | int | yes | Lower clamp bound |
| `<name>.max` | int | yes | Upper clamp bound |

Status values are clamped to `[min, max]` after every adjustment. Win/lose conditions remain health-only — statuses don't affect game outcome directly.

**Example:** A `corruption` status with `{ "initial": 0, "min": 0, "max": 100 }` starts at 0. A device with `"statusEffects": { "corruption": 15 }` raises it to 15 (capped at 100).

## Items (`items.json`)

JSON array of pick-up-able objects. Players can take, drop, equip, and unequip items via commands or triggers.

| Field | Type | Required | Notes |
|---|---|---|---|
| `id` | string | yes | Unique identifier |
| `description` | string | yes | Shown on look when the item is in an area |
| `initialLocationType` | string | no | Where the item starts. **Default: `AREA`** |
| `initialLocationId` | string | no | Target for location type `AREA` or `DEVICE`. Required for those types |

**Location types:** `AREA` (in a room), `DEVICE` (at a device), `CARRIED` (player inventory), `EQUIPPED` (worn by player)

```json
[
  { "id": "pliers", "description": "A pair of sturdy iron pliers", "initialLocationType": "AREA", "initialLocationId": "Tower" }
]
```

## Triggers (`triggers.json`)

JSON array of conditional event definitions. Fire automatically on area entry, device activation, or each turn (status triggers).

| Field | Type | Required | Notes |
|---|---|---|---|
| `id` | string | yes | Unique identifier |
| `ownerType` | string | yes | `AREA`, `DEVICE`, or `STATUS`. Determines when the trigger fires |
| `ownerId` | string | yes | Area/device name for AREA/DEVICE. Status name for STATUS (unused at runtime) |
| `conditions` | object[] | no | All must pass for the trigger to fire |
| `effects` | object[] | yes | Applied when conditions are met |
| `singleUse` | boolean | no | **Default: `true`**. If false, fires repeatedly |
| `intervalTurns` | int | no | Minimum turns between firings (only for `singleUse: false`) |

### Conditions

All conditions must pass. Omit for unconditional triggers.

| Field | Type | Notes |
|---|---|---|
| `checkType` | string | `itemCarried`, `itemEquipped`, or omit for status check |
| `statusName` | string | Status to check (default: STATUS) |
| `op` | string | `>` or `<`. **Default: `>`** |
| `threshold` | int | Value to compare against. **Default: 0** |
| `itemId` | string | Item ID for item checks |

### Effects

Each effect is an object with a `type` field plus type-specific fields.

| Type | Fields | Description |
|---|---|---|
| `displayText` | `text` (string) | Shows a message to the player |
| `changeHealth` | `amount` (int) | Changes health by delta |
| `adjustStatus` | `statusName`, `amount` (int) | Adjusts status value, clamped to range |
| `setStatus` | `statusName`, `value` (int) | Sets status to exact value, clamped |
| `setLocation` | `itemId`, `locationType`, `locationId`? | Moves an item to a new location |
| `lockItem` | `itemId` | Prevents the item from being dropped |
| `unlockItem` | `itemId` | Allows the item to be dropped again |

**Owner type timing:**
- `AREA`: fires when entering `ownerId` area (after move)
- `DEVICE`: fires after activating `ownerId` device
- `STATUS`: fires each turn, useful for ongoing damage/status effects

```json
[
  {
    "id": "corruptionDamage",
    "ownerType": "STATUS",
    "ownerId": "corruption",
    "conditions": [
      {"statusName": "corruption", "op": ">", "threshold": 20}
    ],
    "effects": [
      {"type": "displayText", "text": "Corruption courses through your veins."},
      {"type": "changeHealth", "amount": -1}
    ],
    "singleUse": false,
    "intervalTurns": 2
  }
]
```

## Validation & Rules

Engine throws `IllegalArgumentException` on load if violated:
- `startArea`, all `exits` (or legacy `connections`), and all `deviceId` references must exist
- Exit direction strings must be one of: North, West, South, East
- File paths resolve relative to the config file directory (subdirs supported)
- Status `min < max` for each defined status; `initial` must be within `[min, max]`
- Keys in device `statusEffects` must match a declared status name
- Trigger `ownerId` must exist when `ownerType` is `AREA` or `DEVICE`
- Effect references to `itemId` must match an item ID from `itemsFile`

**Win**: visited every area + activated every device. **Lose**: health ≤ 0 (clamped to [0, maxHealth]). Statuses don't affect win/lose.

---

## Maybe Keep (review → keep or delete)

### Designing for LLMs
1. Sketch connections first — ensure full traversal from startArea
2. Sum of positive healthEffect > absolute sum of negatives for viable win path
3. Place healing devices early, dangerous ones in less obvious spots
4. Keep descriptions thematically consistent
5. Test at health=1 to verify not accidentally unwinnable

### Common Mistakes
- Forgetting to define return exits (A→B requires B→A if you want to go back)
- Mismatched `deviceId` strings (case-sensitive)
- Unreachable areas making the scenario unwinnable
- Referencing a device that doesn't exist in devicesFile
- Referencing a status name in `statusEffects` that isn't declared in config `statuses`
- Forgetting `initialLocationId` when `initialLocationType` is `AREA` or `DEVICE`
- Trigger with no conditions firing unintentionally on every turn (STATUS triggers)
- Setting `singleUse: false` without `intervalTurns`, causing per-turn spam
