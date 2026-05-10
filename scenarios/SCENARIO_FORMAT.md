# Scenario File Format — Exploration Engine

## Overview

A scenario is defined by three JSON files: a **config** file that references two data files (**areas** and **devices**). The game engine loads all three at startup via the config file path passed as its first argument.

```bash
./build/install/exploration-engine/bin/exploration-engine scenarios/default/default.json
```

## File Structure

### Config File (any name, e.g., `default.json`)

The entry point. Contains player starting state and references to areas/devices files.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `playerStart` | object | yes | Player initial status |
| `areasFile` | string | yes | Filename of areas JSON (relative to config location) |
| `devicesFile` | string | yes | Filename of devices JSON (relative to config location) |

#### playerStart object

| Field | Type | Required | Constraints |
|-------|------|----------|-------------|
| `health` | integer | yes | ≥ 0, must be ≤ maxHealth |
| `maxHealth` | integer | yes | > 0 |
| `startArea` | string | yes | Must match the `id` of one area in areasFile |

```json
{
  "playerStart": {
    "health": 3,
    "maxHealth": 20,
    "startArea": "Forest"
  },
  "areasFile": "areas.json",
  "devicesFile": "devices.json"
}
```

### Areas File (e.g., `areas.json`)

A JSON array of area objects. Each area defines a location the player can visit.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `id` | string | yes | Unique identifier for this area. Used by connections and startArea references. Case-sensitive matching with case-insensitive command input at runtime. |
| `description` | string | yes | Shown to the player when they enter or look at this area. |
| `connections` | array of strings | yes | IDs of areas reachable from here (bidirectional travel). Empty array creates a dead-end room. Every entry must match an existing area id. |
| `deviceId` | string or null | no | The id of the device placed in this area. If omitted or null, the area has no device. Must match a deviceId in devicesFile if present. Only one device per area. |

```json
[
  {
    "id": "Forest",
    "description": "A dense forest with tall pines and dappled sunlight.",
    "connections": ["Cave"],
    "deviceId": "Crystal"
  },
  {
    "id": "Cave",
    "description": "A dim cavern with stalactites and a cool breeze.",
    "connections": ["Forest", "Ruins", "Tower"],
    "deviceId": "Glyph Wall"
  }
]
```

### Devices File (e.g., `devices.json`)

A JSON array of device objects. Each device is an interactable item placed in exactly one area.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `id` | string | yes | Unique identifier for this device. Referenced by areas via deviceId. |
| `lookDescription` | string | yes | Shown to the player during a "look" command when they are in the area containing this unactivated device. |
| `activateDescription` | string | yes | Shown when the player activates this device. Include health effect context in text for clarity. |
| `healthEffect` | integer | no | Health change applied on activation. Positive = heal, negative = damage, 0 = neutral. **Default: 0** if omitted. |

```json
[
  {
    "id": "Crystal",
    "lookDescription": "A glowing crystal pulses with soft blue light.",
    "activateDescription": "The crystal flares brightly, warmth flooding through you.",
    "healthEffect": 10
  },
  {
    "id": "Glyph Wall",
    "lookDescription": "Ancient glyphs cover the cave wall, shimmering faintly.",
    "activateDescription": "The glyphs rearrange into a clear message: 'All paths lead to understanding.'",
    "healthEffect": 0
  }
]
```

## Validation Rules

The engine validates these constraints at load time and throws `IllegalArgumentException` if violated:

1. **startArea must exist** — `playerStart.startArea` must match an area id in areasFile.
2. **All connections must be valid** — every entry in an area's `connections` array must match an existing area id.
3. **Device references must be valid** — if an area specifies a `deviceId`, it must exist in devicesFile.
4. **Health constraints** — `health ≥ 0`, `maxHealth > 0`, and `maxHealth ≥ health`.

## Win/Lose Conditions

- **Win**: player has visited every area (via "look") AND activated every device that exists.
- **Lose**: player's health drops to 0 or below after activating a device with negative healthEffect. Health is clamped to [0, maxHealth].

## Path Resolution

`areasFile` and `devicesFile` are resolved relative to the directory containing the config file. If the config is at `/game/scenarios/myscenario/config.json`, then `"areasFile": "locations.json"` resolves to `/game/scenarios/myscenario/locations.json`.

Subdirectories are supported: `"areasFile": "data/areas.json"`.

## Complete Default Scenario Example

### default.json
```json
{
  "playerStart": {
    "health": 3,
    "maxHealth": 20,
    "startArea": "Forest"
  },
  "areasFile": "areas.json",
  "devicesFile": "devices.json"
}
```

### areas.json
```json
[
  {
    "id": "Forest",
    "description": "A dense forest with tall pines and dappled sunlight.",
    "connections": ["Cave"],
    "deviceId": "Crystal"
  },
  {
    "id": "Cave",
    "description": "A dim cavern with stalactites and a cool breeze.",
    "connections": ["Forest", "Ruins", "Tower"],
    "deviceId": "Glyph Wall"
  },
  {
    "id": "Ruins",
    "description": "Crumbled stone ruins overgrown with vines.",
    "connections": ["Cave"],
    "deviceId": "Ancient Machine"
  },
  {
    "id": "Tower",
    "description": "A tall stone tower interior with a spiral staircase.",
    "connections": ["Cave"],
    "deviceId": "Orb"
  }
]
```

### devices.json
```json
[
  {
    "id": "Crystal",
    "lookDescription": "A glowing crystal pulses with soft blue light.",
    "activateDescription": "The crystal flares brightly, warmth flooding through you. (+10 health)",
    "healthEffect": 10
  },
  {
    "id": "Glyph Wall",
    "lookDescription": "Ancient glyphs cover the cave wall, shimmering faintly.",
    "activateDescription": "The glyphs rearrange into a clear message: 'All paths lead to understanding.'",
    "healthEffect": 0
  },
  {
    "id": "Ancient Machine",
    "lookDescription": "A rusted machine hums with unstable energy.",
    "activateDescription": "The machine whirs violently, discharging a burst of raw power. (-5 health)",
    "healthEffect": -5
  },
  {
    "id": "Orb",
    "lookDescription": "A smooth obsidian orb sits on a stone pedestal.",
    "activateDescription": "The orb cracks open, releasing warm light that strengthens you. (+3 health)",
    "healthEffect": 3
  }
]
```

## Designing Scenarios for LLMs

When creating new scenarios:

1. **Start with the map** — sketch area connections first (which rooms connect to which). Ensure the graph is fully traversable from startArea so all areas can be reached.
2. **Balance health effects** — sum of all positive healthEffect values should exceed the absolute sum of negative ones, giving the player a viable path to victory at starting health.
3. **Place devices strategically** — put healing devices early in likely paths and dangerous devices in less obvious locations or dead ends.
4. **Keep descriptions thematic** — maintain consistent tone across all area/device descriptions within a scenario.
5. **Test with low HP** — verify the player can win starting at health=1 to ensure the scenario isn't accidentally unwinnable.

## Common Mistakes

- **Forgetting bidirectional connections**: If Area A lists B in `connections`, then Area B must also list A for travel to work both ways.
- **Mismatched deviceId**: The string in `deviceId` must exactly match a device's `id` field (case-sensitive).
- **Unreachable areas**: An area that no other area connects to cannot be visited, making the scenario unwinnable.
- **Missing devices file entries**: If an area has `"deviceId": "Lamp"` but no device with id "Lamp" exists in devicesFile, loading fails.
