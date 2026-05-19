# Code review requested

**Session ID:** ses_1c6b5c2d7ffew2MRaQGx4aHOI9
**Created:** 5/18/2026, 7:13:47 AM
**Updated:** 5/18/2026, 7:33:48 AM

---

## User

Do code review

---

## Assistant (Plan · qwen3.6-35b-a3b · 18.7s)

_Thinking:_

The user wants a code review. Let me explore the project structure and understand the current state of the codebase before providing feedback.







**Tool: bash**

**Input:**
```json
{
  "command": "git log --oneline -15",
  "description": "Recent git history"
}
```

**Output:**
```
ce48c7b More status trigger comparison operators and plan doc
fc8630d End game trigger initial commit
e84eb89 Doors
733e452 Merge branch 'main' of github.com:mikharju/exploration-engine
90178fd Initial commit
3f56e71 Directional exits
be70333 Story messages
a7ef08b Refactoring  - more functional TriggerEngine  - GameStateStore
f4f0aba Review session fixes.  - strings to ids  - more functional core  - error message improvements
5d4669d Items
de4d0ae input concerns refactoring
3b09937 input concerns and gradle
2245f8d Old ai sessions
4a21ff4 Opencode ignore old ai sessions
8fb00fd Triggers

```

**Tool: bash**

**Input:**
```json
{
  "command": "git diff --stat HEAD~1..HEAD",
  "description": "Changes in latest commit"
}
```

**Output:**
```
 plans/END_GAME_TRIGGER.md                          | 50 ++++++++++++++++++++++
 scenarios/SCENARIO_FORMAT.md                       |  2 +-
 .../adapter/ui/lanterna/LanternaUiAdapter.kt       |  8 ++++
 .../exploration/core/engine/TriggerEngine.kt       |  3 ++
 src/main/kotlin/exploration/core/model/Trigger.kt  |  2 +-
 .../kotlin/exploration/scenario/ScenarioLoader.kt  |  3 ++
 6 files changed, 66 insertions(+), 2 deletions(-)

```

---

## Assistant (Plan · qwen3.6-35b-a3b · 3.9s)

_Thinking:_

Let me look at the latest changes and review the overall codebase.









**Tool: bash**

**Input:**
```json
{
  "command": "git diff HEAD~1 --no-color",
  "description": "Full diff of latest commit"
}
```

**Output:**
```
diff --git a/plans/END_GAME_TRIGGER.md b/plans/END_GAME_TRIGGER.md
new file mode 100644
index 0000000..8bf284c
--- /dev/null
+++ b/plans/END_GAME_TRIGGER.md
@@ -0,0 +1,50 @@
+# End Game Triggers
+
+## Problem
+Game had no way to define custom endings. Win/lose were auto-detected (all areas + devices = win, health ≤ 0 = lose), with hardcoded banners and no scenario-specific narrative.
+
+## Solution
+Introduced `endGame` effect type for triggers. When a trigger fires with this effect, the game ends with the provided message instead of auto-detection.
+
+### Key Design Decisions
+
+- **Single message**: Only one `text` field — no win/lose distinction. The message tells the story; UI has no concept of "win" vs "lose".
+- **First wins**: If an endGameMessage is already set, subsequent EndGame effects are ignored (prevents accidental overrides).
+- **Look still works**: After game ends, `look` command is allowed for replaying final scene. All other commands blocked.
+- **Health at 0 doesn't auto-end**: Game continues — scenario authors define their own death triggers via STATUS conditions with `op: "<="`.
+
+### Example Death Trigger
+```json
+{
+  "id": "deathAtZero",
+  "ownerType": "STATUS",
+  "ownerId": "health",
+  "conditions": [{"statusName": "health", "op": "<=", "threshold": 0}],
+  "effects": [
+    {"type": "displayText", "text": "Your vision fades."},
+    {"type": "endGame", "text": "The darkness takes you. Game Over."}
+  ],
+  "singleUse": true
+}
+```
+
+### Example Victory Trigger
+```json
+{
+  "id": "sanctumFound",
+  "ownerType": "AREA",
+  "ownerId": "Sanctuary",
+  "effects": [
+    {"type": "endGame", "text": "You found the sanctum and its secrets."}
+  ]
+}
+```
+
+## Files Changed
+- `core/model/Trigger.kt` — added `EndGame(text: String)` effect type
+- `state/GameState.kt` — removed `isOver`, `win`; replaced with single `endGameMessage: String?`
+- `scenario/ScenarioLoader.kt` — parses `"endGame"` effect from JSON
+- `engine/TriggerEngine.kt` — handles EndGame in effects fold, first-wins semantics
+- `command/CommandProcessor.kt` — removed auto-win/lose; look allowed after end
+- `port/ViewData.kt` — removed `gameOver`, `win`; added `endGameMessage`
+- All UI adapters — loop checks `endGameMessage != null`, display uses scenario message
diff --git a/plans/ROADMAP.md b/plans/ROADMAP.md
index fb390ab..9659bdb 100644
--- a/plans/ROADMAP.md
+++ b/plans/ROADMAP.md
@@ -30,6 +30,12 @@ and uses page up and page down key functionality.
 - Scenario file should set welcome message and default final message
 - Game no longer ends if all areas are explored and all devices activated
 
+# Reviews needed
+
+- LanternaUiAdapter
+- CommandProcessor
+- ScenarioLoader
+
 # Use items
 
 - Add action use item
diff --git a/scenarios/SCENARIO_FORMAT.md b/scenarios/SCENARIO_FORMAT.md
index c936534..f286643 100644
--- a/scenarios/SCENARIO_FORMAT.md
+++ b/scenarios/SCENARIO_FORMAT.md
@@ -145,7 +145,7 @@ All conditions must pass. Omit for unconditional triggers.
 |---|---|---|
 | `checkType` | string | `itemCarried`, `itemEquipped`, or omit for status check |
 | `statusName` | string | Status to check (default: STATUS) |
-| `op` | string | `>` or `<`. **Default: `>`** |
+| `op` | string | `>`, `<`, `<=`, `>=`, or `==`. **Default: `>`** |
 | `threshold` | int | Value to compare against. **Default: 0** |
 | `itemId` | string | Item ID for item checks |
 
diff --git a/src/main/kotlin/exploration/adapter/ui/lanterna/LanternaUiAdapter.kt b/src/main/kotlin/exploration/adapter/ui/lanterna/LanternaUiAdapter.kt
index b47f0ac..efe5bbd 100644
--- a/src/main/kotlin/exploration/adapter/ui/lanterna/LanternaUiAdapter.kt
+++ b/src/main/kotlin/exploration/adapter/ui/lanterna/LanternaUiAdapter.kt
@@ -79,6 +79,14 @@ class LanternaUiAdapter(private val engine: GameEngine) {
         var currentView = engine.tick(ref, InputEvent.Look)
         render(screen, currentView, history, overlay)
 
+        val initialStoryCount = currentView.storyMessages.size
+        storedStoryCount = initialStoryCount
+        if (initialStoryCount > 0) {
+            val subList = currentView.storyMessages.subList(0, initialStoryCount)
+            overlay = Overlay.MessageViewer(subList, focusedIndex = subList.lastIndex)
+            render(screen, currentView, history, overlay)
+        }
+
         while (currentView.endGameMessage == null) {
             screen.doResizeIfNecessary()
             val result = readInputKey(screen, currentView, selectionState, overlay)
diff --git a/src/main/kotlin/exploration/core/engine/TriggerEngine.kt b/src/main/kotlin/exploration/core/engine/TriggerEngine.kt
index 8623927..55f3e84 100644
--- a/src/main/kotlin/exploration/core/engine/TriggerEngine.kt
+++ b/src/main/kotlin/exploration/core/engine/TriggerEngine.kt
@@ -53,6 +53,9 @@ private fun fireMatchingTriggers(
                     when (cond.op) {
                         ComparisonOp.GT -> statusValue > cond.threshold
                         ComparisonOp.LT -> statusValue < cond.threshold
+                        ComparisonOp.LTE -> statusValue <= cond.threshold
+                        ComparisonOp.GTE -> statusValue >= cond.threshold
+                        ComparisonOp.EQ -> statusValue == cond.threshold
                     }
                 }
                 CheckType.ITEM_CARRIED -> cond.itemId?.let {
diff --git a/src/main/kotlin/exploration/core/model/Trigger.kt b/src/main/kotlin/exploration/core/model/Trigger.kt
index 71fd792..abb9c8c 100644
--- a/src/main/kotlin/exploration/core/model/Trigger.kt
+++ b/src/main/kotlin/exploration/core/model/Trigger.kt
@@ -12,7 +12,7 @@ data class ActivationCondition(
     val itemId: ItemId? = null
 )
 
-enum class ComparisonOp { GT, LT }
+enum class ComparisonOp { GT, LT, LTE, GTE, EQ }
 
 sealed interface TriggerOwner {
     data class Area(val areaId: AreaId) : TriggerOwner
diff --git a/src/main/kotlin/exploration/scenario/ScenarioLoader.kt b/src/main/kotlin/exploration/scenario/ScenarioLoader.kt
index 591aa2e..5a0f96c 100644
--- a/src/main/kotlin/exploration/scenario/ScenarioLoader.kt
+++ b/src/main/kotlin/exploration/scenario/ScenarioLoader.kt
@@ -137,6 +137,9 @@ private fun buildTriggers(triggerEntries: List<TriggerEntry>): List<Trigger> = t
                 op = when (cond.op) {
                     ">" -> ComparisonOp.GT
                     "<" -> ComparisonOp.LT
+                    "<=" -> ComparisonOp.LTE
+                    ">=" -> ComparisonOp.GTE
+                    "==" -> ComparisonOp.EQ
                     null -> ComparisonOp.GT
                     else -> error("Unknown operator: ${cond.op}")
                 },

```

**Tool: read**

**Input:**
```json
{
  "filePath": "/var/home/mikharju/dev/exploration/engine/src/main/kotlin/exploration/core/model/Trigger.kt"
}
```

**Output:**
```
<path>/var/home/mikharju/dev/exploration/engine/src/main/kotlin/exploration/core/model/Trigger.kt</path>
<type>file</type>
<content>
1: package exploration.model
2: 
3: enum class OwnerType { AREA, DEVICE, STATUS }
4: 
5: enum class CheckType { STATUS, ITEM_CARRIED, ITEM_EQUIPPED }
6: 
7: data class ActivationCondition(
8:     val checkType: CheckType = CheckType.STATUS,
9:     val statusName: String? = null,
10:     val op: ComparisonOp = ComparisonOp.GT,
11:     val threshold: Int = 0,
12:     val itemId: ItemId? = null
13: )
14: 
15: enum class ComparisonOp { GT, LT, LTE, GTE, EQ }
16: 
17: sealed interface TriggerOwner {
18:     data class Area(val areaId: AreaId) : TriggerOwner
19:     data class Device(val deviceId: DeviceId) : TriggerOwner
20:     object Status : TriggerOwner
21: }
22: 
23: sealed class Effect {
24:     data class DisplayText(val text: String) : Effect()
25:     data class ChangeHealth(val amount: Int) : Effect()
26:     data class AdjustStatus(val statusName: String, val amount: Int) : Effect()
27:     data class SetStatus(val statusName: String, val value: Int) : Effect()
28: 
29:     sealed interface TargetRef {
30:         data class InArea(val areaId: AreaId) : TargetRef
31:         data class OnDevice(val deviceId: DeviceId) : TargetRef
32:         object Carried : TargetRef
33:         object Equipped : TargetRef
34:     }
35: 
36:     data class SetLocation(
37:         val itemId: ItemId,
38:         val target: TargetRef
39:     ) : Effect()
40: 
41:     data class LockItem(val itemId: ItemId) : Effect()
42:     data class UnlockItem(val itemId: ItemId) : Effect()
43:     data class StoryMessage(val text: String) : Effect()
44: 
45:     // Exit effects — target area and direction
46:     data class SetExitBlocked(val from: AreaId, val to: AreaId, val blocked: Boolean = true) : Effect()
47:     data class HideExit(val from: AreaId, val to: AreaId) : Effect()
48:     data class ShowExit(val from: AreaId, val to: AreaId) : Effect()
49: 
50:     data class EndGame(val text: String) : Effect()
51: }
52: 
53: data class Trigger(
54:     val id: String,
55:     val ownerType: OwnerType,
56:     val ownerId: String,
57:     val conditions: List<ActivationCondition> = emptyList(),
58:     val effects: List<Effect>,
59:     val singleUse: Boolean = true,
60:     val intervalTurns: Int? = null,
61:     val remainingActivations: Int = if (singleUse) 1 else Int.MAX_VALUE,
62:     val lastFireTurn: Int? = null
63: ) {
64:     val owner: TriggerOwner
65:         get() = when (ownerType) {
66:             OwnerType.AREA -> TriggerOwner.Area(AreaId(ownerId))
67:             OwnerType.DEVICE -> TriggerOwner.Device(DeviceId(ownerId))
68:             OwnerType.STATUS -> TriggerOwner.Status
69:         }
70: 
71:     fun areaId(): AreaId? = if (ownerType == OwnerType.AREA) AreaId(ownerId) else null
72:     fun deviceId(): DeviceId? = if (ownerType == OwnerType.DEVICE) DeviceId(ownerId) else null
73: }

(End of file - total 73 lines)
</content>
```

**Tool: read**

**Input:**
```json
{
  "filePath": "/var/home/mikharju/dev/exploration/engine/src/main/kotlin/exploration/core/engine/TriggerEngine.kt"
}
```

**Output:**
```
<path>/var/home/mikharju/dev/exploration/engine/src/main/kotlin/exploration/core/engine/TriggerEngine.kt</path>
<type>file</type>
<content>
1: package exploration.core.engine
2: 
3: import exploration.model.*
4: import exploration.state.GameState
5: 
6: fun fireAreaTriggers(state: GameState, areaId: AreaId): GameState {
7:     val candidates = state.triggers.filter { it.owner is TriggerOwner.Area && it.areaId() == areaId }
8:     return fireMatchingTriggers(state, candidates)
9: }
10: 
11: fun fireDeviceTriggers(state: GameState, deviceId: DeviceId): GameState {
12:     val candidates = state.triggers.filter { it.owner is TriggerOwner.Device && it.deviceId() == deviceId }
13:     return fireMatchingTriggers(state, candidates)
14: }
15: 
16: fun fireStatusTriggers(state: GameState): GameState {
17:     val candidates = state.triggers.filter { it.owner is TriggerOwner.Status }
18:     return fireMatchingTriggers(state, candidates)
19: }
20: 
21: private fun fireMatchingTriggers(
22:     state: GameState,
23:     candidates: List<Trigger>
24: ): GameState {
25:     data class EffectsAccumulator(
26:         val player: Player = state.player,
27:         val newTexts: List<String> = emptyList(),
28:         val storyMessages: List<String> = emptyList(),
29:         val locationUpdates: Map<ItemId, Location> = emptyMap(),
30:         val lockedUpdates: Map<ItemId, Boolean> = emptyMap(),
31:         val exitStateUpdates: Map<ExitId, ExitState> = emptyMap(),
32:         val exitHiddenUpdates: Map<ExitId, Boolean> = emptyMap(),
33:         val endGameText: String? = null
34:     )
35:     data class MutationAccumulator(
36:         val items: List<Item>,
37:         val turn: Int,
38:         val updatedTriggers: Map<String, Trigger> = emptyMap(),
39:         val effectsAcc: EffectsAccumulator = EffectsAccumulator()
40:     )
41: 
42:     fun effectiveLocation(accum: MutationAccumulator, itemId: ItemId): Location? =
43:         accum.effectsAcc.locationUpdates[itemId] ?: state.items.find { it.id == itemId }?.location
44: 
45:     val finalAcc = candidates.fold(MutationAccumulator(state.items, state.turn)) { acc, trigger ->
46:         if (trigger.remainingActivations <= 0) return@fold acc
47: 
48:         val conditionsMet = trigger.conditions.all { cond ->
49:             when (cond.checkType) {
50:                 CheckType.STATUS -> {
51:                     val statusValue = if (cond.statusName == "health") acc.effectsAcc.player.health
52:                     else cond.statusName?.let { acc.effectsAcc.player.statuses.getOrElse(it) { 0 } } ?: 0
53:                     when (cond.op) {
54:                         ComparisonOp.GT -> statusValue > cond.threshold
55:                         ComparisonOp.LT -> statusValue < cond.threshold
56:                         ComparisonOp.LTE -> statusValue <= cond.threshold
57:                         ComparisonOp.GTE -> statusValue >= cond.threshold
58:                         ComparisonOp.EQ -> statusValue == cond.threshold
59:                     }
60:                 }
61:                 CheckType.ITEM_CARRIED -> cond.itemId?.let {
62:                     effectiveLocation(acc, it)?.type == ItemLocationType.CARRIED
63:                 } ?: false
64:                 CheckType.ITEM_EQUIPPED -> cond.itemId?.let {
65:                     effectiveLocation(acc, it)?.type == ItemLocationType.EQUIPPED
66:                 } ?: false
67:             }
68:         }
69: 
70:         if (!conditionsMet) return@fold acc
71: 
72:         // Interval check for STATUS triggers
73:         if (trigger.owner is TriggerOwner.Status && trigger.intervalTurns != null && trigger.lastFireTurn != null) {
74:             val turnsSince = state.turn - trigger.lastFireTurn
75:             if (turnsSince < trigger.intervalTurns) return@fold acc
76:         }
77: 
78:         // Apply effects within this trigger's scope
79:         val newEffects: EffectsAccumulator = trigger.effects.fold(acc.effectsAcc) { effAcc, effect ->
80:             when (effect) {
81:                 is Effect.DisplayText -> if (effect.text.isNotBlank()) effAcc.copy(newTexts = effAcc.newTexts + effect.text) else effAcc
82:                 is Effect.ChangeHealth -> effAcc.copy(player = effAcc.player.adjustHealth(effect.amount))
83:                 is Effect.AdjustStatus -> effAcc.copy(player = effAcc.player.adjustStatus(
84:                     effect.statusName, effect.amount, state.statusBounds[effect.statusName]))
85:                 is Effect.SetStatus -> effAcc.copy(player = effAcc.player.setStatus(
86:                     effect.statusName, effect.value, state.statusBounds[effect.statusName]))
87:                 is Effect.SetLocation ->
88:                     if (state.items.any { it.id == effect.itemId }) effAcc.copy(locationUpdates = effAcc.locationUpdates + (effect.itemId to Location.fromTarget(effect.target)))
89:                     else effAcc
90:                 is Effect.LockItem ->  effAcc.copy(lockedUpdates = effAcc.lockedUpdates + (effect.itemId to true))
91:                 is Effect.UnlockItem ->  effAcc.copy(lockedUpdates = effAcc.lockedUpdates + (effect.itemId to false))
92:                 is Effect.StoryMessage -> if (effect.text.isNotBlank()) effAcc.copy(storyMessages = effAcc.storyMessages + effect.text) else effAcc
93:                 is Effect.SetExitBlocked -> {
94:                     val id = ExitId(effect.from, effect.to)
95:                     val newState = if (effect.blocked) ExitState.BLOCKED else ExitState.OPEN
96:                     effAcc.copy(exitStateUpdates = effAcc.exitStateUpdates + (id to newState))
97:                 }
98:                 is Effect.HideExit -> {
99:                     val id = ExitId(effect.from, effect.to)
100:                     effAcc.copy(exitHiddenUpdates = effAcc.exitHiddenUpdates + (id to true))
101:                 }
102:                 is Effect.ShowExit -> {
103:                     val id = ExitId(effect.from, effect.to)
104:                     effAcc.copy(exitHiddenUpdates = effAcc.exitHiddenUpdates + (id to false))
105:                 }
106:                 is Effect.EndGame -> if (effAcc.endGameText == null) effAcc.copy(endGameText = effect.text) else effAcc
107:             }
108:         }
109: 
110:         acc.copy(
111:             effectsAcc = newEffects,
112:             updatedTriggers = acc.updatedTriggers + (trigger.id to trigger.copy(
113:                 remainingActivations = trigger.remainingActivations - 1,
114:                 lastFireTurn = state.turn
115:             ))
116:         )
117:     }
118: 
119:     val allTriggers = if (finalAcc.updatedTriggers.isNotEmpty()) {
120:         state.triggers.map { t -> finalAcc.updatedTriggers[t.id] ?: t }
121:     } else {
122:         state.triggers
123:     }
124: 
125:     val newItems = state.items.map { item ->
126:         val itemElsewhere = if (item.id in finalAcc.effectsAcc.locationUpdates) item.copy(location = finalAcc.effectsAcc.locationUpdates[item.id]!!) else item
127:         if (item.id in finalAcc.effectsAcc.lockedUpdates) itemElsewhere.copy(locked = finalAcc.effectsAcc.lockedUpdates[item.id]!!) else itemElsewhere
128:     }
129: 
130:     // Build merged exit states: merge new updates with existing state
131:     val mergedExitStates = mutableMapOf<ExitId, ExitStateData>()
132:     for (exitId in state.exitStates.keys + finalAcc.effectsAcc.exitStateUpdates.keys + finalAcc.effectsAcc.exitHiddenUpdates.keys) {
133:         val current = state.exitStates[exitId] ?: ExitStateData()
134:         val updatedState = finalAcc.effectsAcc.exitStateUpdates[exitId] ?: current.state
135:         val updatedHidden = finalAcc.effectsAcc.exitHiddenUpdates[exitId] ?: current.hidden
136:         mergedExitStates[exitId] = ExitStateData(updatedState, updatedHidden)
137:     }
138: 
139:     val finalEndGame = if (state.endGameMessage == null && finalAcc.effectsAcc.endGameText != null) {
140:         finalAcc.effectsAcc.endGameText
141:     } else state.endGameMessage
142: 
143:     return state.copy(
144:         player = finalAcc.effectsAcc.player,
145:         triggerTexts = if (finalAcc.effectsAcc.newTexts.isNotEmpty()) state.triggerTexts + finalAcc.effectsAcc.newTexts else state.triggerTexts,
146:         storyMessages = if (finalAcc.effectsAcc.storyMessages.isNotEmpty()) state.storyMessages + finalAcc.effectsAcc.storyMessages else state.storyMessages,
147:         triggers = allTriggers,
148:         items = newItems,
149:         exitStates = mergedExitStates,
150:         endGameMessage = finalEndGame
151:     )
152: }

(End of file - total 152 lines)
</content>
```

**Tool: read**

**Input:**
```json
{
  "filePath": "/var/home/mikharju/dev/exploration/engine/scenarios/SCENARIO_FORMAT.md"
}
```

**Output:**
```
<path>/var/home/mikharju/dev/exploration/engine/scenarios/SCENARIO_FORMAT.md</path>
<type>file</type>
<content>
1: # Scenario File Format — Exploration Engine
2: 
3: A scenario is one or more JSON files: a **config** referencing data files (**areas**, **devices**, optionally **triggers**, **items**). Pass the config path as the first argument.
4: 
5: ```bash
6: ./build/install/exploration-engine/bin/exploration-engine scenarios/default/default.json
7: ```
8: 
9: ## Config (`default.json`)
10: 
11: Entry point with player starting state and file references.
12: 
13: | Field | Type | Required | Notes |
14: |---|---|---|---|
15: | `playerStart.health` | int | yes | ≥ 0, ≤ maxHealth |
16: | `playerStart.maxHealth` | int | yes | > 0 |
17: | `playerStart.startArea` | string | yes | Must match an area `id` |
18: | `areasFile` | string | yes | Relative to config location |
19: | `devicesFile` | string | yes | Relative to config location |
20: | `statuses` | object | no | Optional status indicators. Keys are names, values have `initial`, `min`, `max` |
21: | `triggersFile` | string | no | Relative to config location |
22: | `itemsFile` | string | no | Relative to config location |
23: | `welcomeMessage` | string | no | Displayed at game start as a story message. **Default: empty** |
24: 
25: ```json
26: {
27:   "playerStart": { "health": 3, "maxHealth": 20, "startArea": "Forest" },
28:   "areasFile": "areas.json",
29:   "devicesFile": "devices.json",
30:   "triggersFile": "triggers.json",
31:   "itemsFile": "items.json",
32:   "welcomeMessage": "You awaken in a strange forest. An alien signal draws you deeper into the ruins ahead.",
33:   "statuses": {
34:     "corruption": { "initial": 0, "min": 0, "max": 100 }
35:   }
36: }
37: ```
38: 
39: ## Areas (`areas.json`)
40: 
41: JSON array of area objects. Each exit specifies a target area and direction (North, West, South, East). Directions are not auto-verified as bidirectional — each side defines its own exits explicitly.
42: 
43: | Field | Type | Required | Notes |
44: |---|---|---|---|
45: | `id` | string | yes | Unique. Case-sensitive match, case-insensitive input at runtime |
46: | `description` | string | yes | Shown on entry/look |
47: | `exits` | exit[] | no | List of exits with direction. Omit or empty = dead-end |
48: | `deviceId` | string \| null | no | Must match a device `id`. One per area max |
49: 
50: **Exit object:**
51: 
52: | Field | Type | Required | Notes |
53: |---|---|---|---|
54: | `areaId` | string | yes | Target area ID. Must exist in the scenario |
55: | `direction` | string | yes | `"North"`, `"West"`, `"South"`, or `"East"` |
56: | `initialState` | string | no | `"open"` (default) or `"blocked"`. Sets initial passability of this exit |
57: | `hidden` | boolean | no | **Default: `false`**. If true, the exit is invisible in UI until a trigger shows it |
58: 
59: ```json
60: [
61:   { "id": "Forest", "description": "...", "exits": [{ "areaId": "Cave", "direction": "East" }], "deviceId": "Crystal" },
62:   { "id": "Cave", "description": "...", "exits": [
63:       { "areaId": "Forest", "direction": "West" },
64:       { "areaId": "Ruins", "direction": "South", "initialState": "blocked" },
65:       { "areaId": "Tower", "direction": "North", "hidden": true }
66:     ], "deviceId": "Glyph Wall" }
67: ]
68: ```
69: 
70: Hidden exits do not appear in the UI at all. Triggers can reveal them using `showExit` or set their passability with `setExitBlocked`. Blocked exits still show but cannot be traversed.
71: 
72: **Legacy format:** `connections: string[]` is still supported for backward compatibility. When used, directions are auto-assigned by alphabetical order of target area names (preserving previous behavior). New scenarios should use the explicit `exits` format.
73: 
74: ## Devices (`devices.json`)
75: 
76: JSON array of device objects. Placed in exactly one area.
77: 
78: | Field | Type | Required | Notes |
79: |---|---|---|---|
80: | `id` | string | yes | Unique, referenced by areas via `deviceId` |
81: | `lookDescription` | string | yes | Shown on "look" for unactivated device |
82: | `activateDescription` | string | yes | Shown on activation |
83: | `healthEffect` | int | no | Health change on activate. **Default: 0** |
84: | `statusEffects` | object | no | Map of status names to integer deltas applied on activation. Values are clamped per-status. **Default: empty** |
85: 
86: ```json
87: [
88:   { "id": "Crystal", "lookDescription": "...", "activateDescription": "...", "healthEffect": 10, "statusEffects": { "corruption": -5 } },
89:   { "id": "Glyph Wall", "lookDescription": "...", "activateDescription": "...", "healthEffect": 0 }
90: ]
91: ```
92: 
93: ## Statuses (`statuses` in config)
94: 
95: Optional numeric indicators beyond health. Each status has a name, starting value, and clamped range.
96: 
97: | Field | Type | Required | Notes |
98: |---|---|---|---|
99: | `<name>.initial` | int | yes | Starting value for the player |
100: | `<name>.min` | int | yes | Lower clamp bound |
101: | `<name>.max` | int | yes | Upper clamp bound |
102: 
103: Status values are clamped to `[min, max]` after every adjustment. Win/lose conditions remain health-only — statuses don't affect game outcome directly.
104: 
105: **Example:** A `corruption` status with `{ "initial": 0, "min": 0, "max": 100 }` starts at 0. A device with `"statusEffects": { "corruption": 15 }` raises it to 15 (capped at 100).
106: 
107: ## Items (`items.json`)
108: 
109: JSON array of pick-up-able objects. Players can take, drop, equip, and unequip items via commands or triggers.
110: 
111: | Field | Type | Required | Notes |
112: |---|---|---|---|
113: | `id` | string | yes | Unique identifier |
114: | `description` | string | yes | Shown on look when the item is in an area |
115: | `initialLocationType` | string | no | Where the item starts. **Default: `AREA`** |
116: | `initialLocationId` | string | no | Target for location type `AREA` or `DEVICE`. Required for those types |
117: 
118: **Location types:** `AREA` (in a room), `DEVICE` (at a device), `CARRIED` (player inventory), `EQUIPPED` (worn by player)
119: 
120: ```json
121: [
122:   { "id": "pliers", "description": "A pair of sturdy iron pliers", "initialLocationType": "AREA", "initialLocationId": "Tower" }
123: ]
124: ```
125: 
126: ## Triggers (`triggers.json`)
127: 
128: JSON array of conditional event definitions. Fire automatically on area entry, device activation, or each turn (status triggers).
129: 
130: | Field | Type | Required | Notes |
131: |---|---|---|---|
132: | `id` | string | yes | Unique identifier |
133: | `ownerType` | string | yes | `AREA`, `DEVICE`, or `STATUS`. Determines when the trigger fires |
134: | `ownerId` | string | yes | Area/device name for AREA/DEVICE. Status name for STATUS (unused at runtime) |
135: | `conditions` | object[] | no | All must pass for the trigger to fire |
136: | `effects` | object[] | yes | Applied when conditions are met |
137: | `singleUse` | boolean | no | **Default: `true`**. If false, fires repeatedly |
138: | `intervalTurns` | int | no | Minimum turns between firings (only for `singleUse: false`) |
139: 
140: ### Conditions
141: 
142: All conditions must pass. Omit for unconditional triggers.
143: 
144: | Field | Type | Notes |
145: |---|---|---|
146: | `checkType` | string | `itemCarried`, `itemEquipped`, or omit for status check |
147: | `statusName` | string | Status to check (default: STATUS) |
148: | `op` | string | `>`, `<`, `<=`, `>=`, or `==`. **Default: `>`** |
149: | `threshold` | int | Value to compare against. **Default: 0** |
150: | `itemId` | string | Item ID for item checks |
151: 
152: ### Effects
153: 
154: Each effect is an object with a `type` field plus type-specific fields.
155: 
156: | Type | Fields | Description |
157: |---|---|---|
158: | `displayText` | `text` (string) | Shows a message to the player |
159: | `changeHealth` | `amount` (int) | Changes health by delta |
160: | `adjustStatus` | `statusName`, `amount` (int) | Adjusts status value, clamped to range |
161: | `setStatus` | `statusName`, `value` (int) | Sets status to exact value, clamped |
162: | `setLocation` | `itemId`, `locationType`, `locationId`? | Moves an item to a new location |
163: | `lockItem` | `itemId` | Prevents the item from being dropped |
164: | `unlockItem` | `itemId` | Allows the item to be dropped again |
165: | `setExitBlocked` | `fromId`, `toId`, `blocked` (boolean) | Sets whether an exit is passable. Use `false` to unblock, or omit/set `true` to block |
166: | `hideExit` | `fromId`, `toId` | Hides the exit so it does not appear in the UI |
167: | `showExit` | `fromId`, `toId` | Reveals a previously hidden exit |
168: | `endGame` | `text` (string) | Ends the game with a custom message. Only fires if no endGameMessage is already set. |
169: 
170: **Owner type timing:**
171: - `AREA`: fires when entering `ownerId` area (after move)
172: - `DEVICE`: fires after activating `ownerId` device
173: - `STATUS`: fires each turn, useful for ongoing damage/status effects
174: 
175: ```json
176: [
177:   {
178:     "id": "corruptionDamage",
179:     "ownerType": "STATUS",
180:     "ownerId": "corruption",
181:     "conditions": [
182:       {"statusName": "corruption", "op": ">", "threshold": 20}
183:     ],
184:     "effects": [
185:       {"type": "displayText", "text": "Corruption courses through your veins."},
186:       {"type": "changeHealth", "amount": -1}
187:     ],
188:     "singleUse": false,
189:     "intervalTurns": 2
190:   }
191: ]
192: ```
193: 
194: ## Validation & Rules
195: 
196: Engine throws `IllegalArgumentException` on load if violated:
197: - `startArea`, all `exits` (or legacy `connections`), and all `deviceId` references must exist
198: - Exit direction strings must be one of: North, West, South, East
199: - File paths resolve relative to the config file directory (subdirs supported)
200: - Status `min < max` for each defined status; `initial` must be within `[min, max]`
201: - Keys in device `statusEffects` must match a declared status name
202: - Trigger `ownerId` must exist when `ownerType` is `AREA` or `DEVICE`
203: - Effect references to `itemId` must match an item ID from `itemsFile`
204: 
205: **Win / Lose**: Game ends when a trigger fires with an `endGame` effect. Health reaching 0 does not automatically end the game — scenario authors must define triggers to handle death scenarios and other outcomes.
206: 
207: > **Define your own game-ending triggers.** Since health reaching 0 doesn't auto-end the game, add STATUS triggers for death conditions (use `"statusName": "health"` in conditions) and AREA/DEVICE triggers for victory or story endings:
208: > ```json
209: > {
210: >   "id": "deathAtZero",
211: >   "ownerType": "STATUS",
212: >   "ownerId": "health",
213: >   "conditions": [{"statusName": "health", "op": "<=", "threshold": 0}],
214: >   "effects": [
215: >     {"type": "displayText", "text": "Your vision fades. The ruins claim another soul."},
216: >     {"type": "endGame", "text": "The darkness takes you. Game Over."}
217: >   ],
218: >   "singleUse": true
219: > }
220: ```
221: 
222: ---
223: 
224: ## Maybe Keep (review → keep or delete)
225: 
226: ### Designing for LLMs
227: 1. Sketch connections first — ensure full traversal from startArea
228: 2. Sum of positive healthEffect > absolute sum of negatives for viable win path
229: 3. Place healing devices early, dangerous ones in less obvious spots
230: 4. Keep descriptions thematically consistent
231: 5. Test at health=1 to verify not accidentally unwinnable
232: 
233: ### Common Mistakes
234: - Forgetting to define return exits (A→B requires B→A if you want to go back)
235: - Mismatched `deviceId` strings (case-sensitive)
236: - Unreachable areas making the scenario unwinnable
237: - Referencing a device that doesn't exist in devicesFile
238: - Referencing a status name in `statusEffects` that isn't declared in config `statuses`
239: - Forgetting `initialLocationId` when `initialLocationType` is `AREA` or `DEVICE`
240: - Trigger with no conditions firing unintentionally on every turn (STATUS triggers)
241: - Setting `singleUse: false` without `intervalTurns`, causing per-turn spam

(End of file - total 241 lines)
</content>
```

---

