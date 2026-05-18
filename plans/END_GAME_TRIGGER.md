# End Game Triggers

## Problem
Game had no way to define custom endings. Win/lose were auto-detected (all areas + devices = win, health ≤ 0 = lose), with hardcoded banners and no scenario-specific narrative.

## Solution
Introduced `endGame` effect type for triggers. When a trigger fires with this effect, the game ends with the provided message instead of auto-detection.

### Key Design Decisions

- **Single message**: Only one `text` field — no win/lose distinction. The message tells the story; UI has no concept of "win" vs "lose".
- **First wins**: If an endGameMessage is already set, subsequent EndGame effects are ignored (prevents accidental overrides).
- **Look still works**: After game ends, `look` command is allowed for replaying final scene. All other commands blocked.
- **Health at 0 doesn't auto-end**: Game continues — scenario authors define their own death triggers via STATUS conditions with `op: "<="`.

### Example Death Trigger
```json
{
  "id": "deathAtZero",
  "ownerType": "STATUS",
  "ownerId": "health",
  "conditions": [{"statusName": "health", "op": "<=", "threshold": 0}],
  "effects": [
    {"type": "displayText", "text": "Your vision fades."},
    {"type": "endGame", "text": "The darkness takes you. Game Over."}
  ],
  "singleUse": true
}
```

### Example Victory Trigger
```json
{
  "id": "sanctumFound",
  "ownerType": "AREA",
  "ownerId": "Sanctuary",
  "effects": [
    {"type": "endGame", "text": "You found the sanctum and its secrets."}
  ]
}
```

## Files Changed
- `core/model/Trigger.kt` — added `EndGame(text: String)` effect type
- `state/GameState.kt` — removed `isOver`, `win`; replaced with single `endGameMessage: String?`
- `scenario/ScenarioLoader.kt` — parses `"endGame"` effect from JSON
- `engine/TriggerEngine.kt` — handles EndGame in effects fold, first-wins semantics
- `command/CommandProcessor.kt` — removed auto-win/lose; look allowed after end
- `port/ViewData.kt` — removed `gameOver`, `win`; added `endGameMessage`
- All UI adapters — loop checks `endGameMessage != null`, display uses scenario message
