# Detailed triggers feature plan

**Session ID:** ses_1e78a68a4ffexTmunQfCLQdEPZ
**Created:** 5/11/2026, 10:13:40 PM
**Updated:** 5/11/2026, 10:46:22 PM

---

## User

@plans/TRIGGERS.md has an early design for new triggers feature. Make a more detailed plan. What still needs to be considered?

---

## Assistant (Plan · qwen3.6-27b · 23.4s)

_Thinking:_

Let me analyze the current TRIGGERS.md design and think about what's missing or needs more consideration. Let me also look at the existing codebase structure to understand how triggers would integrate.




Let me examine the existing codebase structure to understand integration points.





**Tool: read**

**Input:**
```json
{
  "filePath": "/var/home/mikharju/dev/exploration/engine/src/main/kotlin/state"
}
```

**Error:**
```
File not found: /var/home/mikharju/dev/exploration/engine/src/main/kotlin/state
```

**Tool: read**

**Input:**
```json
{
  "filePath": "/var/home/mikharju/dev/exploration/engine/src/main/kotlin/model"
}
```

**Error:**
```
File not found: /var/home/mikharju/dev/exploration/engine/src/main/kotlin/model
```

**Tool: read**

**Input:**
```json
{
  "filePath": "/var/home/mikharju/dev/exploration/engine/src/main/kotlin/command"
}
```

**Error:**
```
File not found: /var/home/mikharju/dev/exploration/engine/src/main/kotlin/command
```

---

## Assistant (Plan · qwen3.6-27b · 2.0s)

_Thinking:_

Let me find the actual source directory structure.






**Tool: glob**

**Input:**
```json
{
  "pattern": "**/*.kt"
}
```

**Output:**
```
/var/home/mikharju/dev/exploration/engine/src/main/kotlin/exploration/core/state/GameState.kt
/var/home/mikharju/dev/exploration/engine/src/main/kotlin/exploration/port/ViewData.kt
/var/home/mikharju/dev/exploration/engine/src/main/kotlin/exploration/scenario/ScenarioFile.kt
/var/home/mikharju/dev/exploration/engine/src/main/kotlin/exploration/scenario/ScenarioLoader.kt
/var/home/mikharju/dev/exploration/engine/src/main/kotlin/exploration/adapter/ui/lanterna/LanternaUiAdapter.kt
/var/home/mikharju/dev/exploration/engine/src/main/kotlin/exploration/adapter/ui/text/TextUiAdapter.kt
/var/home/mikharju/dev/exploration/engine/src/main/kotlin/exploration/core/engine/GameEngineImpl.kt
/var/home/mikharju/dev/exploration/engine/src/main/kotlin/exploration/core/command/CommandProcessor.kt
/var/home/mikharju/dev/exploration/engine/src/main/kotlin/exploration/core/model/StatusDef.kt
/var/home/mikharju/dev/exploration/engine/src/main/kotlin/exploration/core/model/Player.kt
/var/home/mikharju/dev/exploration/engine/src/main/kotlin/exploration/core/model/Device.kt
/var/home/mikharju/dev/exploration/engine/src/main/kotlin/exploration/cli/Main.kt
/var/home/mikharju/dev/exploration/engine/src/main/kotlin/exploration/adapter/ui/key/KeyUiAdapter.kt
/var/home/mikharju/dev/exploration/engine/src/main/kotlin/exploration/port/InputEvent.kt
/var/home/mikharju/dev/exploration/engine/src/test/kotlin/exploration/adapter/ui/text/TextCommandParserTest.kt
/var/home/mikharju/dev/exploration/engine/src/main/kotlin/exploration/adapter/jsonloader/JsonFileReader.kt
/var/home/mikharju/dev/exploration/engine/src/main/kotlin/exploration/adapter/ui/text/TextCommandParser.kt
/var/home/mikharju/dev/exploration/engine/src/test/kotlin/exploration/scenario/ScenarioLoaderTest.kt
/var/home/mikharju/dev/exploration/engine/src/main/kotlin/exploration/adapter/jsonloader/JsonScenarioRepository.kt
/var/home/mikharju/dev/exploration/engine/src/test/kotlin/exploration/command/CommandProcessorTest.kt
/var/home/mikharju/dev/exploration/engine/src/main/kotlin/exploration/core/command/Command.kt
/var/home/mikharju/dev/exploration/engine/src/main/kotlin/exploration/port/ScenarioRepository.kt
/var/home/mikharju/dev/exploration/engine/src/main/kotlin/exploration/port/GameEngine.kt
/var/home/mikharju/dev/exploration/engine/src/test/kotlin/exploration/integration/GameIntegrationTest.kt
/var/home/mikharju/dev/exploration/engine/src/test/kotlin/exploration/model/ModelTest.kt
/var/home/mikharju/dev/exploration/engine/src/main/kotlin/exploration/core/model/World.kt
/var/home/mikharju/dev/exploration/engine/src/main/kotlin/exploration/core/model/Area.kt
```

---

## Assistant (Plan · qwen3.6-27b · 10.4s)

_Thinking:_

Let me read the key files to understand the current architecture.










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
4: import exploration.model.DeviceId
5: import exploration.model.Player
6: import exploration.model.World
7: 
8: import exploration.model.StatusRange
9: 
10: data class GameState(
11:     val world: World,
12:     val player: Player,
13:     val exploredAreas: Set<AreaId> = emptySet(),
14:     val activatedDevices: Set<DeviceId> = emptySet(),
15:     val output: String = "",
16:     val isOver: Boolean = false,
17:     val win: Boolean? = null,
18:     val statusBounds: Map<String, StatusRange> = emptyMap()
19: ) {
20:     fun allDeviceIds(): Set<DeviceId> {
21:         val devices = mutableSetOf<DeviceId>()
22:         for (area in world.areas.values) {
23:             area.device?.let { devices.add(it.id) }
24:         }
25:         return devices
26:     }
27: 
28:     fun checkOutcome(): GameState = if (!isOver) {
29:         val allExplored = exploredAreas == world.areas.keys
30:         val allActivated = activatedDevices.containsAll(allDeviceIds())
31:         when {
32:             player.health <= 0 -> copy(isOver = true, win = false, output = "You collapse from exhaustion... Game Over.")
33:             allExplored && allActivated -> copy(isOver = true, win = true, output = "You've explored every corner and activated every device. Victory!")
34:             else -> this
35:         }
36:     } else this
37: }

