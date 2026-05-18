package exploration.scenario

import exploration.core.engine.fireAreaTriggers
import exploration.core.engine.fireDeviceTriggers
import exploration.core.engine.fireStatusTriggers
import exploration.model.*
import exploration.state.GameState
import org.junit.jupiter.api.Test
import kotlin.test.*

class TriggerTest {

    @Test
    fun `area trigger fires on entry`() {
        val state = baseState().copy(
            triggers = listOf(Trigger("t1", OwnerType.AREA, "Forest", emptyList(), listOf(Effect.DisplayText("You enter the forest."))))
        )
        val result = fireAreaTriggers(state, AreaId("Forest"))
        assertTrue(result.triggerTexts.joinToString("\n").contains("You enter the forest."))
    }

    @Test
    fun `area trigger is single use`() {
        val state = baseState().copy(
            triggers = listOf(Trigger("t1", OwnerType.AREA, "Forest", emptyList(), listOf(Effect.ChangeHealth(-2))))
        )
        val r1 = fireAreaTriggers(state, AreaId("Forest"))
        assertEquals(8, r1.player.health)
        val r2 = fireAreaTriggers(r1, AreaId("Forest"))
        assertEquals(8, r2.player.health)
    }

    @Test
    fun `area trigger with condition that is not met does not fire`() {
        val state = baseState().copy(
            triggers = listOf(Trigger("t1", OwnerType.AREA, "Forest", listOf(ActivationCondition(checkType = CheckType.STATUS, statusName = "stamina", op = ComparisonOp.GT, threshold = 50)), listOf(Effect.ChangeHealth(-3))))
        )
        val result = fireAreaTriggers(state, AreaId("Forest"))
        assertEquals(10, result.player.health)
    }

    @Test
    fun `area trigger with condition met fires`() {
        val state = baseState(statuses = mapOf("stamina" to 60)).copy(
            statusBounds = mapOf("stamina" to StatusRange(0, 100)),
            triggers = listOf(Trigger("t1", OwnerType.AREA, "Forest", listOf(ActivationCondition(checkType = CheckType.STATUS, statusName = "stamina", op = ComparisonOp.GT, threshold = 50)), listOf(Effect.ChangeHealth(-3))))
        )
        val result = fireAreaTriggers(state, AreaId("Forest"))
        assertEquals(7, result.player.health)
    }

    @Test
    fun `device trigger fires on activation`() {
        val state = baseState().copy(
            triggers = listOf(Trigger("t1", OwnerType.DEVICE, "Orb", emptyList(), listOf(Effect.AdjustStatus("magic", 5))))
        )
        val result = fireDeviceTriggers(state, DeviceId("Orb"))
        assertEquals(5, result.player.statuses["magic"])
    }

    @Test
    fun `status trigger with interval fires repeatedly`() {
        val state = baseState(statuses = mapOf("toxicity" to 10)).copy(
            turn = 10,
            statusBounds = mapOf("toxicity" to StatusRange(0, 100)),
            triggers = listOf(Trigger("t1", OwnerType.STATUS, "poison", listOf(ActivationCondition(checkType = CheckType.STATUS, statusName = "toxicity", op = ComparisonOp.GT, threshold = 5)), listOf(Effect.ChangeHealth(-1)), singleUse = false, intervalTurns = 3))
        )
        val r1 = fireStatusTriggers(state)
        assertEquals(9, r1.player.health)
        val r2 = fireStatusTriggers(r1.copy(turn = 11))
        assertEquals(9, r2.player.health)
        val r3 = fireStatusTriggers(r2.copy(turn = 13))
        assertEquals(8, r3.player.health)
    }

    @Test
    fun `status trigger with no condition fires unconditionally`() {
        val state = baseState().copy(
            turn = 5,
            triggers = listOf(Trigger("t1", OwnerType.STATUS, "ambient", emptyList(), listOf(Effect.DisplayText("wind")), singleUse = false))
        )
        val r1 = fireStatusTriggers(state)
        assertTrue(r1.triggerTexts.joinToString("\n").contains("wind"))
    }

