# REVIEW — 1.0 End Triggers & Doors (Re-check)

## Previously Flagged Issues — Status After Fixes

| # | Previous Issue | Status | Notes |
|---|----------------|--------|-------|
| 1 | Stale docs (win/lose rules) | **FIXED** ✅ | SCENARIO_FORMAT.md:205-220 correctly documents trigger-based endings only |
| 2 | `ExitStateData.visible` dead code | **FIXED** ✅ | Property removed; logic now uses `data.hidden` directly in `isExitBlocked()` and `visibleExits()` |
| 3 | Win/lose conditions not implemented | **FIXED** ✅ | Tests confirm: health=0 does NOT end game, all explored+activated does NOT end game. Game ends only via `endGame` trigger effect |
| 4 | `Area.connections` dead code | **FALSE ALARM** | Used by `World.validate()` (line 10) and `runMove()` area name validation (CommandProcessor:50). Not dead |

---

## Remaining Issues

### ISSUE-1 — Non-single-use AREA trigger fires every entry without conditions

**File:** `scenarios/default/triggers.json` lines 114-121

```json
{
    "id": "sanctumFound",
    "ownerType": "AREA",
    "ownerId": "Sanctuary",
    "effects": [
        {"type": "endGame", "text": "You found the sanctum and its secrets."}
    ],
    "singleUse": false
}
```

**Problem:** `singleUse: false` with no conditions means this trigger fires **every single time** the player enters Sanctuary. Since `remainingActivations = Int.MAX_VALUE`, it never exhausts. The first entry sets `endGameMessage` (subsequent entries skip because of the null guard in TriggerEngine:139), so the game ends on first entry — but if the engine is restarted or state loaded, entering again re-triggers it.

**Severity:** Low (functional but redundant)
**Suggestion:** Make `singleUse: true` to fire once and only when player enters Sanctuary for the first time.

---

### ISSUE-2 — Status triggers can fire on Look commands

**File:** `core/command/CommandProcessor.kt` line 30

Status triggers (`fireStatusTriggers`) are called after **every non-Look command**, but they're NOT called during Look. However, the turn counter increments on every non-Look action, and status triggers check `state.turn - trigger.lastFireTurn >= intervalTurns`. This means:

- A poison STATUS trigger with `intervalTurns: 2` will fire after 2 moves
- But a player could also "waste" turns by doing non-Look actions (activate device, take/drop item) just to advance turn count — potentially triggering status effects faster than intended

**Severity:** Low (design-level; likely intentional)
**Note:** Look commands do NOT increment the turn counter and do NOT fire status triggers. This is correct behavior for "passive" examination. Only active actions advance time.

---

### ISSUE-3 — `health` as a status name has no bounds enforcement

**File:** `core/model/Player.kt:29-36`, `core/engine/TriggerEngine.kt:51-52`

If a trigger uses `setStatus("health", value)` or `adjustStatus("health", amount)`, the value goes into `player.statuses["health"]` — NOT `player.health`. The TriggerEngine special-cases `cond.statusName == "health"` for condition checks (line 51), but effects that modify health via status functions won't actually change visible player health.

**Severity:** Medium (potential author confusion)
**Suggestion:** Either:
- Document explicitly that `"health"` as a status name is reserved and should only be checked, not modified via setStatus/adjustStatus, OR
- Route `setStatus("health", ...)` through `adjustHealth()` internally

---

### ISSUE-4 — Hidden exits can't be traversed by area name

**File:** `core/state/GameState.kt:52`

```kotlin
fun isExitBlocked(from: AreaId, to: AreaId): Boolean {
    val data = exitStates[id] ?: return false
    return data.state != ExitState.OPEN || data.hidden  // ← hidden counts as blocked
}
```

If someone constructs a `Command.Move("HiddenArea")` directly (not through WASD), and the exit is both OPEN but hidden, traversal is rejected. This is intentional — hidden exits should be undiscoverable. But it means there's **no UI path** to move by area name: neither TextUiAdapter nor KeyUiAdapter produces `Command.Move(areaName)` for player input. The only path is via WASD directions filtered through `sortedExits()`.

**Severity:** Low (defensive, not a bug)
**Note:** If area-name movement were exposed in the UI, hidden exits would still be blocked — which is correct behavior.

---

### ISSUE-5 — Exit state merge reconstructs all exits on every trigger batch

**File:** `core/engine/TriggerEngine.kt:131-137`

