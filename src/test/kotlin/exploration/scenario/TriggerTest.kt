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
        val result = fireAreaTriggers(state, "Forest")
        assertTrue(result.output.contains("You enter the forest."))
    }

    @Test
    fun `area trigger is single use`() {
        val state = baseState().copy(
            triggers = listOf(Trigger("t1", OwnerType.AREA, "Forest", emptyList(), listOf(Effect.ChangeHealth(-2))))
        )
        val r1 = fireAreaTriggers(state, "Forest")
        assertEquals(8, r1.player.health)
        val r2 = fireAreaTriggers(r1, "Forest")
        assertEquals(8, r2.player.health)
    }

    @Test
    fun `area trigger with condition that is not met does not fire`() {
        val state = baseState().copy(
            triggers = listOf(Trigger("t1", OwnerType.AREA, "Forest", listOf(ActivationCondition(checkType = CheckType.STATUS, statusName = "stamina", op = ComparisonOp.GT, threshold = 50)), listOf(Effect.ChangeHealth(-3))))
        )
        val result = fireAreaTriggers(state, "Forest")
        assertEquals(10, result.player.health)
    }

    @Test
    fun `area trigger with condition met fires`() {
        val state = baseState(statuses = mapOf("stamina" to 60)).copy(
            statusBounds = mapOf("stamina" to StatusRange(0, 100)),
            triggers = listOf(Trigger("t1", OwnerType.AREA, "Forest", listOf(ActivationCondition(checkType = CheckType.STATUS, statusName = "stamina", op = ComparisonOp.GT, threshold = 50)), listOf(Effect.ChangeHealth(-3))))
        )
        val result = fireAreaTriggers(state, "Forest")
        assertEquals(7, result.player.health)
    }

    @Test
    fun `device trigger fires on activation`() {
        val state = baseState().copy(
            triggers = listOf(Trigger("t1", OwnerType.DEVICE, "Orb", emptyList(), listOf(Effect.AdjustStatus("magic", 5))))
        )
        val result = fireDeviceTriggers(state, "Orb")
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
        assertTrue(r1.output.contains("wind"))
    }

    @Test
    fun `set status effect clamps to bounds`() {
        val state = baseState(statuses = mapOf("mana" to 0)).copy(
            statusBounds = mapOf("mana" to StatusRange(0, 20)),
            triggers = listOf(Trigger("t1", OwnerType.AREA, "Spring", emptyList(), listOf(Effect.SetStatus("mana", 30))))
        )
        val result = fireAreaTriggers(state, "Spring")
        assertEquals(20, result.player.statuses["mana"])
    }

    @Test
    fun `multiple triggers concatenate output`() {
        val state = baseState().copy(
            triggers = listOf(
                Trigger("t1", OwnerType.AREA, "Forest", emptyList(), listOf(Effect.DisplayText("A"))),
                Trigger("t2", OwnerType.AREA, "Forest", emptyList(), listOf(Effect.DisplayText("B")))
            )
        )
        val result = fireAreaTriggers(state, "Forest")
        assertTrue(result.output.contains("A"))
        assertTrue(result.output.contains("B"))
    }

    @Test
    fun `trigger loading from entries`() {
        val entry = TriggerEntry(
            id = "t1", ownerType = "AREA", ownerId = "Forest",
            conditions = listOf(TriggerCondition(statusName = "mana", op = ">", threshold = 5)),
            effects = listOf(EffectEntry(type = "changeHealth", amount = -3)),
            singleUse = true, intervalTurns = null
        )
        val triggers = convertTriggerEntries(listOf(entry))
        assertEquals(1, triggers.size)
        assertTrue(triggers[0].conditions.isNotEmpty())
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

private fun baseState(statuses: Map<String, Int> = emptyMap()): GameState {
    val aId = AreaId("Forest")
    val bId = AreaId("Cave")
    return GameState(
        world = World(mapOf(aId to Area(aId, "Forest.", setOf(bId)), bId to Area(bId, "Cave.", setOf(aId))), aId),
        player = Player(10, 20, aId, statuses),
        statusBounds = emptyMap()
    )
}