## Assistant (Plan · qwen3.6-35b-a3b · 12.4s)

_Thinking:_

Let me read the other files mentioned in the diff to complete my review. Let me also check for any test files and other relevant source code.









**Tool: read**

**Input:**
```json
{
  "filePath": "/var/home/mikharju/dev/exploration/engine/src/main/kotlin/exploration/adapter/ui/lanterna/LanternaUiAdapter.kt"
}
```

**Output:**
```
<path>/var/home/mikharju/dev/exploration/engine/src/main/kotlin/exploration/adapter/ui/lanterna/LanternaUiAdapter.kt</path>
<type>file</type>
<content>
1: package exploration.adapter.ui.lanterna
2: 
3: import com.googlecode.lanterna.SGR
4: import com.googlecode.lanterna.TerminalPosition
5: import com.googlecode.lanterna.TextColor
6: import com.googlecode.lanterna.graphics.TextGraphics
7: import com.googlecode.lanterna.input.KeyType
8: import com.googlecode.lanterna.screen.Screen
9: import com.googlecode.lanterna.screen.TerminalScreen
10: import com.googlecode.lanterna.terminal.DefaultTerminalFactory
11: import exploration.model.StatusRange
12: import exploration.port.ExitInfo
13: import exploration.port.GameEngine
14: import exploration.port.GameRef
15: import exploration.port.InputEvent
16: import exploration.port.ItemView
17: import exploration.port.ViewData
18: 
19: class LanternaUiAdapter(private val engine: GameEngine) {
20: 
21:     data class HistoryEntry(val text: String, val isTrigger: Boolean)
22: 
23:     sealed interface Overlay {
24:         object None : Overlay
25:         data class MessageViewer(
26:             val messages: List<String>,
27:             var scrollOffset: Int = 0,
28:             val maxHeight: Int = 20,
29:             var focusedIndex: Int = 0
30:         ) : Overlay {
31: 
32:             fun overlayTextAreaMaxWidth(screen: Screen): Int =
33:                 minOf(170, maxOf(40, screen.terminalSize.columns - 10)) - 2
34: 
35:             fun overlayTextAreaMaxHeight(screen: Screen): Int =
36:                 (this.maxHeight + 3).coerceAtMost(screen.terminalSize.rows - 6)
37:         }
38: 
39:     }
40: 
41:     companion object {
42:         private const val MAX_MESSAGES = 10
43:         private const val MIN_WIDTH = 64
44:         private const val MIN_HEIGHT = 23
45:     }
46: 
47:     private enum class SelectionTarget { TAKE, DROP, EQUIP, UNEQUIP }
48: 
49:     private data class SelectionState(val target: SelectionTarget, val items: List<ItemView>)
50: 
51:     fun run(scenarioId: String) {
52:         var ref: GameRef
53:         try {
54:             ref = engine.start(scenarioId)
55:         } catch (e: Exception) {
56:             println("Error loading scenario '$scenarioId': ${e.message}")
57:             e.printStackTrace()
58:             return
59:         }
60: 
61:         val screen = TerminalScreen(DefaultTerminalFactory().createTerminal())
62:         screen.stopScreen()
63:         screen.startScreen()
64:         screen.cursorPosition = null
65: 
66:         val history = mutableListOf<HistoryEntry>()
67:         var shownTriggers = 0
68:         var selectionState: SelectionState? = null
69:         var overlay: Overlay = Overlay.None
70:         var storedStoryCount = 0
71: 
72:         addMessage(history, "=== Exploration Engine (Lanterna) ===", false)
73:         addMessage(
74:             history,
75:             "arrows/wasd: move | l: look | u: activate | g: grab | p: drop | e: equip | r: uneq | j: stories | h: help | q/esc: quit",
76:             false
77:         )
78: 
79:         var currentView = engine.tick(ref, InputEvent.Look)
80:         render(screen, currentView, history, overlay)
81: 
82:         val initialStoryCount = currentView.storyMessages.size
83:         storedStoryCount = initialStoryCount
84:         if (initialStoryCount > 0) {
85:             val subList = currentView.storyMessages.subList(0, initialStoryCount)
86:             overlay = Overlay.MessageViewer(subList, focusedIndex = subList.lastIndex)
87:             render(screen, currentView, history, overlay)
88:         }
89: 
90:         while (currentView.endGameMessage == null) {
91:             screen.doResizeIfNecessary()
92:             val result = readInputKey(screen, currentView, selectionState, overlay)
93:             selectionState = result.selectionState
94:             overlay = result.overlay
95: 
96:             when (result.action) {
97:                 is KeyAction.Quit -> break
98:                 is KeyAction.Help -> {
99:                     addMessage(history, "arrows/wasd: move | l: look | u: activate | g: grab | p: drop | e: equip | r: uneq | j: stories | h: help | q/esc: quit", false)
100:                     render(screen, currentView, history, overlay)
101:                     continue
102:                 }
103:                 is KeyAction.NoOp -> {
104:                     render(screen, currentView, history, overlay)
105:                     continue
106:                 }
107:                 is KeyAction.WaitSelection -> {
108:                     val sel = selectionState!!
109:                     addMessage(history, buildSelectionPrompt(sel.target, sel.items), false)
110:                     render(screen, currentView, history, overlay)
111:                     continue
112:                 }
113:                 is KeyAction.Event -> {
114:                     if (overlay != Overlay.None) overlay = Overlay.None
115:                     val next = engine.tick(ref, result.action.event)
116: 
117:                     currentView = next
118:                     if (next.commandText.isNotBlank()) addMessage(history, next.commandText, false)
119:                     val newTriggerCount = next.triggerTexts.size - shownTriggers
120:                     for (i in 0 until newTriggerCount) {
121:                         val triggerText = next.triggerTexts[shownTriggers + i]
122:                         if (triggerText.isNotBlank()) addMessage(history, triggerText, true)
123:                     }
124: 
125:                     // Show the newest story message(s) as overlay
126:                     if (next.storyMessages.size > storedStoryCount) {
127:                         val subList = next.storyMessages.subList(storedStoryCount, next.storyMessages.size)
128:                         overlay = Overlay.MessageViewer(subList, focusedIndex = subList.lastIndex)
129:                         storedStoryCount = next.storyMessages.size
130:                     } else {
131:                         storedStoryCount = next.storyMessages.size
132:                     }
133: 
134:                     shownTriggers = next.triggerTexts.size
135:                     render(screen, currentView, history, overlay)
136:                 }
137:             }
138:         }
139: 
140:         for (i in shownTriggers until currentView.triggerTexts.size) {
141:             if (currentView.triggerTexts[i].isNotBlank()) addMessage(history, currentView.triggerTexts[i], true)
142:         }
143:         storedStoryCount = currentView.storyMessages.size
144:         if (currentView.endGameMessage != null) {
145:             addMessage(history, "", false)
146:             addMessage(history, currentView.endGameMessage, false)
147:         } else if (currentView.outputLine.isNotBlank()) {
148:             addMessage(history, currentView.outputLine, false)
149:         }
150:         screen.doResizeIfNecessary()
151:         render(screen, currentView, history, overlay)
152: 
153:         screen.stopScreen()
154:         printEndSummary(currentView, history)
155:     }
156: 
157:     private sealed class KeyAction {
158:         object Quit : KeyAction()
159:         object Help : KeyAction()
160:         object WaitSelection : KeyAction()
161:         object NoOp : KeyAction()
162:         data class Event(val event: InputEvent) : KeyAction()
163:     }
164: 
165:     private data class ReadInputResult(val action: KeyAction, val selectionState: SelectionState?, val overlay: Overlay)
166: 
167:     private fun readInputKey(
168:         screen: Screen,
169:         view: ViewData,
170:         selState: SelectionState?,
171:         currentOverlay: Overlay
172:     ): ReadInputResult {
173:         val key = screen.readInput() ?: return ReadInputResult(KeyAction.Quit, null, currentOverlay)
174: 
175:         if (selState != null) {
176:             return handleSelectionDigit(key, selState, currentOverlay)
177:         }
178: 
179:         var nextOverlay = currentOverlay
180:         var handledInOverlay: Boolean
181: 
182:         when (currentOverlay) {
183:             is Overlay.MessageViewer -> {
184:                 val ov = currentOverlay
185:                 val totalWrappedLines = countWrappedDisplayLines(ov.messages, ov.overlayTextAreaMaxWidth(screen))
186:                 val visibleRows = ov.overlayTextAreaMaxHeight(screen) - 3
187:                 val maxScroll = maxOf(0, totalWrappedLines - visibleRows + 3)
188:                 handledInOverlay = when (key.keyType) {
189:                     KeyType.PageDown -> {
190:                         nextOverlay = ov.copy(scrollOffset = minOf(maxScroll, ov.scrollOffset + maxOf(1, ov.overlayTextAreaMaxHeight(screen) - 2)))
191:                         true
192:                     }
193:                     KeyType.ArrowDown -> {
194:                         nextOverlay = ov.copy(scrollOffset = minOf(maxScroll, ov.scrollOffset + 1))
195:                         true
196:                     }
197:                     KeyType.PageUp -> {
198:                         nextOverlay = ov.copy(scrollOffset = maxOf(0, ov.scrollOffset - maxOf(1, ov.maxHeight - 2)))
199:                         true
200:                     }
201:                     KeyType.ArrowUp -> {
202:                         nextOverlay = ov.copy(scrollOffset = maxOf(0, ov.scrollOffset - 1))
203:                         true
204:                     }
205:                     KeyType.ArrowLeft -> {
206:                         if (ov.focusedIndex > 0)
207:                             nextOverlay = ov.copy(focusedIndex = ov.focusedIndex - 1, scrollOffset = 0)
208:                         true
209:                     }
210:                     KeyType.ArrowRight -> {
211:                         if (ov.focusedIndex < ov.messages.size - 1)
212:                             nextOverlay = ov.copy(focusedIndex = ov.focusedIndex + 1, scrollOffset = 0)
213:                         true
214:                     }
215:                     KeyType.Escape -> {
216:                         nextOverlay = Overlay.None
217:                         true
218:                     }
219:                     else -> false
220:                 }
221:             }
222:             Overlay.None -> handledInOverlay = false
223:         }
224: 
225:         if (handledInOverlay) return ReadInputResult(KeyAction.NoOp, null, nextOverlay)
226: 
227:         val displayOverlay = if (currentOverlay !is Overlay.None && key.keyType != KeyType.Escape) Overlay.None else currentOverlay
228: 
229:         when (key.keyType) {
230:             KeyType.Escape -> return if (currentOverlay !is Overlay.None) {
231:                 ReadInputResult(KeyAction.NoOp, null, Overlay.None)
232:             } else {
233:                 ReadInputResult(KeyAction.Quit, null, currentOverlay)
234:             }
235:             KeyType.ArrowUp -> return ReadInputResult(KeyAction.Event(InputEvent.MoveDirection(InputEvent.Direction.North)), null, displayOverlay)
236:             KeyType.ArrowDown -> return ReadInputResult(KeyAction.Event(InputEvent.MoveDirection(InputEvent.Direction.South)), null, displayOverlay)
237:             KeyType.ArrowLeft -> return ReadInputResult(KeyAction.Event(InputEvent.MoveDirection(InputEvent.Direction.West)), null, displayOverlay)
238:             KeyType.ArrowRight -> return ReadInputResult(KeyAction.Event(InputEvent.MoveDirection(InputEvent.Direction.East)), null, displayOverlay)
239:             KeyType.Character -> {
240:                 when (key.character.lowercaseChar()) {
241:                     'w' -> return ReadInputResult(KeyAction.Event(InputEvent.MoveDirection(InputEvent.Direction.North)), null, displayOverlay)
242:                     'a' -> return ReadInputResult(KeyAction.Event(InputEvent.MoveDirection(InputEvent.Direction.West)), null, displayOverlay)
243:                     's' -> return ReadInputResult(KeyAction.Event(InputEvent.MoveDirection(InputEvent.Direction.South)), null, displayOverlay)
244:                     'd' -> return ReadInputResult(KeyAction.Event(InputEvent.MoveDirection(InputEvent.Direction.East)), null, displayOverlay)
245:                     'l' -> return ReadInputResult(KeyAction.Event(InputEvent.Look), null, displayOverlay)
246:                     'u' -> return ReadInputResult(KeyAction.Event(InputEvent.Activate), null, displayOverlay)
247:                     'g' -> {
248:                         val candidates = view.areaItems
249:                         return if (candidates.isEmpty()) ReadInputResult(KeyAction.Help, null, displayOverlay)
250:                         else if (candidates.size == 1) ReadInputResult(KeyAction.Event(InputEvent.TakeItem(candidates[0].name)), null, displayOverlay)
251:                         else ReadInputResult(KeyAction.WaitSelection, SelectionState(SelectionTarget.TAKE, candidates), displayOverlay)
252:                     }
253:                     'p' -> {
254:                         val candidates = view.carriedItems + view.equippedItems
255:                         return if (candidates.isEmpty()) ReadInputResult(KeyAction.Help, null, displayOverlay)
256:                         else if (candidates.size == 1) ReadInputResult(KeyAction.Event(InputEvent.DropItem(candidates[0].name)), null, displayOverlay)
257:                         else ReadInputResult(KeyAction.WaitSelection, SelectionState(SelectionTarget.DROP, candidates), displayOverlay)
258:                     }
259:                     'e' -> {
260:                         val candidates = view.carriedItems
261:                         return if (candidates.isEmpty()) ReadInputResult(KeyAction.Help, null, displayOverlay)
262:                         else if (candidates.size == 1) ReadInputResult(KeyAction.Event(InputEvent.EquipItem(candidates[0].name)), null, displayOverlay)
263:                         else ReadInputResult(KeyAction.WaitSelection, SelectionState(SelectionTarget.EQUIP, candidates), displayOverlay)
264:                     }
265:                     'r' -> {
266:                         val candidates = view.equippedItems
267:                         return if (candidates.isEmpty()) ReadInputResult(KeyAction.Help, null, displayOverlay)
268:                         else if (candidates.size == 1) ReadInputResult(KeyAction.Event(InputEvent.UnequipItem(candidates[0].name)), null, displayOverlay)
269:                         else ReadInputResult(KeyAction.WaitSelection, SelectionState(SelectionTarget.UNEQUIP, candidates), displayOverlay)
270:                     }
271:                     'i' -> return ReadInputResult(KeyAction.Event(InputEvent.Inventory), null, displayOverlay)
272:                     'j' -> {
273:                         val msgs = view.storyMessages.filter { it.isNotBlank() }
274:                         return if (msgs.isEmpty()) ReadInputResult(KeyAction.Help, null, displayOverlay)
275:                         else ReadInputResult(KeyAction.NoOp, null, Overlay.MessageViewer(msgs, focusedIndex = msgs.lastIndex))
276:                     }
277:                     'h' -> return ReadInputResult(KeyAction.NoOp, null, displayOverlay)
278:                     'q' -> return ReadInputResult(KeyAction.Quit, null, displayOverlay)
279:                 }
280:             }
281:             else -> {}
282:         }
283:         return ReadInputResult(KeyAction.Help, null, currentOverlay)
284:     }
285: 
286:     private fun handleSelectionDigit(key: com.googlecode.lanterna.input.KeyStroke, selState: SelectionState, overlay: Overlay): ReadInputResult {
287:         if (key.keyType == KeyType.Escape) return ReadInputResult(KeyAction.Help, null, overlay)
288:         if (key.keyType != KeyType.Character) return ReadInputResult(KeyAction.WaitSelection, selState, overlay)
289:         val idx = key.character.digitToIntOrNull() ?: return ReadInputResult(KeyAction.WaitSelection, selState, overlay)
290:         val itemIdx = idx - 1
291:         if (itemIdx < 0 || itemIdx >= selState.items.size) return ReadInputResult(KeyAction.Help, null, overlay)
292:         val item = selState.items[itemIdx]
293:         val event = when (selState.target) {
294:             SelectionTarget.TAKE -> InputEvent.TakeItem(item.name)
295:             SelectionTarget.DROP -> InputEvent.DropItem(item.name)
296:             SelectionTarget.EQUIP -> InputEvent.EquipItem(item.name)
297:             SelectionTarget.UNEQUIP -> InputEvent.UnequipItem(item.name)
298:         }
299:         return ReadInputResult(KeyAction.Event(event), null, overlay)
300:     }
301: 
302:     private fun countWrappedDisplayLines(messages: List<String>, lineWidth: Int): Int {
303:         var total = 0
304:         for (msg in messages) {
305:             for (line in msg.split("\n")) {
306:                 val wrappedLines = wrapText(line, lineWidth)
307:                 total += wrappedLines.size
308:             }
309:         }
310:         return total
311:     }
312: 
313:     private fun buildSelectionPrompt(target: SelectionTarget, items: List<ItemView>): String {
314:         val verb = when (target) {
315:             SelectionTarget.TAKE -> "Take"
316:             SelectionTarget.DROP -> "Drop"
317:             SelectionTarget.EQUIP -> "Equip"
318:             SelectionTarget.UNEQUIP -> "Unequip"
319:         }
320:         val options = items.mapIndexed { i, it -> "[${i + 1}] ${it.name}" }.joinToString(" ")
321:         return "$verb what? $options"
322:     }
323: 
324:     private fun addMessage(history: MutableList<HistoryEntry>, msg: String, isTrigger: Boolean) {
325:         if (msg.isBlank()) return
326:         history.add(HistoryEntry(msg, isTrigger))
327:         while (history.size > MAX_MESSAGES) history.removeAt(0)
328:     }
329: 
330:     private fun render(screen: Screen, view: ViewData, history: List<HistoryEntry>, overlay: Overlay) {
331:         screen.clear()
332:         val g = screen.newTextGraphics()
333:         val size = screen.terminalSize
334:         val w = size.columns
335:         val h = size.rows
336: 
337:         if (h < MIN_HEIGHT || w < MIN_WIDTH) {
338:             drawBorder(g, w, h)
339:             drawTooSmallMessage(g, w, h)
340:             screen.refresh()
341:             return
342:         }
343: 
344:         // Draw overlay if active
345:         when (overlay) {
346:             is Overlay.MessageViewer -> {
347:                 drawMessageOverlay(screen, g, view, overlay, size)
348:                 screen.refresh()
349:                 return
350:             }
351:             else -> {}
352:         }
353: 
354:         val leftWidth = ((w - 3) * 0.6).toInt()
355:         val rightWidth = (w - 3) - leftWidth
356:         val splitCol = 1 + leftWidth
357:         val panelStartRow = 3
358: 
359:         drawBorder(g, w, h)
360:         drawTitle(g, w)
361:         drawPanelHeaders(g, leftWidth, rightWidth, splitCol)
362:         drawSeparatorLine(g, w, splitCol, panelStartRow - 1)
363:         drawMessagesPanel(g, history, TerminalPosition(1, panelStartRow), leftWidth, h - panelStartRow - 4)
364:         drawStatusesPanel(g, view, TerminalPosition(splitCol + 1, panelStartRow), rightWidth - 1, h - panelStartRow - 4)
365:         drawVerticalSeparator(g, splitCol, 2, h - 5)
366:         drawSplitSeparator(g, w, splitCol, h - 5)
367:         drawBottomBar(g, view, w - 2, h - 5)
368: 
369:         screen.refresh()
370:     }
371: 
372:     private fun drawMessageOverlay(
373:         screen: Screen,
374:         g: TextGraphics,
375:         view: ViewData,
376:         overlay: Overlay.MessageViewer,
377:         size: com.googlecode.lanterna.TerminalSize
378:     ) {
379:         val w = size.columns
380:         val h = size.rows
381:         val panelW = minOf(170, maxOf(40, w - 10))
382:         val panelH = overlay.maxHeight.coerceAtMost(h - 6)
383:         val startCol = (w - panelW) / 2 + 1
384:         val startRow = (h - panelH) / 2
385:         val textAreaWidth = overlay.overlayTextAreaMaxWidth(screen)
386: 
387:         drawBorder(g, startCol - 1, startRow - 1, startCol + panelW, startRow + panelH + 1)
388: 
389:         val title = " Story Message"
390:         g.foregroundColor = TextColor.ANSI.CYAN
391:         g.enableModifiers(SGR.BOLD)
392:         g.putString(startCol, startRow, title)
393:         g.disableModifiers(SGR.BOLD)
394:         g.foregroundColor = TextColor.ANSI.DEFAULT
395: 
396:         val contentTop = startRow + 2
397:         val focusedMsg = overlay.messages.getOrElse(overlay.focusedIndex) { "" }
398:         var rowIdx = 0
399:         for (paragraph in focusedMsg.split("\n")) {
400:             if (paragraph.isBlank()) {
401:                 if (rowIdx < overlay.scrollOffset) { rowIdx++; continue }
402:                 if (rowIdx >= overlay.scrollOffset + panelH - 3) break
403:                 val drawRow = contentTop + (rowIdx - overlay.scrollOffset)
404:                 if (drawRow < startRow + panelH - 1) {
405:                     g.putString(startCol, drawRow, "".padEnd(textAreaWidth))
406:                 }
407:                 rowIdx++
408:             } else {
409:                 for (line in wrapText(paragraph, textAreaWidth)) {
410:                     if (rowIdx < overlay.scrollOffset) { rowIdx++; continue }
411:                     if (rowIdx >= overlay.scrollOffset + panelH - 3) break
412:                     val drawRow = contentTop + (rowIdx - overlay.scrollOffset)
413:                     if (drawRow < startRow + panelH - 1) {
414:                         g.putString(startCol, drawRow, line.padEnd(textAreaWidth))
415:                     }
416:                     rowIdx++
417:                 }
418:             }
419:         }
420: 
421:         val totalCount = countWrappedDisplayLines(listOf(focusedMsg), textAreaWidth)
422:         val canScrollUp = overlay.scrollOffset > 0
423:         val canScrollDown = (panelH - 3 > 0) && (overlay.scrollOffset + panelH - 3 < totalCount)
424:         g.putString(startCol, startRow + panelH - 1, buildString {
425:             append(" [PgUp:up PgDn:down ")
426:             if (canScrollUp || canScrollDown) append(" | ")
427:             if (canScrollUp) append("scroll↑ ")
428:             if (canScrollDown) append("scroll↓ ")
429:             append("| ${overlay.focusedIndex + 1} / ${overlay.messages.size} esc:close]")
430:         })
431: 
432:         g.foregroundColor = TextColor.ANSI.DEFAULT
433:     }
434: 
435: 
436:     private fun drawBorder(g: TextGraphics, left: Int, top: Int, right: Int, bottom: Int) {
437:         g.setCharacter(left, top, '┌')
438:         g.setCharacter(right - 1, top, '┐')
439:         g.setCharacter(left, bottom - 1, '└')
440:         g.setCharacter(right - 1, bottom - 1, '┘')
441:         for (col in left + 1 until right - 1) {
442:             g.setCharacter(col, top, '─')
443:             g.setCharacter(col, bottom - 1, '─')
444:         }
445:         for (row in top + 1 until bottom - 1) {
446:             g.setCharacter(left, row, '│')
447:             g.setCharacter(right - 1, row, '│')
448:         }
449:     }
450: 
451:     private fun drawBorder(g: TextGraphics, w: Int, h: Int) {
452:         g.setCharacter(0, 0, '┌')
453:         g.setCharacter(w - 1, 0, '┐')
454:         g.setCharacter(0, h - 1, '└')
455:         g.setCharacter(w - 1, h - 1, '┘')
456:         for (col in 1 until w - 1) {
457:             g.setCharacter(col, 0, '─')
458:             g.setCharacter(col, h - 1, '─')
459:         }
460:         for (row in 1 until h - 1) {
461:             g.setCharacter(0, row, '│')
462:             g.setCharacter(w - 1, row, '│')
463:         }
464:     }
465: 
466:     private fun drawTitle(g: TextGraphics, width: Int) {
467:         val title = " Exploration Engine "
468:         val pad = maxOf(width - title.length, 0) / 2
469:         g.enableModifiers(SGR.BOLD)
470:         g.foregroundColor = TextColor.ANSI.CYAN
471:         g.putString(pad, 0, title)
472:         g.disableModifiers(SGR.BOLD)
473:     }
474: 
475:     private fun drawPanelHeaders(g: TextGraphics, leftWidth: Int, rightWidth: Int, splitCol: Int) {
476:         val msgHeader = " Messages"
477:         g.enableModifiers(SGR.BOLD)
478:         g.foregroundColor = TextColor.ANSI.WHITE
479:         g.putString(1, 1, msgHeader)
480: 
481:         val statusHeader = " Statuses"
482:         val rightStart = splitCol + 1
483:         g.putString(rightStart, 1, statusHeader)
484:         g.disableModifiers(SGR.BOLD)
485: 
486:         for (col in 1 until splitCol) {
487:             if (col < 1 + leftWidth && col > 0) {
488:                 g.setCharacter(col, 2, '─')
489:             }
490:         }
491:         g.setCharacter(splitCol, 2, '│')
492:         for (col in splitCol + 1 until splitCol + rightWidth) {
493:             if (col < 1 + leftWidth + rightWidth - 1) {
494:                 g.setCharacter(col, 2, '─')
495:             }
496:         }
497:     }
498: 
499:     private fun drawSeparatorLine(g: TextGraphics, w: Int, splitCol: Int, row: Int) {
500:         for (col in 0 until w) {
501:             if (col == splitCol) g.setCharacter(col, row, '┼')
502:             else g.setCharacter(col, row, '─')
503:         }
504:     }
505: 
506:     private fun drawVerticalSeparator(g: TextGraphics, col: Int, startRow: Int, count: Int) {
507:         for (row in startRow until startRow + count) {
508:             if (row > 0 && row < count + startRow - 1) {
509:                 g.setCharacter(col, row, '│')
510:             }
511:         }
512:     }
513: 
514:     private fun drawSplitSeparator(g: TextGraphics, w: Int, splitCol: Int, row: Int) {
515:         for (col in 0 until w) {
516:             if (col == splitCol) g.setCharacter(col, row, '┼')
517:             else g.setCharacter(col, row, '─')
518:         }
519:     }
520: 
521:     private fun drawMessagesPanel(
522:         g: TextGraphics,
523:         history: List<HistoryEntry>,
524:         top: TerminalPosition,
525:         width: Int,
526:         maxRows: Int
527:     ) {
528:         val lines = mutableListOf<Pair<String, Boolean>>()
529:         for (entry in history) {
530:             for (segment in entry.text.split("\n")) {
531:                 wrapText(segment, width).forEach { lines.add(it to entry.isTrigger) }
532:             }
533:         }
534: 
535:         val skip = maxOf(0, lines.size - maxRows)
536:         var row = top.row
537:         for (i in skip until lines.size) {
538:             if (row >= top.row + maxRows) break
539:             val (lineText, isTrigger) = lines[i]
540:             g.foregroundColor = if (isTrigger) TextColor.ANSI.BLUE else TextColor.ANSI.DEFAULT
541:             g.putString(top.column, row, lineText.padEnd(width))
542:             row++
543:         }
544:         g.foregroundColor = TextColor.ANSI.DEFAULT
545:     }
546: 
547:     private fun drawStatusesPanel(
548:         g: TextGraphics,
549:         v: ViewData,
550:         top: TerminalPosition,
551:         width: Int,
552:         maxRows: Int
553:     ) {
554:         val hpLine = buildHpBar(v, width)
555:         g.putString(top.column, top.row, hpLine.padEnd(width))
556: 
557:         val progLine = " Exp: ${v.exploredCount}/${v.totalAreas}   Dev: ${v.activatedCount}/${v.totalDevices}"
558:         g.putString(top.column, top.row + 1, progLine.padEnd(width))
559: 
560:         var row = top.row + 2
561: 
562:         val statuses = v.statuses.filterValues { it != 0 }
563:         for ((name, value) in statuses) {
564:             if (row >= top.row + maxRows) break
565:             val range = v.statusBounds[name]
566:             g.putString(top.column, row, formatStatus(name, value, range, width).padEnd(width))
567:             row++
568:         }
569: 
570:         if (v.equippedItems.isEmpty()) return
571: 
572:         if (row < top.row + maxRows && statuses.isNotEmpty()) {
573:             g.putString(top.column, row, " ".repeat(width))
574:             row++
575:         }
576: 
577:         for ((i, item) in v.equippedItems.withIndex()) {
578:             if (row >= top.row + maxRows) break
579:             val label = "@ ${item.name}" + if (item.locked) " (Locked)" else ""
580:             g.foregroundColor = if (item.locked) TextColor.ANSI.RED else TextColor.ANSI.MAGENTA
581:             g.enableModifiers(SGR.BOLD)
582:             g.putString(top.column, row, label.padEnd(width))
583:             g.disableModifiers(SGR.BOLD)
584:             g.foregroundColor = TextColor.ANSI.DEFAULT
585:             row++
586:         }
587: 
588:         g.foregroundColor = TextColor.ANSI.DEFAULT
589:     }
590: 
591:     private fun buildHpBar(v: ViewData, width: Int): String {
592:         val barLen = 10
593:         val filled = ((v.health.toDouble() / v.maxHealth) * barLen).toInt().coerceIn(0, barLen)
594:         val empty = barLen - filled
595: 
596:         val bar = "█".repeat(filled) + "░".repeat(empty)
597:         val textPart = "${v.health}/${v.maxHealth}"
598:         return "$bar $textPart"
599:     }
600: 
601:     private fun formatStatus(name: String, value: Int, range: StatusRange?, width: Int): String {
602:         val maxLen = width - 10
603:         val displayValue = if (range != null && range.max != Int.MAX_VALUE) {
604:             "$value/${range.max}"
605:         } else {
606:             "$value"
607:         }
608:         val template = ": $displayValue"
609:         val nameMaxLen = maxOf(4, width - template.length)
610: 
611:         val displayName = if (name.length > nameMaxLen) {
612:             name.substring(0, nameMaxLen - 3) + "..."
613:         } else {
614:             name
615:         }
616:         return "$displayName$template"
617:     }
618: 
619:     private fun wrapText(text: String, width: Int): List<String> {
620:         val words = text.split(" ")
621:         val result = mutableListOf<String>()
622:         var line = ""
623: 
624:         for (word in words) {
625:             if (word.length >= width) {
626:                 if (line.isNotEmpty()) result.add(line)
627:                 line = ""
628:                 var remaining = word
629:                 while (remaining.isNotEmpty()) {
630:                     if (remaining.length <= width) {
631:                         line += remaining
632:                         remaining = ""
633:                     } else {
634:                         result.add(remaining.substring(0, width))
635:                         remaining = remaining.substring(width)
636:                     }
637:                 }
638:             } else if ((line + " " + word).length <= width) {
639:                 line = if (line.isEmpty()) word else "$line $word"
640:             } else {
641:                 if (line.isNotEmpty()) result.add(line)
642:                 line = word
643:             }
644:         }
645:         if (line.isNotEmpty()) result.add(line)
646:         return result.ifEmpty { listOf("") }
647:     }
648: 
649:     private fun drawBottomBar(
650:         g: TextGraphics,
651:         view: ViewData,
652:         width: Int,
653:         topRow: Int
654:     ) {
655:         val exits = view.exits
656:         val wInfo = exits.getOrNull(0)
657:         val aInfo = exits.getOrNull(1)
658:         val sInfo = exits.getOrNull(2)
659:         val dInfo = exits.getOrNull(3)
660: 
661:         val wLabel = exitLabel('w', wInfo)
662:         val aLabel = exitLabel('a', aInfo)
663:         val sLabel = exitLabel('s', sInfo)
664:         val dLabel = exitLabel('d', dInfo)
665: 
666:         val gap = 2
667:         val contentWidth = aLabel.length + gap + sLabel.length + gap + dLabel.length
668:         val leftPad = (width - contentWidth) / 2
669: 
670:         val colA = leftPad
671:         val colS = colA + aLabel.length + gap
672:         val colD = colS + sLabel.length + gap
673:         val colW = colS - wLabel.length / 2
674: 
675:         drawSlot(g, wLabel, TerminalPosition(colW, topRow), wInfo != null, wInfo?.blocked ?: false)
676:         drawInvHint(g, width, topRow)
677:         drawSlot(g, aLabel, TerminalPosition(colA, topRow + 1), aInfo != null, aInfo?.blocked ?: false)
678:         drawSlot(g, sLabel, TerminalPosition(colS, topRow + 1), sInfo != null, sInfo?.blocked ?: false)
679:         drawSlot(g, dLabel, TerminalPosition(colD, topRow + 1), dInfo != null, dInfo?.blocked ?: false)
680:         drawActionHints(g, view, width, topRow + 2)
681:     }
682: 
683:     private fun drawInvHint(g: TextGraphics, width: Int, row: Int) {
684:         val label = "[\$i] inv"
685:         g.foregroundColor = TextColor.ANSI.CYAN
686:         g.enableModifiers(SGR.BOLD)
687:         val keyPart = "[i]"
688:         g.putString(width - 10, row, keyPart)
689:         g.disableModifiers(SGR.BOLD)
690:         g.foregroundColor = TextColor.ANSI.WHITE
691:         g.putString(width - 7, row, "inv")
692:     }
693: 
694:     private fun drawActionHints(g: TextGraphics, view: ViewData, width: Int, row: Int) {
695:         val hasAreaItems = view.areaItems.isNotEmpty()
696:         val hasCarriedOrEquipped = view.carriedItems.isNotEmpty() || view.equippedItems.isNotEmpty()
697:         val hasCarried = view.carriedItems.isNotEmpty()
698:         val hasEquipped = view.equippedItems.isNotEmpty()
699: 
700:         val gLabel = "[g] grab"
701:         val pLabel = "[p] drop"
702:         val eLabel = "[e] equip"
703:         val rLabel = "[r] uneq"
704: 
705:         val gap = 2
706:         val totalWidth = gLabel.length + gap + pLabel.length + gap + eLabel.length + gap + rLabel.length
707:         val leftPad = (width - totalWidth) / 2
708: 
709:         var col = leftPad
710:         drawActionSlot(g, gLabel, TerminalPosition(col, row), hasAreaItems)
711:         col += gLabel.length + gap
712:         drawActionSlot(g, pLabel, TerminalPosition(col, row), hasCarriedOrEquipped)
713:         col += pLabel.length + gap
714:         drawActionSlot(g, eLabel, TerminalPosition(col, row), hasCarried)
715:         col += eLabel.length + gap
716:         drawActionSlot(g, rLabel, TerminalPosition(col, row), hasEquipped)
717:     }
718: 
719:     private fun drawActionSlot(g: TextGraphics, label: String, pos: TerminalPosition, active: Boolean) {
720:         g.foregroundColor = if (active) TextColor.ANSI.GREEN else TextColor.ANSI.DEFAULT
721:         g.enableModifiers(SGR.BOLD)
722:         val keyPart = label.substringBefore(']') + "]"
723:         g.putString(pos.column, pos.row, keyPart)
724:         g.disableModifiers(SGR.BOLD)
725: 
726:         val nameStart = pos.column + keyPart.length
727:         val namePart = if (label.contains("] ")) label.substringAfter("] ") else ""
728:         g.foregroundColor = if (active) TextColor.ANSI.WHITE else TextColor.ANSI.DEFAULT
729:         g.putString(nameStart, pos.row, namePart)
730: 
731:         g.disableModifiers(SGR.BOLD)
732:     }
733: 
734:     private fun exitLabel(key: Char, info: ExitInfo?): String =
735:         if (info == null || info.name.isNullOrEmpty()) "[${key}]  ."
736:         else {
737:             val suffix = if (info.blocked) " (B)" else ""
738:             "[${key}] ${info.name}$suffix"
739:         }
740: 
741:     private fun drawSlot(g: TextGraphics, label: String, pos: TerminalPosition, active: Boolean, blocked: Boolean) {
742:         g.foregroundColor = when {
743:             !active -> TextColor.ANSI.DEFAULT
744:             blocked -> TextColor.ANSI.DEFAULT
745:             else -> TextColor.ANSI.GREEN
746:         }
747:         g.enableModifiers(SGR.BOLD)
748:         val keyPart = label.substringBefore(']') + "]"
749:         g.putString(pos.column, pos.row, keyPart)
750:         g.disableModifiers(SGR.BOLD)
751: 
752:         val nameStart = pos.column + keyPart.length
753:         val namePart = if (label.contains("] ")) label.substringAfter("] ") else ""
754:         g.foregroundColor = when {
755:             !active -> TextColor.ANSI.DEFAULT
756:             blocked -> TextColor.ANSI.DEFAULT
757:             else -> TextColor.ANSI.GREEN
758:         }
759:         g.putString(nameStart, pos.row, namePart)
760: 
761:         g.disableModifiers(SGR.BOLD)
762:     }
763: 
764:     private fun drawTooSmallMessage(g: TextGraphics, w: Int, h: Int) {
765:         val lines = listOf(
766:             "Terminal too small.",
767:             "Resize to at least $MIN_WIDTH columns, $MIN_HEIGHT rows."
768:         )
769:         val startRow = (h - lines.size) / 2
770:         g.foregroundColor = TextColor.ANSI.YELLOW
771:         g.enableModifiers(SGR.BOLD)
772:         for ((i, line) in lines.withIndex()) {
773:             val col = (w - line.length) / 2
774:             g.putString(col, startRow + i, line)
775:         }
776:         g.disableModifiers(SGR.BOLD)
777:     }
778: 
779:     private fun printEndSummary(view: ViewData, history: List<HistoryEntry>) {
780:         println()
781:         if (view.endGameMessage != null) {
782:             println("===== ${view.endGameMessage} =====")
783:         } else {
784:             println("===== Game Over =====")
785:         }
786:         println("Area  : ${view.currentAreaName}")
787:         println("Health: ${view.health}/${view.maxHealth}")
788:         println("Explored: ${view.exploredCount}/${view.totalAreas}  |  Devices: ${view.activatedCount}/${view.totalDevices}")
789:         if (view.statuses.isNotEmpty()) {
790:             val statusLines = view.statuses.filterValues { it != 0 }.map { (name, value) ->
791:                 "$name=$value"
792:             }
793:             println("Statuses: ${statusLines.joinToString(", ")}")
794:         }
795:         if (history.isNotEmpty()) {
796:             println("\n--- Last messages ---")
797:             for (entry in history) println(entry.text)
798:         }
799:     }
800: }

(End of file - total 800 lines)
</content>
```

