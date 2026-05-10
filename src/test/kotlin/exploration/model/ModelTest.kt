package exploration.model

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ModelTest {

    @Test
    fun `player rejects invalid health`() {
        assertFailsWith<IllegalArgumentException> { Player(-1, 10, AreaId("X")) }
        assertFailsWith<IllegalArgumentException> { Player(20, 10, AreaId("X")) }
    }

    @Test
    fun `player allows zero health`() {
        assertEquals(0, Player(0, 10, AreaId("X")).health)
    }

    @Test
    fun `adjustHealth clamps to max`() {
        val p = Player(18, 20, AreaId("A"))
        assertEquals(Player(20, 20, AreaId("A")), p.adjustHealth(5))
    }

    @Test
    fun `adjustHealth clamps to zero`() {
        val p = Player(3, 20, AreaId("A"))
        assertEquals(Player(0, 20, AreaId("A")), p.adjustHealth(-5))
    }

    @Test
    fun `world rejects missing start area`() {
        assertFailsWith<IllegalArgumentException> {
            World(
                areas = mapOf(AreaId("A") to Area(AreaId("A"), "A", setOf())),
                startArea = AreaId("Missing")
            )
        }
    }
}
