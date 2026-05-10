# Scenario File Format — Exploration Engine

A scenario is three JSON files: a **config** referencing two data files (**areas**, **devices**). Pass the config path as the first argument.

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

```json
{
  "playerStart": { "health": 3, "maxHealth": 20, "startArea": "Forest" },
  "areasFile": "areas.json",
  "devicesFile": "devices.json"
}
```

## Areas (`areas.json`)

JSON array of area objects. Connections are bidirectional — if A lists B, B must list A.

| Field | Type | Required | Notes |
|---|---|---|---|
| `id` | string | yes | Unique. Case-sensitive match, case-insensitive input at runtime |
| `description` | string | yes | Shown on entry/look |
| `connections` | string[] | yes | IDs of reachable areas. All must exist. Empty = dead-end |
| `deviceId` | string \| null | no | Must match a device `id`. One per area max |

```json
[
  { "id": "Forest", "description": "...", "connections": ["Cave"], "deviceId": "Crystal" },
  { "id": "Cave", "description": "...", "connections": ["Forest", "Ruins", "Tower"], "deviceId": "Glyph Wall" }
]
```

## Devices (`devices.json`)

JSON array of device objects. Placed in exactly one area.

| Field | Type | Required | Notes |
|---|---|---|---|
| `id` | string | yes | Unique, referenced by areas via `deviceId` |
| `lookDescription` | string | yes | Shown on "look" for unactivated device |
| `activateDescription` | string | yes | Shown on activation |
| `healthEffect` | int | no | Health change on activate. **Default: 0** |

```json
[
  { "id": "Crystal", "lookDescription": "...", "activateDescription": "...", "healthEffect": 10 },
  { "id": "Glyph Wall", "lookDescription": "...", "activateDescription": "...", "healthEffect": 0 }
]
```

## Validation & Rules

Engine throws `IllegalArgumentException` on load if violated:
- `startArea`, all `connections`, and all `deviceId` references must exist
- `health ≥ 0`, `maxHealth > 0`, `health ≤ maxHealth`
- File paths resolve relative to the config file directory (subdirs supported)

**Win**: visited every area + activated every device. **Lose**: health ≤ 0 (clamped to [0, maxHealth]).

---

## Maybe Keep (review → keep or delete)

### Designing for LLMs
1. Sketch connections first — ensure full traversal from startArea
2. Sum of positive healthEffect > absolute sum of negatives for viable win path
3. Place healing devices early, dangerous ones in less obvious spots
4. Keep descriptions thematically consistent
5. Test at health=1 to verify not accidentally unwinnable

### Common Mistakes
- Forgetting bidirectional connections (A→B requires B→A)
- Mismatched `deviceId` strings (case-sensitive)
- Unreachable areas making the scenario unwinnable
- Referencing a device that doesn't exist in devicesFile