    @Test
    fun `set status effect clamps to bounds`() {
        val state = baseState(statuses = mapOf("mana" to 0)).copy(
            statusBounds = mapOf("mana" to StatusRange(0, 20)),
            triggers = listOf(Trigger("t1", OwnerType.AREA, "Spring", emptyList(), listOf(Effect.SetStatus("mana", 30))))
        )
        val result = fireAreaTriggers(state, AreaId("Spring"))
        assertEquals(20, result.player.statuses["mana"])
    }

    @Test
    fun `set location moves item to area`() {
        val a2 = AreaId("Cell")
        val state = GameState(
            world = World(mapOf(AreaId("Forest") to Area(AreaId("Forest"), "Forest.", setOf(Exit(a2, Direction.East)), device = null), a2 to Area(a2, "Cell.", setOf())), AreaId("Forest")),
            player = Player(10, 20, AreaId("Forest")),
            items = listOf(exploration.model.Item(ItemId("Map"), "A map.")),
            triggers = listOf(Trigger("t1", OwnerType.AREA, "Forest", emptyList(), listOf(Effect.SetLocation(ItemId("Map"), Effect.TargetRef.InArea(a2)))))
        )
        val result = fireAreaTriggers(state, AreaId("Forest"))
        assertEquals(1, result.items.size)
        val loc = result.items[0].location.target as? LocationTarget.InArea
        assertEquals(a2, loc?.areaId)
    }

    @Test
    fun `set location moves item to device`() {
        val state = baseState().copy(
            items = listOf(exploration.model.Item(ItemId("Crystal"), "A glowing crystal.")),
            triggers = listOf(Trigger("t1", OwnerType.AREA, "Forest", emptyList(), listOf(Effect.SetLocation(ItemId("Crystal"), Effect.TargetRef.OnDevice(DeviceId("Orb"))))))
        )
        val result = fireAreaTriggers(state, AreaId("Forest"))
        assertEquals(1, result.items.size)
        val loc = result.items[0].location.target as? LocationTarget.OnDevice
        assertEquals(DeviceId("Orb"), loc?.deviceId)
    }

    @Test
    fun `set location for non-existent item is ignored`() {
        val state = baseState().copy(
            items = listOf(exploration.model.Item(ItemId("Map"), "A map.")),
            triggers = listOf(Trigger("t1", OwnerType.AREA, "Forest", emptyList(), listOf(Effect.SetLocation(ItemId("Ghost"), Effect.TargetRef.InArea(AreaId("Cell"))))))
        )
        val result = fireAreaTriggers(state, AreaId("Forest"))
        assertEquals(ItemLocationType.AREA, result.items[0].location.type)
    }

    @Test
    fun `lock item effect prevents dropping`() {
        val state = baseState().copy(
            items = listOf(exploration.model.Item(ItemId("Key"), "A key.", exploration.model.Location(exploration.model.ItemLocationType.CARRIED))),
            triggers = listOf(Trigger("t1", OwnerType.AREA, "Forest", emptyList(), listOf(Effect.LockItem(ItemId("Key")))))
        )
        val result = fireAreaTriggers(state, AreaId("Forest"))
        assertTrue(result.items[0].locked)
    }

    @Test
    fun `unlock item effect removes lock`() {
        val locked = baseState().copy(
            items = listOf(exploration.model.Item(ItemId("Key"), "A key.", exploration.model.Location(exploration.model.ItemLocationType.CARRIED), locked = true)),
            triggers = listOf(Trigger("t1", OwnerType.AREA, "Forest", emptyList(), listOf(Effect.UnlockItem(ItemId("Key")))))
        )
        val result = fireAreaTriggers(locked, AreaId("Forest"))
        assertFalse(result.items[0].locked)
    }