(End of file - total 37 lines)
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
3: import exploration.model.AreaId
4: import exploration.state.GameState
5: 
6: fun processCommand(state: GameState, command: Command): GameState {
7:     if (state.isOver) {
8:         return state.copy(output = "The game is over. Restart to play again.")
9:     }
10: 
11:     val result = when (command) {
12:         is Command.Look -> processLook(state)
13:         is Command.Move -> processMove(state, command.areaName)
14:         is Command.Activate -> processActivate(state)
15:     }
16: 
17:     return result.checkOutcome()
18: }
19: 
20: private fun processLook(state: GameState): GameState {
21:     val areaId = state.player.currentArea
22:     val area = state.world.getArea(areaId)
23: 
24:     val deviceText = area.device?.let { d ->
25:         if (d.id in state.activatedDevices) {
26:             "\nYou see a deactivated ${d.id} here."
27:         } else {
28:             "\n${d.lookDescription}"
29:         }
30:     } ?: ""
31: 
32:     return state.copy(
33:         exploredAreas = state.exploredAreas + areaId,
34:         output = "${area.description}$deviceText"
35:     )
36: }
37: 
38: private fun findAreaId(world: GameState, name: String): AreaId? {
39:     val lower = name.lowercase()
40:     return world.world.areas.keys.find { it.name.lowercase() == lower }
41: }
42: 
43: private fun processMove(state: GameState, targetName: String): GameState {
44:     val currentArea = state.world.getArea(state.player.currentArea)
45:     val targetId = findAreaId(state, targetName)
46: 
47:     if (targetId == null) {
48:         return state.copy(output = "There's no place called '$targetName' here.")
49:     }
50: 
51:     if (targetId !in currentArea.connections) {
52:         val valid = currentArea.connections.joinToString(", ")
53:         return state.copy(output = "You can't go there directly. Connected areas: $valid")
54:     }
55: 
56:     return state.copy(
57:         player = state.player.copy(currentArea = targetId),
58:         output = "You move to the ${targetId.name}."
59:     )
60: }
61: 
62: private fun processActivate(state: GameState): GameState {
63:     val area = state.world.getArea(state.player.currentArea)
64:     val device = area.device
65: 
66:     if (device == null) {
67:         return state.copy(output = "There's nothing here to activate.")
68:     }
69: 
70:     if (device.id in state.activatedDevices) {
71:         return state.copy(output = "${device.id} has already been activated.")
72:     }
73: 
74:     var player = state.player
75:     val msgs = mutableListOf<String>()
76: 
77:     player = player.adjustHealth(device.healthEffect)
78:     when (device.healthEffect) {
79:         0 -> {}
80:         in 1..Int.MAX_VALUE -> msgs.add("+${device.healthEffect} health")
81:         else -> msgs.add("${device.healthEffect} health")
82:     }
83: 
84:     for ((name, amount) in device.statusEffects) {
85:         val oldVal = player.statuses.getOrElse(name) { 0 }
86:         player = player.adjustStatus(name, amount, state.statusBounds[name])
87:         val newVal = player.statuses.getValue(name)
88:         if (amount != 0 || newVal != oldVal) {
89:             msgs.add("$name: $newVal")
90:         }
91:     }
92: 
93:     val suffix = if (msgs.isNotEmpty()) " (${msgs.joinToString(", ")})" else ""
94: 
95:     return state.copy(
96:         player = player,
97:         activatedDevices = state.activatedDevices + device.id,
98:         output = "${device.activateDescription}$suffix"
99:     )
100: }

(End of file - total 100 lines)
</content>
```

**Tool: read**

**Input:**
```json
{
  "filePath": "/var/home/mikharju/dev/exploration/engine/src/main/kotlin/exploration/core/model/Player.kt"
}
```

**Output:**
```
<path>/var/home/mikharju/dev/exploration/engine/src/main/kotlin/exploration/core/model/Player.kt</path>
<type>file</type>
<content>
1: package exploration.model
2: 
3: data class Player(
4:     val health: Int,
5:     val maxHealth: Int,
6:     val currentArea: AreaId,
7:     val statuses: Map<String, Int> = emptyMap()
8: ) {
9:     init {
10:         require(health >= 0) { "Player health must be non-negative" }
11:         require(maxHealth > 0) { "maxHealth must be positive" }
12:         require(maxHealth >= health) { "maxHealth must be >= health" }
13:     }
14: 
15:     fun adjustHealth(amount: Int): Player = copy(
16:         health = minOf(maxHealth, maxOf(0, health + amount))
17:     )
18: 
19:     fun adjustStatus(name: String, amount: Int, range: StatusRange?): Player {
20:         val current = statuses.getOrElse(name) { 0 }
21:         val newValue = if (range != null) {
22:             minOf(range.max, maxOf(range.min, current + amount))
23:         } else {
24:             current + amount
25:         }
26:         return copy(statuses = statuses + (name to newValue))
27:     }
28: }

(End of file - total 28 lines)
</content>
```

**Tool: read**

**Input:**
```json
{
  "filePath": "/var/home/mikharju/dev/exploration/engine/src/main/kotlin/exploration/core/model/World.kt"
}
```

**Output:**
```
<path>/var/home/mikharju/dev/exploration/engine/src/main/kotlin/exploration/core/model/World.kt</path>
<type>file</type>
<content>
1: package exploration.model
2: 
3: data class World(
4:     val areas: Map<AreaId, Area>,
5:     val startArea: AreaId
6: ) {
7:     init {
8:         require(areas.containsKey(startArea)) { "startArea must exist in world" }
9:         areas.keys.forEach { id ->
10:             require(id in areas) { "All connection targets must be valid AreaIds: $id" }
11:         }
12:     }
13: 
14:     fun getArea(id: AreaId): Area =
15:         checkNotNull(areas[id]) { "Unknown area: $id" }
16: }

(End of file - total 16 lines)
</content>
```

**Tool: read**

**Input:**
```json
{
  "filePath": "/var/home/mikharju/dev/exploration/engine/src/main/kotlin/exploration/scenario/ScenarioFile.kt"
}
```

**Output:**
```
<path>/var/home/mikharju/dev/exploration/engine/src/main/kotlin/exploration/scenario/ScenarioFile.kt</path>
<type>file</type>
<content>
1: package exploration.scenario
2: 
3: import kotlinx.serialization.Serializable
4: 
5: @Serializable
6: data class PlayerStart(
7:     val health: Int,
8:     val maxHealth: Int,
9:     val startArea: String
10: ) {
11:     init {
12:         require(health >= 0) { "Player health must be non-negative" }
13:         require(maxHealth > 0) { "maxHealth must be positive" }
14:         require(maxHealth >= health) { "maxHealth must be >= health" }
15:     }
16: }
17: 
18: @Serializable
19: data class StatusEntry(
20:     val initial: Int,
21:     val min: Int = 0,
22:     val max: Int = Int.MAX_VALUE
23: )
24: 
25: @Serializable
26: data class DeviceEntry(
27:     val id: String,
28:     val lookDescription: String,
29:     val activateDescription: String,
30:     val healthEffect: Int = 0,
31:     val statusEffects: Map<String, Int> = emptyMap()
32: )
33: 
34: @Serializable
35: data class AreaEntry(
36:     val id: String,
37:     val description: String,
38:     val connections: List<String>,
39:     val deviceId: String? = null
40: )
41: 
42: @Serializable
43: data class ScenarioConfig(
44:     val playerStart: PlayerStart,
45:     val areasFile: String,
46:     val devicesFile: String,
47:     val statuses: Map<String, StatusEntry> = emptyMap()
48: )

(End of file - total 48 lines)
</content>
```

---

## Assistant (Plan · qwen3.6-27b · 10.3s)

_Thinking:_

Now let me also read the Area model and the existing scenario loader to understand how scenarios are loaded.








**Tool: read**

**Input:**
```json
{
  "filePath": "/var/home/mikharju/dev/exploration/engine/src/main/kotlin/exploration/core/model/Area.kt"
}
```

**Output:**
```
<path>/var/home/mikharju/dev/exploration/engine/src/main/kotlin/exploration/core/model/Area.kt</path>
<type>file</type>
<content>
1: package exploration.model
2: 
3: data class AreaId(val name: String) {
4:     override fun toString(): String = name
5: }
6: 
7: data class Area(
8:     val id: AreaId,
9:     val description: String,
10:     val connections: Set<AreaId>,
11:     val device: Device? = null
12: )