```kotlin
val mergedExitStates = mutableMapOf<ExitId, ExitStateData>()
for (exitId in state.exitStates.keys + finalAcc.effectsAcc.exitStateUpdates.keys + ...) {
    val current = state.exitStates[exitId] ?: ExitStateData()
    // ... merge logic
}
```

Every time triggers fire, ALL exit states are reconstructed from scratch by iterating over the union of existing keys and updated keys. This is O(n) per trigger batch where n = total exits in scenario. For large scenarios with many exits, this could be noticeable if many triggers fire simultaneously.

**Severity:** Low (performance, unlikely to matter for typical scenarios)
**Note:** Correctness is preserved — all exit state updates are properly merged.

---

### ISSUE-6 — Dead field `EffectEntry.win`

**File:** `scenario/ScenarioFile.kt:101`

```kotlin
val win: Boolean? = null  // ← never read anywhere
```

This field was likely used before the trigger-based end-game system replaced a direct win condition. It has no effect on behavior (Kotlin serialization ignores unknown fields but does NOT error on extra JSON fields either — if present in JSON it's silently accepted).

**Severity:** Low (cosmetic)
**Suggestion:** Remove if not needed for backward compatibility with old scenario files.

---

### ISSUE-7 — `EffectEntry.blocked` default is semantically misleading

**File:** `scenario/ScenarioFile.kt:98`

```kotlin
val blocked: Boolean? = null  // in JSON, absent means "unknown" not false
```

In `buildTriggers` (ScenarioLoader.kt:169), this is consumed as:
```kotlin
blocked ?: false  // null → false (unblock)
```

But the field default of `null` vs explicit `false` could confuse scenario authors. If they write `"blocked": true`, exit becomes blocked; if absent, it becomes unblocked. This is correct but counterintuitive — one might expect "no value = don't change" rather than "no value = set to false".

**Severity:** Low (author confusion)
**Note:** Works correctly for the default scenario where `blocked: false` explicitly sets exit open.

---

### ISSUE-8 — Trigger interval uses pre-command turn count

**File:** `core/engine/TriggerEngine.kt:74`

```kotlin
val turnsSince = state.turn - trigger.lastFireTurn  // state.turn is BEFORE increment
```

The fold closure captures `state.turn` from the original GameState (before `s.copy(turn = s.turn + 1)` in CommandProcessor:29). This means interval checks are based on the turn count at the START of the command. For status triggers, this is correct — they should check against when they last fired relative to the current game tick, not a future incremented value.

**Severity:** None (correct behavior)
**Note:** Verified working correctly in `TriggerTest:62-74` (`status trigger with interval fires repeatedly`).

---

## Test Coverage Gaps

| Gap | Description | Risk |
|-----|-------------|------|
| No test for hidden exit traversal attempt | If a `Command.Move(hiddenAreaName)` is constructed, it should be blocked — but there's no explicit test | Low (covered implicitly by `isExitBlocked` tests) |
| No test for status trigger checking "health" condition | The special-cased health check in TriggerEngine:51 has no dedicated test | Medium (core feature) |
| No test for item moved by trigger becoming accessible | If a trigger moves an item from AREA to CARRIED, subsequent `take` commands should fail — not tested | Low (logical consequence of existing tests) |
| No integration test with hidden exits + showExit | The default scenario uses this pattern but no automated test covers it end-to-end | Medium (core door mechanic) |

---

## Architecture Assessment

**Core:** Clean functional design. Immutable `GameState` via `copy()`, sealed hierarchies, fold-based trigger accumulator handles cascading effects correctly.

**Interfaces:** Thin adapters around core logic. `ScenarioRepository` and `GameEngine` provide clean boundaries.

**Potential improvement — Direction handling friction:**
- `InputEvent.MoveDirection(direction: Direction)` wraps model's `Direction` enum (InputEvent.kt:15 typealias)
- `Command.Move(areaName: String)` takes area name, not direction
- GameEngineImpl bridges them via `resolveDirection()` → `sortedExits()[index]`
- This works but couples command semantics to index-based direction ordering

---

## Summary

| Category | Count | Details |
|----------|-------|---------|
| Critical bugs | 0 | — |
| Medium issues | 2 | Issue-3 (health status), Issue-8 trigger interval (actually correct, listed for completeness) |
| Low issues / suggestions | 5 | Issues 1,4,6,7 + test gaps |
| False alarms from prev review | 1 | `Area.connections` is used |

**Overall: Stable.** The engine correctly implements trigger-based game endings, door mechanics (blocked/hidden/show), and the functional core is sound. Remaining issues are minor improvements or documentation clarifications.