    @Test
    fun `lock and unlock applied to same item in one batch`() {
        val state = baseState().copy(
            items = listOf(exploration.model.Item(ItemId("Key"), "A key.", exploration.model.Location(exploration.model.ItemLocationType.CARRIED))),
            triggers = listOf(
                Trigger("t1", OwnerType.AREA, "Forest", emptyList(), listOf(Effect.LockItem(ItemId("Key")))),
                Trigger("t2", OwnerType.AREA, "Forest", emptyList(), listOf(Effect.UnlockItem(ItemId("Key"))))
            )
        )
        val result = fireAreaTriggers(state, AreaId("Forest"))
        assertFalse(result.items[0].locked)
    }

    @Test
    fun `item carried condition is met`() {
        val state = baseState().copy(
            items = listOf(exploration.model.Item(ItemId("Map"), "A map.", exploration.model.Location(exploration.model.ItemLocationType.CARRIED))),
            triggers = listOf(Trigger("t1", OwnerType.AREA, "Forest", listOf(ActivationCondition(checkType = CheckType.ITEM_CARRIED, itemId = ItemId("Map"))), listOf(Effect.DisplayText("Found!"))))
        )
        val result = fireAreaTriggers(state, AreaId("Forest"))
        assertTrue(result.triggerTexts.joinToString("\n").contains("Found!"))
    }

    @Test
    fun `item carried condition is not met`() {
        val state = baseState().copy(
            items = listOf(exploration.model.Item(ItemId("Gem"), "A gem.", exploration.model.Location(exploration.model.ItemLocationType.AREA))),
            triggers = listOf(Trigger("t1", OwnerType.AREA, "Forest", listOf(ActivationCondition(checkType = CheckType.ITEM_CARRIED, itemId = ItemId("Gem"))), listOf(Effect.DisplayText("Found!"))))
        )
        val result = fireAreaTriggers(state, AreaId("Forest"))
        assertTrue(result.triggerTexts.isEmpty())
    }

    @Test
    fun `item equipped condition is met`() {
        val state = baseState().copy(
            items = listOf(exploration.model.Item(ItemId("Ring"), "A ring.", exploration.model.Location(exploration.model.ItemLocationType.EQUIPPED))),
            triggers = listOf(Trigger("t1", OwnerType.AREA, "Forest", listOf(ActivationCondition(checkType = CheckType.ITEM_EQUIPPED, itemId = ItemId("Ring"))), listOf(Effect.DisplayText("Blessed!"))))
        )
        val result = fireAreaTriggers(state, AreaId("Forest"))
        assertTrue(result.triggerTexts.joinToString("\n").contains("Blessed!"))
    }

    @Test
    fun `item equipped condition is not met`() {
        val state = baseState().copy(
            items = listOf(exploration.model.Item(ItemId("Ring"), "A ring.", exploration.model.Location(exploration.model.ItemLocationType.CARRIED))),
            triggers = listOf(Trigger("t1", OwnerType.AREA, "Forest", listOf(ActivationCondition(checkType = CheckType.ITEM_EQUIPPED, itemId = ItemId("Ring"))), listOf(Effect.DisplayText("Blessed!"))))
        )
        val result = fireAreaTriggers(state, AreaId("Forest"))
        assertTrue(result.triggerTexts.isEmpty())
    }

    @Test
    fun `item carried condition with non-existent item is not met`() {
        val state = baseState().copy(
            items = emptyList(),
            triggers = listOf(Trigger("t1", OwnerType.AREA, "Forest", listOf(ActivationCondition(checkType = CheckType.ITEM_CARRIED, itemId = ItemId("Phantom"))), listOf(Effect.DisplayText("Found!"))))
        )
        val result = fireAreaTriggers(state, AreaId("Forest"))
        assertTrue(result.triggerTexts.isEmpty())
    }

    @Test
    fun `set exit blocked effect blocks passage`() {
        val a2 = AreaId("Cell")
        val state = GameState(
            world = World(mapOf(AreaId("Forest") to Area(AreaId("Forest"), "Forest.", setOf(Exit(a2, Direction.East)), device = null), a2 to Area(a2, "Cell.", setOf())), AreaId("Forest")),
            player = Player(10, 20, AreaId("Forest")),
            triggers = listOf(Trigger("t1", OwnerType.AREA, "Forest", emptyList(), listOf(Effect.SetExitBlocked(AreaId("Forest"), a2))))
        )
        val result = fireAreaTriggers(state, AreaId("Forest"))
        assertEquals(1, result.exitStates.size)
        val data = result.exitStates[exploration.model.ExitId(AreaId("Forest"), a2)]!!
        assertEquals(exploration.model.ExitState.BLOCKED, data.state)
    }