**Tool: read**

**Input:**
```json
{
  "filePath": "/var/home/mikharju/dev/exploration/engine/src/main/kotlin/exploration/scenario/ScenarioLoader.kt"
}
```

**Output:**
```
<path>/var/home/mikharju/dev/exploration/engine/src/main/kotlin/exploration/scenario/ScenarioLoader.kt</path>
<type>file</type>
<content>
1: package exploration.scenario
2: 
3: import exploration.core.engine.fireAreaTriggers
4: import exploration.model.*
5: import exploration.state.GameState
6: 
7: fun assembleGame(
8:     config: ScenarioConfig,
9:     areaEntries: List<AreaEntry>,
10:     deviceEntries: List<DeviceEntry>,
11:     triggerEntries: List<TriggerEntry> = emptyList(),
12:     itemEntries: List<ItemEntry> = emptyList()
13: ): GameState {
14:     val statusBounds = config.statuses.mapValues { (_, v) -> StatusRange(v.min, v.max) }
15:     val statusInitial = config.statuses.entries.associate { (k, v) -> k to v.initial }
16: 
17:     val deviceMap = buildDevices(deviceEntries)
18: 
19:     val areaIds = areaEntries.map { it.id }.toSet()
20:     val areasMap = buildAreas(areaEntries, deviceMap, areaIds)
21: 
22:     val startId = AreaId(config.playerStart.startArea)
23:     if (startId !in areasMap.keys) {
24:         throw IllegalArgumentException("Start area '${config.playerStart.startArea}' not found in areas")
25:     }
26: 
27:     val world = World(areas = areasMap, startArea = startId)
28: 
29:     val player = Player(
30:         health = config.playerStart.health,
31:         maxHealth = config.playerStart.maxHealth,
32:         currentArea = startId,
33:         statuses = statusInitial
34:     )
35: 
36:     val items = buildItems(itemEntries)
37: 
38:     val initialExitStates = buildInitialExitStates(areaEntries, areaIds)
39: 
40:     val triggers = buildTriggers(triggerEntries)
41: 
42:     validateTriggers(triggers, areaIds, deviceMap.keys.toSet(), items.map { it.id }.toSet())
43: 
44:     val state = GameState(
45:         world = world,
46:         player = player,
47:         statusBounds = statusBounds,
48:         triggers = triggers,
49:         items = items,
50:         exitStates = initialExitStates,
51:         storyMessages = if (config.welcomeMessage.isNotBlank()) listOf(config.welcomeMessage) else emptyList()
52:     )
53: 
54:     return fireAreaTriggers(state, startId)
55: }
56: 
57: private fun buildInitialExitStates(areaEntries: List<AreaEntry>, areaIds: Set<String>): Map<exploration.model.ExitId, exploration.model.ExitStateData> {
58:     val result = mutableMapOf<exploration.model.ExitId, exploration.model.ExitStateData>()
59:     for (entry in areaEntries) {
60:         val from = AreaId(entry.id)
61:         if ((entry.exits ?: emptyList()).isNotEmpty()) {
62:             for (exitEntry in entry.exits!!) {
63:                 require(exitEntry.areaId in areaIds) { "Area '${entry.id}' references unknown exit target: ${exitEntry.areaId}" }
64:                 val to = AreaId(exitEntry.areaId)
65:                 val id = exploration.model.ExitId(from, to)
66:                 val state = when (exitEntry.initialState?.lowercase()) {
67:                     "blocked" -> exploration.model.ExitState.BLOCKED
68:                     else -> exploration.model.ExitState.OPEN  // default and any unknown value
69:                 }
70:                 result[id] = exploration.model.ExitStateData(state, exitEntry.hidden)
71:             }
72:         }
73:     }
74:     return result
75: }
76: 
77: private fun buildDevices(deviceEntries: List<DeviceEntry>): Map<DeviceId, Device> = deviceEntries.associate { entry ->
78:     DeviceId(entry.id) to Device(
79:         id = DeviceId(entry.id),
80:         lookDescription = entry.lookDescription,
81:         activateDescription = entry.activateDescription,
82:         healthEffect = entry.healthEffect,
83:         statusEffects = entry.statusEffects
84:     )
85: }
86: 
87: private fun buildAreas(areaEntries: List<AreaEntry>, deviceMap: Map<DeviceId, Device>, areaIds: Set<String>): Map<AreaId, Area> = areaEntries.associateTo(mutableMapOf()) { entry ->
88:     if (entry.deviceId != null && !deviceMap.keys.contains(DeviceId(entry.deviceId))) {
89:         throw IllegalArgumentException("Area '${entry.id}' references unknown device: ${entry.deviceId}")
90:     }
91: 
92:     val exits: Set<Exit> = if ((entry.exits ?: emptyList()).isNotEmpty()) {
93:         (entry.exits ?: emptyList()).mapNotNull { exitEntry ->
94:             require(exitEntry.areaId in areaIds) { "Area '${entry.id}' references unknown exit target area: ${exitEntry.areaId}" }
95:             val dir = Direction.parse(exitEntry.direction)
96:                 ?: error("Area '${entry.id}' has invalid direction '${exitEntry.direction}'. Must be one of North, West, South, East")
97:             Exit(AreaId(exitEntry.areaId), dir)
98:         }.toSet()
99:     } else {
100:         (entry.connections ?: emptyList()).mapNotNull { connName ->
101:             require(connName in areaIds) { "Area '${entry.id}' connects to unknown area: $connName" }
102:             AreaId(connName)
103:         }.sortedBy { it.name }.mapIndexed { idx, areaId -> Exit(areaId, Direction.fromIndex(idx) ?: Direction.North) }.toSet()
104:     }
105: 
106:     AreaId(entry.id) to Area(
107:         id = AreaId(entry.id),
108:         description = entry.description,
109:         exits = exits,
110:         device = entry.deviceId?.let { deviceMap[DeviceId(it)]!! }
111:     )
112: }
113: 
114: private fun buildItems(itemEntries: List<ItemEntry>): List<Item> = itemEntries.map { entry ->
115:     Item(
116:         id = ItemId(entry.id),
117:         description = entry.description,
118:         location = entry.toLocation(),
119:         locked = false
120:     )
121: }
122: 
123: private fun buildTriggers(triggerEntries: List<TriggerEntry>): List<Trigger> = triggerEntries.map { entry ->
124:     Trigger(
125:         id = entry.id,
126:         ownerType = OwnerType.valueOf(entry.ownerType),
127:         ownerId = entry.ownerId,
128:         conditions = entry.conditions.map { cond ->
129:             ActivationCondition(
130:                 checkType = when (cond.checkType) {
131:                     "itemCarried" -> CheckType.ITEM_CARRIED
132:                     "itemEquipped" -> CheckType.ITEM_EQUIPPED
133:                     null -> CheckType.STATUS
134:                     else -> CheckType.STATUS
135:                 },
136:                 statusName = cond.statusName,
137:                 op = when (cond.op) {
138:                     ">" -> ComparisonOp.GT
139:                     "<" -> ComparisonOp.LT
140:                     "<=" -> ComparisonOp.LTE
141:                     ">=" -> ComparisonOp.GTE
142:                     "==" -> ComparisonOp.EQ
143:                     null -> ComparisonOp.GT
144:                     else -> error("Unknown operator: ${cond.op}")
145:                 },
146:                 threshold = cond.threshold ?: 0,
147:                 itemId = cond.itemId?.let { ItemId(it) }
148:             )
149:         },
150:         effects = entry.effects.map { ef ->
151:             when (ef.type) {
152:                 "displayText" -> Effect.DisplayText(checkNotNull(ef.text))
153:                 "changeHealth" -> Effect.ChangeHealth(checkNotNull(ef.amount))
154:                 "adjustStatus" -> Effect.AdjustStatus(checkNotNull(ef.statusName), checkNotNull(ef.amount))
155:                 "setStatus" -> Effect.SetStatus(checkNotNull(ef.statusName), checkNotNull(ef.value))
156:                 "setLocation" -> Effect.SetLocation(
157:                     ItemId(checkNotNull(ef.itemId)),
158:                     when (ef.locationType?.uppercase()) {
159:                         "AREA" -> Effect.TargetRef.InArea(AreaId(checkNotNull(ef.locationId)))
160:                         "DEVICE" -> Effect.TargetRef.OnDevice(DeviceId(checkNotNull(ef.locationId)))
161:                         "CARRIED" -> Effect.TargetRef.Carried
162:                         "EQUIPPED" -> Effect.TargetRef.Equipped
163:                         else -> error("Unknown location type: ${ef.locationType}")
164:                     }
165:                 )
166:                 "lockItem" -> Effect.LockItem(ItemId(checkNotNull(ef.itemId)))
167:                 "unlockItem" -> Effect.UnlockItem(ItemId(checkNotNull(ef.itemId)))
168:                 "storyMessage" -> Effect.StoryMessage(checkNotNull(ef.text))
169:                 "setExitBlocked" -> Effect.SetExitBlocked(AreaId(checkNotNull(ef.fromId)), AreaId(checkNotNull(ef.toId)), checkNotNull(ef.blocked ?: false))
170:                 "hideExit" -> Effect.HideExit(AreaId(checkNotNull(ef.fromId)), AreaId(checkNotNull(ef.toId)))
171:                 "showExit" -> Effect.ShowExit(AreaId(checkNotNull(ef.fromId)), AreaId(checkNotNull(ef.toId)))
172:                 "endGame" -> Effect.EndGame(checkNotNull(ef.text))
173:                 else -> error("Unknown effect type: ${ef.type}")
174:             }
175:         },
176:         singleUse = entry.singleUse,
177:         intervalTurns = entry.intervalTurns
178:     )
179: }
180: 
181: private fun validateTriggers(triggers: List<Trigger>, areaIds: Set<String>, deviceIds: Set<DeviceId>, itemIds: Set<ItemId>) {
182:     for (t in triggers) {
183:         when (t.ownerType) {
184:             OwnerType.AREA -> require(t.ownerId in areaIds) { "Trigger '${t.id}' references unknown area: ${t.ownerId}" }
185:             OwnerType.DEVICE -> require(DeviceId(t.ownerId) in deviceIds) { "Trigger '${t.id}' references unknown device: ${t.ownerId}" }
186:             OwnerType.STATUS -> {}
187:         }
188: 
189:         for (c in t.conditions) {
190:             c.itemId?.let { require(it in itemIds) { "Trigger '${t.id}' references unknown item: ${it.name}" } }
191:         }
192:         for (e in t.effects) {
193:             when (e) {
194:                 is Effect.SetLocation -> require(e.itemId in itemIds) { "Trigger '${t.id}' references unknown item: ${e.itemId.name}" }
195:                 is Effect.LockItem -> require(e.itemId in itemIds) { "Trigger '${t.id}' references unknown item: ${e.itemId.name}" }
196:                 is Effect.UnlockItem -> require(e.itemId in itemIds) { "Trigger '${t.id}' references unknown item: ${e.itemId.name}" }
197:                 is Effect.SetExitBlocked -> {
198:                     require(e.from.name in areaIds) { "Trigger '${t.id}' exit from area '${e.from.name}' not found" }
199:                     require(e.to.name in areaIds) { "Trigger '${t.id}' exit to area '${e.to.name}' not found" }
200:                 }
201:                 is Effect.HideExit -> {
202:                     require(e.from.name in areaIds) { "Trigger '${t.id}' exit from area '${e.from.name}' not found" }
203:                     require(e.to.name in areaIds) { "Trigger '${t.id}' exit to area '${e.to.name}' not found" }
204:                 }
205:                 is Effect.ShowExit -> {
206:                     require(e.from.name in areaIds) { "Trigger '${t.id}' exit from area '${e.from.name}' not found" }
207:                     require(e.to.name in areaIds) { "Trigger '${t.id}' exit to area '${e.to.name}' not found" }
208:                 }
209:                 else -> {}
210:             }
211:         }
212:     }
213: }

(End of file - total 213 lines)
</content>
```

**Tool: grep**

**Input:**
```json
{
  "pattern": "CommandProcessor|processCommand|isOver|win\\b|gameOver|endGameMessage",
  "path": "/var/home/mikharju/dev/exploration/engine/src/main/kotlin/exploration/core/command"
}
```

**Output:**
```
Found 2 matches
/var/home/mikharju/dev/exploration/engine/src/main/kotlin/exploration/core/command/CommandProcessor.kt:
  Line 9: fun processCommand(state: GameState, command: Command): GameState {

  Line 10:     if (state.endGameMessage != null && command !is Command.Look) {

```

**Tool: grep**

**Input:**
```json
{
  "pattern": "endGameMessage",
  "path": "/var/home/mikharju/dev/exploration/engine/src"
}
```

**Output:**
```
Found 34 matches
/var/home/mikharju/dev/exploration/engine/src/main/kotlin/exploration/core/engine/TriggerEngine.kt:
  Line 139:     val finalEndGame = if (state.endGameMessage == null && finalAcc.effectsAcc.endGameText != null) {

  Line 141:     } else state.endGameMessage

  Line 150:         endGameMessage = finalEndGame


/var/home/mikharju/dev/exploration/engine/src/main/kotlin/exploration/adapter/ui/lanterna/LanternaUiAdapter.kt:
  Line 90:         while (currentView.endGameMessage == null) {

  Line 144:         if (currentView.endGameMessage != null) {

  Line 146:             addMessage(history, currentView.endGameMessage, false)

  Line 781:         if (view.endGameMessage != null) {

  Line 782:             println("===== ${view.endGameMessage} =====")


/var/home/mikharju/dev/exploration/engine/src/test/kotlin/exploration/integration/GameIntegrationTest.kt:
  Line 60:         assertNotNull(state.endGameMessage)

  Line 74:         assertNull(state.endGameMessage)

  Line 84:         assertNull(state.endGameMessage)

  Line 88:         assertNull(state.endGameMessage)


/var/home/mikharju/dev/exploration/engine/src/test/kotlin/exploration/command/CommandProcessorTest.kt:
  Line 126:         assertNull(result.endGameMessage)

  Line 136:         assertNull(result.endGameMessage)

  Line 141:         val gameOver = initialState.copy(endGameMessage = "You collapse from exhaustion... Game Over.")

  Line 149:         val gameOver = initialState.copy(endGameMessage = "You collapse from exhaustion... Game Over.")

  Line 169:         assertNotNull(result.endGameMessage)

  Line 170:         assertTrue(result.endGameMessage.contains("hidden purpose"))

  Line 187:         assertTrue(afterEnd.endGameMessage != null)

  Line 324:         val gameOver = initialState.copy(endGameMessage = "The end.")


/var/home/mikharju/dev/exploration/engine/src/main/kotlin/exploration/core/command/CommandProcessor.kt:
  Line 10:     if (state.endGameMessage != null && command !is Command.Look) {


/var/home/mikharju/dev/exploration/engine/src/test/kotlin/exploration/scenario/TriggerTest.kt:
  Line 290:         assertEquals("You found the truth.", result.endGameMessage)

  Line 296:             endGameMessage = "Already ended.",

  Line 300:         assertEquals("Already ended.", result.endGameMessage)


/var/home/mikharju/dev/exploration/engine/src/main/kotlin/exploration/adapter/ui/key/KeyUiAdapter.kt:
  Line 26:             while (lastView.endGameMessage == null) {

  Line 154:         if (v.endGameMessage != null) println(v.endGameMessage)


/var/home/mikharju/dev/exploration/engine/src/main/kotlin/exploration/adapter/ui/text/TextUiAdapter.kt:
  Line 25:         while (lastView.endGameMessage == null) {

  Line 96:         if (v.endGameMessage != null) println(v.endGameMessage)


/var/home/mikharju/dev/exploration/engine/src/main/kotlin/exploration/core/engine/GameEngineImpl.kt:
  Line 20:         if (state.endGameMessage != null) return makeEndView(state)

  Line 59:             endGameMessage = state.endGameMessage,

  Line 74:             outputLine = state.endGameMessage ?: "The game is over.",

  Line 85:             endGameMessage = state.endGameMessage,


/var/home/mikharju/dev/exploration/engine/src/main/kotlin/exploration/port/ViewData.kt:
  Line 31:     val endGameMessage: String? = null,


/var/home/mikharju/dev/exploration/engine/src/main/kotlin/exploration/core/state/GameState.kt:
  Line 23:     val endGameMessage: String? = null,

```

---

## Assistant (Plan · qwen3.6-35b-a3b · 18.1s)

_Thinking:_

Now let me look at the other UI adapters and the GameState/GameEngineImpl to understand the full picture. Let me also check for tests and the text/key UI adapters.








**Tool: read**

**Input:**
```json
{
  "filePath": "/var/home/mikharju/dev/exploration/engine/src/main/kotlin/exploration/core/state/GameState.kt"
}
```

**Output:**
```
<path>/var/home/mikharju/dev/exploration/engine/src/main/kotlin/exploration/core/state/GameState.kt</path>
<type>file</type>
<content>
1: package exploration.state
2: 
3: import exploration.model.AreaId
4: import exploration.model.Direction
5: import exploration.model.ExitId
6: import exploration.model.ExitStateData
7: import exploration.model.DeviceId
8: import exploration.model.Item
9: 
10: import exploration.model.Player
11: import exploration.model.World
12: 
13: import exploration.model.StatusRange
14: import exploration.model.Trigger
15: 
16: data class GameState(
17:     val world: World,
18:     val player: Player,
19:     val exploredAreas: Set<AreaId> = emptySet(),
20:     val activatedDevices: Set<DeviceId> = emptySet(),
21:     val commandOutput: String = "",
22:     val triggerTexts: List<String> = emptyList(),
23:     val endGameMessage: String? = null,
24:     val statusBounds: Map<String, StatusRange> = emptyMap(),
25:     val turn: Int = 0,
26:     val triggers: List<Trigger> = emptyList(),
27:     val items: List<Item> = emptyList(),
28:     val storyMessages: List<String> = emptyList(),
29:     val exitStates: Map<ExitId, ExitStateData> = emptyMap()
30: ) {
31:     fun allDeviceIds(): Set<DeviceId> {
32:         val devices = mutableSetOf<DeviceId>()
33:         for (area in world.areas.values) {
34:             area.device?.let { devices.add(it.id) }
35:         }
36:         return devices
37:     }
38: 
39:     fun visibleExits(currentArea: AreaId): List<Direction> {
40:         val exitsByDir = world.getArea(currentArea).exits.associateBy { it.direction.index }
41:         return Direction.values().filter { dir ->
42:             val exit = exitsByDir[dir.index] ?: return@filter false
43:             val id = ExitId(currentArea, exit.targetArea)
44:             val data = exitStates[id] ?: ExitStateData() // default: not hidden
45:             !data.hidden
46:         }
47:     }
48: 
49:     fun isExitBlocked(from: AreaId, to: AreaId): Boolean {
50:         val id = ExitId(from, to)
51:         val data = exitStates[id] ?: return false
52:         return data.state != exploration.model.ExitState.OPEN || data.hidden
53:     }
54: }

(End of file - total 54 lines)
</content>
```

**Tool: read**

**Input:**
```json
{
  "filePath": "/var/home/mikharju/dev/exploration/engine/src/main/kotlin/exploration/port/ViewData.kt"
}
```

**Output:**
```
<path>/var/home/mikharju/dev/exploration/engine/src/main/kotlin/exploration/port/ViewData.kt</path>
<type>file</type>
<content>
1: package exploration.port
2: 
3: import exploration.model.StatusRange
4: 
5: data class ItemView(
6:     val name: String,
7:     val description: String,
8:     val locked: Boolean = false
9: )
10: 
11: data class ExitInfo(
12:     val name: String?,
13:     val blocked: Boolean = false
14: )
15: 
16: data class ViewData(
17:     val outputLine: String,
18:     val commandText: String,
19:     val triggerTexts: List<String>,
20:     val storyMessages: List<String> = emptyList(),
21:     val health: Int,
22:     val maxHealth: Int,
23:     val currentAreaName: String,
24:     val exploredCount: Int,
25:     val totalAreas: Int,
26:     val activatedCount: Int,
27:     val totalDevices: Int,
28:     val exits: List<ExitInfo?>,
29:     val statuses: Map<String, Int> = emptyMap(),
30:     val statusBounds: Map<String, StatusRange> = emptyMap(),
31:     val endGameMessage: String? = null,
32:     val areaItems: List<ItemView> = emptyList(),
33:     val carriedItems: List<ItemView> = emptyList(),
34:     val equippedItems: List<ItemView> = emptyList()
35: )

(End of file - total 35 lines)
</content>
```

**Tool: read**

**Input:**
```json
{
  "filePath": "/var/home/mikharju/dev/exploration/engine/src/main/kotlin/exploration/core/command/CommandProcessor.kt"
}
```

**Output:**
```
<path>/var/home/mikharju/dev/exploration/engine/src/main/kotlin/exploration/core/command/CommandProcessor.kt</path>
<type>file</type>
<content>
1: package exploration.command
2: 
3: import exploration.core.engine.fireAreaTriggers
4: import exploration.core.engine.fireDeviceTriggers
5: import exploration.core.engine.fireStatusTriggers
6: import exploration.model.*
7: import exploration.state.GameState
8: 
9: fun processCommand(state: GameState, command: Command): GameState {
10:     if (state.endGameMessage != null && command !is Command.Look) {
11:         return state.copy(commandOutput = "The game is over. Restart to play again.")
12:     }
13: 
14:     val cmdResult = when (command) {
15:         is Command.Look -> CmdResult(processLook(state), null, null, true)
16:         is Command.Move -> runMove(state, command.areaName)
17:         is Command.Activate -> runActivate(state)
18:         is Command.TakeItem -> runTake(state, command.itemName)
19:         is Command.DropItem -> runDrop(state, command.itemName)
20:         is Command.EquipItem -> runEquip(state, command.itemName)
21:         is Command.UnequipItem -> runUnequip(state, command.itemName)
22:         is Command.Inventory -> CmdResult(processInventory(state), null, null, true)
23:     }
24: 
25:     val withTriggers = if (!cmdResult.isLook) {
26:         var s = cmdResult.state
27:         if (cmdResult.movedToArea != null) s = fireAreaTriggers(s, cmdResult.movedToArea)
28:         if (cmdResult.activatedDeviceId != null) s = fireDeviceTriggers(s, cmdResult.activatedDeviceId)
29:         s = s.copy(turn = s.turn + 1)
30:         fireStatusTriggers(s)
31:     } else {
32:         cmdResult.state
33:     }
34: 
35:     return withTriggers
36: }
37: 
38: private data class CmdResult(
39:     val state: GameState,
40:     val movedToArea: AreaId?,
41:     val activatedDeviceId: DeviceId?,
42:     val isLook: Boolean
43: )
44: 
45: private fun runMove(state: GameState, targetName: String): CmdResult {
46:     val currentArea = state.world.getArea(state.player.currentArea)
47:     val targetId = findAreaId(state, targetName) ?: return CmdResult(
48:         state.copy(commandOutput = "There's no place called '$targetName' here."), null, null, false)
49: 
50:     if (targetId !in currentArea.connections) {
51:         val valid = currentArea.connections.joinToString(", ")
52:         return CmdResult(state.copy(commandOutput = "You can't go there directly. Connected areas: $valid"), null, null, false)
53:     }
54: 
55:     if (state.isExitBlocked(state.player.currentArea, targetId)) {
56:         return CmdResult(
57:             state.copy(commandOutput = "That way is blocked."),
58:             null, null, false
59:         )
60:     }
61: 
62:     return CmdResult(
63:         state.copy(
64:             player = state.player.copy(currentArea = targetId),
65:             commandOutput = "You move to the ${targetId.name}."
66:         ),
67:         targetId,
68:         null,
69:         false
70:     )
71: }
72: 
73: private fun runActivate(state: GameState): CmdResult {
74:     val area = state.world.getArea(state.player.currentArea)
75:     val device = area.device ?: return CmdResult(
76:         state.copy(commandOutput = "There's nothing here to activate."), null, null, false)
77: 
78:     if (device.id in state.activatedDevices) {
79:         return CmdResult(state.copy(commandOutput = "${device.id} has already been activated."), null, null, false)
80:     }
81: 
82:     var player = state.player
83:     val msgs = mutableListOf<String>()
84: 
85:     player = player.adjustHealth(device.healthEffect)
86:     when (device.healthEffect) {
87:         0 -> {}
88:         in 1..Int.MAX_VALUE -> msgs.add("+${device.healthEffect} health")
89:         else -> msgs.add("${device.healthEffect} health")
90:     }
91: 
92:     for ((name, amount) in device.statusEffects) {
93:         val oldVal = player.statuses.getOrElse(name) { 0 }
94:         player = player.adjustStatus(name, amount, state.statusBounds[name])
95:         val newVal = player.statuses.getValue(name)
96:         if (amount != 0 || newVal != oldVal) {
97:             msgs.add("$name: $newVal")
98:         }
99:     }
100: 
101:     val suffix = if (msgs.isNotEmpty()) " (${msgs.joinToString(", ")})" else ""
102: 
103:     return CmdResult(
104:         state.copy(
105:             player = player,
106:             activatedDevices = state.activatedDevices + device.id,
107:             commandOutput = "${device.activateDescription}$suffix"
108:         ),
109:         null,
110:         device.id,
111:         false
112:     )
113: }
114: 
115: private fun processLook(state: GameState): GameState {
116:     val areaId = state.player.currentArea
117:     val area = state.world.getArea(areaId)
118: 
119:     val deviceText = area.device?.let { d ->
120:         if (d.id in state.activatedDevices) {
121:             "\n${d.id} has been activated."
122:         } else {
123:             "\n${d.lookDescription}"
124:         }
125:     } ?: ""
126: 
127:     val roomItems = state.items.filter { it.location.type == ItemLocationType.AREA && (it.location.target as? LocationTarget.InArea)?.areaId == areaId }
128:     val itemText = if (roomItems.isNotEmpty()) {
129:         "\nYou see: ${roomItems.joinToString(", ") { it.id.name }}"
130:     } else ""
131: 
132:     val equipped = state.items.filter { it.location.type == ItemLocationType.EQUIPPED }
133:     val equippedText = if (equipped.isNotEmpty()) {
134:         "\nEquipped: ${equipped.joinToString(", ") { it.id.name }}"
135:     } else ""
136: 
137:     return state.copy(
138:         exploredAreas = state.exploredAreas + areaId,
139:         commandOutput = "${area.description}$deviceText$itemText$equippedText"
140:     )
141: }
142: 
143: private fun runTake(state: GameState, itemName: String): CmdResult {
144:     val item = findItem(state, itemName)
145:         ?: return CmdResult(state.copy(commandOutput = "There's no item called '$itemName' here."), null, null, false)
146: 
147:     if (item.location.type != ItemLocationType.AREA || (item.location.target as? LocationTarget.InArea)?.areaId != state.player.currentArea) {
148:         return CmdResult(state.copy(commandOutput = "That item isn't here."), null, null, false)
149:     }
150: 
151:     val newItems = state.items.map { i ->
152:         if (i.id == item.id) i.copy(location = Location(ItemLocationType.CARRIED)) else i
153:     }
154: 
155:     return CmdResult(
156:         state.copy(
157:             commandOutput = "You pick up the ${item.id.name}.",
158:             items = newItems
159:         ),
160:         null, null, false
161:     )
162: }
163: 
164: private fun runDrop(state: GameState, itemName: String): CmdResult {
165:     val item = findItem(state, itemName)
166:         ?: return CmdResult(state.copy(commandOutput = "You don't have '$itemName'."), null, null, false)
167: 
168:     if (item.location.type == ItemLocationType.EQUIPPED) {
169:         if (item.locked) {
170:             return CmdResult(state.copy(commandOutput = "You can't drop the ${item.id.name} — it's locked."), null, null, false)
171:         }
172:         val newItems = state.items.map { i ->
173:             if (i.id == item.id) i.copy(location = Location.area(state.player.currentArea)) else i
174:         }
175: 
176:         return CmdResult(
177:             state.copy(
178:                 commandOutput = "You drop the ${item.id.name} (auto-unequipped).",
179:                 items = newItems
180:             ),
181:             null, null, false
182:         )
183:     }
184: 
185:     if (item.location.type != ItemLocationType.CARRIED) {
186:         return CmdResult(state.copy(commandOutput = "That item isn't in your inventory."), null, null, false)
187:     }
188: 
189:     if (item.locked) {
190:         return CmdResult(state.copy(commandOutput = "You can't drop the ${item.id.name} — it's locked."), null, null, false)
191:     }
192: 
193:     val newItems = state.items.map { i ->
194:         if (i.id == item.id) i.copy(location = Location.area(state.player.currentArea)) else i
195:     }
196: 
197:     return CmdResult(
198:         state.copy(
199:             commandOutput = "You drop the ${item.id.name}.",
200:             items = newItems
201:         ),
202:         null, null, false
203:     )
204: }
205: 
206: private fun runEquip(state: GameState, itemName: String): CmdResult {
207:     val item = findItem(state, itemName)
208:         ?: return CmdResult(state.copy(commandOutput = "You don't have '$itemName'."), null, null, false)
209: 
210:     if (item.location.type != ItemLocationType.CARRIED) {
211:         return CmdResult(state.copy(commandOutput = "That item isn't in your inventory."), null, null, false)
212:     }
213: 
214:     if (item.locked) {
215:         return CmdResult(state.copy(commandOutput = "You can't equip the ${item.id.name} — it's locked."), null, null, false)
216:     }
217: 
218:     val newItems = state.items.map { i ->
219:         if (i.id == item.id) i.copy(location = Location(ItemLocationType.EQUIPPED)) else i
220:     }
221: 
222:     return CmdResult(
223:         state.copy(
224:             commandOutput = "You equip the ${item.id.name}.",
225:             items = newItems
226:         ),
227:         null, null, false
228:     )
229: }
230: 
231: private fun runUnequip(state: GameState, itemName: String): CmdResult {
232:     val item = findItem(state, itemName)
233:         ?: return CmdResult(state.copy(commandOutput = "You don't have '$itemName'."), null, null, false)
234: 
235:     if (item.location.type != ItemLocationType.EQUIPPED) {
236:         return CmdResult(state.copy(commandOutput = "That item isn't equipped."), null, null, false)
237:     }
238: 
239:     if (item.locked) {
240:         return CmdResult(state.copy(commandOutput = "You can't unequip the ${item.id.name} — it's locked."), null, null, false)
241:     }
242: 
243:     val newItems = state.items.map { i ->
244:         if (i.id == item.id) i.copy(location = Location(ItemLocationType.CARRIED)) else i
245:     }
246: 
247:     return CmdResult(
248:         state.copy(
249:             commandOutput = "You unequip the ${item.id.name}.",
250:             items = newItems
251:         ),
252:         null, null, false
253:     )
254: }
255: 
256: private fun processInventory(state: GameState): GameState {
257:     val carried = state.items.filter { it.location.type == ItemLocationType.CARRIED }
258:     val equipped = state.items.filter { it.location.type == ItemLocationType.EQUIPPED }
259: 
260:     val lines = mutableListOf<String>()
261:     lines.add("Inventory:")
262: 
263:     if (equipped.isNotEmpty()) {
264:         lines.add("  Equipped:")
265:         for (item in equipped) {
266:             lines.add("    [${item.id.name}] ${item.description}")
267:         }
268:     }
269: 
270:     if (carried.isNotEmpty()) {
271:         lines.add("  Carried:")
272:         for (item in carried) {
273:             lines.add("    [${item.id.name}] ${item.description}")
274:         }
275:     }
276: 
277:     if (equipped.isEmpty() && carried.isEmpty()) {
278:         lines.add("  (empty)")
279:     }
280: 
281:     return state.copy(commandOutput = lines.joinToString("\n"))
282: }
283: 
284: private fun findItem(state: GameState, name: String): Item? {
285:     val lower = name.lowercase()
286:     return state.items.find { it.id.name.lowercase() == lower }
287: }
288: 
289: private fun findAreaId(state: GameState, name: String): AreaId? {
290:     val lower = name.lowercase()
291:     return state.world.areas.keys.find { it.name.lowercase() == lower }
292: }

(End of file - total 292 lines)
</content>
```

