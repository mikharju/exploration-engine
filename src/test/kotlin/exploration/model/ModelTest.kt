package exploration.model

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ModelTest {

    @Test
    fun `area has correct structure`() {
        val id = AreaId("Room")
        val area = Area(id, "A room.", setOf(), Device(
            id = DeviceId("Lamp"),
            lookDescription = "A lamp glows softly.",
            activateDescription = "The lamp burns bright. (+2)",
            healthEffect = 2
        ))

        assertEquals(id, area.id)
        assertEquals("A room.", area.description)
        assertTrue(area.device != null)
    }

    @Test
    fun `player construction with valid values`() {
        val p = Player(10, 20, AreaId("Start"))
        assertEquals(10, p.health)
        assertEquals(20, p.maxHealth)
    }

    @Test
    fun `player rejects negative health`() {
        assertThrows<IllegalArgumentException> {
            Player(-1, 10, AreaId("X"))
        }
    }

    @Test
    fun `player allows zero health for game over state`() {
        val p = Player(0, 10, AreaId("X"))
        assertEquals(0, p.health)
    }

    @Test
    fun `player rejects maxHealth less than health`() {
        assertThrows<IllegalArgumentException> {
            Player(20, 10, AreaId("X"))
        }
    }

    @Test
    fun `adjustHealth positive clamps to max`() {
        val p = Player(18, 20, AreaId("A"))
        assertEquals(Player(20, 20, AreaId("A")), p.adjustHealth(5))
    }

    @Test
    fun `adjustHealth negative clamps to zero`() {
        val p = Player(3, 20, AreaId("A"))
        assertEquals(Player(0, 20, AreaId("A")), p.adjustHealth(-5))
    }

    @Test
    fun `adjustHealth normal range`() {
        val p = Player(10, 20, AreaId("A"))
        assertEquals(Player(13, 20, AreaId("A")), p.adjustHealth(3))
    }

    @Test
    fun `world construction with valid connections`() {
        val a = AreaId("A")
        val b = AreaId("B")
        val world = World(
            areas = mapOf(
                a to Area(a, "Area A", setOf(b)),
                b to Area(b, "Area B", setOf(a))
            ),
            startArea = a
        )
        assertEquals(2, world.areas.size)
        assertEquals(a, world.startArea)
    }

    @Test
    fun `world rejects missing start area`() {
        assertThrows<IllegalArgumentException> {
            World(
                areas = mapOf(AreaId("A") to Area(AreaId("A"), "A", setOf())),
                startArea = AreaId("Missing")
            )
        }
    }

    @Test
    fun `world with multiple rooms has correct structure`() {
        val aId = AreaId("A")
        val bId = AreaId("B")
        val w = World(
            areas = mapOf(
                aId to Area(aId, "Room A", setOf(bId), Device(DeviceId("d1"), "look1", "act1", 5)),
                bId to Area(bId, "Room B", setOf(aId), Device(DeviceId("d2"), "look2", "act2", -3))
            ),
            startArea = aId
        )
        assertEquals(2, w.areas.size)
        assertTrue(w.startArea in w.areas.keys)
    }

    @Test
    fun `world connectivity forms valid graph`() {
        val aId = AreaId("A")
        val bId = AreaId("B")
        val cId = AreaId("C")
        val w = World(
            areas = mapOf(
                aId to Area(aId, "Room A", setOf(bId)),
                bId to Area(bId, "Room B", setOf(aId, cId)),
                cId to Area(cId, "Room C", setOf(bId))
            ),
            startArea = aId
        )
        w.areas.values.forEach { area ->
            area.connections.forEach { connId ->
                assertTrue(connId in w.areas, "Connection $connId not found in world")
            }
        }
    }

    @Test
    fun `world can have rooms with devices`() {
        val aId = AreaId("A")
        val bId = AreaId("B")
        val w = World(
            areas = mapOf(
                aId to Area(aId, "Room A", setOf(bId), Device(DeviceId("d1"), "look1", "act1", 5)),
                bId to Area(bId, "Room B", setOf(aId), Device(DeviceId("d2"), "look2", "act2", -3))
            ),
            startArea = aId
        )
        w.areas.values.forEach { area ->
            assertTrue(area.device != null, "Room ${area.id} has no device")
        }
    }

    @Test
    fun `device health effects sum correctly`() {
        val aId = AreaId("A")
        val bId = AreaId("B")
        val w = World(
            areas = mapOf(
                aId to Area(aId, "Room A", setOf(bId), Device(DeviceId("d1"), "look1", "act1", 5)),
                bId to Area(bId, "Room B", setOf(aId), Device(DeviceId("d2"), "look2", "act2", -3))
            ),
            startArea = aId
        )
        val totalHealthEffect = w.areas.values.sumOf { it.device?.healthEffect ?: 0 }
        assertEquals(2, totalHealthEffect) // +5 + (-3) = 2
    }

    @Test
    fun `player starting at 3 health can survive one bad device`() {
        val p = Player(3, 20, AreaId("A"))
        val afterMachine = p.adjustHealth(-5)
        assertEquals(0, afterMachine.health)
    }

    @Test
    fun `area id toString returns name`() {
        assertEquals("Forest", AreaId("Forest").toString())
    }

    @Test
    fun `device id toString returns name`() {
        assertEquals("Crystal", DeviceId("Crystal").toString())
    }
}