(End of file - total 12 lines)
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
3: import exploration.model.*
4: import exploration.state.GameState
5: 
6: fun assembleGame(config: ScenarioConfig, areaEntries: List<AreaEntry>, deviceEntries: List<DeviceEntry>): GameState {
7:     val statusInitial = mutableMapOf<String, Int>()
8:     val statusBounds = mutableMapOf<String, StatusRange>()
9:     for ((name, entry) in config.statuses) {
10:         statusInitial[name] = entry.initial
11:         statusBounds[name] = StatusRange(entry.min, entry.max)
12:     }
13: 
14:     val deviceMap = mutableMapOf<String, Device>()
15:     for (entry in deviceEntries) {
16:         deviceMap[entry.id] = Device(
17:             id = DeviceId(entry.id),
18:             lookDescription = entry.lookDescription,
19:             activateDescription = entry.activateDescription,
20:             healthEffect = entry.healthEffect,
21:             statusEffects = entry.statusEffects
22:         )
23:     }
24: 
25:     val areaIds = mutableSetOf<String>()
26:     for (entry in areaEntries) {
27:         areaIds.add(entry.id)
28:     }
29: 
30:     val areasMap = mutableMapOf<AreaId, Area>()
31:     for (entry in areaEntries) {
32:         if (entry.deviceId != null && entry.deviceId !in deviceMap) {
33:             throw IllegalArgumentException("Area '${entry.id}' references unknown device: ${entry.deviceId}")
34:         }
35: 
36:         val connections = mutableSetOf<AreaId>()
37:         for (connName in entry.connections) {
38:             if (connName !in areaIds) {
39:                 throw IllegalArgumentException(
40:                     "Area '${entry.id}' connects to unknown area: $connName"
41:                 )
42:             }
43:             connections.add(AreaId(connName))
44:         }
45: 
46:         areasMap[AreaId(entry.id)] = Area(
47:             id = AreaId(entry.id),
48:             description = entry.description,
49:             connections = connections,
50:             device = entry.deviceId?.let { deviceMap[it]!! }
51:         )
52:     }
53: 
54:     val startId = AreaId(config.playerStart.startArea)
55:     if (startId !in areasMap.keys) {
56:         throw IllegalArgumentException("Start area '${config.playerStart.startArea}' not found in areas")
57:     }
58: 
59:     val world = World(areas = areasMap, startArea = startId)
60: 
61:     val player = Player(
62:         health = config.playerStart.health,
63:         maxHealth = config.playerStart.maxHealth,
64:         currentArea = startId,
65:         statuses = statusInitial
66:     )
67: 
68:     return GameState(world = world, player = player, statusBounds = statusBounds)
69: }

(End of file - total 69 lines)
</content>
```

**Tool: read**

**Input:**
```json
{
  "filePath": "/var/home/mikharju/dev/exploration/engine/src/main/kotlin/exploration/core/command/Command.kt"
}
```

**Output:**
```
<path>/var/home/mikharju/dev/exploration/engine/src/main/kotlin/exploration/core/command/Command.kt</path>
<type>file</type>
<content>
1: package exploration.command
2: 
3: sealed class Command {
4:     object Look : Command()
5:     data class Move(val areaName: String) : Command()
6:     object Activate : Command()
7: }