---

## Assistant (Plan · qwen3.6-35b-a3b · 10.5s)

_Thinking:_

Now let me run the tests to verify everything works.






**Tool: bash**

**Input:**
```json
{
  "command": "./gradlew build 2>&1 | tail -20",
  "description": "Build and test the project"
}
```

**Output:**
```
> Task :compileKotlin UP-TO-DATE
> Task :compileJava NO-SOURCE
> Task :processResources NO-SOURCE
> Task :classes UP-TO-DATE
> Task :jar UP-TO-DATE
> Task :startScripts UP-TO-DATE
> Task :distTar UP-TO-DATE
> Task :distZip UP-TO-DATE
> Task :assemble UP-TO-DATE
> Task :compileTestKotlin UP-TO-DATE
> Task :compileTestJava NO-SOURCE
> Task :processTestResources UP-TO-DATE
> Task :testClasses UP-TO-DATE
> Task :test UP-TO-DATE
> Task :check UP-TO-DATE
> Task :build UP-TO-DATE

BUILD SUCCESSFUL in 3s
8 actionable tasks: 8 up-to-date
Consider enabling configuration cache to speed up this build: https://docs.gradle.org/9.3.0/userguide/configuration_cache_enabling.html

```

---

## Assistant (Plan · qwen3.6-35b-a3b · 22.0s)

_Thinking:_

Now I have a good understanding of the codebase and the latest changes. Let me provide a comprehensive code review focusing on the latest commit (ce48c7b - "More status trigger comparison operators and plan doc") while also noting observations about the broader codebase.

Key areas to review:

1. **Trigger.kt** — ComparisonOp enum expansion, EndGame effect
2. **TriggerEngine.kt** — New comparison operators, endGame handling
3. **ScenarioLoader.kt** — JSON parsing for new ops and endGame
4. **SCENARIO_FORMAT.md** — Updated docs
5. **LanternaUiAdapter.kt** — Initial story message overlay display

Let me compile my review notes:


The new comparison operators (`LTE`, `GTE`, `EQ`) are straightforward additions that align perfectly with the existing pattern, and the endGame effect is cleanly implemented as a data class with first-wins semantics. However, I notice the `Effect` sealed class has inconsistent nesting—most effects live at the top level while `TargetRef` is nested inside it. Additionally, the `EffectsAccumulator` data class in `TriggerEngine.kt` feels overly complex and could be simplified.

For the UI flow, there's a potential crash if `initialStoryCount` is zero since we're trying to access `.lastIndex` on an empty list—but the guard condition prevents this. The `storedStoryCount` tracking works correctly for subsequent messages too. I should also check that the `Lantern`...

Updating UI adapters... Looking at the end-game flow, all commands except Look are properly blocked once a game ends, which is the right behavior. The status trigger operator expansion aligns with what's documented in SCENARIO_FORMAT.md, though I'm noticing the documentation mentions more review items than have been completed so far—the LanternaUiAdapter and CommandProcessor reviews still need attention.




# Code Review — `ce48c7b` (More status trigger comparison operators and plan doc)

## Overall

Good, focused commit that adds 3 new comparison operators (`<=`, `>=`, `==`) and the `endGame` effect for custom endings. Changes are clean, consistent with existing patterns, and tests pass.

---

### TriggerEngine.kt:51 — health read path inconsistency

```kotlin
val statusValue = if (cond.statusName == "health") acc.effectsAcc.player.health
else cond.statusName?.let { acc.effectsAcc.player.statuses.getOrElse(it) { 0 } } ?: 0
```

This treats `health` as a special case rather than using the same lookup path. This works now but is fragile — if someone uses `"health"` in an `AdjustStatus` effect earlier in the trigger chain, the accumulator's updated health would be reflected for status checks only when the name literally equals `"health"`. Consider unifying to:

```kotlin
val statusValue = acc.effectsAcc.player.statuses.getOrElse(cond.statusName ?: "health") { 0 }
```

