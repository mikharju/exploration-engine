# Triggers

## Task

Add triggers: conditions that fire during gameplay to change player state or display text.

## Overview

Triggers are loaded from scenario JSON, stored as a flat list on `GameState`, and checked at specific points in the command flow. Trigger runtime state (consumption count, last-fire turn) lives inside each trigger data class.

## Data Model

### Trigger Data Class

```kotlin
data class Trigger(
    val id: String,
    val ownerType: OwnerType,   // AREA, DEVICE, STATUS
    val ownerId: String,         // area name, device ID, or status name
    val conditions: List<ActivationCondition>,  // empty = unconditional fire
    val effects: List<Effect>,
    val singleUse: Boolean = true,
    val intervalTurns: Int? = null,              // status triggers only — min turns between fires
    // Runtime state (mutable via copy):
    val remainingActivations: Int = if (singleUse) 1 else Int.MAX_VALUE,
    val lastFireTurn: Int? = null
)

enum class OwnerType { AREA, DEVICE, STATUS }

data class ActivationCondition(
    val statusName: String,      // which player status to check
    val op: ComparisonOp,        // ">" or "<"
    val threshold: Int
)

enum class ComparisonOp { GT, LT }

sealed class Effect {
    data class DisplayText(val text: String) : Effect()
    data class ChangeHealth(val amount: Int) : Effect()
    data class AdjustStatus(val statusName: String, val amount: Int) : Effect()
    data class SetStatus(val statusName: String, val value: Int) : Effect()
}
```

### Why flat list with owner fields (not nested under Area/Device)

- Keeps `Area` and `Device` models unchanged — triggers are gameplay logic, not entity properties
- Status triggers have no natural parent entity to nest under
- Single collection is simpler to iterate during evaluation
- No nesting changes to existing data classes

### Why runtime state inside the trigger

- Data co-location: consumption rules visible alongside the definition
- Immutable updates via `copy()` in the flat list — cheap at expected scale
- Fields: `remainingActivations` for single/repeat use, `lastFireTurn` for "every X turns" check

## Activation Conditions

A trigger can have zero or more activation conditions. All conditions must be met for the trigger to fire (AND logic). If a trigger has no conditions, it fires unconditionally on its owning event.

Each condition checks one player status value against a threshold: `status X > N` or `status X < N`.

| Trigger Type | When Checked |
|---|---|
| Status trigger | After every non-Look command |
| Device trigger | When activating the owned device |
| Area trigger | When entering the owned area |

Status triggers additionally support `intervalTurns: Int` — once all conditions are met, the trigger also checks that enough turns have passed since `lastFireTurn`. This lets a status trigger fire repeatedly at fixed intervals while the condition holds.

## Effects on Activation

Effects execute in declaration order. Health is separate from statuses:
- `ChangeHealth(amount)` modifies health via existing `Player.adjustHealth`
- `AdjustStatus(name, amount)` / `SetStatus(name, value)` modify named statuses within their bounds

A trigger can have multiple effects of any combination. Text from `DisplayText` effects appends to the command's output string. When multiple triggers fire in one step, their text outputs concatenate in firing order.

## Trigger Evaluation Flow

For each command in `processCommand`:

```
1. Execute command logic (move/activate/look) → intermediate state
2. Fire area triggers (if player moved to new area)
3. Fire device triggers (if player activated a device)
4. Increment turn counter (unless command was Look)
5. Check status triggers (after all state changes from this command)
6. Run checkOutcome() for win/lose
```

Area entry triggers fire on game start for the starting area, evaluated in `assembleGame` after initial state is built.

### Firing a Trigger Set

For each candidate trigger:
1. Skip if `remainingActivations <= 0`
2. Check all conditions — every condition must evaluate to true (read status value, compare against threshold)
3. For status triggers with `intervalTurns`, also check that enough turns have passed since `lastFireTurn`
4. If everything passes: apply effects in order, decrement remainingActivations, set lastFireTurn

## Scenario File Format

New trigger file referenced from `ScenarioConfig`:

```kotlin
data class ScenarioConfig(
    val playerStart: PlayerStart,
    val areasFile: String,
    val devicesFile: String,
    val statuses: Map<String, StatusEntry> = emptyMap(),
    val triggersFile: String? = null   // NEW
)

@Serializable
data class TriggerEntry(
    val id: String,
    val ownerType: OwnerType,
    val ownerId: String,
    val conditions: List<TriggerCondition> = emptyList(),  // empty = unconditional fire
    val effects: List<EffectEntry>,
    val singleUse: Boolean = true,
    val intervalTurns: Int? = null      // status triggers only — min turns between fires
)

@Serializable
data class TriggerCondition(
    val statusName: String,             // which player status to check
    val op: String,                     // ">" or "<"
    val threshold: Int
)
```

## GameState Changes

```kotlin
data class GameState(
    // ... existing fields ...
    val turn: Int = 0,
    val triggers: List<Trigger> = emptyList()
)
```

`turn` increments after each non-Look command. Used for "every X turns" interval check and for status trigger timing.

## Implementation Order

1. Add `Trigger`, `OwnerType`, `ActivationCondition`, `Effect` data classes to `model/`
2. Add `TriggerEntry`, `TriggerCondition`, `EffectEntry` serializable types to `scenario/ScenarioFile.kt`
3. Extend `GameState` with `turn` and `triggers` fields
4. Write trigger loading logic in `ScenarioLoader` — map entries to domain objects, validate owner references exist
5. Implement `evaluateTriggers(GameState, TriggerEventType) -> GameState` helper
6. Wire into `CommandProcessor`: area triggers after move, device triggers after activate, status triggers at end of every non-look command
7. Fire starting-area triggers in `assembleGame`
8. Tests for trigger evaluation logic and integration