    @Test
    fun `set exit unblocked effect opens passage`() {
        val blockedState = GameState(
            world = World(mapOf(AreaId("A") to Area(AreaId("A"), "A.", setOf(Exit(AreaId("B"), Direction.East)), device = null), AreaId("B") to Area(AreaId("B"), "B.", setOf())), AreaId("A")),
            player = Player(10, 20, AreaId("A")),
            exitStates = mapOf(exploration.model.ExitId(AreaId("A"), AreaId("B")) to exploration.model.ExitStateData(exploration.model.ExitState.BLOCKED)),
            triggers = listOf(Trigger("t1", OwnerType.AREA, "A", emptyList(), listOf(Effect.SetExitBlocked(AreaId("A"), AreaId("B"), blocked = false))))
        )
        val result = fireAreaTriggers(blockedState, AreaId("A"))
        val data = result.exitStates[exploration.model.ExitId(AreaId("A"), AreaId("B"))]!!
        assertEquals(exploration.model.ExitState.OPEN, data.state)
    }

    @Test
    fun `hide exit effect hides passage`() {
        val a2 = AreaId("Cell")
        val state = GameState(
            world = World(mapOf(AreaId("Forest") to Area(AreaId("Forest"), "Forest.", setOf(Exit(a2, Direction.East)), device = null), a2 to Area(a2, "Cell.", setOf())), AreaId("Forest")),
            player = Player(10, 20, AreaId("Forest")),
            triggers = listOf(Trigger("t1", OwnerType.AREA, "Forest", emptyList(), listOf(Effect.HideExit(AreaId("Forest"), a2))))
        )
        val result = fireAreaTriggers(state, AreaId("Forest"))
        val data = result.exitStates[exploration.model.ExitId(AreaId("Forest"), a2)]!!
        assertTrue(data.hidden)
    }

    @Test
    fun `show exit effect unhides passage`() {
        val hiddenState = GameState(
            world = World(mapOf(AreaId("A") to Area(AreaId("A"), "A.", setOf(Exit(AreaId("B"), Direction.East)), device = null), AreaId("B") to Area(AreaId("B"), "B.", setOf())), AreaId("A")),
            player = Player(10, 20, AreaId("A")),
            exitStates = mapOf(exploration.model.ExitId(AreaId("A"), AreaId("B")) to exploration.model.ExitStateData(exploration.model.ExitState.OPEN, hidden = true)),
            triggers = listOf(Trigger("t1", OwnerType.AREA, "A", emptyList(), listOf(Effect.ShowExit(AreaId("A"), AreaId("B")))))
        )
        val result = fireAreaTriggers(hiddenState, AreaId("A"))
        val data = result.exitStates[exploration.model.ExitId(AreaId("A"), AreaId("B"))]!!
        assertFalse(data.hidden)
    }

    @Test
    fun `exit state and hidden can be combined`() {
        val a2 = AreaId("Cell")
        val state = GameState(
            world = World(mapOf(AreaId("Forest") to Area(AreaId("Forest"), "Forest.", setOf(Exit(a2, Direction.East)), device = null), a2 to Area(a2, "Cell.", setOf())), AreaId("Forest")),
            player = Player(10, 20, AreaId("Forest")),
            triggers = listOf(Trigger("t1", OwnerType.AREA, "Forest", emptyList(), listOf(Effect.SetExitBlocked(AreaId("Forest"), a2), Effect.HideExit(AreaId("Forest"), a2))))
        )
        val result = fireAreaTriggers(state, AreaId("Forest"))
        val data = result.exitStates[exploration.model.ExitId(AreaId("Forest"), a2)]!!
        assertEquals(exploration.model.ExitState.BLOCKED, data.state)
        assertTrue(data.hidden)
        assertFalse(data.visible)
    }