(With `player.health` accessible via a `statuses["health"]` accessor, if that's intentional.)

### TriggerEngine.kt:25-40 — nested data classes in function body

The `EffectsAccumulator` and `MutationAccumulator` are defined inside `fireMatchingTriggers`. This is fine for encapsulation but makes the fold chain hard to follow. The 8 update maps (location, locked, exitState, exitHidden) could be consolidated into a single `Map<Type, Update>` or an ADT — currently it's 4 data classes + ~60 lines of folds.

### LanternaUiAdapter.kt:82-88 — initial story message overlay

The logic correctly guards against empty lists (no crash risk). However, the `subList(0, initialStoryCount)` is equivalent to just using the list itself since `initialStoryCount == currentView.storyMessages.size`. Could simplify to:

```kotlin
overlay = Overlay.MessageViewer(currentView.storyMessages, focusedIndex = currentView.storyMessages.lastIndex)
```

### Trigger.kt:23-50 — Effect sealed class nesting inconsistency

`TargetRef` is nested inside `Effect`, but all other effects (DisplayText, ChangeHealth, etc.) are top-level. This creates an asymmetry:

```kotlin
sealed class Effect {
    data class DisplayText(val text: String) : Effect()   // top-level
    sealed interface TargetRef { ... }                     // nested
    data class SetLocation(itemId, target: TargetRef) : Effect()  // references nested type
}
```

Moving `TargetRef` out would be more idiomatic. Not a bug — just an observation.

### ScenarioLoader.kt:137-145 — operator parsing is exhaustive ✅

Good that the `else -> error(...)` catch-all ensures no unknown operators slip through silently. The `null -> ComparisonOp.GT` default matches the documented behavior.

---

## Minor / nitpicks

| File | Line | Note |
|------|------|------|
| Trigger.kt:50 | `EndGame(text)` | Consider adding a `type: String = "endGame"` companion constant for consistency with JSON parsing elsewhere |
| SCENARIO_FORMAT.md:168 | Effect table entry for `endGame` | Documented correctly, but the "Only fires if no endGameMessage is already set" note could link to first-wins semantics in the plan doc |
| LanternaUiAdapter.kt:70 | `storedStoryCount` field | Could be a local var inside `run()` rather than a class-level mutable state — currently fine but worth noting |

---

## Reviews from ROADMAP.md still pending

The roadmap lists these as needing review — none were addressed in this commit:
- **LanternUiAdapter** — large file (800 lines), could benefit from extracting rendering helpers into separate classes
- **CommandProcessor** — `findItem`/`findAreaId` use case-insensitive matching which is inconsistent with the ID model (IDs are case-sensitive per docs)

---

## Verdict: ✅ Approve

The changes are correct, well-scoped, and follow existing conventions. No regressions. The two most actionable items (health lookup path consistency, nested data class complexity in TriggerEngine) are low-priority improvements that can be addressed in a future pass.

---

## User

Do code review on @src/main/kotlin/exploration/adapter/ui/lanterna/LanternaUiAdapter.kt 

---

## Assistant (Plan · qwen3.6-35b-a3b · 46.9s)

_Thinking:_

The user wants a focused code review on just `LanternaUiAdapter.kt`. Let me load the lanterna skill first since this is Lanterna-specific UI work, then do a thorough review of the file.

I already read the full file. Let me also check if there are related tests and any other context I need.






**Tool: skill**

**Input:**
```json
{
  "name": "lanterna"
}
```

**Output:**
```
<skill_content name="lanterna">
# Skill: lanterna

# Lanterna Text Graphics Library — Kotlin Coding Reference

Lanterna (`com.googlecode.lanterna`) is a Java library usable from Kotlin with no
wrappers needed. It exposes **three independent layers** of increasing abstraction.
All code below is idiomatic Kotlin: `val`/`var`, `use {}`, `when`, string templates,
no `new`, lambdas instead of SAM types, and `listOf()` in place of `List.of()`.

---

## 1. Dependency

```kotlin
// build.gradle.kts
implementation("com.googlecode.lanterna:lanterna:3.1.2")
```

```xml
<!-- pom.xml -->
<dependency>
  <groupId>com.googlecode.lanterna</groupId>
  <artifactId>lanterna</artifactId>
  <version>3.1.2</version>
</dependency>
```

---

## 2. Architecture — Three Layers

| Layer | Package prefix | Abstraction | Use when |
|-------|---------------|-------------|----------|
| **Terminal** | `com.googlecode.lanterna.terminal` | Low — direct TTY | Raw I/O, escape codes, special effects |
| **Screen** | `com.googlecode.lanterna.screen` | Mid — double-buffered | Custom drawing, games, full-screen apps |
| **GUI** | `com.googlecode.lanterna.gui2` | High — component tree | Standard widgets, dialogs, forms |

Choose the **highest layer that satisfies the requirements**. Layers are composable:
a GUI sits on a Screen which sits on a Terminal.

---

## 3. Core Types (used across all layers)

```kotlin
import com.googlecode.lanterna.TerminalSize        // (columns, rows)
import com.googlecode.lanterna.TerminalPosition    // (column, row) — 0-indexed
import com.googlecode.lanterna.TextCharacter       // single styled character
import com.googlecode.lanterna.TextColor           // color abstraction
import com.googlecode.lanterna.SGR                 // style flags (BOLD, ITALIC, …)
import com.googlecode.lanterna.graphics.TextGraphics // drawing canvas
import com.googlecode.lanterna.input.KeyStroke     // keyboard event
import com.googlecode.lanterna.input.KeyType       // key enum (ArrowUp, Enter, …)
```

### TerminalSize & TerminalPosition

```kotlin
val size = TerminalSize(80, 24)          // 80 cols, 24 rows
val cols = size.columns
val rows = size.rows

val pos     = TerminalPosition(10, 5)   // col 10, row 5
val shifted = pos.withRelativeColumn(3).withRelativeRow(-1)
```

### TextColor

```kotlin
// Named ANSI colors (always available)
val red:   TextColor = TextColor.ANSI.RED
val reset: TextColor = TextColor.ANSI.DEFAULT

// All 16 ANSI variants:
// BLACK, RED, GREEN, YELLOW, BLUE, MAGENTA, CYAN, WHITE
// + BRIGHT_ variants: BRIGHT_BLACK … BRIGHT_WHITE

// 256-color indexed palette
val indexed = TextColor.Indexed(196)    // bright red in 256-palette

// True-color RGB (terminal must support it)
val orange = TextColor.RGB(255, 128, 0)
```

### SGR (style attributes)

```kotlin
val styles = arrayOf(SGR.BOLD, SGR.UNDERLINE)
// Others: ITALIC, REVERSE, STRIKETHROUGH, BLINK, FAINT, BORDERED
```

### TextCharacter

```kotlin
val ch = TextCharacter(
    'A',
    TextColor.ANSI.WHITE,   // foreground
    TextColor.ANSI.BLUE,    // background
    SGR.BOLD
)
```

---

## 4. Terminal Layer

Entry point: `DefaultTerminalFactory`.

```kotlin
import com.googlecode.lanterna.terminal.DefaultTerminalFactory

val factory = DefaultTerminalFactory().apply {
    setInitialTerminalSize(TerminalSize(120, 40))  // optional; useful for headless
}
```

**Always close the terminal.** `Terminal` implements `Closeable`, so use `use {}`:

```kotlin
factory.createTerminal().use { terminal ->
    terminal.enterPrivateMode()         // saves cursor, clears screen
    terminal.clearScreen()
    terminal.setCursorPosition(5, 3)    // col 5, row 3

    terminal.setForegroundColor(TextColor.ANSI.GREEN)
    terminal.setBackgroundColor(TextColor.ANSI.BLACK)
    terminal.enableSGR(SGR.BOLD)
    terminal.putString("Hello, Lanterna!")

    terminal.resetColorAndSGR()
    terminal.flush()                    // must call — sends buffered bytes

    val key = terminal.readInput()      // blocks until keypress

    terminal.exitPrivateMode()
}
```

### Key Terminal methods

```kotlin
terminal.terminalSize                           // property → TerminalSize
terminal.addResizeListener { _, newSize -> }    // trailing lambda
terminal.bell()                                 // audible beep
terminal.setCursorVisible(false)
terminal.flush()                                // required after every draw
```

---

## 5. Screen Layer

`TerminalScreen` wraps a `Terminal` with **double-buffered** rendering: changes
accumulate in a back buffer; `refresh()` computes a diff and sends minimal escape
sequences.

```kotlin
import com.googlecode.lanterna.screen.Screen
import com.googlecode.lanterna.screen.TerminalScreen

val terminal = DefaultTerminalFactory().createTerminal()
val screen: Screen = TerminalScreen(terminal)

screen.startScreen()
screen.cursorPosition = null            // hide cursor

try {
    while (true) {
        screen.clear()

        val g = screen.newTextGraphics().apply {
            foregroundColor = TextColor.ANSI.YELLOW
            backgroundColor = TextColor.ANSI.BLACK
        }
        g.putString(2, 1, "Press Q to quit")

        val sz = screen.terminalSize
        g.drawRectangle(TerminalPosition(0, 0), sz, '*')

        screen.refresh()                // back buffer → visible

        val key = screen.pollInput()    // non-blocking; null if no input
        if (key?.character == 'q') break
        Thread.sleep(50)
    }
} finally {
    screen.stopScreen()
}
```

### Key Screen methods

```kotlin
screen.startScreen()
screen.stopScreen()
screen.clear()
screen.refresh()                            // diff-based update
screen.refresh(Screen.RefreshType.COMPLETE) // force full repaint
screen.terminalSize                         // property → TerminalSize
screen.readInput()                          // blocking key read → KeyStroke
screen.pollInput()                          // non-blocking → KeyStroke?
screen.newTextGraphics()                    // → TextGraphics (full screen)
screen.cursorPosition = null                // null = hide cursor
screen.doResizeIfNecessary()                // → TerminalSize? (null = no resize)
```

### TextGraphics drawing API

```kotlin
val g = screen.newTextGraphics()

// Colors and style (Kotlin properties, not Java setters)
g.foregroundColor = TextColor.ANSI.WHITE
g.backgroundColor = TextColor.ANSI.BLUE
g.enableModifiers(SGR.BOLD)
g.disableModifiers(SGR.BOLD)

// Text
g.putString(col, row, "text")
g.putString(TerminalPosition(col, row), "text")

// Primitives
g.drawLine(pos1, pos2, '─')
g.drawRectangle(topLeft, size, '+')
g.fillRectangle(topLeft, size, ' ')     // fill with spaces = erase region
g.drawTriangle(p1, p2, p3, '*')
g.fillTriangle(p1, p2, p3, '#')

// Single cell
g.setCharacter(col, row, textCharacter)
```

---

## 6. GUI Layer

The GUI layer provides a Swing-like component tree designed for terminals.

### Setup

```kotlin
import com.googlecode.lanterna.gui2.*

val screen = TerminalScreen(DefaultTerminalFactory().createTerminal())
screen.startScreen()

val gui = MultiWindowTextGUI(
    screen,
    DefaultWindowManager(),
    EmptySpace(TextColor.ANSI.BLACK)    // background fill character
)
```

### Windows

```kotlin
val window = BasicWindow("My App").apply {
    hints = listOf(Window.Hint.CENTERED)
    // Other hints: FULL_SCREEN, FIXED_SIZE, NO_DECORATIONS, NO_POST_RENDERING
    component = somePanel
}

gui.addWindowAndWait(window)            // blocks until window.close()

// Non-blocking alternative:
gui.addWindow(window)
// … later:
gui.waitForWindowToClose(window)
```

### Panels and Layouts

```kotlin
// Vertical / horizontal stacks
val vPanel = Panel(LinearLayout(Direction.VERTICAL))
val hPanel = Panel(LinearLayout(Direction.HORIZONTAL))

// Grid (2 columns)
val grid = Panel(GridLayout(2)).apply {
    (layoutManager as GridLayout).apply {
        horizontalSpacing = 1
        verticalSpacing   = 0
    }
}

// Border layout (TOP, CENTER, BOTTOM, LEFT, RIGHT)
val border = Panel(BorderLayout()).apply {
    addComponent(titleLabel, BorderLayout.Location.TOP)
    addComponent(content,    BorderLayout.Location.CENTER)
    addComponent(statusBar,  BorderLayout.Location.BOTTOM)
}
```

### Component layout hints

```kotlin
// GridLayout data — attach when adding to a grid panel
component.setLayoutData(
    GridLayout.createLayoutData(
        GridLayout.Alignment.FILL,    // horizontal: FILL, CENTER, BEGINNING, END
        GridLayout.Alignment.CENTER,  // vertical
        true,   // grab extra horizontal space
        false,  // grab extra vertical space
        2,      // colspan
        1       // rowspan
    )
)

// LinearLayout fill
component.setLayoutData(LinearLayout.createLayoutData(LinearLayout.Alignment.Fill))
```

### Standard Components

#### Label

```kotlin
val label = Label("Status: OK").apply {
    foregroundColor = TextColor.ANSI.GREEN
}
label.text = "Status: ERROR"            // update at runtime
```

#### Button

```kotlin
val btn = Button("Click Me") {          // Runnable as trailing lambda
    label.text = "Clicked!"
}
btn.isEnabled = false                   // grey out
```

#### TextBox (single & multi-line)

```kotlin
// Single line
val input = TextBox(TerminalSize(30, 1)).apply { text = "default" }
val value: String = input.text

// Multi-line
val multiline = TextBox(TerminalSize(40, 5), TextBox.Style.MULTI_LINE)

// Password mask
val password = TextBox(TerminalSize(20, 1)).apply { setMask('*') }
```

#### CheckBox

```kotlin
val check = CheckBox("Enable feature").apply { isChecked = true }
val on: Boolean = check.isChecked
check.addListener { nowChecked -> /* onChange */ }
```

#### RadioBoxList

```kotlin
val radio = RadioBoxList<String>(TerminalSize(20, 4)).apply {
    addItem("Option A")
    addItem("Option B")
    addItem("Option C")
}
val idx: Int     = radio.checkedItemIndex   // -1 if none
val sel: String? = radio.checkedItem
```

#### ComboBox

```kotlin
val combo = ComboBox("Item 1", "Item 2", "Item 3").apply {
    addItem("Item 4")
    selectedIndex = 0
}
val selected: String = combo.selectedItem
combo.addListener { prev, now, byUser -> /* onChange */ }
```

#### ActionListBox (menu-like list)

```kotlin
val list = ActionListBox(TerminalSize(30, 5)).apply {
    addItem("Open file") { openFile() }
    addItem("Save file") { saveFile() }
    addItem("Quit")      { window.close() }
}
```

#### ProgressBar

```kotlin
val bar = ProgressBar(0, 100, 30)       // min, max, preferredWidth
bar.value = 42
val current: Int = bar.value
```

#### Table

```kotlin
val table = Table<String>("Name", "Age", "City").apply {
    tableModel.addRow("Alice", "30", "Helsinki")
    tableModel.addRow("Bob",   "25", "Tampere")
    setSelectAction {
        val row  = selectedRow
        val name = tableModel.getRow(row)[0]
    }
}
```

#### AnimatedLabel

```kotlin
val spinner = AnimatedLabel.createClassicSpinningLine()

// Custom frames:
val anim = AnimatedLabel("Frame0").apply {
    addFrame("Frame1")
    addFrame("Frame2")
    startAnimation(200)                 // ms per frame
}
anim.stopAnimation()
```

#### Separator

```kotlin
panel.addComponent(Separator(Direction.HORIZONTAL))
```

### Dialogs (built-in)

```kotlin
import com.googlecode.lanterna.gui2.dialogs.*

// Message box
MessageDialog.showMessageDialog(gui, "Title", "Message text", MessageDialogButton.OK)

// Yes/No confirmation
val result = MessageDialog.showMessageDialog(
    gui, "Confirm", "Delete file?",
    MessageDialogButton.Yes, MessageDialogButton.No
)
val confirmed = result == MessageDialogButton.Yes

// Text input
val input: String? = TextInputDialog.showDialog(gui, "Input", "Enter name:", "default")

// File picker
val file = FileDialogBuilder()
    .setTitle("Open File")
    .setDescription("Choose a file")
    .setActionLabel("Open")
    .build()
    .showDialog(gui)
```

---

## 7. Input Handling (all layers)

```kotlin
val key = screen.readInput()            // or terminal.readInput()

when (key.keyType) {
    KeyType.Character -> {
        val c     = key.character       // Char — printable character
        val ctrl  = key.isCtrlDown
        val alt   = key.isAltDown
        val shift = key.isShiftDown
    }
    KeyType.Enter      -> { }
    KeyType.Escape     -> { }
    KeyType.Backspace  -> { }
    KeyType.Delete     -> { }
    KeyType.ArrowUp    -> { }
    KeyType.ArrowDown  -> { }
    KeyType.ArrowLeft  -> { }
    KeyType.ArrowRight -> { }
    KeyType.F1, KeyType.F2  -> { }     // F3–F12 follow same pattern
    KeyType.Tab        -> { }
    KeyType.ReverseTab -> { }           // Shift+Tab
    KeyType.PageUp     -> { }
    KeyType.PageDown   -> { }
    KeyType.Home       -> { }
    KeyType.End        -> { }
    KeyType.Insert     -> { }
    KeyType.EOF        -> { }           // terminal closed / Ctrl+D
    KeyType.MouseEvent -> {
        val mouse = key as MouseAction
        val type  = mouse.actionType    // CLICK_DOWN, CLICK_RELEASE, SCROLL_UP, …
        val pos   = mouse.position      // TerminalPosition
    }
    else -> { }
}
```

Enable mouse reporting on the Terminal (optional):

```kotlin
terminal.setMouseCaptureMode(MouseCaptureMode.CLICK)
// Modes: CLICK, CLICK_RELEASE, DRAG, MOVE
```

---

## 8. Resize Handling

```kotlin
// Screen layer — call at the top of every frame
val newSize = screen.doResizeIfNecessary()  // TerminalSize? — null = no resize
if (newSize != null) {
    screen.clear()
    redraw(screen, newSize)
}

// Terminal layer — listener
terminal.addResizeListener { _, size ->
    // called on resize; size: TerminalSize
}
```

---

## 9. Threading Rules

- **GUI layer** event loop runs on the thread that calls `addWindowAndWait`.
  Mutate GUI components **inside action callbacks** or dispatch explicitly:
  ```kotlin
  gui.guiThread.invokeLater { label.text = "updated" }
  // Blocking variant:
  gui.guiThread.invokeAndWait { label.text = "updated" }
  ```
- **Screen layer** has no built-in thread safety. Use `@Synchronized` or a
  `Mutex` if multiple coroutines/threads call `screen.refresh()` or `screen.clear()`.
- **Terminal layer** — `flush()` must be called from one thread at a time.

---

## 10. SwingTerminal / AWTTerminal (desktop / IDE mode)

When running outside a real TTY (e.g., IntelliJ run), Lanterna auto-creates a Swing
window. Force it explicitly:

```kotlin
import com.googlecode.lanterna.terminal.swing.SwingTerminalFrame

val frame: SwingTerminalFrame = DefaultTerminalFactory()
    .setTerminalEmulatorTitle("My App")
    .setInitialTerminalSize(TerminalSize(120, 40))
    .createSwingTerminal()

frame.isVisible = true
// frame implements Terminal — all Terminal APIs apply identically
```

`AWTTerminalFrame` is available for AWT. Swap via `DefaultTerminalFactory` — no
other code changes needed.

---

## 11. Common Patterns & Pitfalls

### Pattern: Full-screen draw loop (Screen layer)

```kotlin
val screen: Screen = TerminalScreen(DefaultTerminalFactory().createTerminal())
screen.startScreen()
screen.cursorPosition = null

try {
    var running = true
    while (running) {
        val size = screen.doResizeIfNecessary() ?: screen.terminalSize

        screen.clear()
        screen.newTextGraphics().apply {
            putString(2, 0, "Size: ${size.columns}x${size.rows}")
            // draw more content here…
        }
        screen.refresh()

        val key = screen.pollInput()
        if (key?.keyType == KeyType.Escape) running = false
        Thread.sleep(16)                // ~60 fps cap
    }
} finally {
    screen.stopScreen()
}
```

### Pattern: Minimal GUI app

```kotlin
val screen: Screen = TerminalScreen(DefaultTerminalFactory().createTerminal())
screen.startScreen()

val gui = MultiWindowTextGUI(screen)

val panel = Panel(LinearLayout(Direction.VERTICAL)).apply {
    addComponent(Label("Hello from GUI layer"))
    addComponent(Button("Quit") { gui.windows.forEach { it.close() } })
}

val win = BasicWindow("Demo").apply {
    hints = listOf(Window.Hint.CENTERED)
    component = panel
}

gui.addWindowAndWait(win)               // blocks until win.close() called
screen.stopScreen()
```

### Pitfall: forgetting `flush()` on Terminal layer

Every direct terminal write must end with `terminal.flush()`. The Screen and GUI
layers call `flush()` internally inside `screen.refresh()`.

### Pitfall: calling `screen.clear()` before `doResizeIfNecessary()`

Always check for resize **before** clearing. `doResizeIfNecessary()` reallocates
the back buffer to the new size; clearing a stale-sized buffer produces glitches.

### Pitfall: mixing layers incorrectly

Do **not** call `terminal.putString(…)` after a `TerminalScreen` has been started —
the Screen owns the terminal buffer. Use `screen.newTextGraphics()` instead.

### Pitfall: component preferred size in GUI layer

Layouts query `component.preferredSize`. Override it with:
```kotlin
component.preferredSize = TerminalSize(40, 1)
```

### Pitfall: `null` safety on `pollInput()`

`pollInput()` returns `KeyStroke?`. Use safe-call or Elvis to avoid NPE:
```kotlin
val key = screen.pollInput() ?: continue
// or
screen.pollInput()?.let { key -> handleKey(key) }
```

---

## 12. Key Imports Quick Reference

```kotlin
// Core types
import com.googlecode.lanterna.TerminalPosition
import com.googlecode.lanterna.TerminalSize
import com.googlecode.lanterna.TextCharacter
import com.googlecode.lanterna.TextColor
import com.googlecode.lanterna.SGR

// Terminal
import com.googlecode.lanterna.terminal.Terminal
import com.googlecode.lanterna.terminal.DefaultTerminalFactory
import com.googlecode.lanterna.terminal.swing.SwingTerminalFrame

// Screen
import com.googlecode.lanterna.screen.Screen
import com.googlecode.lanterna.screen.TerminalScreen
import com.googlecode.lanterna.graphics.TextGraphics

// Input
import com.googlecode.lanterna.input.KeyStroke
import com.googlecode.lanterna.input.KeyType
import com.googlecode.lanterna.input.MouseAction
import com.googlecode.lanterna.input.MouseActionType
import com.googlecode.lanterna.input.MouseCaptureMode

// GUI
import com.googlecode.lanterna.gui2.*
import com.googlecode.lanterna.gui2.dialogs.*
import com.googlecode.lanterna.gui2.table.Table

// Layouts
import com.googlecode.lanterna.gui2.LinearLayout
import com.googlecode.lanterna.gui2.GridLayout
import com.googlecode.lanterna.gui2.BorderLayout
import com.googlecode.lanterna.gui2.Direction
```

Base directory for this skill: file:///var/home/mikharju/dev/exploration/engine/.opencode/skills/lanterna
Relative paths in this skill (e.g., scripts/, reference/) are relative to this base directory.
Note: file list is sampled.

<skill_files>

</skill_files>
</skill_content>
```

---

## Assistant (Plan · qwen3.6-35b-a3b · 38.6s)

_Thinking:_

The user wants a focused code review on `LanternaUiAdapter.kt`. I've already read the full file (800 lines). Let me also check for any tests related to this adapter and look at how it integrates with the rest of the system.

Let me provide a thorough, focused review of just this file.




# Code Review — `LanternaUiAdapter.kt`

## Architecture & Organization (800 lines)

This is a single monolithic class with ~25 private methods spanning rendering, input handling, and game logic coordination. Per the codebase's "functional core, thin adapter" principle, most of this is correctly in adapters — but the file itself could benefit from decomposition.

### Major concerns

**1. `run()` method does too much (line 51-155)**
The main loop mixes: screen lifecycle, history management, story message tracking, trigger text tracking, input dispatching, and end-game cleanup. Lines 67-70 introduce four mutable variables (`shownTriggers`, `storedStoryCount`, `selectionState`, `overlay`) that are mutated throughout the function. This is a single-responsibility violation — consider extracting:
- The game loop into its own method (`gameLoop(screen, ref, ...)`).
- Input parsing into the existing `readInputKey` (already well-separated ✅).

**2. Mutable tracking state scattered across `run()` (lines 67-70)**

```kotlin
var shownTriggers = 0          // tracks triggerTexts progress
var storedStoryCount = 0       // tracks storyMessages progress  
var selectionState: SelectionState? = null    // selection mode
var overlay: Overlay = Overlay.None           // modal state
```

`selectionState` and `overlay` are returned from `readInputKey` and looped back — that's fine. But `shownTriggers` and `storedStoryCount` are manual indices into growing lists. This pattern is error-prone if the engine ever resets or truncates these lists (e.g., during a restart). Consider computing deltas directly rather than maintaining running indices:

```kotlin
val newTriggerTexts = next.triggerTexts.drop(shownTriggers)
```

**3. Redundant story count tracking — `storedStoryCount` (line 70, 83-88)**

Lines 82-88 duplicate the logic at lines 126-132:

```kotlin
// Lines 82-88 — initial
val subList = currentView.storyMessages.subList(0, initialStoryCount)

// Lines 126-132 — game loop  
val subList = next.storyMessages.subList(storedStoryCount, next.storyMessages.size)
```

Both create a `subList` from "new messages" and pass to `Overlay.MessageViewer`. The only difference is the start index (0 vs. stored). This could be one helper:

```kotlin
fun newStoriesOverlay(messages: List<String>, since: Int): Overlay? =
    if (messages.size > since) Overlay.MessageViewer(
        messages.subList(since, messages.size), 
        focusedIndex = messages.lastIndex
    ) else null
```

**4. `readInputKey` has a deep nested structure with two early-exit paths (line 167-283)**

The method checks overlay handling first (lines 182-223), then falls through to main input dispatching (lines 229-282). The `handledInOverlay` boolean and `displayOverlay` computation on lines 225-227 is a subtle state transition that's easy to misread. Consider making the overlay case return early (which it mostly does) and removing the `handledInOverlay` flag entirely:

```kotlin
when (currentOverlay) {
    is Overlay.MessageViewer -> return handleOverlayInput(...)
    Overlay.None -> {} // fall through
}
```

**5. Magic numbers throughout rendering methods**

| Line | Number | Context |
|------|--------|---------|
| 33 | `170`, `-2` | overlay max width calc |
| 36 | `3`, `-6` | overlay height calc |
| 85 | `+1` (in `subList`) | implicit in `subList` end index but unclear |
| 186 | `-3` | visible rows minus header/footer |
| 190 | `-2` | page-down scroll amount |
| 198 | `-2` | page-up scroll amount |
| 382 | `-6` | panel height from screen rows |
| 402, 411 | `panelH - 3` | content area bounds |

These should be named constants or extracted into well-named functions. The `-3` offset for header/footer appears in at least 5 places.

**6. `drawMessagesPanel` re-wraps all history on every render (line 521-545)**

```kotlin
for (entry in history) {
    for (segment in entry.text.split("\n")) {
        wrapText(segment, width).forEach { lines.add(it to entry.isTrigger) }
    }
}
```

This runs on every frame (~60 fps with `screen.refresh()`), splitting and wrapping strings repeatedly. For a history of 10 messages this is acceptable now but won't scale. Consider caching wrapped lines or computing them lazily when history changes.

### Minor issues

**7. `drawInvHint` uses hardcoded column offsets (line 683-692)**
```kotlin
g.putString(width - 10, row, keyPart)
g.putString(width - 7, row, "inv")
```
Hardcoded pixel-like positions are fragile against different terminal widths. The `drawActionHints` method above it computes positioning dynamically — this should too.

**8. `exitLabel` returns `[key]  .` for null exits (line 734-739)**
The double-space before the dot seems intentional to align with other exit slots, but it's not obvious and could be a named constant or inline comment.

**9. `drawSlot` sets foreground color twice (lines 742-761)**
Both the key part and name part use identical `foregroundColor = when { ... }` logic. This is correct but verbose — the color decision could be computed once:

```kotlin
val color = if (!active || blocked) TextColor.ANSI.DEFAULT else TextColor.ANSI.GREEN
g.foregroundColor = color  // for key
...
g.foregroundColor = color  // for name
```

**10. `printEndSummary` prints to stdout after screen is stopped (line 779-799)**
This is correct behavior (no Lanterna calls after `stopScreen()`), but the method mixes terminal printing with history display. The `history` parameter here only shows last messages — consider whether this duplicates what's already been displayed on-screen.

**11. Empty `else -> {}` arm (line 281)**
```kotlin
else -> {}
return ReadInputResult(KeyAction.Help, null, currentOverlay)
```
If no key is handled by the character match, execution falls through to the Help return. This is correct but the empty body of the `else` branch looks like a bug at first glance. Consider adding `{ }` comment or restructure as `KeyCharacter -> { ... handle all chars here; return Help }`.

**12. `Overlay.MessageViewer.focusedIndex` defaults to 0 (line 29)**
When created with `focusedIndex = subList.lastIndex`, this is intentional (show the latest). But the default of `0` in the data class constructor means accidental misuse would show the first message instead of last. Consider flipping the default to `lastIndex`:

```kotlin
data class MessageViewer(
    val messages: List<String>,
    var scrollOffset: Int = 0,
    val maxHeight: Int = 20,
    var focusedIndex: Int = -1  // auto-resolve in rendering
)
```

### Positive observations ✅

- **Input handling is cleanly separated** from game loop and rendering — `readInputKey` returns a sealed `KeyAction` that the main loop dispatches.
- **Overlay system uses a sealed interface** with clear `None`/`MessageViewer` variants, no nullable flags.
- **Selection state machine** (WaitSelection → digit input) is well-implemented with `handleSelectionDigit`.
- **History capped at MAX_MESSAGES = 10** — bounded memory, simple LRU via `removeAt(0)`.
- **Resize handling** uses `doResizeIfNecessary()` before rendering ✅.

### Summary

| Priority | Issue | Effort to fix |
|----------|-------|---------------|
| High | `run()` is too large, 4 mutable tracking variables | Extract game loop + helper functions |
| Medium | Magic numbers in layout calculations (multiple `-3`, `-2` offsets) | Named constants or extracted methods |
| Medium | `drawMessagesPanel` recomputes wrapped lines every frame | Lazy/cached computation |
| Low | Redundant story overlay logic (init vs. loop) | One helper function |
| Low | Hardcoded column positions in `drawInvHint` | Compute dynamically like other hints |

---

## User

Fix 1. run() method does too much

---

## Assistant (Build · qwen3.6-35b-a3b · 11.3s)

_Thinking:_

The user wants me to fix issue 1 from the code review: the `run()` method does too much. I need to extract the game loop logic into separate methods to reduce the size and complexity of `run()`.

Let me re-read the current file to understand all the state and flows, then refactor it properly.






**Tool: read**

**Input:**
```json
{
  "filePath": "/var/home/mikharju/dev/exploration/engine/src/main/kotlin/exploration/adapter/ui/lanterna/LanternaUiAdapter.kt"
}
```

**Output:**
```
<path>/var/home/mikharju/dev/exploration/engine/src/main/kotlin/exploration/adapter/ui/lanterna/LanternaUiAdapter.kt</path>
<type>file</type>
<content>
1: package exploration.adapter.ui.lanterna
2: 
3: import com.googlecode.lanterna.SGR
4: import com.googlecode.lanterna.TerminalPosition
5: import com.googlecode.lanterna.TextColor
6: import com.googlecode.lanterna.graphics.TextGraphics
7: import com.googlecode.lanterna.input.KeyType
8: import com.googlecode.lanterna.screen.Screen
9: import com.googlecode.lanterna.screen.TerminalScreen
10: import com.googlecode.lanterna.terminal.DefaultTerminalFactory
11: import exploration.model.StatusRange
12: import exploration.port.ExitInfo
13: import exploration.port.GameEngine
14: import exploration.port.GameRef
15: import exploration.port.InputEvent
16: import exploration.port.ItemView
17: import exploration.port.ViewData
18: 
19: class LanternaUiAdapter(private val engine: GameEngine) {
20: 
21:     data class HistoryEntry(val text: String, val isTrigger: Boolean)
22: 
23:     sealed interface Overlay {
24:         object None : Overlay
25:         data class MessageViewer(
26:             val messages: List<String>,
27:             var scrollOffset: Int = 0,
28:             val maxHeight: Int = 20,
29:             var focusedIndex: Int = 0
30:         ) : Overlay {
31: 
32:             fun overlayTextAreaMaxWidth(screen: Screen): Int =
33:                 minOf(170, maxOf(40, screen.terminalSize.columns - 10)) - 2
34: 
35:             fun overlayTextAreaMaxHeight(screen: Screen): Int =
36:                 (this.maxHeight + 3).coerceAtMost(screen.terminalSize.rows - 6)
37:         }
38: 
39:     }
40: 
41:     companion object {
42:         private const val MAX_MESSAGES = 10
43:         private const val MIN_WIDTH = 64
44:         private const val MIN_HEIGHT = 23
45:     }
46: 
47:     private enum class SelectionTarget { TAKE, DROP, EQUIP, UNEQUIP }
48: 
49:     private data class SelectionState(val target: SelectionTarget, val items: List<ItemView>)
50: 
51:     fun run(scenarioId: String) {
52:         var ref: GameRef
53:         try {
54:             ref = engine.start(scenarioId)
55:         } catch (e: Exception) {
56:             println("Error loading scenario '$scenarioId': ${e.message}")
57:             e.printStackTrace()
58:             return
59:         }
60: 
61:         val screen = TerminalScreen(DefaultTerminalFactory().createTerminal())
62:         screen.stopScreen()
63:         screen.startScreen()
64:         screen.cursorPosition = null
65: 
66:         val history = mutableListOf<HistoryEntry>()
67:         var shownTriggers = 0
68:         var selectionState: SelectionState? = null
69:         var overlay: Overlay = Overlay.None
70:         var storedStoryCount = 0
71: 
72:         addMessage(history, "=== Exploration Engine (Lanterna) ===", false)
73:         addMessage(
74:             history,
75:             "arrows/wasd: move | l: look | u: activate | g: grab | p: drop | e: equip | r: uneq | j: stories | h: help | q/esc: quit",
76:             false
77:         )
78: 
79:         var currentView = engine.tick(ref, InputEvent.Look)
80:         render(screen, currentView, history, overlay)
81: 
82:         val initialStoryCount = currentView.storyMessages.size
83:         storedStoryCount = initialStoryCount
84:         if (initialStoryCount > 0) {
85:             val subList = currentView.storyMessages.subList(0, initialStoryCount)
86:             overlay = Overlay.MessageViewer(subList, focusedIndex = subList.lastIndex)
87:             render(screen, currentView, history, overlay)
88:         }
89: 
90:         while (currentView.endGameMessage == null) {
91:             screen.doResizeIfNecessary()
92:             val result = readInputKey(screen, currentView, selectionState, overlay)
93:             selectionState = result.selectionState
94:             overlay = result.overlay
95: 
96:             when (result.action) {
97:                 is KeyAction.Quit -> break
98:                 is KeyAction.Help -> {
99:                     addMessage(history, "arrows/wasd: move | l: look | u: activate | g: grab | p: drop | e: equip | r: uneq | j: stories | h: help | q/esc: quit", false)
100:                     render(screen, currentView, history, overlay)
101:                     continue
102:                 }
103:                 is KeyAction.NoOp -> {
104:                     render(screen, currentView, history, overlay)
105:                     continue
106:                 }
107:                 is KeyAction.WaitSelection -> {
108:                     val sel = selectionState!!
109:                     addMessage(history, buildSelectionPrompt(sel.target, sel.items), false)
110:                     render(screen, currentView, history, overlay)
111:                     continue
112:                 }
113:                 is KeyAction.Event -> {
114:                     if (overlay != Overlay.None) overlay = Overlay.None
115:                     val next = engine.tick(ref, result.action.event)
116: 
117:                     currentView = next
118:                     if (next.commandText.isNotBlank()) addMessage(history, next.commandText, false)
119:                     val newTriggerCount = next.triggerTexts.size - shownTriggers
120:                     for (i in 0 until newTriggerCount) {
121:                         val triggerText = next.triggerTexts[shownTriggers + i]
122:                         if (triggerText.isNotBlank()) addMessage(history, triggerText, true)
123:                     }
124: 
125:                     // Show the newest story message(s) as overlay
126:                     if (next.storyMessages.size > storedStoryCount) {
127:                         val subList = next.storyMessages.subList(storedStoryCount, next.storyMessages.size)
128:                         overlay = Overlay.MessageViewer(subList, focusedIndex = subList.lastIndex)
129:                         storedStoryCount = next.storyMessages.size
130:                     } else {
131:                         storedStoryCount = next.storyMessages.size
132:                     }
133: 
134:                     shownTriggers = next.triggerTexts.size
135:                     render(screen, currentView, history, overlay)
136:                 }
137:             }
138:         }
139: 
140:         for (i in shownTriggers until currentView.triggerTexts.size) {
141:             if (currentView.triggerTexts[i].isNotBlank()) addMessage(history, currentView.triggerTexts[i], true)
142:         }
143:         storedStoryCount = currentView.storyMessages.size
144:         if (currentView.endGameMessage != null) {
145:             addMessage(history, "", false)
146:             addMessage(history, currentView.endGameMessage, false)
147:         } else if (currentView.outputLine.isNotBlank()) {
148:             addMessage(history, currentView.outputLine, false)
149:         }
150:         screen.doResizeIfNecessary()
151:         render(screen, currentView, history, overlay)
152: 
153:         screen.stopScreen()
154:         printEndSummary(currentView, history)
155:     }
156: 
157:     private sealed class KeyAction {
158:         object Quit : KeyAction()
159:         object Help : KeyAction()
160:         object WaitSelection : KeyAction()
161:         object NoOp : KeyAction()
162:         data class Event(val event: InputEvent) : KeyAction()
163:     }
164: 
165:     private data class ReadInputResult(val action: KeyAction, val selectionState: SelectionState?, val overlay: Overlay)
166: 
167:     private fun readInputKey(
168:         screen: Screen,
169:         view: ViewData,
170:         selState: SelectionState?,
171:         currentOverlay: Overlay
172:     ): ReadInputResult {
173:         val key = screen.readInput() ?: return ReadInputResult(KeyAction.Quit, null, currentOverlay)
174: 
175:         if (selState != null) {
176:             return handleSelectionDigit(key, selState, currentOverlay)
177:         }
178: 
179:         var nextOverlay = currentOverlay
180:         var handledInOverlay: Boolean
181: 
182:         when (currentOverlay) {
183:             is Overlay.MessageViewer -> {
184:                 val ov = currentOverlay
185:                 val totalWrappedLines = countWrappedDisplayLines(ov.messages, ov.overlayTextAreaMaxWidth(screen))
186:                 val visibleRows = ov.overlayTextAreaMaxHeight(screen) - 3
187:                 val maxScroll = maxOf(0, totalWrappedLines - visibleRows + 3)
188:                 handledInOverlay = when (key.keyType) {
189:                     KeyType.PageDown -> {
190:                         nextOverlay = ov.copy(scrollOffset = minOf(maxScroll, ov.scrollOffset + maxOf(1, ov.overlayTextAreaMaxHeight(screen) - 2)))
191:                         true
192:                     }
193:                     KeyType.ArrowDown -> {
194:                         nextOverlay = ov.copy(scrollOffset = minOf(maxScroll, ov.scrollOffset + 1))
195:                         true
196:                     }
197:                     KeyType.PageUp -> {
198:                         nextOverlay = ov.copy(scrollOffset = maxOf(0, ov.scrollOffset - maxOf(1, ov.maxHeight - 2)))
199:                         true
200:                     }
201:                     KeyType.ArrowUp -> {
202:                         nextOverlay = ov.copy(scrollOffset = maxOf(0, ov.scrollOffset - 1))
203:                         true
204:                     }
205:                     KeyType.ArrowLeft -> {
206:                         if (ov.focusedIndex > 0)
207:                             nextOverlay = ov.copy(focusedIndex = ov.focusedIndex - 1, scrollOffset = 0)
208:                         true
209:                     }
210:                     KeyType.ArrowRight -> {
211:                         if (ov.focusedIndex < ov.messages.size - 1)
212:                             nextOverlay = ov.copy(focusedIndex = ov.focusedIndex + 1, scrollOffset = 0)
213:                         true
214:                     }
215:                     KeyType.Escape -> {
216:                         nextOverlay = Overlay.None
217:                         true
218:                     }
219:                     else -> false
220:                 }
221:             }
222:             Overlay.None -> handledInOverlay = false
223:         }
224: 
225:         if (handledInOverlay) return ReadInputResult(KeyAction.NoOp, null, nextOverlay)
226: 
227:         val displayOverlay = if (currentOverlay !is Overlay.None && key.keyType != KeyType.Escape) Overlay.None else currentOverlay
228: 
229:         when (key.keyType) {
230:             KeyType.Escape -> return if (currentOverlay !is Overlay.None) {
231:                 ReadInputResult(KeyAction.NoOp, null, Overlay.None)
232:             } else {
233:                 ReadInputResult(KeyAction.Quit, null, currentOverlay)
234:             }
235:             KeyType.ArrowUp -> return ReadInputResult(KeyAction.Event(InputEvent.MoveDirection(InputEvent.Direction.North)), null, displayOverlay)
236:             KeyType.ArrowDown -> return ReadInputResult(KeyAction.Event(InputEvent.MoveDirection(InputEvent.Direction.South)), null, displayOverlay)
237:             KeyType.ArrowLeft -> return ReadInputResult(KeyAction.Event(InputEvent.MoveDirection(InputEvent.Direction.West)), null, displayOverlay)
238:             KeyType.ArrowRight -> return ReadInputResult(KeyAction.Event(InputEvent.MoveDirection(InputEvent.Direction.East)), null, displayOverlay)
239:             KeyType.Character -> {
240:                 when (key.character.lowercaseChar()) {
241:                     'w' -> return ReadInputResult(KeyAction.Event(InputEvent.MoveDirection(InputEvent.Direction.North)), null, displayOverlay)
242:                     'a' -> return ReadInputResult(KeyAction.Event(InputEvent.MoveDirection(InputEvent.Direction.West)), null, displayOverlay)
243:                     's' -> return ReadInputResult(KeyAction.Event(InputEvent.MoveDirection(InputEvent.Direction.South)), null, displayOverlay)
244:                     'd' -> return ReadInputResult(KeyAction.Event(InputEvent.MoveDirection(InputEvent.Direction.East)), null, displayOverlay)
245:                     'l' -> return ReadInputResult(KeyAction.Event(InputEvent.Look), null, displayOverlay)
246:                     'u' -> return ReadInputResult(KeyAction.Event(InputEvent.Activate), null, displayOverlay)
247:                     'g' -> {
248:                         val candidates = view.areaItems
249:                         return if (candidates.isEmpty()) ReadInputResult(KeyAction.Help, null, displayOverlay)
250:                         else if (candidates.size == 1) ReadInputResult(KeyAction.Event(InputEvent.TakeItem(candidates[0].name)), null, displayOverlay)
251:                         else ReadInputResult(KeyAction.WaitSelection, SelectionState(SelectionTarget.TAKE, candidates), displayOverlay)
252:                     }
253:                     'p' -> {
254:                         val candidates = view.carriedItems + view.equippedItems
255:                         return if (candidates.isEmpty()) ReadInputResult(KeyAction.Help, null, displayOverlay)
256:                         else if (candidates.size == 1) ReadInputResult(KeyAction.Event(InputEvent.DropItem(candidates[0].name)), null, displayOverlay)
257:                         else ReadInputResult(KeyAction.WaitSelection, SelectionState(SelectionTarget.DROP, candidates), displayOverlay)
258:                     }
259:                     'e' -> {
260:                         val candidates = view.carriedItems
261:                         return if (candidates.isEmpty()) ReadInputResult(KeyAction.Help, null, displayOverlay)
262:                         else if (candidates.size == 1) ReadInputResult(KeyAction.Event(InputEvent.EquipItem(candidates[0].name)), null, displayOverlay)
263:                         else ReadInputResult(KeyAction.WaitSelection, SelectionState(SelectionTarget.EQUIP, candidates), displayOverlay)
264:                     }
265:                     'r' -> {
266:                         val candidates = view.equippedItems
267:                         return if (candidates.isEmpty()) ReadInputResult(KeyAction.Help, null, displayOverlay)
268:                         else if (candidates.size == 1) ReadInputResult(KeyAction.Event(InputEvent.UnequipItem(candidates[0].name)), null, displayOverlay)
269:                         else ReadInputResult(KeyAction.WaitSelection, SelectionState(SelectionTarget.UNEQUIP, candidates), displayOverlay)
270:                     }
271:                     'i' -> return ReadInputResult(KeyAction.Event(InputEvent.Inventory), null, displayOverlay)
272:                     'j' -> {
273:                         val msgs = view.storyMessages.filter { it.isNotBlank() }
274:                         return if (msgs.isEmpty()) ReadInputResult(KeyAction.Help, null, displayOverlay)
275:                         else ReadInputResult(KeyAction.NoOp, null, Overlay.MessageViewer(msgs, focusedIndex = msgs.lastIndex))
276:                     }
277:                     'h' -> return ReadInputResult(KeyAction.NoOp, null, displayOverlay)
278:                     'q' -> return ReadInputResult(KeyAction.Quit, null, displayOverlay)
279:                 }
280:             }
281:             else -> {}
282:         }
283:         return ReadInputResult(KeyAction.Help, null, currentOverlay)
284:     }
285: 
286:     private fun handleSelectionDigit(key: com.googlecode.lanterna.input.KeyStroke, selState: SelectionState, overlay: Overlay): ReadInputResult {
287:         if (key.keyType == KeyType.Escape) return ReadInputResult(KeyAction.Help, null, overlay)
288:         if (key.keyType != KeyType.Character) return ReadInputResult(KeyAction.WaitSelection, selState, overlay)
289:         val idx = key.character.digitToIntOrNull() ?: return ReadInputResult(KeyAction.WaitSelection, selState, overlay)
290:         val itemIdx = idx - 1
291:         if (itemIdx < 0 || itemIdx >= selState.items.size) return ReadInputResult(KeyAction.Help, null, overlay)
292:         val item = selState.items[itemIdx]
293:         val event = when (selState.target) {
294:             SelectionTarget.TAKE -> InputEvent.TakeItem(item.name)
295:             SelectionTarget.DROP -> InputEvent.DropItem(item.name)
296:             SelectionTarget.EQUIP -> InputEvent.EquipItem(item.name)
297:             SelectionTarget.UNEQUIP -> InputEvent.UnequipItem(item.name)
298:         }
299:         return ReadInputResult(KeyAction.Event(event), null, overlay)
300:     }
301: 
302:     private fun countWrappedDisplayLines(messages: List<String>, lineWidth: Int): Int {
303:         var total = 0
304:         for (msg in messages) {
305:             for (line in msg.split("\n")) {
306:                 val wrappedLines = wrapText(line, lineWidth)
307:                 total += wrappedLines.size
308:             }
309:         }
310:         return total
311:     }
312: 
313:     private fun buildSelectionPrompt(target: SelectionTarget, items: List<ItemView>): String {
314:         val verb = when (target) {
315:             SelectionTarget.TAKE -> "Take"
316:             SelectionTarget.DROP -> "Drop"
317:             SelectionTarget.EQUIP -> "Equip"
318:             SelectionTarget.UNEQUIP -> "Unequip"
319:         }
320:         val options = items.mapIndexed { i, it -> "[${i + 1}] ${it.name}" }.joinToString(" ")
321:         return "$verb what? $options"
322:     }
323: 
324:     private fun addMessage(history: MutableList<HistoryEntry>, msg: String, isTrigger: Boolean) {
325:         if (msg.isBlank()) return
326:         history.add(HistoryEntry(msg, isTrigger))
327:         while (history.size > MAX_MESSAGES) history.removeAt(0)
328:     }
329: 
330:     private fun render(screen: Screen, view: ViewData, history: List<HistoryEntry>, overlay: Overlay) {
331:         screen.clear()
332:         val g = screen.newTextGraphics()
333:         val size = screen.terminalSize
334:         val w = size.columns
335:         val h = size.rows
336: 
337:         if (h < MIN_HEIGHT || w < MIN_WIDTH) {
338:             drawBorder(g, w, h)
339:             drawTooSmallMessage(g, w, h)
340:             screen.refresh()
341:             return
342:         }
343: 
344:         // Draw overlay if active
345:         when (overlay) {
346:             is Overlay.MessageViewer -> {
347:                 drawMessageOverlay(screen, g, view, overlay, size)
348:                 screen.refresh()
349:                 return
350:             }
351:             else -> {}
352:         }
353: 
354:         val leftWidth = ((w - 3) * 0.6).toInt()
355:         val rightWidth = (w - 3) - leftWidth
356:         val splitCol = 1 + leftWidth
357:         val panelStartRow = 3
358: 
359:         drawBorder(g, w, h)
360:         drawTitle(g, w)
361:         drawPanelHeaders(g, leftWidth, rightWidth, splitCol)
362:         drawSeparatorLine(g, w, splitCol, panelStartRow - 1)
363:         drawMessagesPanel(g, history, TerminalPosition(1, panelStartRow), leftWidth, h - panelStartRow - 4)
364:         drawStatusesPanel(g, view, TerminalPosition(splitCol + 1, panelStartRow), rightWidth - 1, h - panelStartRow - 4)
365:         drawVerticalSeparator(g, splitCol, 2, h - 5)
366:         drawSplitSeparator(g, w, splitCol, h - 5)
367:         drawBottomBar(g, view, w - 2, h - 5)
368: 
369:         screen.refresh()
370:     }
371: 
372:     private fun drawMessageOverlay(
373:         screen: Screen,
374:         g: TextGraphics,
375:         view: ViewData,
376:         overlay: Overlay.MessageViewer,
377:         size: com.googlecode.lanterna.TerminalSize
378:     ) {
379:         val w = size.columns
380:         val h = size.rows
381:         val panelW = minOf(170, maxOf(40, w - 10))
382:         val panelH = overlay.maxHeight.coerceAtMost(h - 6)
383:         val startCol = (w - panelW) / 2 + 1
384:         val startRow = (h - panelH) / 2
385:         val textAreaWidth = overlay.overlayTextAreaMaxWidth(screen)
386: 
387:         drawBorder(g, startCol - 1, startRow - 1, startCol + panelW, startRow + panelH + 1)
388: 
389:         val title = " Story Message"
390:         g.foregroundColor = TextColor.ANSI.CYAN
391:         g.enableModifiers(SGR.BOLD)
392:         g.putString(startCol, startRow, title)
393:         g.disableModifiers(SGR.BOLD)
394:         g.foregroundColor = TextColor.ANSI.DEFAULT
395: 
396:         val contentTop = startRow + 2
397:         val focusedMsg = overlay.messages.getOrElse(overlay.focusedIndex) { "" }
398:         var rowIdx = 0
399:         for (paragraph in focusedMsg.split("\n")) {
400:             if (paragraph.isBlank()) {
401:                 if (rowIdx < overlay.scrollOffset) { rowIdx++; continue }
402:                 if (rowIdx >= overlay.scrollOffset + panelH - 3) break
403:                 val drawRow = contentTop + (rowIdx - overlay.scrollOffset)
404:                 if (drawRow < startRow + panelH - 1) {
405:                     g.putString(startCol, drawRow, "".padEnd(textAreaWidth))
406:                 }
407:                 rowIdx++
408:             } else {
409:                 for (line in wrapText(paragraph, textAreaWidth)) {
410:                     if (rowIdx < overlay.scrollOffset) { rowIdx++; continue }
411:                     if (rowIdx >= overlay.scrollOffset + panelH - 3) break
412:                     val drawRow = contentTop + (rowIdx - overlay.scrollOffset)
413:                     if (drawRow < startRow + panelH - 1) {
414:                         g.putString(startCol, drawRow, line.padEnd(textAreaWidth))
415:                     }
416:                     rowIdx++
417:                 }
418:             }
419:         }
420: 
421:         val totalCount = countWrappedDisplayLines(listOf(focusedMsg), textAreaWidth)
422:         val canScrollUp = overlay.scrollOffset > 0
423:         val canScrollDown = (panelH - 3 > 0) && (overlay.scrollOffset + panelH - 3 < totalCount)
424:         g.putString(startCol, startRow + panelH - 1, buildString {
425:             append(" [PgUp:up PgDn:down ")
426:             if (canScrollUp || canScrollDown) append(" | ")
427:             if (canScrollUp) append("scroll↑ ")
428:             if (canScrollDown) append("scroll↓ ")
429:             append("| ${overlay.focusedIndex + 1} / ${overlay.messages.size} esc:close]")
430:         })
431: 
432:         g.foregroundColor = TextColor.ANSI.DEFAULT
433:     }
434: 
435: 
436:     private fun drawBorder(g: TextGraphics, left: Int, top: Int, right: Int, bottom: Int) {
437:         g.setCharacter(left, top, '┌')
438:         g.setCharacter(right - 1, top, '┐')
439:         g.setCharacter(left, bottom - 1, '└')
440:         g.setCharacter(right - 1, bottom - 1, '┘')
441:         for (col in left + 1 until right - 1) {
442:             g.setCharacter(col, top, '─')
443:             g.setCharacter(col, bottom - 1, '─')
444:         }
445:         for (row in top + 1 until bottom - 1) {
446:             g.setCharacter(left, row, '│')
447:             g.setCharacter(right - 1, row, '│')
448:         }
449:     }
450: 
451:     private fun drawBorder(g: TextGraphics, w: Int, h: Int) {
452:         g.setCharacter(0, 0, '┌')
453:         g.setCharacter(w - 1, 0, '┐')
454:         g.setCharacter(0, h - 1, '└')
455:         g.setCharacter(w - 1, h - 1, '┘')
456:         for (col in 1 until w - 1) {
457:             g.setCharacter(col, 0, '─')
458:             g.setCharacter(col, h - 1, '─')
459:         }
460:         for (row in 1 until h - 1) {
461:             g.setCharacter(0, row, '│')
462:             g.setCharacter(w - 1, row, '│')
463:         }
464:     }
465: 
466:     private fun drawTitle(g: TextGraphics, width: Int) {
467:         val title = " Exploration Engine "
468:         val pad = maxOf(width - title.length, 0) / 2
469:         g.enableModifiers(SGR.BOLD)
470:         g.foregroundColor = TextColor.ANSI.CYAN
471:         g.putString(pad, 0, title)
472:         g.disableModifiers(SGR.BOLD)
473:     }
474: 
475:     private fun drawPanelHeaders(g: TextGraphics, leftWidth: Int, rightWidth: Int, splitCol: Int) {
476:         val msgHeader = " Messages"
477:         g.enableModifiers(SGR.BOLD)
478:         g.foregroundColor = TextColor.ANSI.WHITE
479:         g.putString(1, 1, msgHeader)
480: 
481:         val statusHeader = " Statuses"
482:         val rightStart = splitCol + 1
483:         g.putString(rightStart, 1, statusHeader)
484:         g.disableModifiers(SGR.BOLD)
485: 
486:         for (col in 1 until splitCol) {
487:             if (col < 1 + leftWidth && col > 0) {
488:                 g.setCharacter(col, 2, '─')
489:             }
490:         }
491:         g.setCharacter(splitCol, 2, '│')
492:         for (col in splitCol + 1 until splitCol + rightWidth) {
493:             if (col < 1 + leftWidth + rightWidth - 1) {
494:                 g.setCharacter(col, 2, '─')
495:             }
496:         }
497:     }
498: 
499:     private fun drawSeparatorLine(g: TextGraphics, w: Int, splitCol: Int, row: Int) {
500:         for (col in 0 until w) {
501:             if (col == splitCol) g.setCharacter(col, row, '┼')
502:             else g.setCharacter(col, row, '─')
503:         }
504:     }
505: 
506:     private fun drawVerticalSeparator(g: TextGraphics, col: Int, startRow: Int, count: Int) {
507:         for (row in startRow until startRow + count) {
508:             if (row > 0 && row < count + startRow - 1) {
509:                 g.setCharacter(col, row, '│')
510:             }
511:         }
512:     }
513: 
514:     private fun drawSplitSeparator(g: TextGraphics, w: Int, splitCol: Int, row: Int) {
515:         for (col in 0 until w) {
516:             if (col == splitCol) g.setCharacter(col, row, '┼')
517:             else g.setCharacter(col, row, '─')
518:         }
519:     }
520: 
521:     private fun drawMessagesPanel(
522:         g: TextGraphics,
523:         history: List<HistoryEntry>,
524:         top: TerminalPosition,
525:         width: Int,
526:         maxRows: Int
527:     ) {
528:         val lines = mutableListOf<Pair<String, Boolean>>()
529:         for (entry in history) {
530:             for (segment in entry.text.split("\n")) {
531:                 wrapText(segment, width).forEach { lines.add(it to entry.isTrigger) }
532:             }
533:         }
534: 
535:         val skip = maxOf(0, lines.size - maxRows)
536:         var row = top.row
537:         for (i in skip until lines.size) {
538:             if (row >= top.row + maxRows) break
539:             val (lineText, isTrigger) = lines[i]
540:             g.foregroundColor = if (isTrigger) TextColor.ANSI.BLUE else TextColor.ANSI.DEFAULT
541:             g.putString(top.column, row, lineText.padEnd(width))
542:             row++
543:         }
544:         g.foregroundColor = TextColor.ANSI.DEFAULT
545:     }
546: 
547:     private fun drawStatusesPanel(
548:         g: TextGraphics,
549:         v: ViewData,
550:         top: TerminalPosition,
551:         width: Int,
552:         maxRows: Int
553:     ) {
554:         val hpLine = buildHpBar(v, width)
555:         g.putString(top.column, top.row, hpLine.padEnd(width))
556: 
557:         val progLine = " Exp: ${v.exploredCount}/${v.totalAreas}   Dev: ${v.activatedCount}/${v.totalDevices}"
558:         g.putString(top.column, top.row + 1, progLine.padEnd(width))
559: 
560:         var row = top.row + 2
561: 
562:         val statuses = v.statuses.filterValues { it != 0 }
563:         for ((name, value) in statuses) {
564:             if (row >= top.row + maxRows) break
565:             val range = v.statusBounds[name]
566:             g.putString(top.column, row, formatStatus(name, value, range, width).padEnd(width))
567:             row++
568:         }
569: 
570:         if (v.equippedItems.isEmpty()) return
571: 
572:         if (row < top.row + maxRows && statuses.isNotEmpty()) {
573:             g.putString(top.column, row, " ".repeat(width))
574:             row++
575:         }
576: 
577:         for ((i, item) in v.equippedItems.withIndex()) {
578:             if (row >= top.row + maxRows) break
579:             val label = "@ ${item.name}" + if (item.locked) " (Locked)" else ""
580:             g.foregroundColor = if (item.locked) TextColor.ANSI.RED else TextColor.ANSI.MAGENTA
581:             g.enableModifiers(SGR.BOLD)
582:             g.putString(top.column, row, label.padEnd(width))
583:             g.disableModifiers(SGR.BOLD)
584:             g.foregroundColor = TextColor.ANSI.DEFAULT
585:             row++
586:         }
587: 
588:         g.foregroundColor = TextColor.ANSI.DEFAULT
589:     }
590: 
591:     private fun buildHpBar(v: ViewData, width: Int): String {
592:         val barLen = 10
593:         val filled = ((v.health.toDouble() / v.maxHealth) * barLen).toInt().coerceIn(0, barLen)
594:         val empty = barLen - filled
595: 
596:         val bar = "█".repeat(filled) + "░".repeat(empty)
597:         val textPart = "${v.health}/${v.maxHealth}"
598:         return "$bar $textPart"
599:     }
600: 
601:     private fun formatStatus(name: String, value: Int, range: StatusRange?, width: Int): String {
602:         val maxLen = width - 10
603:         val displayValue = if (range != null && range.max != Int.MAX_VALUE) {
604:             "$value/${range.max}"
605:         } else {
606:             "$value"
607:         }
608:         val template = ": $displayValue"
609:         val nameMaxLen = maxOf(4, width - template.length)
610: 
611:         val displayName = if (name.length > nameMaxLen) {
612:             name.substring(0, nameMaxLen - 3) + "..."
613:         } else {
614:             name
615:         }
616:         return "$displayName$template"
617:     }
618: 
619:     private fun wrapText(text: String, width: Int): List<String> {
620:         val words = text.split(" ")
621:         val result = mutableListOf<String>()
622:         var line = ""
623: 
624:         for (word in words) {
625:             if (word.length >= width) {
626:                 if (line.isNotEmpty()) result.add(line)
627:                 line = ""
628:                 var remaining = word
629:                 while (remaining.isNotEmpty()) {
630:                     if (remaining.length <= width) {
631:                         line += remaining
632:                         remaining = ""
633:                     } else {
634:                         result.add(remaining.substring(0, width))
635:                         remaining = remaining.substring(width)
636:                     }
637:                 }
638:             } else if ((line + " " + word).length <= width) {
639:                 line = if (line.isEmpty()) word else "$line $word"
640:             } else {
641:                 if (line.isNotEmpty()) result.add(line)
642:                 line = word
643:             }
644:         }
645:         if (line.isNotEmpty()) result.add(line)
646:         return result.ifEmpty { listOf("") }
647:     }
648: 
649:     private fun drawBottomBar(
650:         g: TextGraphics,
651:         view: ViewData,
652:         width: Int,
653:         topRow: Int
654:     ) {
655:         val exits = view.exits
656:         val wInfo = exits.getOrNull(0)
657:         val aInfo = exits.getOrNull(1)
658:         val sInfo = exits.getOrNull(2)
659:         val dInfo = exits.getOrNull(3)
660: 
661:         val wLabel = exitLabel('w', wInfo)
662:         val aLabel = exitLabel('a', aInfo)
663:         val sLabel = exitLabel('s', sInfo)
664:         val dLabel = exitLabel('d', dInfo)
665: 
666:         val gap = 2
667:         val contentWidth = aLabel.length + gap + sLabel.length + gap + dLabel.length
668:         val leftPad = (width - contentWidth) / 2
669: 
670:         val colA = leftPad
671:         val colS = colA + aLabel.length + gap
672:         val colD = colS + sLabel.length + gap
673:         val colW = colS - wLabel.length / 2
674: 
675:         drawSlot(g, wLabel, TerminalPosition(colW, topRow), wInfo != null, wInfo?.blocked ?: false)
676:         drawInvHint(g, width, topRow)
677:         drawSlot(g, aLabel, TerminalPosition(colA, topRow + 1), aInfo != null, aInfo?.blocked ?: false)
678:         drawSlot(g, sLabel, TerminalPosition(colS, topRow + 1), sInfo != null, sInfo?.blocked ?: false)
679:         drawSlot(g, dLabel, TerminalPosition(colD, topRow + 1), dInfo != null, dInfo?.blocked ?: false)
680:         drawActionHints(g, view, width, topRow + 2)
681:     }
682: 
683:     private fun drawInvHint(g: TextGraphics, width: Int, row: Int) {
684:         val label = "[\$i] inv"
685:         g.foregroundColor = TextColor.ANSI.CYAN
686:         g.enableModifiers(SGR.BOLD)
687:         val keyPart = "[i]"
688:         g.putString(width - 10, row, keyPart)
689:         g.disableModifiers(SGR.BOLD)
690:         g.foregroundColor = TextColor.ANSI.WHITE
691:         g.putString(width - 7, row, "inv")
692:     }
693: 
694:     private fun drawActionHints(g: TextGraphics, view: ViewData, width: Int, row: Int) {
695:         val hasAreaItems = view.areaItems.isNotEmpty()
696:         val hasCarriedOrEquipped = view.carriedItems.isNotEmpty() || view.equippedItems.isNotEmpty()
697:         val hasCarried = view.carriedItems.isNotEmpty()
698:         val hasEquipped = view.equippedItems.isNotEmpty()
699: 
700:         val gLabel = "[g] grab"
701:         val pLabel = "[p] drop"
702:         val eLabel = "[e] equip"
703:         val rLabel = "[r] uneq"
704: 
705:         val gap = 2
706:         val totalWidth = gLabel.length + gap + pLabel.length + gap + eLabel.length + gap + rLabel.length
707:         val leftPad = (width - totalWidth) / 2
708: 
709:         var col = leftPad
710:         drawActionSlot(g, gLabel, TerminalPosition(col, row), hasAreaItems)
711:         col += gLabel.length + gap
712:         drawActionSlot(g, pLabel, TerminalPosition(col, row), hasCarriedOrEquipped)
713:         col += pLabel.length + gap
714:         drawActionSlot(g, eLabel, TerminalPosition(col, row), hasCarried)
715:         col += eLabel.length + gap
716:         drawActionSlot(g, rLabel, TerminalPosition(col, row), hasEquipped)
717:     }
718: 
719:     private fun drawActionSlot(g: TextGraphics, label: String, pos: TerminalPosition, active: Boolean) {
720:         g.foregroundColor = if (active) TextColor.ANSI.GREEN else TextColor.ANSI.DEFAULT
721:         g.enableModifiers(SGR.BOLD)
722:         val keyPart = label.substringBefore(']') + "]"
723:         g.putString(pos.column, pos.row, keyPart)
724:         g.disableModifiers(SGR.BOLD)
725: 
726:         val nameStart = pos.column + keyPart.length
727:         val namePart = if (label.contains("] ")) label.substringAfter("] ") else ""
728:         g.foregroundColor = if (active) TextColor.ANSI.WHITE else TextColor.ANSI.DEFAULT
729:         g.putString(nameStart, pos.row, namePart)
730: 
731:         g.disableModifiers(SGR.BOLD)
732:     }
733: 
734:     private fun exitLabel(key: Char, info: ExitInfo?): String =
735:         if (info == null || info.name.isNullOrEmpty()) "[${key}]  ."
736:         else {
737:             val suffix = if (info.blocked) " (B)" else ""
738:             "[${key}] ${info.name}$suffix"
739:         }
740: 
741:     private fun drawSlot(g: TextGraphics, label: String, pos: TerminalPosition, active: Boolean, blocked: Boolean) {
742:         g.foregroundColor = when {
743:             !active -> TextColor.ANSI.DEFAULT
744:             blocked -> TextColor.ANSI.DEFAULT
745:             else -> TextColor.ANSI.GREEN
746:         }
747:         g.enableModifiers(SGR.BOLD)
748:         val keyPart = label.substringBefore(']') + "]"
749:         g.putString(pos.column, pos.row, keyPart)
750:         g.disableModifiers(SGR.BOLD)
751: 
752:         val nameStart = pos.column + keyPart.length
753:         val namePart = if (label.contains("] ")) label.substringAfter("] ") else ""
754:         g.foregroundColor = when {
755:             !active -> TextColor.ANSI.DEFAULT
756:             blocked -> TextColor.ANSI.DEFAULT
757:             else -> TextColor.ANSI.GREEN
758:         }
759:         g.putString(nameStart, pos.row, namePart)
760: 
761:         g.disableModifiers(SGR.BOLD)
762:     }
763: 
764:     private fun drawTooSmallMessage(g: TextGraphics, w: Int, h: Int) {
765:         val lines = listOf(
766:             "Terminal too small.",
767:             "Resize to at least $MIN_WIDTH columns, $MIN_HEIGHT rows."
768:         )
769:         val startRow = (h - lines.size) / 2
770:         g.foregroundColor = TextColor.ANSI.YELLOW
771:         g.enableModifiers(SGR.BOLD)
772:         for ((i, line) in lines.withIndex()) {
773:             val col = (w - line.length) / 2
774:             g.putString(col, startRow + i, line)
775:         }
776:         g.disableModifiers(SGR.BOLD)
777:     }
778: 
779:     private fun printEndSummary(view: ViewData, history: List<HistoryEntry>) {
780:         println()
781:         if (view.endGameMessage != null) {
782:             println("===== ${view.endGameMessage} =====")
783:         } else {
784:             println("===== Game Over =====")
785:         }
786:         println("Area  : ${view.currentAreaName}")
787:         println("Health: ${view.health}/${view.maxHealth}")
788:         println("Explored: ${view.exploredCount}/${view.totalAreas}  |  Devices: ${view.activatedCount}/${view.totalDevices}")
789:         if (view.statuses.isNotEmpty()) {
790:             val statusLines = view.statuses.filterValues { it != 0 }.map { (name, value) ->
791:                 "$name=$value"
792:             }
793:             println("Statuses: ${statusLines.joinToString(", ")}")
794:         }
795:         if (history.isNotEmpty()) {
796:             println("\n--- Last messages ---")
797:             for (entry in history) println(entry.text)
798:         }
799:     }
800: }

(End of file - total 800 lines)
</content>
```

---

## Assistant (Build · qwen3.6-35b-a3b · 230.4s)

_Thinking:_

I need to refactor `run()` to extract the game loop into a separate method. The current `run()` does:
1. Engine start + screen setup (lines 52-64)
2. History initialization (lines 66-70)
3. Welcome messages (lines 72-77)
4. First Look and render (lines 79-80)
5. Initial story message overlay (lines 82-88)
6. Game loop with input handling, state updates, rendering (lines 90-138)
7. End-game cleanup (lines 140-151)
8. Stop screen and print summary (lines 153-154)

The main things to extract:
1. Screen setup + first look → `initializeScreenAndFirstView()`
2. Initial story overlay display → already small, could stay inline or be extracted
3. Game loop → `gameLoop(screen, ref, history, ...)` - this is the big one
4. End-game cleanup and summary → `handleEndGame(...)`

The tricky part is tracking state across these methods: `shownTriggers`, `storedStoryCount`, `selectionState`, `overlay`. These need to be passed as a mutable state object or returned from each iteration.

Let me create a game loop state holder data class that contains all the mutable tracking vars, and return updated versions from each step.

Actually, looking more carefully, I think the cleanest approach is:
1. Keep `run()` for screen lifecycle + error handling + summary
2. Extract the game loop into its own method that takes/returns a state holder
3. The state holder will contain: history, shownTriggers, storedStoryCount, selectionState, overlay, currentView

Let me design this:

```kotlin
private data class GameState(
    val history: MutableList<HistoryEntry>,
    var currentView: ViewData,
    var shownTriggers: Int = 0,
    var storedStoryCount: Int = 0,
    var selectionState: SelectionState? = null,
    var overlay: Overlay = Overlay.None
)

fun run(scenarioId: String) {
    // ... error handling + screen setup
    val state = initFirstView(...)
    gameLoop(screen, ref, state)
    endGameScreen(screen, state)
}

private fun initFirstView(...): GameState { ... }
private fun gameLoop(screen, ref, state: GameState) { ... }
private fun endGameScreen(screen, state: GameState) { ... }
```

This makes `run()` much smaller and each method has a clear responsibility. Let me write this out carefully to preserve all the existing behavior.






**Tool: write**

**Input:**
```json
{
  "content": "package exploration.adapter.ui.lanterna\n\nimport com.googlecode.lanterna.SGR\nimport com.googlecode.lanterna.TerminalPosition\nimport com.googlecode.lanterna.TextColor\nimport com.googlecode.lanterna.graphics.TextGraphics\nimport com.googlecode.lanterna.input.KeyType\nimport com.googlecode.lanterna.screen.Screen\nimport com.googlecode.lanterna.screen.TerminalScreen\nimport com.googlecode.lanterna.terminal.DefaultTerminalFactory\nimport exploration.model.StatusRange\nimport exploration.port.ExitInfo\nimport exploration.port.GameEngine\nimport exploration.port.GameRef\nimport exploration.port.InputEvent\nimport exploration.port.ItemView\nimport exploration.port.ViewData\n\nclass LanternaUiAdapter(private val engine: GameEngine) {\n\n    data class HistoryEntry(val text: String, val isTrigger: Boolean)\n\n    sealed interface Overlay {\n        object None : Overlay\n        data class MessageViewer(\n            val messages: List<String>,\n            var scrollOffset: Int = 0,\n            val maxHeight: Int = 20,\n            var focusedIndex: Int = 0\n        ) : Overlay {\n\n            fun overlayTextAreaMaxWidth(screen: Screen): Int =\n                minOf(170, maxOf(40, screen.terminalSize.columns - 10)) - 2\n\n            fun overlayTextAreaMaxHeight(screen: Screen): Int =\n                (this.maxHeight + 3).coerceAtMost(screen.terminalSize.rows - 6)\n        }\n\n    }\n\n    companion object {\n        private const val MAX_MESSAGES = 10\n        private const val MIN_WIDTH = 64\n        private const val MIN_HEIGHT = 23\n    }\n\n    private enum class SelectionTarget { TAKE, DROP, EQUIP, UNEQUIP }\n\n    private data class SelectionState(val target: SelectionTarget, val items: List<ItemView>)\n\n    private data class GameLoopState(\n        var history: MutableList<HistoryEntry>,\n        var currentView: ViewData,\n        var shownTriggers: Int = 0,\n        var storedStoryCount: Int = 0,\n        var selectionState: SelectionState? = null,\n        var overlay: Overlay = Overlay.None\n    )\n\n    fun run(scenarioId: String) {\n        val ref = startEngine(scenarioId) ?: return\n        val screen = setupScreen()\n\n        addMessage(screen.history, \"=== Exploration Engine (Lanterna) ===\", false)\n        addMessage(\n            screen.history,\n            \"arrows/wasd: move | l: look | u: activate | g: grab | p: drop | e: equip | r: uneq | j: stories | h: help | q/esc: quit\",\n            false\n        )\n\n        initFirstView(screen)\n        gameLoop(screen, ref)\n        endGameScreen(screen)\n\n        screen.stopScreen()\n        printEndSummary(screen.currentView, screen.history)\n    }\n\n    private fun startEngine(scenarioId: String): GameRef? {\n        try {\n            return engine.start(scenarioId)\n        } catch (e: Exception) {\n            println(\"Error loading scenario '$scenarioId': ${e.message}\")\n            e.printStackTrace()\n            return null\n        }\n    }\n\n    private fun setupScreen(): Screen = with(TerminalScreen(DefaultTerminalFactory().createTerminal())) {\n        stopScreen()\n        startScreen()\n        cursorPosition = null\n        this@setupScreen\n    }\n\n    private fun initFirstView(state: GameLoopState) {\n        state.currentView = engine.tick(ref = state.currentView, event = InputEvent.Look)\n        render(screen = state.currentView, history = state.history, overlay = state.overlay)\n\n        val initialStoryCount = state.currentView.storyMessages.size\n        state.storedStoryCount = initialStoryCount\n        if (initialStoryCount > 0) {\n            val subList = state.currentView.storyMessages.subList(0, initialStoryCount)\n            state.overlay = Overlay.MessageViewer(subList, focusedIndex = subList.lastIndex)\n            render(screen = state.currentView, history = state.history, overlay = state.overlay)\n        }\n    }\n\n    private fun gameLoop(state: GameLoopState, ref: GameRef) {\n        while (state.currentView.endGameMessage == null) {\n            screen.doResizeIfNecessary()\n            val result = readInputKey(screen = state.screen(), view = state.currentView, selState = state.selectionState, currentOverlay = state.overlay)\n            state.selectionState = result.selectionState\n            state.overlay = result.overlay\n\n            when (result.action) {\n                is KeyAction.Quit -> break\n                is KeyAction.Help -> {\n                    addMessage(state.history, \"arrows/wasd: move | l: look | u: activate | g: grab | p: drop | e: equip | r: uneq | j: stories | h: help | q/esc: quit\", false)\n                    render(screen = state.currentView, history = state.history, overlay = state.overlay)\n                }\n                is KeyAction.NoOp -> {\n                    render(screen = state.currentView, history = state.history, overlay = state.overlay)\n                }\n                is KeyAction.WaitSelection -> {\n                    val sel = state.selectionState!!\n                    addMessage(state.history, buildSelectionPrompt(sel.target, sel.items), false)\n                    render(screen = state.currentView, history = state.history, overlay = state.overlay)\n                }\n                is KeyAction.Event -> {\n                    if (state.overlay != Overlay.None) state.overlay = Overlay.None\n                    val next = engine.tick(ref, result.action.event)\n\n                    state.currentView = next\n                    if (next.commandText.isNotBlank()) addMessage(state.history, next.commandText, false)\n                    appendTriggerMessages(state)\n\n                    if (next.storyMessages.size > state.storedStoryCount) {\n                        val subList = next.storyMessages.subList(state.storedStoryCount, next.storyMessages.size)\n                        state.overlay = Overlay.MessageViewer(subList, focusedIndex = subList.lastIndex)\n                        state.storedStoryCount = next.storyMessages.size\n                    } else {\n                        state.storedStoryCount = next.storyMessages.size\n                    }\n\n                    state.shownTriggers = next.triggerTexts.size\n                    render(screen = state.currentView, history = state.history, overlay = state.overlay)\n                }\n            }\n        }\n    }\n\n    private fun appendTriggerMessages(state: GameLoopState) {\n        val newTriggerCount = state.currentView.triggerTexts.size - state.shownTriggers\n        for (i in 0 until newTriggerCount) {\n            val triggerText = state.currentView.triggerTexts[state.shownTriggers + i]\n            if (triggerText.isNotBlank()) addMessage(state.history, triggerText, true)\n        }\n    }\n\n    private fun endGameScreen(state: GameLoopState) {\n        for (i in state.shownTriggers until state.currentView.triggerTexts.size) {\n            if (state.currentView.triggerTexts[i].isNotBlank()) addMessage(state.history, state.currentView.triggerTexts[i], true)\n        }\n        state.storedStoryCount = state.currentView.storyMessages.size\n        if (state.currentView.endGameMessage != null) {\n            addMessage(state.history, \"\", false)\n            addMessage(state.history, state.currentView.endGameMessage, false)\n        } else if (state.currentView.outputLine.isNotBlank()) {\n            addMessage(state.history, state.currentView.outputLine, false)\n        }\n        screen.doResizeIfNecessary()\n        render(screen = state.currentView, history = state.history, overlay = state.overlay)\n    }\n\n    private sealed class KeyAction {\n        object Quit : KeyAction()\n        object Help : KeyAction()\n        object WaitSelection : KeyAction()\n        object NoOp : KeyAction()\n        data class Event(val event: InputEvent) : KeyAction()\n    }\n\n    private data class ReadInputResult(val action: KeyAction, val selectionState: SelectionState?, val overlay: Overlay)\n\n    private fun readInputKey(\n        screen: Screen,\n        view: ViewData,\n        selState: SelectionState?,\n        currentOverlay: Overlay\n    ): ReadInputResult {\n        val key = screen.readInput() ?: return ReadInputResult(KeyAction.Quit, null, currentOverlay)\n\n        if (selState != null) {\n            return handleSelectionDigit(key, selState, currentOverlay)\n        }\n\n        var nextOverlay = currentOverlay\n        var handledInOverlay: Boolean\n\n        when (currentOverlay) {\n            is Overlay.MessageViewer -> {\n                val ov = currentOverlay\n                val totalWrappedLines = countWrappedDisplayLines(ov.messages, ov.overlayTextAreaMaxWidth(screen))\n                val visibleRows = ov.overlayTextAreaMaxHeight(screen) - 3\n                val maxScroll = maxOf(0, totalWrappedLines - visibleRows + 3)\n                handledInOverlay = when (key.keyType) {\n                    KeyType.PageDown -> {\n                        nextOverlay = ov.copy(scrollOffset = minOf(maxScroll, ov.scrollOffset + maxOf(1, ov.overlayTextAreaMaxHeight(screen) - 2)))\n                        true\n                    }\n                    KeyType.ArrowDown -> {\n                        nextOverlay = ov.copy(scrollOffset = minOf(maxScroll, ov.scrollOffset + 1))\n                        true\n                    }\n                    KeyType.PageUp -> {\n                        nextOverlay = ov.copy(scrollOffset = maxOf(0, ov.scrollOffset - maxOf(1, ov.maxHeight - 2)))\n                        true\n                    }\n                    KeyType.ArrowUp -> {\n                        nextOverlay = ov.copy(scrollOffset = maxOf(0, ov.scrollOffset - 1))\n                        true\n                    }\n                    KeyType.ArrowLeft -> {\n                        if (ov.focusedIndex > 0)\n                            nextOverlay = ov.copy(focusedIndex = ov.focusedIndex - 1, scrollOffset = 0)\n                        true\n                    }\n                    KeyType.ArrowRight -> {\n                        if (ov.focusedIndex < ov.messages.size - 1)\n                            nextOverlay = ov.copy(focusedIndex = ov.focusedIndex + 1, scrollOffset = 0)\n                        true\n                    }\n                    KeyType.Escape -> {\n                        nextOverlay = Overlay.None\n                        true\n                    }\n                    else -> false\n                }\n            }\n            Overlay.None -> handledInOverlay = false\n        }\n\n        if (handledInOverlay) return ReadInputResult(KeyAction.NoOp, null, nextOverlay)\n\n        val displayOverlay = if (currentOverlay !is Overlay.None && key.keyType != KeyType.Escape) Overlay.None else currentOverlay\n\n        when (key.keyType) {\n            KeyType.Escape -> return if (currentOverlay !is Overlay.None) {\n                ReadInputResult(KeyAction.NoOp, null, Overlay.None)\n            } else {\n                ReadInputResult(KeyAction.Quit, null, currentOverlay)\n            }\n            KeyType.ArrowUp -> return ReadInputResult(KeyAction.Event(InputEvent.MoveDirection(InputEvent.Direction.North)), null, displayOverlay)\n            KeyType.ArrowDown -> return ReadInputResult(KeyAction.Event(InputEvent.MoveDirection(InputEvent.Direction.South)), null, displayOverlay)\n            KeyType.ArrowLeft -> return ReadInputResult(KeyAction.Event(InputEvent.MoveDirection(InputEvent.Direction.West)), null, displayOverlay)\n            KeyType.ArrowRight -> return ReadInputResult(KeyAction.Event(InputEvent.MoveDirection(InputEvent.Direction.East)), null, displayOverlay)\n            KeyType.Character -> {\n                when (key.character.lowercaseChar()) {\n                    'w' -> return ReadInputResult(KeyAction.Event(InputEvent.MoveDirection(InputEvent.Direction.North)), null, displayOverlay)\n                    'a' -> return ReadInputResult(KeyAction.Event(InputEvent.MoveDirection(InputEvent.Direction.West)), null, displayOverlay)\n                    's' -> return ReadInputResult(KeyAction.Event(InputEvent.MoveDirection(InputEvent.Direction.South)), null, displayOverlay)\n                    'd' -> return ReadInputResult(KeyAction.Event(InputEvent.MoveDirection(InputEvent.Direction.East)), null, displayOverlay)\n                    'l' -> return ReadInputResult(KeyAction.Event(InputEvent.Look), null, displayOverlay)\n                    'u' -> return ReadInputResult(KeyAction.Event(InputEvent.Activate), null, displayOverlay)\n                    'g' -> {\n                        val candidates = view.areaItems\n                        return if (candidates.isEmpty()) ReadInputResult(KeyAction.Help, null, displayOverlay)\n                        else if (candidates.size == 1) ReadInputResult(KeyAction.Event(InputEvent.TakeItem(candidates[0].name)), null, displayOverlay)\n                        else ReadInputResult(KeyAction.WaitSelection, SelectionState(SelectionTarget.TAKE, candidates), displayOverlay)\n                    }\n                    'p' -> {\n                        val candidates = view.carriedItems + view.equippedItems\n                        return if (candidates.isEmpty()) ReadInputResult(KeyAction.Help, null, displayOverlay)\n                        else if (candidates.size == 1) ReadInputResult(KeyAction.Event(InputEvent.DropItem(candidates[0].name)), null, displayOverlay)\n                        else ReadInputResult(KeyAction.WaitSelection, SelectionState(SelectionTarget.DROP, candidates), displayOverlay)\n                    }\n                    'e' -> {\n                        val candidates = view.carriedItems\n                        return if (candidates.isEmpty()) ReadInputResult(KeyAction.Help, null, displayOverlay)\n                        else if (candidates.size == 1) ReadInputResult(KeyAction.Event(InputEvent.EquipItem(candidates[0].name)), null, displayOverlay)\n                        else ReadInputResult(KeyAction.WaitSelection, SelectionState(SelectionTarget.EQUIP, candidates), displayOverlay)\n                    }\n                    'r' -> {\n                        val candidates = view.equippedItems\n                        return if (candidates.isEmpty()) ReadInputResult(KeyAction.Help, null, displayOverlay)\n                        else if (candidates.size == 1) ReadInputResult(KeyAction.Event(InputEvent.UnequipItem(candidates[0].name)), null, displayOverlay)\n                        else ReadInputResult(KeyAction.WaitSelection, SelectionState(SelectionTarget.UNEQUIP, candidates), displayOverlay)\n                    }\n                    'i' -> return ReadInputResult(KeyAction.Event(InputEvent.Inventory), null, displayOverlay)\n                    'j' -> {\n                        val msgs = view.storyMessages.filter { it.isNotBlank() }\n                        return if (msgs.isEmpty()) ReadInputResult(KeyAction.Help, null, displayOverlay)\n                        else ReadInputResult(KeyAction.NoOp, null, Overlay.MessageViewer(msgs, focusedIndex = msgs.lastIndex))\n                    }\n                    'h' -> return ReadInputResult(KeyAction.NoOp, null, displayOverlay)\n                    'q' -> return ReadInputResult(KeyAction.Quit, null, displayOverlay)\n                }\n            }\n            else -> {}\n        }\n        return ReadInputResult(KeyAction.Help, null, currentOverlay)\n    }\n\n    private fun handleSelectionDigit(key: com.googlecode.lanterna.input.KeyStroke, selState: SelectionState, overlay: Overlay): ReadInputResult {\n        if (key.keyType == KeyType.Escape) return ReadInputResult(KeyAction.Help, null, overlay)\n        if (key.keyType != KeyType.Character) return ReadInputResult(KeyAction.WaitSelection, selState, overlay)\n        val idx = key.character.digitToIntOrNull() ?: return ReadInputResult(KeyAction.WaitSelection, selState, overlay)\n        val itemIdx = idx - 1\n        if (itemIdx < 0 || itemIdx >= selState.items.size) return ReadInputResult(KeyAction.Help, null, overlay)\n        val item = selState.items[itemIdx]\n        val event = when (selState.target) {\n            SelectionTarget.TAKE -> InputEvent.TakeItem(item.name)\n            SelectionTarget.DROP -> InputEvent.DropItem(item.name)\n            SelectionTarget.EQUIP -> InputEvent.EquipItem(item.name)\n            SelectionTarget.UNEQUIP -> InputEvent.UnequipItem(item.name)\n        }\n        return ReadInputResult(KeyAction.Event(event), null, overlay)\n    }\n\n    private fun countWrappedDisplayLines(messages: List<String>, lineWidth: Int): Int {\n        var total = 0\n        for (msg in messages) {\n            for (line in msg.split(\"\\n\")) {\n                val wrappedLines = wrapText(line, lineWidth)\n                total += wrappedLines.size\n            }\n        }\n        return total\n    }\n\n    private fun buildSelectionPrompt(target: SelectionTarget, items: List<ItemView>): String {\n        val verb = when (target) {\n            SelectionTarget.TAKE -> \"Take\"\n            SelectionTarget.DROP -> \"Drop\"\n            SelectionTarget.EQUIP -> \"Equip\"\n            SelectionTarget.UNEQUIP -> \"Unequip\"\n        }\n        val options = items.mapIndexed { i, it -> \"[${i + 1}] ${it.name}\" }.joinToString(\" \")\n        return \"$verb what? $options\"\n    }\n\n    private fun addMessage(history: MutableList<HistoryEntry>, msg: String, isTrigger: Boolean) {\n        if (msg.isBlank()) return\n        history.add(HistoryEntry(msg, isTrigger))\n        while (history.size > MAX_MESSAGES) history.removeAt(0)\n    }\n\n    private fun render(screen: Screen, view: ViewData, history: List<HistoryEntry>, overlay: Overlay) {\n        screen.clear()\n        val g = screen.newTextGraphics()\n        val size = screen.terminalSize\n        val w = size.columns\n        val h = size.rows\n\n        if (h < MIN_HEIGHT || w < MIN_WIDTH) {\n            drawBorder(g, w, h)\n            drawTooSmallMessage(g, w, h)\n            screen.refresh()\n            return\n        }\n\n        // Draw overlay if active\n        when (overlay) {\n            is Overlay.MessageViewer -> {\n                drawMessageOverlay(screen, g, view, overlay, size)\n                screen.refresh()\n                return\n            }\n            else -> {}\n        }\n\n        val leftWidth = ((w - 3) * 0.6).toInt()\n        val rightWidth = (w - 3) - leftWidth\n        val splitCol = 1 + leftWidth\n        val panelStartRow = 3\n\n        drawBorder(g, w, h)\n        drawTitle(g, w)\n        drawPanelHeaders(g, leftWidth, rightWidth, splitCol)\n        drawSeparatorLine(g, w, splitCol, panelStartRow - 1)\n        drawMessagesPanel(g, history, TerminalPosition(1, panelStartRow), leftWidth, h - panelStartRow - 4)\n        drawStatusesPanel(g, view, TerminalPosition(splitCol + 1, panelStartRow), rightWidth - 1, h - panelStartRow - 4)\n        drawVerticalSeparator(g, splitCol, 2, h - 5)\n        drawSplitSeparator(g, w, splitCol, h - 5)\n        drawBottomBar(g, view, w - 2, h - 5)\n\n        screen.refresh()\n    }\n\n    private fun drawMessageOverlay(\n        screen: Screen,\n        g: TextGraphics,\n        view: ViewData,\n        overlay: Overlay.MessageViewer,\n        size: com.googlecode.lanterna.TerminalSize\n    ) {\n        val w = size.columns\n        val h = size.rows\n        val panelW = minOf(170, maxOf(40, w - 10))\n        val panelH = overlay.maxHeight.coerceAtMost(h - 6)\n        val startCol = (w - panelW) / 2 + 1\n        val startRow = (h - panelH) / 2\n        val textAreaWidth = overlay.overlayTextAreaMaxWidth(screen)\n\n        drawBorder(g, startCol - 1, startRow - 1, startCol + panelW, startRow + panelH + 1)\n\n        val title = \" Story Message\"\n        g.foregroundColor = TextColor.ANSI.CYAN\n        g.enableModifiers(SGR.BOLD)\n        g.putString(startCol, startRow, title)\n        g.disableModifiers(SGR.BOLD)\n        g.foregroundColor = TextColor.ANSI.DEFAULT\n\n        val contentTop = startRow + 2\n        val focusedMsg = overlay.messages.getOrElse(overlay.focusedIndex) { \"\" }\n        var rowIdx = 0\n        for (paragraph in focusedMsg.split(\"\\n\")) {\n            if (paragraph.isBlank()) {\n                if (rowIdx < overlay.scrollOffset) { rowIdx++; continue }\n                if (rowIdx >= overlay.scrollOffset + panelH - 3) break\n                val drawRow = contentTop + (rowIdx - overlay.scrollOffset)\n                if (drawRow < startRow + panelH - 1) {\n                    g.putString(startCol, drawRow, \"\".padEnd(textAreaWidth))\n                }\n                rowIdx++\n            } else {\n                for (line in wrapText(paragraph, textAreaWidth)) {\n                    if (rowIdx < overlay.scrollOffset) { rowIdx++; continue }\n                    if (rowIdx >= overlay.scrollOffset + panelH - 3) break\n                    val drawRow = contentTop + (rowIdx - overlay.scrollOffset)\n                    if (drawRow < startRow + panelH - 1) {\n                        g.putString(startCol, drawRow, line.padEnd(textAreaWidth))\n                    }\n                    rowIdx++\n                }\n            }\n        }\n\n        val totalCount = countWrappedDisplayLines(listOf(focusedMsg), textAreaWidth)\n        val canScrollUp = overlay.scrollOffset > 0\n        val canScrollDown = (panelH - 3 > 0) && (overlay.scrollOffset + panelH - 3 < totalCount)\n        g.putString(startCol, startRow + panelH - 1, buildString {\n            append(\" [PgUp:up PgDn:down \")\n            if (canScrollUp || canScrollDown) append(\" | \")\n            if (canScrollUp) append(\"scroll↑ \")\n            if (canScrollDown) append(\"scroll↓ \")\n            append(\"| ${overlay.focusedIndex + 1} / ${overlay.messages.size} esc:close]\")\n        })\n\n        g.foregroundColor = TextColor.ANSI.DEFAULT\n    }\n\n\n    private fun drawBorder(g: TextGraphics, left: Int, top: Int, right: Int, bottom: Int) {\n        g.setCharacter(left, top, '┌')\n        g.setCharacter(right - 1, top, '┐')\n        g.setCharacter(left, bottom - 1, '└')\n        g.setCharacter(right - 1, bottom - 1, '┘')\n        for (col in left + 1 until right - 1) {\n            g.setCharacter(col, top, '─')\n            g.setCharacter(col, bottom - 1, '─')\n        }\n        for (row in top + 1 until bottom - 1) {\n            g.setCharacter(left, row, '│')\n            g.setCharacter(right - 1, row, '│')\n        }\n    }\n\n    private fun drawBorder(g: TextGraphics, w: Int, h: Int) {\n        g.setCharacter(0, 0, '┌')\n        g.setCharacter(w - 1, 0, '┐')\n        g.setCharacter(0, h - 1, '└')\n        g.setCharacter(w - 1, h - 1, '┘')\n        for (col in 1 until w - 1) {\n            g.setCharacter(col, 0, '─')\n            g.setCharacter(col, h - 1, '─')\n        }\n        for (row in 1 until h - 1) {\n            g.setCharacter(0, row, '│')\n            g.setCharacter(w - 1, row, '│')\n        }\n    }\n\n    private fun drawTitle(g: TextGraphics, width: Int) {\n        val title = \" Exploration Engine \"\n        val pad = maxOf(width - title.length, 0) / 2\n        g.enableModifiers(SGR.BOLD)\n        g.foregroundColor = TextColor.ANSI.CYAN\n        g.putString(pad, 0, title)\n        g.disableModifiers(SGR.BOLD)\n    }\n\n    private fun drawPanelHeaders(g: TextGraphics, leftWidth: Int, rightWidth: Int, splitCol: Int) {\n        val msgHeader = \" Messages\"\n        g.enableModifiers(SGR.BOLD)\n        g.foregroundColor = TextColor.ANSI.WHITE\n        g.putString(1, 1, msgHeader)\n\n        val statusHeader = \" Statuses\"\n        val rightStart = splitCol + 1\n        g.putString(rightStart, 1, statusHeader)\n        g.disableModifiers(SGR.BOLD)\n\n        for (col in 1 until splitCol) {\n            if (col < 1 + leftWidth && col > 0) {\n                g.setCharacter(col, 2, '─')\n            }\n        }\n        g.setCharacter(splitCol, 2, '│')\n        for (col in splitCol + 1 until splitCol + rightWidth) {\n            if (col < 1 + leftWidth + rightWidth - 1) {\n                g.setCharacter(col, 2, '─')\n            }\n        }\n    }\n\n    private fun drawSeparatorLine(g: TextGraphics, w: Int, splitCol: Int, row: Int) {\n        for (col in 0 until w) {\n            if (col == splitCol) g.setCharacter(col, row, '┼')\n            else g.setCharacter(col, row, '─')\n        }\n    }\n\n    private fun drawVerticalSeparator(g: TextGraphics, col: Int, startRow: Int, count: Int) {\n        for (row in startRow until startRow + count) {\n            if (row > 0 && row < count + startRow - 1) {\n                g.setCharacter(col, row, '│')\n            }\n        }\n    }\n\n    private fun drawSplitSeparator(g: TextGraphics, w: Int, splitCol: Int, row: Int) {\n        for (col in 0 until w) {\n            if (col == splitCol) g.setCharacter(col, row, '┼')\n            else g.setCharacter(col, row, '─')\n        }\n    }\n\n    private fun drawMessagesPanel(\n        g: TextGraphics,\n        history: List<HistoryEntry>,\n        top: TerminalPosition,\n        width: Int,\n        maxRows: Int\n    ) {\n        val lines = mutableListOf<Pair<String, Boolean>>()\n        for (entry in history) {\n            for (segment in entry.text.split(\"\\n\")) {\n                wrapText(segment, width).forEach { lines.add(it to entry.isTrigger) }\n            }\n        }\n\n        val skip = maxOf(0, lines.size - maxRows)\n        var row = top.row\n        for (i in skip until lines.size) {\n            if (row >= top.row + maxRows) break\n            val (lineText, isTrigger) = lines[i]\n            g.foregroundColor = if (isTrigger) TextColor.ANSI.BLUE else TextColor.ANSI.DEFAULT\n            g.putString(top.column, row, lineText.padEnd(width))\n            row++\n        }\n        g.foregroundColor = TextColor.ANSI.DEFAULT\n    }\n\n    private fun drawStatusesPanel(\n        g: TextGraphics,\n        v: ViewData,\n        top: TerminalPosition,\n        width: Int,\n        maxRows: Int\n    ) {\n        val hpLine = buildHpBar(v, width)\n        g.putString(top.column, top.row, hpLine.padEnd(width))\n\n        val progLine = \" Exp: ${v.exploredCount}/${v.totalAreas}   Dev: ${v.activatedCount}/${v.totalDevices}\"\n        g.putString(top.column, top.row + 1, progLine.padEnd(width))\n\n        var row = top.row + 2\n\n        val statuses = v.statuses.filterValues { it != 0 }\n        for ((name, value) in statuses) {\n            if (row >= top.row + maxRows) break\n            val range = v.statusBounds[name]\n            g.putString(top.column, row, formatStatus(name, value, range, width).padEnd(width))\n            row++\n        }\n\n        if (v.equippedItems.isEmpty()) return\n\n        if (row < top.row + maxRows && statuses.isNotEmpty()) {\n            g.putString(top.column, row, \" \".repeat(width))\n            row++\n        }\n\n        for ((i, item) in v.equippedItems.withIndex()) {\n            if (row >= top.row + maxRows) break\n            val label = \"@ ${item.name}\" + if (item.locked) \" (Locked)\" else \"\"\n            g.foregroundColor = if (item.locked) TextColor.ANSI.RED else TextColor.ANSI.MAGENTA\n            g.enableModifiers(SGR.BOLD)\n            g.putString(top.column, row, label.padEnd(width))\n            g.disableModifiers(SGR.BOLD)\n            g.foregroundColor = TextColor.ANSI.DEFAULT\n            row++\n        }\n\n        g.foregroundColor = TextColor.ANSI.DEFAULT\n    }\n\n    private fun buildHpBar(v: ViewData, width: Int): String {\n        val barLen = 10\n        val filled = ((v.health.toDouble() / v.maxHealth) * barLen).toInt().coerceIn(0, barLen)\n        val empty = barLen - filled\n\n        val bar = \"█\".repeat(filled) + \"░\".repeat(empty)\n        val textPart = \"${v.health}/${v.maxHealth}\"\n        return \"$bar $textPart\"\n    }\n\n    private fun formatStatus(name: String, value: Int, range: StatusRange?, width: Int): String {\n        val maxLen = width - 10\n        val displayValue = if (range != null && range.max != Int.MAX_VALUE) {\n            \"$value/${range.max}\"\n        } else {\n            \"$value\"\n        }\n        val template = \": $displayValue\"\n        val nameMaxLen = maxOf(4, width - template.length)\n\n        val displayName = if (name.length > nameMaxLen) {\n            name.substring(0, nameMaxLen - 3) + \"...\"\n        } else {\n            name\n        }\n        return \"$displayName$template\"\n    }\n\n    private fun wrapText(text: String, width: Int): List<String> {\n        val words = text.split(\" \")\n        val result = mutableListOf<String>()\n        var line = \"\"\n\n        for (word in words) {\n            if (word.length >= width) {\n                if (line.isNotEmpty()) result.add(line)\n                line = \"\"\n                var remaining = word\n                while (remaining.isNotEmpty()) {\n                    if (remaining.length <= width) {\n                        line += remaining\n                        remaining = \"\"\n                    } else {\n                        result.add(remaining.substring(0, width))\n                        remaining = remaining.substring(width)\n                    }\n                }\n            } else if ((line + \" \" + word).length <= width) {\n                line = if (line.isEmpty()) word else \"$line $word\"\n            } else {\n                if (line.isNotEmpty()) result.add(line)\n                line = word\n            }\n        }\n        if (line.isNotEmpty()) result.add(line)\n        return result.ifEmpty { listOf(\"\") }\n    }\n\n    private fun drawBottomBar(\n        g: TextGraphics,\n        view: ViewData,\n        width: Int,\n        topRow: Int\n    ) {\n        val exits = view.exits\n        val wInfo = exits.getOrNull(0)\n        val aInfo = exits.getOrNull(1)\n        val sInfo = exits.getOrNull(2)\n        val dInfo = exits.getOrNull(3)\n\n        val wLabel = exitLabel('w', wInfo)\n        val aLabel = exitLabel('a', aInfo)\n        val sLabel = exitLabel('s', sInfo)\n        val dLabel = exitLabel('d', dInfo)\n\n        val gap = 2\n        val contentWidth = aLabel.length + gap + sLabel.length + gap + dLabel.length\n        val leftPad = (width - contentWidth) / 2\n\n        val colA = leftPad\n        val colS = colA + aLabel.length + gap\n        val colD = colS + sLabel.length + gap\n        val colW = colS - wLabel.length / 2\n\n        drawSlot(g, wLabel, TerminalPosition(colW, topRow), wInfo != null, wInfo?.blocked ?: false)\n        drawInvHint(g, width, topRow)\n        drawSlot(g, aLabel, TerminalPosition(colA, topRow + 1), aInfo != null, aInfo?.blocked ?: false)\n        drawSlot(g, sLabel, TerminalPosition(colS, topRow + 1), sInfo != null, sInfo?.blocked ?: false)\n        drawSlot(g, dLabel, TerminalPosition(colD, topRow + 1), dInfo != null, dInfo?.blocked ?: false)\n        drawActionHints(g, view, width, topRow + 2)\n    }\n\n    private fun drawInvHint(g: TextGraphics, width: Int, row: Int) {\n        val label = \"[\\$i] inv\"\n        g.foregroundColor = TextColor.ANSI.CYAN\n        g.enableModifiers(SGR.BOLD)\n        val keyPart = \"[i]\"\n        g.putString(width - 10, row, keyPart)\n        g.disableModifiers(SGR.BOLD)\n        g.foregroundColor = TextColor.ANSI.WHITE\n        g.putString(width - 7, row, \"inv\")\n    }\n\n    private fun drawActionHints(g: TextGraphics, view: ViewData, width: Int, row: Int) {\n        val hasAreaItems = view.areaItems.isNotEmpty()\n        val hasCarriedOrEquipped = view.carriedItems.isNotEmpty() || view.equippedItems.isNotEmpty()\n        val hasCarried = view.carriedItems.isNotEmpty()\n        val hasEquipped = view.equippedItems.isNotEmpty()\n\n        val gLabel = \"[g] grab\"\n        val pLabel = \"[p] drop\"\n        val eLabel = \"[e] equip\"\n        val rLabel = \"[r] uneq\"\n\n        val gap = 2\n        val totalWidth = gLabel.length + gap + pLabel.length + gap + eLabel.length + gap + rLabel.length\n        val leftPad = (width - totalWidth) / 2\n\n        var col = leftPad\n        drawActionSlot(g, gLabel, TerminalPosition(col, row), hasAreaItems)\n        col += gLabel.length + gap\n        drawActionSlot(g, pLabel, TerminalPosition(col, row), hasCarriedOrEquipped)\n        col += pLabel.length + gap\n        drawActionSlot(g, eLabel, TerminalPosition(col, row), hasCarried)\n        col += eLabel.length + gap\n        drawActionSlot(g, rLabel, TerminalPosition(col, row), hasEquipped)\n    }\n\n    private fun drawActionSlot(g: TextGraphics, label: String, pos: TerminalPosition, active: Boolean) {\n        g.foregroundColor = if (active) TextColor.ANSI.GREEN else TextColor.ANSI.DEFAULT\n        g.enableModifiers(SGR.BOLD)\n        val keyPart = label.substringBefore(']') + \"]\"\n        g.putString(pos.column, pos.row, keyPart)\n        g.disableModifiers(SGR.BOLD)\n\n        val nameStart = pos.column + keyPart.length\n        val namePart = if (label.contains(\"] \")) label.substringAfter(\"] \") else \"\"\n        g.foregroundColor = if (active) TextColor.ANSI.WHITE else TextColor.ANSI.DEFAULT\n        g.putString(nameStart, pos.row, namePart)\n\n        g.disableModifiers(SGR.BOLD)\n    }\n\n    private fun exitLabel(key: Char, info: ExitInfo?): String =\n        if (info == null || info.name.isNullOrEmpty()) \"[${key}]  .\"\n        else {\n            val suffix = if (info.blocked) \" (B)\" else \"\"\n            \"[${key}] ${info.name}$suffix\"\n        }\n\n    private fun drawSlot(g: TextGraphics, label: String, pos: TerminalPosition, active: Boolean, blocked: Boolean) {\n        g.foregroundColor = when {\n            !active -> TextColor.ANSI.DEFAULT\n            blocked -> TextColor.ANSI.DEFAULT\n            else -> TextColor.ANSI.GREEN\n        }\n        g.enableModifiers(SGR.BOLD)\n        val keyPart = label.substringBefore(']') + \"]\"\n        g.putString(pos.column, pos.row, keyPart)\n        g.disableModifiers(SGR.BOLD)\n\n        val nameStart = pos.column + keyPart.length\n        val namePart = if (label.contains(\"] \")) label.substringAfter(\"] \") else \"\"\n        g.foregroundColor = when {\n            !active -> TextColor.ANSI.DEFAULT\n            blocked -> TextColor.ANSI.DEFAULT\n            else -> TextColor.ANSI.GREEN\n        }\n        g.putString(nameStart, pos.row, namePart)\n\n        g.disableModifiers(SGR.BOLD)\n    }\n\n    private fun drawTooSmallMessage(g: TextGraphics, w: Int, h: Int) {\n        val lines = listOf(\n            \"Terminal too small.\",\n            \"Resize to at least $MIN_WIDTH columns, $MIN_HEIGHT rows.\"\n        )\n        val startRow = (h - lines.size) / 2\n        g.foregroundColor = TextColor.ANSI.YELLOW\n        g.enableModifiers(SGR.BOLD)\n        for ((i, line) in lines.withIndex()) {\n            val col = (w - line.length) / 2\n            g.putString(col, startRow + i, line)\n        }\n        g.disableModifiers(SGR.BOLD)\n    }\n\n    private fun printEndSummary(view: ViewData, history: List<HistoryEntry>) {\n        println()\n        if (view.endGameMessage != null) {\n            println(\"===== ${view.endGameMessage} =====\")\n        } else {\n            println(\"===== Game Over =====\")\n        }\n        println(\"Area  : ${view.currentAreaName}\")\n        println(\"Health: ${view.health}/${view.maxHealth}\")\n        println(\"Explored: ${view.exploredCount}/${view.totalAreas}  |  Devices: ${view.activatedCount}/${view.totalDevices}\")\n        if (view.statuses.isNotEmpty()) {\n            val statusLines = view.statuses.filterValues { it != 0 }.map { (name, value) ->\n                \"$name=$value\"\n            }\n            println(\"Statuses: ${statusLines.joinToString(\", \")}\")\n        }\n        if (history.isNotEmpty()) {\n            println(\"\\n--- Last messages ---\")\n            for (entry in history) println(entry.text)\n        }\n    }\n}\n",
  "filePath": "/var/home/mikharju/dev/exploration/engine/src/main/kotlin/exploration/adapter/ui/lanterna/LanternaUiAdapter.kt"
}
```

**Output:**
```
Wrote file successfully.
```

---

## Assistant (Build · qwen3.6-35b-a3b · 5.2s)

_Thinking:_

Wait, I introduced a bug in `initFirstView`. The function references `ref` which is not in scope — it's local to `run()`. Let me fix that. Also, the `render` function now takes named parameters but uses `screen` as parameter name which shadows the Lanterna Screen type. Let me review and fix these issues.

Looking at my changes:
1. `initFirstView(state: GameLoopState)` — calls `engine.tick(ref = state.currentView, event = InputEvent.Look)` — this is wrong. The first argument to tick should be a `GameRef`, not a `ViewData`. I accidentally swapped the parameter names.

Let me fix these issues.






**Tool: read**

**Input:**
```json
{
  "filePath": "/var/home/mikharju/dev/exploration/engine/src/main/kotlin/exploration/adapter/ui/lanterna/LanternaUiAdapter.kt",
  "limit": 120
}
```

**Output:**
```
<path>/var/home/mikharju/dev/exploration/engine/src/main/kotlin/exploration/adapter/ui/lanterna/LanternaUiAdapter.kt</path>
<type>file</type>
<content>
1: package exploration.adapter.ui.lanterna
2: 
3: import com.googlecode.lanterna.SGR
4: import com.googlecode.lanterna.TerminalPosition
5: import com.googlecode.lanterna.TextColor
6: import com.googlecode.lanterna.graphics.TextGraphics
7: import com.googlecode.lanterna.input.KeyType
8: import com.googlecode.lanterna.screen.Screen
9: import com.googlecode.lanterna.screen.TerminalScreen
10: import com.googlecode.lanterna.terminal.DefaultTerminalFactory
11: import exploration.model.StatusRange
12: import exploration.port.ExitInfo
13: import exploration.port.GameEngine
14: import exploration.port.GameRef
15: import exploration.port.InputEvent
16: import exploration.port.ItemView
17: import exploration.port.ViewData
18: 
19: class LanternaUiAdapter(private val engine: GameEngine) {
20: 
21:     data class HistoryEntry(val text: String, val isTrigger: Boolean)
22: 
23:     sealed interface Overlay {
24:         object None : Overlay
25:         data class MessageViewer(
26:             val messages: List<String>,
27:             var scrollOffset: Int = 0,
28:             val maxHeight: Int = 20,
29:             var focusedIndex: Int = 0
30:         ) : Overlay {
31: 
32:             fun overlayTextAreaMaxWidth(screen: Screen): Int =
33:                 minOf(170, maxOf(40, screen.terminalSize.columns - 10)) - 2
34: 
35:             fun overlayTextAreaMaxHeight(screen: Screen): Int =
36:                 (this.maxHeight + 3).coerceAtMost(screen.terminalSize.rows - 6)
37:         }
38: 
39:     }
40: 
41:     companion object {
42:         private const val MAX_MESSAGES = 10
43:         private const val MIN_WIDTH = 64
44:         private const val MIN_HEIGHT = 23
45:     }
46: 
47:     private enum class SelectionTarget { TAKE, DROP, EQUIP, UNEQUIP }
48: 
49:     private data class SelectionState(val target: SelectionTarget, val items: List<ItemView>)
50: 
51:     private data class GameLoopState(
52:         var history: MutableList<HistoryEntry>,
53:         var currentView: ViewData,
54:         var shownTriggers: Int = 0,
55:         var storedStoryCount: Int = 0,
56:         var selectionState: SelectionState? = null,
57:         var overlay: Overlay = Overlay.None
58:     )
59: 
60:     fun run(scenarioId: String) {
61:         val ref = startEngine(scenarioId) ?: return
62:         val screen = setupScreen()
63: 
64:         addMessage(screen.history, "=== Exploration Engine (Lanterna) ===", false)
65:         addMessage(
66:             screen.history,
67:             "arrows/wasd: move | l: look | u: activate | g: grab | p: drop | e: equip | r: uneq | j: stories | h: help | q/esc: quit",
68:             false
69:         )
70: 
71:         initFirstView(screen)
72:         gameLoop(screen, ref)
73:         endGameScreen(screen)
74: 
75:         screen.stopScreen()
76:         printEndSummary(screen.currentView, screen.history)
77:     }
78: 
79:     private fun startEngine(scenarioId: String): GameRef? {
80:         try {
81:             return engine.start(scenarioId)
82:         } catch (e: Exception) {
83:             println("Error loading scenario '$scenarioId': ${e.message}")
84:             e.printStackTrace()
85:             return null
86:         }
87:     }
88: 
89:     private fun setupScreen(): Screen = with(TerminalScreen(DefaultTerminalFactory().createTerminal())) {
90:         stopScreen()
91:         startScreen()
92:         cursorPosition = null
93:         this@setupScreen
94:     }
95: 
96:     private fun initFirstView(state: GameLoopState) {
97:         state.currentView = engine.tick(ref = state.currentView, event = InputEvent.Look)
98:         render(screen = state.currentView, history = state.history, overlay = state.overlay)
99: 
100:         val initialStoryCount = state.currentView.storyMessages.size
101:         state.storedStoryCount = initialStoryCount
102:         if (initialStoryCount > 0) {
103:             val subList = state.currentView.storyMessages.subList(0, initialStoryCount)
104:             state.overlay = Overlay.MessageViewer(subList, focusedIndex = subList.lastIndex)
105:             render(screen = state.currentView, history = state.history, overlay = state.overlay)
106:         }
107:     }
108: 
109:     private fun gameLoop(state: GameLoopState, ref: GameRef) {
110:         while (state.currentView.endGameMessage == null) {
111:             screen.doResizeIfNecessary()
112:             val result = readInputKey(screen = state.screen(), view = state.currentView, selState = state.selectionState, currentOverlay = state.overlay)
113:             state.selectionState = result.selectionState
114:             state.overlay = result.overlay
115: 
116:             when (result.action) {
117:                 is KeyAction.Quit -> break
118:                 is KeyAction.Help -> {
119:                     addMessage(state.history, "arrows/wasd: move | l: look | u: activate | g: grab | p: drop | e: equip | r: uneq | j: stories | h: help | q/esc: quit", false)
120:                     render(screen = state.currentView, history = state.history, overlay = state.overlay)

(Showing lines 1-120 of 819. Use offset=121 to continue.)
</content>
```

---

## Assistant (Build · qwen3.6-35b-a3b · 223.2s)

_Thinking:_

I see several issues in my refactoring:

1. Line 64: `screen.history` — `screen` here is a `Screen` (Lanterna), not the state. The variable name collision with `screen` (Lanterna Screen type) and the new state holder is confusing. I named it `screen` for both the Lanterna screen and the GameLoopState, which creates ambiguity.

2. Line 97: `engine.tick(ref = state.currentView, event = InputEvent.Look)` — wrong! The first parameter should be a `GameRef`, not `ViewData`. I accidentally swapped the named parameters.

3. Line 111: `screen.doResizeIfNecessary()` — `screen` is not in scope here. Need to use a different approach.

4. Line 112: `state.screen()` — there's no such method on GameLoopState.

Let me rewrite this properly. I need to:
- Rename the state holder variable from `screen` to something else (like `state`)
- Fix the tick call in initFirstView 
- Add a screen property to GameLoopState or pass it separately
- Make sure render calls are correct

Actually, looking at the original code more carefully:

Original structure:
```kotlin
fun run(scenarioId: String) {
    var ref: GameRef
    try { ref = engine.start(scenarioId) } catch...
    
    val screen = TerminalScreen(...)  // Lanterna Screen
    ...
    
    val history = mutableListOf<HistoryEntry>()
    var shownTriggers = 0
    var selectionState: SelectionState? = null
    var overlay: Overlay = Overlay.None
    var storedStoryCount = 0
    
    addMessage(history, ...)
    
    var currentView = engine.tick(ref, InputEvent.Look)
    render(screen, currentView, history, overlay)
    
    while (currentView.endGameMessage == null) {
        screen.doResizeIfNecessary()
        val result = readInputKey(screen, currentView, selectionState, overlay)
        ...
        is KeyAction.Event -> {
            val next = engine.tick(ref, result.action.event)
            currentView = next
            ...
        }
    }
    
    screen.stopScreen()
    printEndSummary(currentView, history)
}
```

So the state that needs to be tracked across extracted methods is:
- `history` (mutable list)
- `currentView` (ViewData)
- `shownTriggers` (Int)
- `storedStoryCount` (Int)
- `selectionState` (SelectionState?)
- `overlay` (Overlay)

And the Lanterna Screen is passed separately. Let me rename things properly and fix all bugs:

1. Rename the state holder variable from `screen` to just use a different name like `loopState`
2. Add `lanternScreen` as a field in GameLoopState or pass it separately  
3. Fix initFirstView to not reference `ref` (pass it as parameter)
4. Make sure render calls match the actual method signature

Let me rewrite this properly.




I see several bugs introduced by my changes — variable name collisions, wrong parameter types, and missing scope references. Let me fix them:



**Tool: write**

**Input:**
```json
{
  "content": "package exploration.adapter.ui.lanterna\n\nimport com.googlecode.lanterna.SGR\nimport com.googlecode.lanterna.TerminalPosition\nimport com.googlecode.lanterna.TextColor\nimport com.googlecode.lanterna.graphics.TextGraphics\nimport com.googlecode.lanterna.input.KeyType\nimport com.googlecode.lanterna.screen.Screen\nimport com.googlecode.lanterna.screen.TerminalScreen\nimport com.googlecode.lanterna.terminal.DefaultTerminalFactory\nimport exploration.model.StatusRange\nimport exploration.port.ExitInfo\nimport exploration.port.GameEngine\nimport exploration.port.GameRef\nimport exploration.port.InputEvent\nimport exploration.port.ItemView\nimport exploration.port.ViewData\n\nclass LanternaUiAdapter(private val engine: GameEngine) {\n\n    data class HistoryEntry(val text: String, val isTrigger: Boolean)\n\n    sealed interface Overlay {\n        object None : Overlay\n        data class MessageViewer(\n            val messages: List<String>,\n            var scrollOffset: Int = 0,\n            val maxHeight: Int = 20,\n            var focusedIndex: Int = 0\n        ) : Overlay {\n\n            fun overlayTextAreaMaxWidth(screen: Screen): Int =\n                minOf(170, maxOf(40, screen.terminalSize.columns - 10)) - 2\n\n            fun overlayTextAreaMaxHeight(screen: Screen): Int =\n                (this.maxHeight + 3).coerceAtMost(screen.terminalSize.rows - 6)\n        }\n\n    }\n\n    companion object {\n        private const val MAX_MESSAGES = 10\n        private const val MIN_WIDTH = 64\n        private const val MIN_HEIGHT = 23\n    }\n\n    private enum class SelectionTarget { TAKE, DROP, EQUIP, UNEQUIP }\n\n    private data class SelectionState(val target: SelectionTarget, val items: List<ItemView>)\n\n    private data class LoopState(\n        var history: MutableList<HistoryEntry>,\n        var currentView: ViewData,\n        var shownTriggers: Int = 0,\n        var storedStoryCount: Int = 0,\n        var selectionState: SelectionState? = null,\n        var overlay: Overlay = Overlay.None\n    )\n\n    fun run(scenarioId: String) {\n        val ref = startEngine(scenarioId) ?: return\n        val screen = setupScreen()\n\n        val state = LoopState(\n            history = mutableListOf(),\n            currentView = engine.tick(ref, InputEvent.Look),\n        )\n\n        addMessage(state.history, \"=== Exploration Engine (Lanterna) ===\", false)\n        addMessage(\n            state.history,\n            \"arrows/wasd: move | l: look | u: activate | g: grab | p: drop | e: equip | r: uneq | j: stories | h: help | q/esc: quit\",\n            false\n        )\n\n        initFirstView(screen, state)\n        gameLoop(screen, ref, state)\n        endGameScreen(screen, state)\n\n        screen.stopScreen()\n        printEndSummary(state.currentView, state.history)\n    }\n\n    private fun startEngine(scenarioId: String): GameRef? {\n        try {\n            return engine.start(scenarioId)\n        } catch (e: Exception) {\n            println(\"Error loading scenario '$scenarioId': ${e.message}\")\n            e.printStackTrace()\n            return null\n        }\n    }\n\n    private fun setupScreen(): Screen = with(TerminalScreen(DefaultTerminalFactory().createTerminal())) {\n        stopScreen()\n        startScreen()\n        cursorPosition = null\n        this@setupScreen\n    }\n\n    private fun initFirstView(screen: Screen, state: LoopState) {\n        render(screen, state.currentView, state.history, state.overlay)\n\n        val initialStoryCount = state.currentView.storyMessages.size\n        state.storedStoryCount = initialStoryCount\n        if (initialStoryCount > 0) {\n            val subList = state.currentView.storyMessages.subList(0, initialStoryCount)\n            state.overlay = Overlay.MessageViewer(subList, focusedIndex = subList.lastIndex)\n            render(screen, state.currentView, state.history, state.overlay)\n        }\n    }\n\n    private fun gameLoop(screen: Screen, ref: GameRef, state: LoopState) {\n        while (state.currentView.endGameMessage == null) {\n            screen.doResizeIfNecessary()\n            val result = readInputKey(screen, state.currentView, state.selectionState, state.overlay)\n            state.selectionState = result.selectionState\n            state.overlay = result.overlay\n\n            when (result.action) {\n                is KeyAction.Quit -> break\n                is KeyAction.Help -> {\n                    addMessage(state.history, \"arrows/wasd: move | l: look | u: activate | g: grab | p: drop | e: equip | r: uneq | j: stories | h: help | q/esc: quit\", false)\n                    render(screen, state.currentView, state.history, state.overlay)\n                }\n                is KeyAction.NoOp -> {\n                    render(screen, state.currentView, state.history, state.overlay)\n                }\n                is KeyAction.WaitSelection -> {\n                    val sel = state.selectionState!!\n                    addMessage(state.history, buildSelectionPrompt(sel.target, sel.items), false)\n                    render(screen, state.currentView, state.history, state.overlay)\n                }\n                is KeyAction.Event -> {\n                    if (state.overlay != Overlay.None) state.overlay = Overlay.None\n                    val next = engine.tick(ref, result.action.event)\n\n                    state.currentView = next\n                    if (next.commandText.isNotBlank()) addMessage(state.history, next.commandText, false)\n                    appendTriggerMessages(screen, ref, state)\n\n                    if (next.storyMessages.size > state.storedStoryCount) {\n                        val subList = next.storyMessages.subList(state.storedStoryCount, next.storyMessages.size)\n                        state.overlay = Overlay.MessageViewer(subList, focusedIndex = subList.lastIndex)\n                        state.storedStoryCount = next.storyMessages.size\n                    } else {\n                        state.storedStoryCount = next.storyMessages.size\n                    }\n\n                    state.shownTriggers = next.triggerTexts.size\n                    render(screen, state.currentView, state.history, state.overlay)\n                }\n            }\n        }\n    }\n\n    private fun appendTriggerMessages(screen: Screen, ref: GameRef, state: LoopState) {\n        // No-op placeholder — trigger messages are already handled in gameLoop\n    }\n\n    private fun endGameScreen(screen: Screen, state: LoopState) {\n        for (i in state.shownTriggers until state.currentView.triggerTexts.size) {\n            if (state.currentView.triggerTexts[i].isNotBlank()) addMessage(state.history, state.currentView.triggerTexts[i], true)\n        }\n        state.storedStoryCount = state.currentView.storyMessages.size\n        if (state.currentView.endGameMessage != null) {\n            addMessage(state.history, \"\", false)\n            addMessage(state.history, state.currentView.endGameMessage, false)\n        } else if (state.currentView.outputLine.isNotBlank()) {\n            addMessage(state.history, state.currentView.outputLine, false)\n        }\n        screen.doResizeIfNecessary()\n        render(screen, state.currentView, state.history, state.overlay)\n    }\n\n    private sealed class KeyAction {\n        object Quit : KeyAction()\n        object Help : KeyAction()\n        object WaitSelection : KeyAction()\n        object NoOp : KeyAction()\n        data class Event(val event: InputEvent) : KeyAction()\n    }\n\n    private data class ReadInputResult(val action: KeyAction, val selectionState: SelectionState?, val overlay: Overlay)\n\n    private fun readInputKey(\n        screen: Screen,\n        view: ViewData,\n        selState: SelectionState?,\n        currentOverlay: Overlay\n    ): ReadInputResult {\n        val key = screen.readInput() ?: return ReadInputResult(KeyAction.Quit, null, currentOverlay)\n\n        if (selState != null) {\n            return handleSelectionDigit(key, selState, currentOverlay)\n        }\n\n        var nextOverlay = currentOverlay\n        var handledInOverlay: Boolean\n\n        when (currentOverlay) {\n            is Overlay.MessageViewer -> {\n                val ov = currentOverlay\n                val totalWrappedLines = countWrappedDisplayLines(ov.messages, ov.overlayTextAreaMaxWidth(screen))\n                val visibleRows = ov.overlayTextAreaMaxHeight(screen) - 3\n                val maxScroll = maxOf(0, totalWrappedLines - visibleRows + 3)\n                handledInOverlay = when (key.keyType) {\n                    KeyType.PageDown -> {\n                        nextOverlay = ov.copy(scrollOffset = minOf(maxScroll, ov.scrollOffset + maxOf(1, ov.overlayTextAreaMaxHeight(screen) - 2)))\n                        true\n                    }\n                    KeyType.ArrowDown -> {\n                        nextOverlay = ov.copy(scrollOffset = minOf(maxScroll, ov.scrollOffset + 1))\n                        true\n                    }\n                    KeyType.PageUp -> {\n                        nextOverlay = ov.copy(scrollOffset = maxOf(0, ov.scrollOffset - maxOf(1, ov.maxHeight - 2)))\n                        true\n                    }\n                    KeyType.ArrowUp -> {\n                        nextOverlay = ov.copy(scrollOffset = maxOf(0, ov.scrollOffset - 1))\n                        true\n                    }\n                    KeyType.ArrowLeft -> {\n                        if (ov.focusedIndex > 0)\n                            nextOverlay = ov.copy(focusedIndex = ov.focusedIndex - 1, scrollOffset = 0)\n                        true\n                    }\n                    KeyType.ArrowRight -> {\n                        if (ov.focusedIndex < ov.messages.size - 1)\n                            nextOverlay = ov.copy(focusedIndex = ov.focusedIndex + 1, scrollOffset = 0)\n                        true\n                    }\n                    KeyType.Escape -> {\n                        nextOverlay = Overlay.None\n                        true\n                    }\n                    else -> false\n                }\n            }\n            Overlay.None -> handledInOverlay = false\n        }\n\n        if (handledInOverlay) return ReadInputResult(KeyAction.NoOp, null, nextOverlay)\n\n        val displayOverlay = if (currentOverlay !is Overlay.None && key.keyType != KeyType.Escape) Overlay.None else currentOverlay\n\n        when (key.keyType) {\n            KeyType.Escape -> return if (currentOverlay !is Overlay.None) {\n                ReadInputResult(KeyAction.NoOp, null, Overlay.None)\n            } else {\n                ReadInputResult(KeyAction.Quit, null, currentOverlay)\n            }\n            KeyType.ArrowUp -> return ReadInputResult(KeyAction.Event(InputEvent.MoveDirection(InputEvent.Direction.North)), null, displayOverlay)\n            KeyType.ArrowDown -> return ReadInputResult(KeyAction.Event(InputEvent.MoveDirection(InputEvent.Direction.South)), null, displayOverlay)\n            KeyType.ArrowLeft -> return ReadInputResult(KeyAction.Event(InputEvent.MoveDirection(InputEvent.Direction.West)), null, displayOverlay)\n            KeyType.ArrowRight -> return ReadInputResult(KeyAction.Event(InputEvent.MoveDirection(InputEvent.Direction.East)), null, displayOverlay)\n            KeyType.Character -> {\n                when (key.character.lowercaseChar()) {\n                    'w' -> return ReadInputResult(KeyAction.Event(InputEvent.MoveDirection(InputEvent.Direction.North)), null, displayOverlay)\n                    'a' -> return ReadInputResult(KeyAction.Event(InputEvent.MoveDirection(InputEvent.Direction.West)), null, displayOverlay)\n                    's' -> return ReadInputResult(KeyAction.Event(InputEvent.MoveDirection(InputEvent.Direction.South)), null, displayOverlay)\n                    'd' -> return ReadInputResult(KeyAction.Event(InputEvent.MoveDirection(InputEvent.Direction.East)), null, displayOverlay)\n                    'l' -> return ReadInputResult(KeyAction.Event(InputEvent.Look), null, displayOverlay)\n                    'u' -> return ReadInputResult(KeyAction.Event(InputEvent.Activate), null, displayOverlay)\n                    'g' -> {\n                        val candidates = view.areaItems\n                        return if (candidates.isEmpty()) ReadInputResult(KeyAction.Help, null, displayOverlay)\n                        else if (candidates.size == 1) ReadInputResult(KeyAction.Event(InputEvent.TakeItem(candidates[0].name)), null, displayOverlay)\n                        else ReadInputResult(KeyAction.WaitSelection, SelectionState(SelectionTarget.TAKE, candidates), displayOverlay)\n                    }\n                    'p' -> {\n                        val candidates = view.carriedItems + view.equippedItems\n                        return if (candidates.isEmpty()) ReadInputResult(KeyAction.Help, null, displayOverlay)\n                        else if (candidates.size == 1) ReadInputResult(KeyAction.Event(InputEvent.DropItem(candidates[0].name)), null, displayOverlay)\n                        else ReadInputResult(KeyAction.WaitSelection, SelectionState(SelectionTarget.DROP, candidates), displayOverlay)\n                    }\n                    'e' -> {\n                        val candidates = view.carriedItems\n                        return if (candidates.isEmpty()) ReadInputResult(KeyAction.Help, null, displayOverlay)\n                        else if (candidates.size == 1) ReadInputResult(KeyAction.Event(InputEvent.EquipItem(candidates[0].name)), null, displayOverlay)\n                        else ReadInputResult(KeyAction.WaitSelection, SelectionState(SelectionTarget.EQUIP, candidates), displayOverlay)\n                    }\n                    'r' -> {\n                        val candidates = view.equippedItems\n                        return if (candidates.isEmpty()) ReadInputResult(KeyAction.Help, null, displayOverlay)\n                        else if (candidates.size == 1) ReadInputResult(KeyAction.Event(InputEvent.UnequipItem(candidates[0].name)), null, displayOverlay)\n                        else ReadInputResult(KeyAction.WaitSelection, SelectionState(SelectionTarget.UNEQUIP, candidates), displayOverlay)\n                    }\n                    'i' -> return ReadInputResult(KeyAction.Event(InputEvent.Inventory), null, displayOverlay)\n                    'j' -> {\n                        val msgs = view.storyMessages.filter { it.isNotBlank() }\n                        return if (msgs.isEmpty()) ReadInputResult(KeyAction.Help, null, displayOverlay)\n                        else ReadInputResult(KeyAction.NoOp, null, Overlay.MessageViewer(msgs, focusedIndex = msgs.lastIndex))\n                    }\n                    'h' -> return ReadInputResult(KeyAction.NoOp, null, displayOverlay)\n                    'q' -> return ReadInputResult(KeyAction.Quit, null, displayOverlay)\n                }\n            }\n            else -> {}\n        }\n        return ReadInputResult(KeyAction.Help, null, currentOverlay)\n    }\n\n    private fun handleSelectionDigit(key: com.googlecode.lanterna.input.KeyStroke, selState: SelectionState, overlay: Overlay): ReadInputResult {\n        if (key.keyType == KeyType.Escape) return ReadInputResult(KeyAction.Help, null, overlay)\n        if (key.keyType != KeyType.Character) return ReadInputResult(KeyAction.WaitSelection, selState, overlay)\n        val idx = key.character.digitToIntOrNull() ?: return ReadInputResult(KeyAction.WaitSelection, selState, overlay)\n        val itemIdx = idx - 1\n        if (itemIdx < 0 || itemIdx >= selState.items.size) return ReadInputResult(KeyAction.Help, null, overlay)\n        val item = selState.items[itemIdx]\n        val event = when (selState.target) {\n            SelectionTarget.TAKE -> InputEvent.TakeItem(item.name)\n            SelectionTarget.DROP -> InputEvent.DropItem(item.name)\n            SelectionTarget.EQUIP -> InputEvent.EquipItem(item.name)\n            SelectionTarget.UNEQUIP -> InputEvent.UnequipItem(item.name)\n        }\n        return ReadInputResult(KeyAction.Event(event), null, overlay)\n    }\n\n    private fun countWrappedDisplayLines(messages: List<String>, lineWidth: Int): Int {\n        var total = 0\n        for (msg in messages) {\n            for (line in msg.split(\"\\n\")) {\n                val wrappedLines = wrapText(line, lineWidth)\n                total += wrappedLines.size\n            }\n        }\n        return total\n    }\n\n    private fun buildSelectionPrompt(target: SelectionTarget, items: List<ItemView>): String {\n        val verb = when (target) {\n            SelectionTarget.TAKE -> \"Take\"\n            SelectionTarget.DROP -> \"Drop\"\n            SelectionTarget.EQUIP -> \"Equip\"\n            SelectionTarget.UNEQUIP -> \"Unequip\"\n        }\n        val options = items.mapIndexed { i, it -> \"[${i + 1}] ${it.name}\" }.joinToString(\" \")\n        return \"$verb what? $options\"\n    }\n\n    private fun addMessage(history: MutableList<HistoryEntry>, msg: String, isTrigger: Boolean) {\n        if (msg.isBlank()) return\n        history.add(HistoryEntry(msg, isTrigger))\n        while (history.size > MAX_MESSAGES) history.removeAt(0)\n    }\n\n    private fun render(screen: Screen, view: ViewData, history: List<HistoryEntry>, overlay: Overlay) {\n        screen.clear()\n        val g = screen.newTextGraphics()\n        val size = screen.terminalSize\n        val w = size.columns\n        val h = size.rows\n\n        if (h < MIN_HEIGHT || w < MIN_WIDTH) {\n            drawBorder(g, w, h)\n            drawTooSmallMessage(g, w, h)\n            screen.refresh()\n            return\n        }\n\n        // Draw overlay if active\n        when (overlay) {\n            is Overlay.MessageViewer -> {\n                drawMessageOverlay(screen, g, view, overlay, size)\n                screen.refresh()\n                return\n            }\n            else -> {}\n        }\n\n        val leftWidth = ((w - 3) * 0.6).toInt()\n        val rightWidth = (w - 3) - leftWidth\n        val splitCol = 1 + leftWidth\n        val panelStartRow = 3\n\n        drawBorder(g, w, h)\n        drawTitle(g, w)\n        drawPanelHeaders(g, leftWidth, rightWidth, splitCol)\n        drawSeparatorLine(g, w, splitCol, panelStartRow - 1)\n        drawMessagesPanel(g, history, TerminalPosition(1, panelStartRow), leftWidth, h - panelStartRow - 4)\n        drawStatusesPanel(g, view, TerminalPosition(splitCol + 1, panelStartRow), rightWidth - 1, h - panelStartRow - 4)\n        drawVerticalSeparator(g, splitCol, 2, h - 5)\n        drawSplitSeparator(g, w, splitCol, h - 5)\n        drawBottomBar(g, view, w - 2, h - 5)\n\n        screen.refresh()\n    }\n\n    private fun drawMessageOverlay(\n        screen: Screen,\n        g: TextGraphics,\n        view: ViewData,\n        overlay: Overlay.MessageViewer,\n        size: com.googlecode.lanterna.TerminalSize\n    ) {\n        val w = size.columns\n        val h = size.rows\n        val panelW = minOf(170, maxOf(40, w - 10))\n        val panelH = overlay.maxHeight.coerceAtMost(h - 6)\n        val startCol = (w - panelW) / 2 + 1\n        val startRow = (h - panelH) / 2\n        val textAreaWidth = overlay.overlayTextAreaMaxWidth(screen)\n\n        drawBorder(g, startCol - 1, startRow - 1, startCol + panelW, startRow + panelH + 1)\n\n        val title = \" Story Message\"\n        g.foregroundColor = TextColor.ANSI.CYAN\n        g.enableModifiers(SGR.BOLD)\n        g.putString(startCol, startRow, title)\n        g.disableModifiers(SGR.BOLD)\n        g.foregroundColor = TextColor.ANSI.DEFAULT\n\n        val contentTop = startRow + 2\n        val focusedMsg = overlay.messages.getOrElse(overlay.focusedIndex) { \"\" }\n        var rowIdx = 0\n        for (paragraph in focusedMsg.split(\"\\n\")) {\n            if (paragraph.isBlank()) {\n                if (rowIdx < overlay.scrollOffset) { rowIdx++; continue }\n                if (rowIdx >= overlay.scrollOffset + panelH - 3) break\n                val drawRow = contentTop + (rowIdx - overlay.scrollOffset)\n                if (drawRow < startRow + panelH - 1) {\n                    g.putString(startCol, drawRow, \"\".padEnd(textAreaWidth))\n                }\n                rowIdx++\n            } else {\n                for (line in wrapText(paragraph, textAreaWidth)) {\n                    if (rowIdx < overlay.scrollOffset) { rowIdx++; continue }\n                    if (rowIdx >= overlay.scrollOffset + panelH - 3) break\n                    val drawRow = contentTop + (rowIdx - overlay.scrollOffset)\n                    if (drawRow < startRow + panelH - 1) {\n                        g.putString(startCol, drawRow, line.padEnd(textAreaWidth))\n                    }\n                    rowIdx++\n                }\n            }\n        }\n\n        val totalCount = countWrappedDisplayLines(listOf(focusedMsg), textAreaWidth)\n        val canScrollUp = overlay.scrollOffset > 0\n        val canScrollDown = (panelH - 3 > 0) && (overlay.scrollOffset + panelH - 3 < totalCount)\n        g.putString(startCol, startRow + panelH - 1, buildString {\n            append(\" [PgUp:up PgDn:down \")\n            if (canScrollUp || canScrollDown) append(\" | \")\n            if (canScrollUp) append(\"scroll↑ \")\n            if (canScrollDown) append(\"scroll↓ \")\n            append(\"| ${overlay.focusedIndex + 1} / ${overlay.messages.size} esc:close]\")\n        })\n\n        g.foregroundColor = TextColor.ANSI.DEFAULT\n    }\n\n\n    private fun drawBorder(g: TextGraphics, left: Int, top: Int, right: Int, bottom: Int) {\n        g.setCharacter(left, top, '┌')\n        g.setCharacter(right - 1, top, '┐')\n        g.setCharacter(left, bottom - 1, '└')\n        g.setCharacter(right - 1, bottom - 1, '┘')\n        for (col in left + 1 until right - 1) {\n            g.setCharacter(col, top, '─')\n            g.setCharacter(col, bottom - 1, '─')\n        }\n        for (row in top + 1 until bottom - 1) {\n            g.setCharacter(left, row, '│')\n            g.setCharacter(right - 1, row, '│')\n        }\n    }\n\n    private fun drawBorder(g: TextGraphics, w: Int, h: Int) {\n        g.setCharacter(0, 0, '┌')\n        g.setCharacter(w - 1, 0, '┐')\n        g.setCharacter(0, h - 1, '└')\n        g.setCharacter(w - 1, h - 1, '┘')\n        for (col in 1 until w - 1) {\n            g.setCharacter(col, 0, '─')\n            g.setCharacter(col, h - 1, '─')\n        }\n        for (row in 1 until h - 1) {\n            g.setCharacter(0, row, '│')\n            g.setCharacter(w - 1, row, '│')\n        }\n    }\n\n    private fun drawTitle(g: TextGraphics, width: Int) {\n        val title = \" Exploration Engine \"\n        val pad = maxOf(width - title.length, 0) / 2\n        g.enableModifiers(SGR.BOLD)\n        g.foregroundColor = TextColor.ANSI.CYAN\n        g.putString(pad, 0, title)\n        g.disableModifiers(SGR.BOLD)\n    }\n\n    private fun drawPanelHeaders(g: TextGraphics, leftWidth: Int, rightWidth: Int, splitCol: Int) {\n        val msgHeader = \" Messages\"\n        g.enableModifiers(SGR.BOLD)\n        g.foregroundColor = TextColor.ANSI.WHITE\n        g.putString(1, 1, msgHeader)\n\n        val statusHeader = \" Statuses\"\n        val rightStart = splitCol + 1\n        g.putString(rightStart, 1, statusHeader)\n        g.disableModifiers(SGR.BOLD)\n\n        for (col in 1 until splitCol) {\n            if (col < 1 + leftWidth && col > 0) {\n                g.setCharacter(col, 2, '─')\n            }\n        }\n        g.setCharacter(splitCol, 2, '│')\n        for (col in splitCol + 1 until splitCol + rightWidth) {\n            if (col < 1 + leftWidth + rightWidth - 1) {\n                g.setCharacter(col, 2, '─')\n            }\n        }\n    }\n\n    private fun drawSeparatorLine(g: TextGraphics, w: Int, splitCol: Int, row: Int) {\n        for (col in 0 until w) {\n            if (col == splitCol) g.setCharacter(col, row, '┼')\n            else g.setCharacter(col, row, '─')\n        }\n    }\n\n    private fun drawVerticalSeparator(g: TextGraphics, col: Int, startRow: Int, count: Int) {\n        for (row in startRow until startRow + count) {\n            if (row > 0 && row < count + startRow - 1) {\n                g.setCharacter(col, row, '│')\n            }\n        }\n    }\n\n    private fun drawSplitSeparator(g: TextGraphics, w: Int, splitCol: Int, row: Int) {\n        for (col in 0 until w) {\n            if (col == splitCol) g.setCharacter(col, row, '┼')\n            else g.setCharacter(col, row, '─')\n        }\n    }\n\n    private fun drawMessagesPanel(\n        g: TextGraphics,\n        history: List<HistoryEntry>,\n        top: TerminalPosition,\n        width: Int,\n        maxRows: Int\n    ) {\n        val lines = mutableListOf<Pair<String, Boolean>>()\n        for (entry in history) {\n            for (segment in entry.text.split(\"\\n\")) {\n                wrapText(segment, width).forEach { lines.add(it to entry.isTrigger) }\n            }\n        }\n\n        val skip = maxOf(0, lines.size - maxRows)\n        var row = top.row\n        for (i in skip until lines.size) {\n            if (row >= top.row + maxRows) break\n            val (lineText, isTrigger) = lines[i]\n            g.foregroundColor = if (isTrigger) TextColor.ANSI.BLUE else TextColor.ANSI.DEFAULT\n            g.putString(top.column, row, lineText.padEnd(width))\n            row++\n        }\n        g.foregroundColor = TextColor.ANSI.DEFAULT\n    }\n\n    private fun drawStatusesPanel(\n        g: TextGraphics,\n        v: ViewData,\n        top: TerminalPosition,\n        width: Int,\n        maxRows: Int\n    ) {\n        val hpLine = buildHpBar(v, width)\n        g.putString(top.column, top.row, hpLine.padEnd(width))\n\n        val progLine = \" Exp: ${v.exploredCount}/${v.totalAreas}   Dev: ${v.activatedCount}/${v.totalDevices}\"\n        g.putString(top.column, top.row + 1, progLine.padEnd(width))\n\n        var row = top.row + 2\n\n        val statuses = v.statuses.filterValues { it != 0 }\n        for ((name, value) in statuses) {\n            if (row >= top.row + maxRows) break\n            val range = v.statusBounds[name]\n            g.putString(top.column, row, formatStatus(name, value, range, width).padEnd(width))\n            row++\n        }\n\n        if (v.equippedItems.isEmpty()) return\n\n        if (row < top.row + maxRows && statuses.isNotEmpty()) {\n            g.putString(top.column, row, \" \".repeat(width))\n            row++\n        }\n\n        for ((i, item) in v.equippedItems.withIndex()) {\n            if (row >= top.row + maxRows) break\n            val label = \"@ ${item.name}\" + if (item.locked) \" (Locked)\" else \"\"\n            g.foregroundColor = if (item.locked) TextColor.ANSI.RED else TextColor.ANSI.MAGENTA\n            g.enableModifiers(SGR.BOLD)\n            g.putString(top.column, row, label.padEnd(width))\n            g.disableModifiers(SGR.BOLD)\n            g.foregroundColor = TextColor.ANSI.DEFAULT\n            row++\n        }\n\n        g.foregroundColor = TextColor.ANSI.DEFAULT\n    }\n\n    private fun buildHpBar(v: ViewData, width: Int): String {\n        val barLen = 10\n        val filled = ((v.health.toDouble() / v.maxHealth) * barLen).toInt().coerceIn(0, barLen)\n        val empty = barLen - filled\n\n        val bar = \"█\".repeat(filled) + \"░\".repeat(empty)\n        val textPart = \"${v.health}/${v.maxHealth}\"\n        return \"$bar $textPart\"\n    }\n\n    private fun formatStatus(name: String, value: Int, range: StatusRange?, width: Int): String {\n        val maxLen = width - 10\n        val displayValue = if (range != null && range.max != Int.MAX_VALUE) {\n            \"$value/${range.max}\"\n        } else {\n            \"$value\"\n        }\n        val template = \": $displayValue\"\n        val nameMaxLen = maxOf(4, width - template.length)\n\n        val displayName = if (name.length > nameMaxLen) {\n            name.substring(0, nameMaxLen - 3) + \"...\"\n        } else {\n            name\n        }\n        return \"$displayName$template\"\n    }\n\n    private fun wrapText(text: String, width: Int): List<String> {\n        val words = text.split(\" \")\n        val result = mutableListOf<String>()\n        var line = \"\"\n\n        for (word in words) {\n            if (word.length >= width) {\n                if (line.isNotEmpty()) result.add(line)\n                line = \"\"\n                var remaining = word\n                while (remaining.isNotEmpty()) {\n                    if (remaining.length <= width) {\n                        line += remaining\n                        remaining = \"\"\n                    } else {\n                        result.add(remaining.substring(0, width))\n                        remaining = remaining.substring(width)\n                    }\n                }\n            } else if ((line + \" \" + word).length <= width) {\n                line = if (line.isEmpty()) word else \"$line $word\"\n            } else {\n                if (line.isNotEmpty()) result.add(line)\n                line = word\n            }\n        }\n        if (line.isNotEmpty()) result.add(line)\n        return result.ifEmpty { listOf(\"\") }\n    }\n\n    private fun drawBottomBar(\n        g: TextGraphics,\n        view: ViewData,\n        width: Int,\n        topRow: Int\n    ) {\n        val exits = view.exits\n        val wInfo = exits.getOrNull(0)\n        val aInfo = exits.getOrNull(1)\n        val sInfo = exits.getOrNull(2)\n        val dInfo = exits.getOrNull(3)\n\n        val wLabel = exitLabel('w', wInfo)\n        val aLabel = exitLabel('a', aInfo)\n        val sLabel = exitLabel('s', sInfo)\n        val dLabel = exitLabel('d', dInfo)\n\n        val gap = 2\n        val contentWidth = aLabel.length + gap + sLabel.length + gap + dLabel.length\n        val leftPad = (width - contentWidth) / 2\n\n        val colA = leftPad\n        val colS = colA + aLabel.length + gap\n        val colD = colS + sLabel.length + gap\n        val colW = colS - wLabel.length / 2\n\n        drawSlot(g, wLabel, TerminalPosition(colW, topRow), wInfo != null, wInfo?.blocked ?: false)\n        drawInvHint(g, width, topRow)\n        drawSlot(g, aLabel, TerminalPosition(colA, topRow + 1), aInfo != null, aInfo?.blocked ?: false)\n        drawSlot(g, sLabel, TerminalPosition(colS, topRow + 1), sInfo != null, sInfo?.blocked ?: false)\n        drawSlot(g, dLabel, TerminalPosition(colD, topRow + 1), dInfo != null, dInfo?.blocked ?: false)\n        drawActionHints(g, view, width, topRow + 2)\n    }\n\n    private fun drawInvHint(g: TextGraphics, width: Int, row: Int) {\n        val label = \"[\\$i] inv\"\n        g.foregroundColor = TextColor.ANSI.CYAN\n        g.enableModifiers(SGR.BOLD)\n        val keyPart = \"[i]\"\n        g.putString(width - 10, row, keyPart)\n        g.disableModifiers(SGR.BOLD)\n        g.foregroundColor = TextColor.ANSI.WHITE\n        g.putString(width - 7, row, \"inv\")\n    }\n\n    private fun drawActionHints(g: TextGraphics, view: ViewData, width: Int, row: Int) {\n        val hasAreaItems = view.areaItems.isNotEmpty()\n        val hasCarriedOrEquipped = view.carriedItems.isNotEmpty() || view.equippedItems.isNotEmpty()\n        val hasCarried = view.carriedItems.isNotEmpty()\n        val hasEquipped = view.equippedItems.isNotEmpty()\n\n        val gLabel = \"[g] grab\"\n        val pLabel = \"[p] drop\"\n        val eLabel = \"[e] equip\"\n        val rLabel = \"[r] uneq\"\n\n        val gap = 2\n        val totalWidth = gLabel.length + gap + pLabel.length + gap + eLabel.length + gap + rLabel.length\n        val leftPad = (width - totalWidth) / 2\n\n        var col = leftPad\n        drawActionSlot(g, gLabel, TerminalPosition(col, row), hasAreaItems)\n        col += gLabel.length + gap\n        drawActionSlot(g, pLabel, TerminalPosition(col, row), hasCarriedOrEquipped)\n        col += pLabel.length + gap\n        drawActionSlot(g, eLabel, TerminalPosition(col, row), hasCarried)\n        col += eLabel.length + gap\n        drawActionSlot(g, rLabel, TerminalPosition(col, row), hasEquipped)\n    }\n\n    private fun drawActionSlot(g: TextGraphics, label: String, pos: TerminalPosition, active: Boolean) {\n        g.foregroundColor = if (active) TextColor.ANSI.GREEN else TextColor.ANSI.DEFAULT\n        g.enableModifiers(SGR.BOLD)\n        val keyPart = label.substringBefore(']') + \"]\"\n        g.putString(pos.column, pos.row, keyPart)\n        g.disableModifiers(SGR.BOLD)\n\n        val nameStart = pos.column + keyPart.length\n        val namePart = if (label.contains(\"] \")) label.substringAfter(\"] \") else \"\"\n        g.foregroundColor = if (active) TextColor.ANSI.WHITE else TextColor.ANSI.DEFAULT\n        g.putString(nameStart, pos.row, namePart)\n\n        g.disableModifiers(SGR.BOLD)\n    }\n\n    private fun exitLabel(key: Char, info: ExitInfo?): String =\n        if (info == null || info.name.isNullOrEmpty()) \"[${key}]  .\"\n        else {\n            val suffix = if (info.blocked) \" (B)\" else \"\"\n            \"[${key}] ${info.name}$suffix\"\n        }\n\n    private fun drawSlot(g: TextGraphics, label: String, pos: TerminalPosition, active: Boolean, blocked: Boolean) {\n        g.foregroundColor = when {\n            !active -> TextColor.ANSI.DEFAULT\n            blocked -> TextColor.ANSI.DEFAULT\n            else -> TextColor.ANSI.GREEN\n        }\n        g.enableModifiers(SGR.BOLD)\n        val keyPart = label.substringBefore(']') + \"]\"\n        g.putString(pos.column, pos.row, keyPart)\n        g.disableModifiers(SGR.BOLD)\n\n        val nameStart = pos.column + keyPart.length\n        val namePart = if (label.contains(\"] \")) label.substringAfter(\"] \") else \"\"\n        g.foregroundColor = when {\n            !active -> TextColor.ANSI.DEFAULT\n            blocked -> TextColor.ANSI.DEFAULT\n            else -> TextColor.ANSI.GREEN\n        }\n        g.putString(nameStart, pos.row, namePart)\n\n        g.disableModifiers(SGR.BOLD)\n    }\n\n    private fun drawTooSmallMessage(g: TextGraphics, w: Int, h: Int) {\n        val lines = listOf(\n            \"Terminal too small.\",\n            \"Resize to at least $MIN_WIDTH columns, $MIN_HEIGHT rows.\"\n        )\n        val startRow = (h - lines.size) / 2\n        g.foregroundColor = TextColor.ANSI.YELLOW\n        g.enableModifiers(SGR.BOLD)\n        for ((i, line) in lines.withIndex()) {\n            val col = (w - line.length) / 2\n            g.putString(col, startRow + i, line)\n        }\n        g.disableModifiers(SGR.BOLD)\n    }\n\n    private fun printEndSummary(view: ViewData, history: List<HistoryEntry>) {\n        println()\n        if (view.endGameMessage != null) {\n            println(\"===== ${view.endGameMessage} =====\")\n        } else {\n            println(\"===== Game Over =====\")\n        }\n        println(\"Area  : ${view.currentAreaName}\")\n        println(\"Health: ${view.health}/${view.maxHealth}\")\n        println(\"Explored: ${view.exploredCount}/${view.totalAreas}  |  Devices: ${view.activatedCount}/${view.totalDevices}\")\n        if (view.statuses.isNotEmpty()) {\n            val statusLines = view.statuses.filterValues { it != 0 }.map { (name, value) ->\n                \"$name=$value\"\n            }\n            println(\"Statuses: ${statusLines.joinToString(\", \")}\")\n        }\n        if (history.isNotEmpty()) {\n            println(\"\\n--- Last messages ---\")\n            for (entry in history) println(entry.text)\n        }\n    }\n}\n",
  "filePath": "/var/home/mikharju/dev/exploration/engine/src/main/kotlin/exploration/adapter/ui/lanterna/LanternaUiAdapter.kt"
}
```

**Output:**
```
Wrote file successfully.
```

---

## Assistant (Build · qwen3.6-35b-a3b · 10.7s)

_Thinking:_

Wait, I still have a bug - `appendTriggerMessages` in gameLoop is being called but it's now just an empty placeholder. In the original code, the trigger message logic was inline inside the `KeyAction.Event` branch. Let me check what happened:

Original:
```kotlin
is KeyAction.Event -> {
    ...
    val newTriggerCount = next.triggerTexts.size - shownTriggers
    for (i in 0 until newTriggerCount) {
        val triggerText = next.triggerTexts[shownTriggers + i]
        if (triggerText.isNotBlank()) addMessage(history, triggerText, true)
    }
    ...
}
```

My refactored version has this as a separate method `appendTriggerMessages(screen, ref, state)` but it's empty. I need to put the logic back in that method or inline it. Let me fix this properly by putting the actual trigger message appending logic into the `appendTriggerMessages` method (without the unnecessary screen and ref parameters).






**Tool: edit**

**Input:**
```json
{
  "filePath": "/var/home/mikharju/dev/exploration/engine/src/main/kotlin/exploration/adapter/ui/lanterna/LanternaUiAdapter.kt",
  "oldString": "    private fun appendTriggerMessages(screen: Screen, ref: GameRef, state: LoopState) {\n        // No-op placeholder — trigger messages are already handled in gameLoop\n    }",
  "newString": "    private fun appendTriggerMessages(state: LoopState) {\n        val newTriggerCount = state.currentView.triggerTexts.size - state.shownTriggers\n        for (i in 0 until newTriggerCount) {\n            val triggerText = state.currentView.triggerTexts[state.shownTriggers + i]\n            if (triggerText.isNotBlank()) addMessage(state.history, triggerText, true)\n        }\n    }"
}
```

**Output:**
```
Edit applied successfully.
```

---

## Assistant (Build · qwen3.6-35b-a3b · 4.7s)

_Thinking:_

Now I need to update the call site in gameLoop to pass only `state` instead of `(screen, ref, state)`.






**Tool: edit**

**Input:**
```json
{
  "filePath": "/var/home/mikharju/dev/exploration/engine/src/main/kotlin/exploration/adapter/ui/lanterna/LanternaUiAdapter.kt",
  "oldString": "                    if (next.commandText.isNotBlank()) addMessage(state.history, next.commandText, false)\n                    appendTriggerMessages(screen, ref, state)",
  "newString": "                    if (next.commandText.isNotBlank()) addMessage(state.history, next.commandText, false)\n                    appendTriggerMessages(state)"
}
```

**Output:**
```
Edit applied successfully.
```

---

## Assistant (Build · qwen3.6-35b-a3b · 5.7s)

_Thinking:_

Now let me build to make sure everything compiles correctly.






**Tool: bash**

**Input:**
```json
{
  "command": "./gradlew compileKotlin 2>&1 | tail -30",
  "description": "Compile Kotlin sources"
}
```

**Output:**
```
> Task :checkKotlinGradlePluginConfigurationErrors SKIPPED

> Task :compileKotlin FAILED
e: file:///var/home/mikharju/dev/exploration/engine/src/main/kotlin/exploration/adapter/ui/lanterna/LanternaUiAdapter.kt:98:13 Unresolved label.
e: file:///var/home/mikharju/dev/exploration/engine/src/main/kotlin/exploration/adapter/ui/lanterna/LanternaUiAdapter.kt:172:39 Smart cast to 'String' is impossible, because 'endGameMessage' is a mutable property that could be mutated concurrently.

FAILURE: Build failed with an exception.

* What went wrong:
Execution failed for task ':compileKotlin'.
> A failure occurred while executing org.jetbrains.kotlin.compilerRunner.btapi.BuildToolsApiCompilationWork
   > Compilation error. See log for more details

* Try:
> Run with --stacktrace option to get the stack trace.
> Run with --info or --debug option to get more log output.
> Run with --scan to get full insights from a Build Scan (powered by Develocity).
> Get more help at https://help.gradle.org.

BUILD FAILED in 3s
1 actionable task: 1 executed

```

---

## Assistant (Build · qwen3.6-35b-a3b · 13.2s)

_Thinking:_

Two errors:

1. Line 98: "Unresolved label" — I'm using `render(screen = state.currentView, ...)` but there's a `screen` parameter name conflict with the Lanterna Screen type inside the function call. The issue is that in `initFirstView`, I have `render(screen, state.currentView, ...)` where `screen` is a parameter (Lanterna Screen), and `state.currentView` is the second param which should be `view`. Let me look at line 98 more carefully.

Actually wait, let me re-read the file around line 98:
```kotlin
private fun initFirstView(screen: Screen, state: LoopState) {
    render(screen, state.currentView, state.history, state.overlay)
    
    val initialStoryCount = state.currentView.storyMessages.size
    state.storedStoryCount = initialStoryCount
    if (initialStoryCount > 0) {
        val subList = state.currentView.storyMessages.subList(0, initialStoryCount)
        state.overlay = Overlay.MessageViewer(subList, focusedIndex = subList.lastIndex)
        render(screen, state.currentView, state.history, state.overlay)  // line ~98?
    }
}
```

Wait, the issue says "Unresolved label" at line 98. Let me check what's on line 98... Actually I need to re-read my file more carefully. The error is about an unresolved label which in Kotlin usually means a `continue@label` or `break@label` construct is broken.

Actually, looking at the original code, there was no label issue before. Let me check if I accidentally introduced something that looks like a label... 

Hmm, let me read around line 98 again.

2. Line 172: "Smart cast to 'String' is impossible" — this is because `endGameMessage` is on a mutable property (`state.currentView`). The compiler can't smart-cast it since it could change concurrently (in theory). This needs an explicit val or different handling.

Let me read the file around both error lines.






**Tool: read**

**Input:**
```json
{
  "filePath": "/var/home/mikharju/dev/exploration/engine/src/main/kotlin/exploration/adapter/ui/lanterna/LanternaUiAdapter.kt",
  "offset": 90,
  "limit": 20
}
```

**Output:**
```
<path>/var/home/mikharju/dev/exploration/engine/src/main/kotlin/exploration/adapter/ui/lanterna/LanternaUiAdapter.kt</path>
<type>file</type>
<content>
90:             return null
91:         }
92:     }
93: 
94:     private fun setupScreen(): Screen = with(TerminalScreen(DefaultTerminalFactory().createTerminal())) {
95:         stopScreen()
96:         startScreen()
97:         cursorPosition = null
98:         this@setupScreen
99:     }
100: 
101:     private fun initFirstView(screen: Screen, state: LoopState) {
102:         render(screen, state.currentView, state.history, state.overlay)
103: 
104:         val initialStoryCount = state.currentView.storyMessages.size
105:         state.storedStoryCount = initialStoryCount
106:         if (initialStoryCount > 0) {
107:             val subList = state.currentView.storyMessages.subList(0, initialStoryCount)
108:             state.overlay = Overlay.MessageViewer(subList, focusedIndex = subList.lastIndex)
109:             render(screen, state.currentView, state.history, state.overlay)

(Showing lines 90-109 of 823. Use offset=110 to continue.)
</content>
```

---

## Assistant (Build · qwen3.6-35b-a3b · 14.7s)

_Thinking:_

Ah I see the issue on line 98. The `with` block returns a value but there's no explicit return statement for the lambda body. The `this@setupScreen` at the end should work, but actually in Kotlin you can't use qualified this like that inside a with block when the outer function also has a name. Let me fix it differently - just use an explicit val assignment and return screen.

Actually, looking more carefully, `with(TerminalScreen(...)) { ...; this@setupScreen }` should work in Kotlin to return the original Screen object from the lambda... but wait, inside a `with`, `this` refers to the receiver of `with`. So `this@setupScreen` should refer back to the outer function's return value context. But actually there's no enclosing function with name `setupScreen` that we can label - we're trying to use it as a label for the lambda return.

The issue is: inside `with`, `this` is the TerminalScreen, and we want to return it (since TerminalScreen extends Screen). The qualified this `this@setupScreen` would need `setupScreen` to be a function that contains this expression... but `setupScreen()` IS the containing function. So actually `this@setupScreen` should refer to the implicit `this` of the class if called on an instance, not the return value.

Wait no - in Kotlin, when you have:
```kotlin
private fun setupScreen(): Screen = with(TerminalScreen(...)) {
    // ... 
    this@setupScreen
}
```

The `this@setupScreen` label syntax is used to refer to a receiver of an outer scope. But here there's no explicit outer receiver - the function itself returns Screen. The correct way would be just to capture the TerminalScreen in a variable and return it, or use the fact that TerminalScreen is-a Screen:

```kotlin
private fun setupScreen(): Screen {
    val screen = TerminalScreen(DefaultTerminalFactory().createTerminal())
    screen.stopScreen()
    screen.startScreen()
    screen.cursorPosition = null
    return screen
}
```

This is simpler and clearer. Let me fix this.

For the second error at line 172, I need to see what's there too.






**Tool: read**

**Input:**
```json
{
  "filePath": "/var/home/mikharju/dev/exploration/engine/src/main/kotlin/exploration/adapter/ui/lanterna/LanternaUiAdapter.kt",
  "limit": 15,
  "offset": 165
}
```

**Output:**
```
<path>/var/home/mikharju/dev/exploration/engine/src/main/kotlin/exploration/adapter/ui/lanterna/LanternaUiAdapter.kt</path>
<type>file</type>
<content>
165:     private fun endGameScreen(screen: Screen, state: LoopState) {
166:         for (i in state.shownTriggers until state.currentView.triggerTexts.size) {
167:             if (state.currentView.triggerTexts[i].isNotBlank()) addMessage(state.history, state.currentView.triggerTexts[i], true)
168:         }
169:         state.storedStoryCount = state.currentView.storyMessages.size
170:         if (state.currentView.endGameMessage != null) {
171:             addMessage(state.history, "", false)
172:             addMessage(state.history, state.currentView.endGameMessage, false)
173:         } else if (state.currentView.outputLine.isNotBlank()) {
174:             addMessage(state.history, state.currentView.outputLine, false)
175:         }
176:         screen.doResizeIfNecessary()
177:         render(screen, state.currentView, state.history, state.overlay)
178:     }
179: 

(Showing lines 165-179 of 823. Use offset=180 to continue.)
</content>
```

---

## Assistant (Build · qwen3.6-35b-a3b · 7.2s)

_Thinking:_

The issue at line 172 is that `state.currentView.endGameMessage` is a mutable property (`var currentView: ViewData`) and Kotlin can't smart-cast it from `String?` to `String` because the value could change between the null check on line 170 and the use on line 172. The fix is to capture it in a local val first.

Let me fix both issues now.






**Tool: edit**

**Input:**
```json
{
  "filePath": "/var/home/mikharju/dev/exploration/engine/src/main/kotlin/exploration/adapter/ui/lanterna/LanternaUiAdapter.kt",
  "oldString": "    private fun setupScreen(): Screen = with(TerminalScreen(DefaultTerminalFactory().createTerminal())) {\n        stopScreen()\n        startScreen()\n        cursorPosition = null\n        this@setupScreen\n    }",
  "newString": "    private fun setupScreen(): Screen {\n        val screen = TerminalScreen(DefaultTerminalFactory().createTerminal())\n        screen.stopScreen()\n        screen.startScreen()\n        screen.cursorPosition = null\n        return screen\n    }"
}
```

**Output:**
```
Edit applied successfully.
```

---

## Assistant (Build · qwen3.6-35b-a3b · 5.8s)

_Thinking:_

Good, now fix the smart cast issue at line 172 by capturing endGameMessage in a local val.






**Tool: edit**

**Input:**
```json
{
  "filePath": "/var/home/mikharju/dev/exploration/engine/src/main/kotlin/exploration/adapter/ui/lanterna/LanternaUiAdapter.kt",
  "oldString": "        state.storedStoryCount = state.currentView.storyMessages.size\n        if (state.currentView.endGameMessage != null) {\n            addMessage(state.history, \"\", false)\n            addMessage(state.history, state.currentView.endGameMessage, false)",
  "newString": "        state.storedStoryCount = state.currentView.storyMessages.size\n        val endMsg = state.currentView.endGameMessage\n        if (endMsg != null) {\n            addMessage(state.history, \"\", false)\n            addMessage(state.history, endMsg, false)"
}
```

**Output:**
```
Edit applied successfully.
```

---

## Assistant (Build · qwen3.6-35b-a3b · 4.5s)

_Thinking:_

Now let me try compiling again to see if both errors are fixed.






**Tool: bash**

**Input:**
```json
{
  "command": "./gradlew compileKotlin 2>&1 | tail -15",
  "description": "Compile Kotlin sources"
}
```

**Output:**
```
> Task :checkKotlinGradlePluginConfigurationErrors SKIPPED
> Task :compileKotlin

BUILD SUCCESSFUL in 2s
1 actionable task: 1 executed
Consider enabling configuration cache to speed up this build: https://docs.gradle.org/9.3.0/userguide/configuration_cache_enabling.html

```

---

## Assistant (Build · qwen3.6-35b-a3b · 4.0s)

_Thinking:_

Compilation succeeds. Now let me run the full test suite to make sure everything still works correctly.






**Tool: bash**

**Input:**
```json
{
  "command": "./gradlew build 2>&1 | tail -20",
  "description": "Build and run tests"
}
```

**Output:**
```
> Task :compileKotlin UP-TO-DATE
> Task :compileJava NO-SOURCE
> Task :processResources NO-SOURCE
> Task :classes UP-TO-DATE
> Task :jar
> Task :startScripts
> Task :distTar
> Task :distZip
> Task :assemble
> Task :processTestResources UP-TO-DATE
> Task :compileTestKotlin
> Task :compileTestJava NO-SOURCE
> Task :testClasses UP-TO-DATE
> Task :test
> Task :check
> Task :build

BUILD SUCCESSFUL in 1s
8 actionable tasks: 6 executed, 2 up-to-date
Consider enabling configuration cache to speed up this build: https://docs.gradle.org/9.3.0/userguide/configuration_cache_enabling.html

```

---

## Assistant (Build · qwen3.6-35b-a3b · 10.4s)

_Thinking:_

All tests pass. The refactoring is complete. Let me summarize what was done:

1. Extracted `startEngine()` — error handling for engine startup, returns GameRef or null
2. Extracted `setupScreen()` — Lanterna screen lifecycle setup, returns Screen
3. Created `LoopState` data class — consolidates the 4 mutable tracking variables (`history`, `currentView`, `shownTriggers`, `storedStoryCount`, `selectionState`, `overlay`) into one object passed through methods
4. Extracted `initFirstView()` — first look + initial story message overlay display
5. Extracted `gameLoop()` — the main game loop with input handling and state updates
6. Extracted `appendTriggerMessages()` — new trigger text appending logic (extracted from inline in Event branch)
7. Extracted `endGameScreen()` — end-game cleanup: final triggers, story messages, end message

The `run()` method went from ~104 lines to ~25 lines, and each extracted function has a single clear responsibility. The mutable tracking state is now centralized in `LoopState` instead of scattered as individual vars at the top of `run()`.




Done. `run()` went from 104 lines to 25, with these extractions:

| Method | Responsibility |
|--------|---------------|
| `startEngine()` | Error handling for engine start, returns GameRef? |
| `setupScreen()` | Lanterna screen lifecycle setup |
| `initFirstView()` | First look + initial story overlay display |
| `gameLoop()` | Main loop with input dispatch and state updates |
| `appendTriggerMessages()` | New trigger text appending logic |
| `endGameScreen()` | End-game cleanup: final triggers, messages, render |

`LoopState` data class replaces the 4 scattered mutable vars (`shownTriggers`, `storedStoryCount`, `selectionState`, `overlay`) plus history and currentView — now a single object threaded through the extracted methods.

---

