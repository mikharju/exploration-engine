package exploration.scenario

import exploration.adapter.jsonloader.loadScenarioFiles
import exploration.model.AreaId
import exploration.model.DeviceId
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.*

class ScenarioLoaderTest {

    private fun resourcePath(name: String): Path =
        Path.of(javaClass.classLoader.getResource("scenarios/test-default/$name")!!.toURI())

    @Test
    fun `load valid scenario`() {
        val files = loadScenarioFiles(resourcePath("config.json"))
        val state = assembleGame(files.config, files.areas, files.devices)

        assertEquals(2, state.world.areas.size)
        assertEquals(AreaId("A"), state.world.startArea)
        assertEquals(10, state.player.health)
        assertEquals(20, state.player.maxHealth)
        assertEquals(AreaId("A"), state.player.currentArea)

        val areaA = state.world.getArea(AreaId("A"))
        assertEquals("Room A.", areaA.description)
        assertEquals(setOf(AreaId("B")), areaA.connections)
        val device1 = checkNotNull(areaA.device)
        assertEquals(DeviceId("device1"), device1.id)
        assertEquals(5, device1.healthEffect)

        val areaB = state.world.getArea(AreaId("B"))
        assertEquals("Room B.", areaB.description)
        assertEquals(setOf(AreaId("A")), areaB.connections)
        val device2 = checkNotNull(areaB.device)
        assertEquals(DeviceId("device2"), device2.id)
        assertEquals(0, device2.healthEffect)
    }

    @Test
    fun `load from non-existent file throws`() {
        assertFailsWith<IllegalArgumentException> {
            loadScenarioFiles(Path.of("/nonexistent/scenario.json"))
        }
    }

    @Test
    fun `bad device reference throws`() {
        val dir = Files.createTempDirectory("scenario-test")
        try {
            Files.writeString(dir.resolve("devices.json"), "[]")
            Files.writeString(dir.resolve("areas.json"), """[{"id":"A","description":".","connections":[],"deviceId":"missing"}]""")
            Files.writeString(dir.resolve("config.json"), """{"playerStart":{"health":10,"maxHealth":20,"startArea":"A"},"areasFile":"areas.json","devicesFile":"devices.json"}""")

            val files = loadScenarioFiles(dir.resolve("config.json"))
            assertFailsWith<IllegalArgumentException> { assembleGame(files.config, files.areas, files.devices) }
        } finally { dir.toFile().deleteRecursively() }
    }
}