    @Test
    fun `endGame effect ends the game with custom message`() {
        val state = baseState().copy(
            triggers = listOf(Trigger("t1", OwnerType.AREA, "Forest", emptyList(), listOf(Effect.EndGame("You found the truth."))))
        )
        val result = fireAreaTriggers(state, AreaId("Forest"))
        assertEquals("You found the truth.", result.endGameMessage)
    }

    @Test
    fun `endGame does not override existing end game message`() {
        val state = baseState().copy(
            endGameMessage = "Already ended.",
            triggers = listOf(Trigger("t1", OwnerType.AREA, "Forest", emptyList(), listOf(Effect.EndGame("This should be ignored."))))
        )
        val result = fireAreaTriggers(state, AreaId("Forest"))
        assertEquals("Already ended.", result.endGameMessage)
    }

    @Test
    fun `trigger loading with new exit effects`() {
        val entry = TriggerEntry(
            id = "t1", ownerType = "AREA", ownerId = "Forest",
            conditions = emptyList(),
            effects = listOf(
                EffectEntry(type = "setExitBlocked", fromId = "Forest", toId = "Cave", blocked = true),
                EffectEntry(type = "hideExit", fromId = "Forest", toId = "Cave")
            ),
            singleUse = false, intervalTurns = null
        )
        val triggers = convertTriggerEntriesWithExits(listOf(entry))
        assertEquals(1, triggers.size)
        assertEquals(2, triggers[0].effects.size)
    }
}

private fun convertTriggerEntries(entries: List<TriggerEntry>): List<Trigger> {
    return entries.map { ef ->
        Trigger(
            id = ef.id, ownerType = OwnerType.valueOf(ef.ownerType), ownerId = ef.ownerId,
            conditions = ef.conditions.map { ActivationCondition(checkType = CheckType.STATUS, statusName = it.statusName, op = when (it.op) { ">" -> ComparisonOp.GT; "<" -> ComparisonOp.LT; else -> error("bad") }, threshold = it.threshold ?: 0) },
            effects = ef.effects.map { map ->
                when (map.type) { "changeHealth" -> Effect.ChangeHealth(map.amount!!); "displayText" -> Effect.DisplayText(map.text!!); else -> error("bad") }
            }, singleUse = ef.singleUse, intervalTurns = ef.intervalTurns
        )
    }
}

private fun convertTriggerEntriesWithExits(entries: List<TriggerEntry>): List<Trigger> {
    return entries.map { ef ->
        Trigger(
            id = ef.id, ownerType = OwnerType.valueOf(ef.ownerType), ownerId = ef.ownerId,
            conditions = ef.conditions.map { ActivationCondition(checkType = CheckType.STATUS, statusName = it.statusName, op = when (it.op) { ">" -> ComparisonOp.GT; "<" -> ComparisonOp.LT; else -> error("bad") }, threshold = it.threshold ?: 0) },
            effects = ef.effects.map { map ->
                when (map.type) {
                    "changeHealth" -> Effect.ChangeHealth(map.amount!!)
                    "displayText" -> Effect.DisplayText(map.text!!)
                    "setExitBlocked" -> Effect.SetExitBlocked(AreaId(checkNotNull(map.fromId)), AreaId(checkNotNull(map.toId)), map.blocked ?: false)
                    "hideExit" -> Effect.HideExit(AreaId(checkNotNull(map.fromId)), AreaId(checkNotNull(map.toId)))
                    else -> error("bad")
                }
            }, singleUse = ef.singleUse, intervalTurns = ef.intervalTurns
        )
    }
}

private fun baseState(statuses: Map<String, Int> = emptyMap()): GameState {
    val aId = AreaId("Forest")
    val bId = AreaId("Cave")
    return GameState(
        world = World(mapOf(aId to Area(aId, "Forest.", setOf(Exit(bId, Direction.East)), device = null), bId to Area(bId, "Cave.", setOf(Exit(aId, Direction.West)), device = null)), aId),
        player = Player(10, 20, aId, statuses),
        statusBounds = emptyMap()
    )
}