(End of file - total 7 lines)
</content>
```

---

## Assistant (Plan · qwen3.6-27b · 20.0s)

_Thinking:_

Now I have a good understanding. Let me also check if there's a turn counter or something similar in the GameState, since the plan mentions "once every X turns".




Now I have a clear picture of the codebase. Here's my analysis of what needs more consideration:

## Gaps & Questions

**1. Turn tracking** — The plan mentions "once every X turns" for status triggers, but `GameState` has no turn counter. Do you want to add one? What counts as a turn (every command, only move/activate)?

**2. Trigger attachment model** — Where do triggers live in the data model?
- Area trigger → attached to `Area`? Or a separate list keyed by area ID?
- Device trigger → same question for `Device`
- Status trigger → needs a global list somewhere in `GameState` or `World`

**3. Trigger ordering within a category** — If an area has 3 triggers, what order do they fire? JSON insertion order? Explicit priority field?

**4. Multi-effect execution order** — A trigger can have "display text" + "change status" + "set status". Does order matter among effects? (Probably yes for display timing.)

**5. Trigger state tracking** — Single-use triggers need to be tracked as consumed. Where does this live? In `GameState` as a `Set<TriggerId>` of fired triggers?

**6. Status trigger: which status?** — The plan says "value of this status" but doesn't specify which one per trigger. Each status trigger needs to reference a specific status name.

**7. Activation condition granularity** — Currently only `> threshold` and `< threshold`. What about `>=`, `<=`, `==`? Or is the early design intentionally limited?

**8. Trigger on game start** — Should any triggers fire when the game begins (e.g., initial area entry)? The plan doesn't specify this.

**9. Effect scope for "change any player status"** — Can a trigger modify `health` directly through the status system, or is health special? Currently health and statuses are separate on `Player`.

**10. Scenario file format** — How do triggers appear in JSON? Nested under areas/devices, or a separate triggers section/file?

Want me to draft a detailed plan addressing these, or would you like to answer some of these questions first so the plan is aligned with your intent?

---

## User

Give recommendations for all of these.

---

## Assistant (Plan · qwen3.6-27b · 28.3s)

_Thinking:_

The user wants me to provide recommendations for all 10 gaps I identified. Let me think through each one carefully, considering the existing codebase architecture (hexagonal, functional core, immutable state) and game design principles.




## Recommendations

**1. Turn tracking** — Add a `turn: Int` field to `GameState`. Increment on every command except `look`. Status triggers with "every X turns" check against this counter. Simple, consistent, and cheap.

**2. Trigger attachment model** — Keep it separate from Area/Device data classes. Store triggers as a flat list in `World` (or `GameState` if they need runtime state). Each trigger carries an `ownerType` (Area/Device) + `ownerId`. This keeps the core model clean and lets you attach multiple triggers without nesting changes to existing data classes.

**3. Trigger ordering within a category** — JSON insertion order is fine for now. No priority field unless it becomes needed later. Keep it simple.

**4. Multi-effect execution order** — Yes, matter. Effects execute in declaration order. Display text effects just append to the `output` string, so they interleave naturally with status changes.

**5. Trigger state tracking** — `Set<TriggerId>` of consumed single-use triggers on `GameState`. Each trigger gets a unique ID at load time. Clean, cheap to check.

**6. Status trigger: which status?** — Each status trigger references exactly one status name by string. It's part of the trigger definition in JSON.

**7. Activation condition granularity** — Stick with `>` and `<` for now. The plan says "early design" — if you need `>=`/`==`, it's a trivial addition later. Avoid feature creep.

**8. Trigger on game start** — Yes, fire area triggers for the starting area on initialization. Otherwise the first `move` out and back is needed to trigger them, which feels wrong. Fire this in `assembleGame` or at the top of the first command loop iteration.

**9. Effect scope for health** — Health stays special. Triggers can modify it via a dedicated `ChangeHealth` effect type, not through the generic status system. Keeps the existing `Player.adjustHealth` logic intact and avoids confusion.

**10. Scenario file format** — Separate triggers file referenced from `ScenarioConfig`, like areas/devices already are. Each trigger entry has: `id`, `ownerType`, `ownerId`, `condition`, `effects`, `singleUse`. Keeps the config modular and consistent with the existing pattern.

Want me to write these recommendations into the plan document, or is there anything you'd like to push back on first?

---

## User

5. I think having state inside the trigger itself might be easier to understand. Have the data live near to where it is used and grouped with other related data. Would this produce problems? 6. Status triggers should be owned by one status whose state they are tracking for activation. Once they activate, they may change any other status values as well though. Other recommendations seem fine.

---

## Assistant (Plan · qwen3.6-27b · 20.9s)

_Thinking:_

The user wants to:

1. For point 5 (trigger state tracking): Put state INSIDE the trigger itself rather than a separate `Set<TriggerId>` on GameState. They want data co-located with where it's used. I need to think about whether this causes problems.

2. For point 6 (status triggers): Clarify that status triggers are owned by a specific status they track, but can affect any other statuses on activation.

Let me think through their feedback:

**Point 5 - Trigger state inside the trigger:**
The concern is about having mutable state in what would otherwise be an immutable data model. Currently the codebase uses pure functional patterns with `copy()`. If triggers carry their own state (e.g., a `fired` boolean or `fireCount`), then we'd need to update the trigger collection on each state transition.
 
Since triggers are part of World or GameState, and those are immutable data classes, updating a consumed trigger would mean copying the entire trigger collection with that one change. This is actually feasible—it's just replacing one trigger in the list/markings as a problem worth solving yet. I should write up these recommendations clearly so we can move forward with implementation.




## On Point 5: Trigger-internal state

No real problems, but trade-offs worth noting:

**Pro:** Co-location is clearer — you read the trigger definition and see its consumption rules right there. No cross-referencing a separate set.

**Con:** It means triggers can't be pure data anymore — they'd carry runtime state like `remainingActivations` or a `fireCount`. Since your architecture is immutable (`copy()` everywhere), this just means replacing the trigger in the collection on each fire:
```
GameState.triggers = triggers.map { if (it.id == firedId) it.copy(remainingActivations = 0) else it }
```

That's fine for the expected scale. Only a problem if you have thousands of triggers and do O(N) scans every command — unlikely.

One subtlety: for "every X turns" triggers, you'd also need `lastFireTurn` on the trigger. That's still just a field on the trigger data class. No architectural issue.

**Recommendation:** Keep it inside the trigger. It aligns with your "data near usage" principle and doesn't break the functional model.

## On Point 6: Status trigger ownership

Clear. A status trigger is attached to one status name — that's what it monitors for activation (`status "corruption" > 15`). Effects can still target any other statuses, health, or display text. This means:

- The triggers collection needs to be indexable by the owned status name so you don't scan all triggers on every status change
- Or, simpler: just check all status triggers after every command (since there likely won't be many)

Want me to write the updated plan into `plans/TRIGGERS.md` now?

---

## User

update the plan

---

## Assistant (Build · qwen3.6-27b · 3.2s)

_Thinking:_

Let me update the plans/TRIGGERS.md file with the detailed plan incorporating all recommendations.






**Tool: read**

**Input:**
```json
{
  "filePath": "/var/home/mikharju/dev/exploration/engine/plans/TRIGGERS.md"
}
```

**Output:**
```
<path>/var/home/mikharju/dev/exploration/engine/plans/TRIGGERS.md</path>
<type>file</type>
<content>
1: # Triggers
2: 
3: ## Task
4: 
5: Make plan for adding triggers.
6: 
7: ## General descripton
8: 
9: Triggers conditionally change player status or display text. Triggers are loaded from scenario files.
10: 
11: ## Activation conditions
12: 
13: All triggers have one of the following conditions for activation
14: - value of this status > given threshold
15: - value of this status < given threshold
16: 
17: All triggers may be either single activation, where they are removed after they 
18: trigger once, or they may be multi activation, where they remain in place and 
19: may activate more than once.
20: 
21: ## Effects on activation
22: 
23: All triggers may have multiple effects once they activate
24: - display text
25: - change any player status by adding or subtracting
26: - set any player status to given value
27:  
28: ## Trigger types
29: 
30: ### Status trigger
31: 
32: - One status may have multiple triggers
33: - Additional option for activation condition: once every X turns
34: - Activation condition checked when player moves or activates devices
35: - Check activation conditions last after all other triggers
36: 
37: ### Device trigger
38: 
39: - One device may have multiple triggers
40: - Activation condition checked when player activates device
41: - Check activation before status triggers
42: 
43: ### Area trigger
44: 
45: - One area may have multiple triggers
46: - Activation condition checked when player enters area
47: - Check activation condition before status triggers or device triggers

(End of file - total 47 lines)
</content>
```

---

## Assistant (Build · qwen3.6-27b · 49.8s)

_Thinking:_

Let me think about the updated plan more carefully. I need to incorporate all the decisions we've made and write a detailed implementation plan. Let me also think about what needs to be considered for each part.

Key decisions:
1. Turn tracking in GameState, increment on non-look commands
2. Flat trigger list with ownerType+ownerId fields
3. JSON insertion order for execution within same category
4. Effects execute in declaration order
5. Trigger state (remainingActivations, lastFireTurn) lives inside the trigger itself
6. Status triggers owned by one status name they monitor
7. > and < only for activation conditions
8. Fire area entry triggers on game start
9. Health is separate from statuses, has its own effect type


10. Triggers will be loaded from a separate JSON file referenced in ScenarioConfig

Now I'll write out the detailed implementation plan with all these specifications integrated.






**Tool: write**

**Input:**
```json
{
  "content": "# Triggers\n\n## Task\n\nAdd triggers: conditions that fire during gameplay to change player state or display text.\n\n## Overview\n\nTriggers are loaded from scenario JSON, stored as a flat list on `GameState`, and checked at specific points in the command flow. Trigger runtime state (consumption count, last-fire turn) lives inside each trigger data class.\n\n## Data Model\n\n### Trigger Data Class\n\n```kotlin\ndata class Trigger(\n    val id: String,\n    val ownerType: OwnerType,   // AREA, DEVICE, STATUS\n    val ownerId: String,         // area name, device ID, or status name\n    val condition: ActivationCondition,\n    val effects: List<Effect>,\n    val singleUse: Boolean = true,\n    // Runtime state (mutable via copy):\n    val remainingActivations: Int = if (singleUse) 1 else Int.MAX_VALUE,\n    val lastFireTurn: Int? = null\n)\n\nenum class OwnerType { AREA, DEVICE, STATUS }\n\nsealed class ActivationCondition {\n    data class GreaterThan(val threshold: Int) : ActivationCondition()\n    data class LessThan(val threshold: Int) : ActivationCondition()\n}\n\nsealed class Effect {\n    data class DisplayText(val text: String) : Effect()\n    data class ChangeHealth(val amount: Int) : Effect()\n    data class AdjustStatus(val statusName: String, val amount: Int) : Effect()\n    data class SetStatus(val statusName: String, val value: Int) : Effect()\n}\n```\n\n### Why flat list with owner fields (not nested under Area/Device)\n\n- Keeps `Area` and `Device` models unchanged — triggers are gameplay logic, not entity properties\n- Status triggers have no natural parent entity to nest under\n- Single collection is simpler to iterate during evaluation\n- No nesting changes to existing data classes\n\n### Why runtime state inside the trigger\n\n- Data co-location: consumption rules visible alongside the definition\n- Immutable updates via `copy()` in the flat list — cheap at expected scale\n- Fields: `remainingActivations` for single/repeat use, `lastFireTurn` for \"every X turns\" check\n\n## Activation Conditions\n\nAll triggers monitor one value and fire when a threshold is crossed:\n\n| Trigger Type | Monitored Value | Condition Options |\n|---|---|---|\n| Status trigger | The status it owns | `> threshold`, `< threshold` |\n| Device trigger | Always true on activation | (no condition — fires unconditionally) |\n| Area trigger | Always true on entry | (no condition — fires unconditionally) |\n\nOnly status triggers have a numeric threshold. Device and area triggers fire whenever their owning event occurs, subject to consumption rules.\n\nStatus triggers also support `fireInterval: Int` — if set, the trigger can only activate once every N turns (checked against `lastFireTurn`).\n\n## Effects on Activation\n\nEffects execute in declaration order. Health is separate from statuses:\n- `ChangeHealth(amount)` modifies health via existing `Player.adjustHealth`\n- `AdjustStatus(name, amount)` / `SetStatus(name, value)` modify named statuses within their bounds\n\nA trigger can have multiple effects of any combination.\n\n## Trigger Evaluation Flow\n\nFor each command in `processCommand`:\n\n```\n1. Execute command logic (move/activate/look) → intermediate state\n2. Fire area triggers (if player moved to new area)\n3. Fire device triggers (if player activated a device)\n4. Increment turn counter (unless command was Look)\n5. Check status triggers (after all state changes from this command)\n6. Run checkOutcome() for win/lose\n```\n\nArea entry triggers fire on game start for the starting area, evaluated in `assembleGame` after initial state is built.\n\n### Firing a Trigger Set\n\nFor each candidate trigger:\n1. Skip if `remainingActivations <= 0`\n2. For \"every X turns\" triggers, skip if current turn - lastFireTurn < interval\n3. Check condition (status value vs threshold, or unconditional)\n4. If met: apply effects in order, decrement remainingActivations, set lastFireTurn\n\n## Scenario File Format\n\nNew trigger file referenced from `ScenarioConfig`:\n\n```kotlin\ndata class ScenarioConfig(\n    val playerStart: PlayerStart,\n    val areasFile: String,\n    val devicesFile: String,\n    val statuses: Map<String, StatusEntry> = emptyMap(),\n    val triggersFile: String? = null   // NEW\n)\n\n@Serializable\ndata class TriggerEntry(\n    val id: String,\n    val ownerType: OwnerType,\n    val ownerId: String,\n    val condition: TriggerCondition?,   // null for area/device (unconditional)\n    val effects: List<EffectEntry>,\n    val singleUse: Boolean = true,\n    val fireInterval: Int? = null       // every N turns, for status triggers\n)\n\n@Serializable\ndata class TriggerCondition(\n    val op: String,   // \">\" or \"<\"\n    val threshold: Int\n)\n```\n\n## GameState Changes\n\n```kotlin\ndata class GameState(\n    // ... existing fields ...\n    val turn: Int = 0,\n    val triggers: List<Trigger> = emptyList()\n)\n```\n\n`turn` increments after each non-Look command. Used for \"every X turns\" interval check and for status trigger timing.\n\n## Implementation Order\n\n1. Add `Trigger`, `OwnerType`, `ActivationCondition`, `Effect` data classes to `model/`\n2. Add `TriggerEntry`, `TriggerCondition`, `EffectEntry` serializable types to `scenario/ScenarioFile.kt`\n3. Extend `GameState` with `turn` and `triggers` fields\n4. Write trigger loading logic in `ScenarioLoader` — map entries to domain objects, validate owner references exist\n5. Implement `evaluateTriggers(GameState, TriggerEventType) -> GameState` helper\n6. Wire into `CommandProcessor`: area triggers after move, device triggers after activate, status triggers at end of every non-look command\n7. Fire starting-area triggers in `assembleGame`\n8. Tests for trigger evaluation logic and integration\n",
  "filePath": "/var/home/mikharju/dev/exploration/engine/plans/TRIGGERS.md"
}
```

**Output:**
```
Wrote file successfully.
```

---

## Assistant (Build · qwen3.6-27b · 10.5s)

_Thinking:_

Wait, I made an error in the plan. Let me re-read it to check for issues.

Looking at the trigger evaluation flow - I said "increment turn counter (unless command was Look)" but then also said status triggers are checked after incrementing. But status triggers need the updated turn count. That's fine actually.

Also, I should double-check: in step 4 of evaluation flow, I say "Increment turn counter (unless command was Look)". But for status triggers to check against `lastFireTurn` with the interval, they need access to the current turn value. Since we're doing immutable updates via copy(), this is fine - we increment turn and then pass it to status trigger checking.

Let me also verify that the plan mentions effect output handling - effects produce text that should be appended to GameState.output. I have `DisplayText` as an effect type but should note how output concatenation works when multiple triggers fire. Let me add a note about that.




Done. Added one clarification about output handling:



**Tool: edit**

**Input:**
```json
{
  "filePath": "/var/home/mikharju/dev/exploration/engine/plans/TRIGGERS.md",
  "oldString": "A trigger can have multiple effects of any combination.",
  "newString": "A trigger can have multiple effects of any combination. Text from `DisplayText` effects appends to the command's output string. When multiple triggers fire in one step, their text outputs concatenate in firing order."
}
```

**Output:**
```
Edit applied successfully.
```

---

## Assistant (Build · qwen3.6-27b · 8.1s)

_Thinking:_

Done with the plan update. Let me summarize what was added for the user.




Updated `plans/TRIGGERS.md` with:

- **Data model**: Full Kotlin data classes for `Trigger`, `OwnerType`, `ActivationCondition`, and sealed `Effect` types (including separate `ChangeHealth`)
- **Flat list design**: Triggers stored on `GameState`, identified by `(ownerType, ownerId)` pairs — no nesting under Area/Device
- **Runtime state in triggers**: `remainingActivations` + `lastFireTurn` fields for consumption tracking via immutable `copy()`
- **Status trigger ownership**: Each status trigger owns one status name it monitors; effects can target any other statuses or health
- **Evaluation flow**: Explicit ordering — command → area triggers → device triggers → increment turn → status triggers → checkOutcome
- **Game-start firing**: Starting area triggers fire in `assembleGame`
- **Scenario file format**: New optional `triggersFile` on `ScenarioConfig`, with serializable entry types matching the domain model
- **GameState changes**: Added `turn: Int` and `triggers: List<Trigger>` fields
- **8-step implementation order** from data classes through integration tests

---

## User

Area and device triggers should also be able to have activation condition that checks one player status value against a threshold.

---

## Assistant (Build · qwen3.6-27b · 18.9s)

_Thinking:_

The user wants area and device triggers to also support status-based activation conditions (checking a player status value against a threshold), not just be unconditional. This makes all three trigger types have the same condition structure - they all check a status value against a threshold. The difference is when they're checked, not what they check.

Let me update the plan document to reflect this change.






**Tool: edit**

**Input:**
```json
{
  "filePath": "/var/home/mikharju/dev/exploration/engine/plans/TRIGGERS.md",
  "oldString": "## Activation Conditions\n\nAll triggers monitor one value and fire when a threshold is crossed:\n\n| Trigger Type | Monitored Value | Condition Options |\n|---|---|---|\n| Status trigger | The status it owns | `> threshold`, `< threshold` |\n| Device trigger | Always true on activation | (no condition — fires unconditionally) |\n| Area trigger | Always true on entry | (no condition — fires unconditional) |\n\nOnly status triggers have a numeric threshold. Device and area triggers fire whenever their owning event occurs, subject to consumption rules.\n\nStatus triggers also support `fireInterval: Int` — if set, the trigger can only activate once every N turns (checked against `lastFireTurn`).",
  "newString": "## Activation Conditions\n\nAll three trigger types share the same activation condition structure — they monitor a named player status value against a threshold:\n\n| Trigger Type | When Checked | Condition Options |\n|---|---|---|\n| Status trigger | After every non-Look command | `status X > threshold`, `status X < threshold` |\n| Device trigger | When activating the owned device | `status X > threshold`, `status X < threshold` |\n| Area trigger | When entering the owned area | `status X > threshold`, `status X < threshold` |\n\nThe condition is optional — if omitted, the trigger fires unconditionally on its owning event.\n\nStatus triggers additionally support `fireInterval: Int` — if set, the trigger can only activate once every N turns (checked against `lastFireTurn`)."
}
```

**Output:**
```
Edit applied successfully.
```

---

## Assistant (Build · qwen3.6-27b · 16.4s)

_Thinking:_

Now I need to update the data model section and the scenario file format so that all triggers reference a status name for their condition, not just status triggers. The `ownerId` for status triggers is still the status they track, but device/area triggers now also have a separate `conditionStatusName` field (or we make it part of the condition struct itself).






**Tool: edit**

**Input:**
```json
{
  "filePath": "/var/home/mikharju/dev/exploration/engine/plans/TRIGGERS.md",
  "oldString": "data class Trigger(\n    val id: String,\n    val ownerType: OwnerType,   // AREA, DEVICE, STATUS\n    val ownerId: String,         // area name, device ID, or status name\n    val condition: ActivationCondition,\n    val effects: List<Effect>,\n    val singleUse: Boolean = true,\n    // Runtime state (mutable via copy):\n    val remainingActivations: Int = if (singleUse) 1 else Int.MAX_VALUE,\n    val lastFireTurn: Int? = null\n)\n\nenum class OwnerType { AREA, DEVICE, STATUS }\n\nsealed class ActivationCondition {\n    data class GreaterThan(val threshold: Int) : ActivationCondition()\n    data class LessThan(val threshold: Int) : ActivationCondition()\n}",
  "newString": "data class Trigger(\n    val id: String,\n    val ownerType: OwnerType,   // AREA, DEVICE, STATUS\n    val ownerId: String,         // area name, device ID, or status name\n    val condition: ActivationCondition?,  // null = unconditional fire\n    val effects: List<Effect>,\n    val singleUse: Boolean = true,\n    // Runtime state (mutable via copy):\n    val remainingActivations: Int = if (singleUse) 1 else Int.MAX_VALUE,\n    val lastFireTurn: Int? = null\n)\n\nenum class OwnerType { AREA, DEVICE, STATUS }\n\ndata class ActivationCondition(\n    val statusName: String,      // which player status to check\n    val op: ComparisonOp,        // \">\" or \"<\"\n    val threshold: Int\n)\n\nenum class ComparisonOp { GT, LT }"
}
```

**Output:**
```
Edit applied successfully.
```

---

## Assistant (Build · qwen3.6-27b · 8.5s)

_Thinking:_

Now update the firing logic section to reflect that area/device triggers also check conditions now, not just unconditional fire.






**Tool: edit**

**Input:**
```json
{
  "filePath": "/var/home/mikharju/dev/exploration/engine/plans/TRIGGERS.md",
  "oldString": "### Firing a Trigger Set\n\nFor each candidate trigger:\n1. Skip if `remainingActivations <= 0`\n2. For \"every X turns\" triggers, skip if current turn - lastFireTurn < interval\n3. Check condition (status value vs threshold, or unconditional)\n4. If met: apply effects in order, decrement remainingActivations, set lastFireTurn",
  "newString": "### Firing a Trigger Set\n\nFor each candidate trigger:\n1. Skip if `remainingActivations <= 0`\n2. For \"every X turns\" triggers, skip if current turn - lastFireTurn < interval\n3. Check condition — read the named status value and compare against threshold (skip if condition absent)\n4. If met: apply effects in order, decrement remainingActivations, set lastFireTurn"
}
```

**Output:**
```
Edit applied successfully.
```

---

## Assistant (Build · qwen3.6-27b · 11.0s)

_Thinking:_

Now update the scenario file format section to match the new ActivationCondition structure with statusName.






**Tool: edit**

**Input:**
```json
{
  "filePath": "/var/home/mikharju/dev/exploration/engine/plans/TRIGGERS.md",
  "oldString": "@Serializable\ndata class TriggerEntry(\n    val id: String,\n    val ownerType: OwnerType,\n    val ownerId: String,\n    val condition: TriggerCondition?,   // null for area/device (unconditional)\n    val effects: List<EffectEntry>,\n    val singleUse: Boolean = true,\n    val fireInterval: Int? = null       // every N turns, for status triggers\n)\n\n@Serializable\ndata class TriggerCondition(\n    val op: String,   // \">\" or \"<\"\n    val threshold: Int\n)",
  "newString": "@Serializable\ndata class TriggerEntry(\n    val id: String,\n    val ownerType: OwnerType,\n    val ownerId: String,\n    val condition: TriggerCondition?,   // null = unconditional fire\n    val effects: List<EffectEntry>,\n    val singleUse: Boolean = true,\n    val fireInterval: Int? = null       // every N turns, for status triggers\n)\n\n@Serializable\ndata class TriggerCondition(\n    val statusName: String,             // which player status to check\n    val op: String,                     // \">\" or \"<\"\n    val threshold: Int\n)"
}
```

**Output:**
```
Edit applied successfully.
```

---

## Assistant (Build · qwen3.6-27b · 2.6s)

_Thinking:_

Done updating the plan. Let me summarize what changed for the user.




Updated. All three trigger types now share the same condition model — each optionally monitors a named status against `>` / `<` threshold. If `condition` is null, fires unconditionally on its owning event (area entry, device activation, or post-command check).

---

## User

Lets change more: Allow all triggers to have multiple activation conditions. All conditions must be met for trigger to activate. Once per X turns in status trigger can be a simple property of the status trigger. So it checks conditions every turn, but if all conditions are met, then it also checks that enough turns have passed since last activation.

---

## Assistant (Build · qwen3.6-27b · 19.7s)

_Thinking:_

The user wants two changes:

1. All triggers can have multiple activation conditions (all must be met — AND logic)
2. "Once per X turns" becomes a simple property of status triggers that checks after all conditions are met

Let me update the plan document accordingly.






**Tool: edit**

**Input:**
```json
{
  "filePath": "/var/home/mikharju/dev/exploration/engine/plans/TRIGGERS.md",
  "oldString": "data class Trigger(\n    val id: String,\n    val ownerType: OwnerType,   // AREA, DEVICE, STATUS\n    val ownerId: String,         // area name, device ID, or status name\n    val condition: ActivationCondition?,  // null = unconditional fire\n    val effects: List<Effect>,\n    val singleUse: Boolean = true,\n    // Runtime state (mutable via copy):\n    val remainingActivations: Int = if (singleUse) 1 else Int.MAX_VALUE,\n    val lastFireTurn: Int? = null\n)\n\nenum class OwnerType { AREA, DEVICE, STATUS }\n\ndata class ActivationCondition(\n    val statusName: String,      // which player status to check\n    val op: ComparisonOp,        // \">\" or \"<\"\n    val threshold: Int\n)\n\nenum class ComparisonOp { GT, LT }",
  "newString": "data class Trigger(\n    val id: String,\n    val ownerType: OwnerType,   // AREA, DEVICE, STATUS\n    val ownerId: String,         // area name, device ID, or status name\n    val conditions: List<ActivationCondition>,  // empty = unconditional fire\n    val effects: List<Effect>,\n    val singleUse: Boolean = true,\n    val intervalTurns: Int? = null,              // status triggers only — min turns between fires\n    // Runtime state (mutable via copy):\n    val remainingActivations: Int = if (singleUse) 1 else Int.MAX_VALUE,\n    val lastFireTurn: Int? = null\n)\n\nenum class OwnerType { AREA, DEVICE, STATUS }\n\ndata class ActivationCondition(\n    val statusName: String,      // which player status to check\n    val op: ComparisonOp,        // \">\" or \"<\"\n    val threshold: Int\n)\n\nenum class ComparisonOp { GT, LT }"
}
```

**Output:**
```
Edit applied successfully.
```

---

## Assistant (Build · qwen3.6-27b · 13.6s)

_Thinking:_

Let me continue updating the plan document with the changes for multiple activation conditions and the interval turns property.






**Tool: edit**

**Input:**
```json
{
  "filePath": "/var/home/mikharju/dev/exploration/engine/plans/TRIGGERS.md",
  "oldString": "## Activation Conditions\n\nAll three trigger types share the same activation condition structure — they monitor a named player status value against a threshold:\n\n| Trigger Type | When Checked | Condition Options |\n|---|---|---|\n| Status trigger | After every non-Look command | `status X > threshold`, `status X < threshold` |\n| Device trigger | When activating the owned device | `status X > threshold`, `status X < threshold` |\n| Area trigger | When entering the owned area | `status X > threshold`, `status X < threshold` |\n\nThe condition is optional — if omitted, the trigger fires unconditionally on its owning event.\n\nStatus triggers additionally support `fireInterval: Int` — if set, the trigger can only activate once every N turns (checked against `lastFireTurn`).",
  "newString": "## Activation Conditions\n\nA trigger can have zero or more activation conditions. All conditions must be met for the trigger to fire (AND logic). If a trigger has no conditions, it fires unconditionally on its owning event.\n\nEach condition checks one player status value against a threshold: `status X > N` or `status X < N`.\n\n| Trigger Type | When Checked |\n|---|---|\n| Status trigger | After every non-Look command |\n| Device trigger | When activating the owned device |\n| Area trigger | When entering the owned area |\n\nStatus triggers additionally support `intervalTurns: Int` — once all conditions are met, the trigger also checks that enough turns have passed since `lastFireTurn`. This lets a status trigger fire repeatedly at fixed intervals while the condition holds."
}
```

**Output:**
```
Edit applied successfully.
```

---

## Assistant (Build · qwen3.6-27b · 8.6s)

_Thinking:_

Now I need to update the firing logic section and the scenario file format section.






**Tool: edit**

**Input:**
```json
{
  "filePath": "/var/home/mikharju/dev/exploration/engine/plans/TRIGGERS.md",
  "oldString": "### Firing a Trigger Set\n\nFor each candidate trigger:\n1. Skip if `remainingActivations <= 0`\n2. For \"every X turns\" triggers, skip if current turn - lastFireTurn < interval\n3. Check condition — read the named status value and compare against threshold (skip if condition absent)\n4. If met: apply effects in order, decrement remainingActivations, set lastFireTurn",
  "newString": "### Firing a Trigger Set\n\nFor each candidate trigger:\n1. Skip if `remainingActivations <= 0`\n2. Check all conditions — every condition must evaluate to true (read status value, compare against threshold)\n3. For status triggers with `intervalTurns`, also check that enough turns have passed since `lastFireTurn`\n4. If everything passes: apply effects in order, decrement remainingActivations, set lastFireTurn"
}
```

**Output:**
```
Edit applied successfully.
```

---

## Assistant (Build · qwen3.6-27b · 11.3s)

_Thinking:_

Now update the scenario file format section to reflect multiple conditions.






**Tool: edit**

**Input:**
```json
{
  "filePath": "/var/home/mikharju/dev/exploration/engine/plans/TRIGGERS.md",
  "oldString": "@Serializable\ndata class TriggerEntry(\n    val id: String,\n    val ownerType: OwnerType,\n    val ownerId: String,\n    val condition: TriggerCondition?,   // null = unconditional fire\n    val effects: List<EffectEntry>,\n    val singleUse: Boolean = true,\n    val fireInterval: Int? = null       // every N turns, for status triggers\n)\n\n@Serializable\ndata class TriggerCondition(\n    val statusName: String,             // which player status to check\n    val op: String,                     // \">\" or \"<\"\n    val threshold: Int\n)",
  "newString": "@Serializable\ndata class TriggerEntry(\n    val id: String,\n    val ownerType: OwnerType,\n    val ownerId: String,\n    val conditions: List<TriggerCondition> = emptyList(),  // empty = unconditional fire\n    val effects: List<EffectEntry>,\n    val singleUse: Boolean = true,\n    val intervalTurns: Int? = null      // status triggers only — min turns between fires\n)\n\n@Serializable\ndata class TriggerCondition(\n    val statusName: String,             // which player status to check\n    val op: String,                     // \">\" or \"<\"\n    val threshold: Int\n)"
}
```

**Output:**
```
Edit applied successfully.
```

---

## Assistant (Build · qwen3.6-27b · 3.7s)

_Thinking:_

Let me also update the implementation order section if needed. It probably doesn't need changes since the data model already reflects these changes. Let me check and see if there's anything else that needs updating in the plan.

Actually, let me read through the updated file to make sure everything is consistent.






**Tool: read**

**Input:**
```json
{
  "filePath": "/var/home/mikharju/dev/exploration/engine/plans/TRIGGERS.md"
}
```

**Output:**
```
<path>/var/home/mikharju/dev/exploration/engine/plans/TRIGGERS.md</path>
<type>file</type>
<content>
1: # Triggers
2: 
3: ## Task
4: 
5: Add triggers: conditions that fire during gameplay to change player state or display text.
6: 
7: ## Overview
8: 
9: Triggers are loaded from scenario JSON, stored as a flat list on `GameState`, and checked at specific points in the command flow. Trigger runtime state (consumption count, last-fire turn) lives inside each trigger data class.
10: 
11: ## Data Model
12: 
13: ### Trigger Data Class
14: 
15: ```kotlin
16: data class Trigger(
17:     val id: String,
18:     val ownerType: OwnerType,   // AREA, DEVICE, STATUS
19:     val ownerId: String,         // area name, device ID, or status name
20:     val conditions: List<ActivationCondition>,  // empty = unconditional fire
21:     val effects: List<Effect>,
22:     val singleUse: Boolean = true,
23:     val intervalTurns: Int? = null,              // status triggers only — min turns between fires
24:     // Runtime state (mutable via copy):
25:     val remainingActivations: Int = if (singleUse) 1 else Int.MAX_VALUE,
26:     val lastFireTurn: Int? = null
27: )
28: 
29: enum class OwnerType { AREA, DEVICE, STATUS }
30: 
31: data class ActivationCondition(
32:     val statusName: String,      // which player status to check
33:     val op: ComparisonOp,        // ">" or "<"
34:     val threshold: Int
35: )
36: 
37: enum class ComparisonOp { GT, LT }
38: 
39: sealed class Effect {
40:     data class DisplayText(val text: String) : Effect()
41:     data class ChangeHealth(val amount: Int) : Effect()
42:     data class AdjustStatus(val statusName: String, val amount: Int) : Effect()
43:     data class SetStatus(val statusName: String, val value: Int) : Effect()
44: }
45: ```
46: 
47: ### Why flat list with owner fields (not nested under Area/Device)
48: 
49: - Keeps `Area` and `Device` models unchanged — triggers are gameplay logic, not entity properties
50: - Status triggers have no natural parent entity to nest under
51: - Single collection is simpler to iterate during evaluation
52: - No nesting changes to existing data classes
53: 
54: ### Why runtime state inside the trigger
55: 
56: - Data co-location: consumption rules visible alongside the definition
57: - Immutable updates via `copy()` in the flat list — cheap at expected scale
58: - Fields: `remainingActivations` for single/repeat use, `lastFireTurn` for "every X turns" check
59: 
60: ## Activation Conditions
61: 
62: A trigger can have zero or more activation conditions. All conditions must be met for the trigger to fire (AND logic). If a trigger has no conditions, it fires unconditionally on its owning event.
63: 
64: Each condition checks one player status value against a threshold: `status X > N` or `status X < N`.
65: 
66: | Trigger Type | When Checked |
67: |---|---|
68: | Status trigger | After every non-Look command |
69: | Device trigger | When activating the owned device |
70: | Area trigger | When entering the owned area |
71: 
72: Status triggers additionally support `intervalTurns: Int` — once all conditions are met, the trigger also checks that enough turns have passed since `lastFireTurn`. This lets a status trigger fire repeatedly at fixed intervals while the condition holds.
73: 
74: ## Effects on Activation
75: 
76: Effects execute in declaration order. Health is separate from statuses:
77: - `ChangeHealth(amount)` modifies health via existing `Player.adjustHealth`
78: - `AdjustStatus(name, amount)` / `SetStatus(name, value)` modify named statuses within their bounds
79: 
80: A trigger can have multiple effects of any combination. Text from `DisplayText` effects appends to the command's output string. When multiple triggers fire in one step, their text outputs concatenate in firing order.
81: 
82: ## Trigger Evaluation Flow
83: 
84: For each command in `processCommand`:
85: 
86: ```
87: 1. Execute command logic (move/activate/look) → intermediate state
88: 2. Fire area triggers (if player moved to new area)
89: 3. Fire device triggers (if player activated a device)
90: 4. Increment turn counter (unless command was Look)
91: 5. Check status triggers (after all state changes from this command)
92: 6. Run checkOutcome() for win/lose
93: ```
94: 
95: Area entry triggers fire on game start for the starting area, evaluated in `assembleGame` after initial state is built.
96: 
97: ### Firing a Trigger Set
98: 
99: For each candidate trigger:
100: 1. Skip if `remainingActivations <= 0`
101: 2. Check all conditions — every condition must evaluate to true (read status value, compare against threshold)
102: 3. For status triggers with `intervalTurns`, also check that enough turns have passed since `lastFireTurn`
103: 4. If everything passes: apply effects in order, decrement remainingActivations, set lastFireTurn
104: 
105: ## Scenario File Format
106: 
107: New trigger file referenced from `ScenarioConfig`:
108: 
109: ```kotlin
110: data class ScenarioConfig(
111:     val playerStart: PlayerStart,
112:     val areasFile: String,
113:     val devicesFile: String,
114:     val statuses: Map<String, StatusEntry> = emptyMap(),
115:     val triggersFile: String? = null   // NEW
116: )
117: 
118: @Serializable
119: data class TriggerEntry(
120:     val id: String,
121:     val ownerType: OwnerType,
122:     val ownerId: String,
123:     val conditions: List<TriggerCondition> = emptyList(),  // empty = unconditional fire
124:     val effects: List<EffectEntry>,
125:     val singleUse: Boolean = true,
126:     val intervalTurns: Int? = null      // status triggers only — min turns between fires
127: )
128: 
129: @Serializable
130: data class TriggerCondition(
131:     val statusName: String,             // which player status to check
132:     val op: String,                     // ">" or "<"
133:     val threshold: Int
134: )
135: ```
136: 
137: ## GameState Changes
138: 
139: ```kotlin
140: data class GameState(
141:     // ... existing fields ...
142:     val turn: Int = 0,
143:     val triggers: List<Trigger> = emptyList()
144: )
145: ```
146: 
147: `turn` increments after each non-Look command. Used for "every X turns" interval check and for status trigger timing.
148: 
149: ## Implementation Order
150: 
151: 1. Add `Trigger`, `OwnerType`, `ActivationCondition`, `Effect` data classes to `model/`
152: 2. Add `TriggerEntry`, `TriggerCondition`, `EffectEntry` serializable types to `scenario/ScenarioFile.kt`
153: 3. Extend `GameState` with `turn` and `triggers` fields
154: 4. Write trigger loading logic in `ScenarioLoader` — map entries to domain objects, validate owner references exist
155: 5. Implement `evaluateTriggers(GameState, TriggerEventType) -> GameState` helper
156: 6. Wire into `CommandProcessor`: area triggers after move, device triggers after activate, status triggers at end of every non-look command
157: 7. Fire starting-area triggers in `assembleGame`
158: 8. Tests for trigger evaluation logic and integration

(End of file - total 158 lines)
</content>
```

---

## Assistant (Build · qwen3.6-27b · 11.3s)

_Thinking:_

The plan looks good now with the changes applied. Let me review it one more time to make sure everything is consistent and complete.

Actually, let me also update the implementation order section since we changed from single condition to multiple conditions and renamed fireInterval to intervalTurns.

Wait, looking at the updated content, the data model already reflects:
- `conditions: List<ActivationCondition>` instead of a single optional condition
- `intervalTurns` instead of `fireInterval`
- The firing logic checks "all conditions" with AND logic

The implementation order section still looks fine. Let me check if there's anything else that needs updating...


Everything in the plan is now aligned — multiple conditions are enforced with AND logic, and interval turns functions as a status-only cooldown property. The changes are ready to go.




Updated. Changes:

- **Multiple conditions**: `conditions: List<ActivationCondition>` — empty list means unconditional fire, all must be true (AND) for the trigger to activate
- **intervalTurns**: Moved from separate concept into a simple `Int?` property on the Trigger data class. Status triggers check it after all conditions pass — enforces minimum turns between activations

---

