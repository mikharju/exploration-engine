package exploration.scenario

import exploration.model.AreaId
import exploration.model.DeviceId
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.*

class ScenarioLoaderTest {

    private fun resourcePath(name: String): Path {
        val url = javaClass.classLoader.getResource("scenarios/test-default/$name")
            ?: throw AssertionError("Resource not found on classpath: scenarios/test-default/$name")
        return Path.of(url.toURI())
    }

    @Test
    fun `load valid scenario returns correct game state`() {
        val configPath = resourcePath("config.json")
        val state = loadScenario(configPath)

        assertEquals(2, state.world.areas.size)
        assertEquals(AreaId("A"), state.world.startArea)
        assertEquals(10, state.player.health)
        assertEquals(20, state.player.maxHealth)
        assertEquals(AreaId("A"), state.player.currentArea)
        assertTrue(state.exploredAreas.isEmpty())
        assertTrue(state.activatedDevices.isEmpty())
    }

    @Test
    fun `loaded areas have correct descriptions`() {
        val configPath = resourcePath("config.json")
        val state = loadScenario(configPath)

        assertEquals("Room A.", state.world.getArea(AreaId("A")).description)
        assertEquals("Room B.", state.world.getArea(AreaId("B")).description)
    }

    @Test
    fun `loaded areas have correct connections`() {
        val configPath = resourcePath("config.json")
        val state = loadScenario(configPath)

        assertEquals(setOf(AreaId("B")), state.world.getArea(AreaId("A")).connections)
        assertEquals(setOf(AreaId("A")), state.world.getArea(AreaId("B")).connections)
    }

    @Test
    fun `loaded devices have correct properties`() {
        val configPath = resourcePath("config.json")
        val state = loadScenario(configPath)

        val device1 = state.world.getArea(AreaId("A")).device!!
        assertEquals(DeviceId("device1"), device1.id)
        assertEquals(5, device1.healthEffect)
        assertTrue(device1.lookDescription.contains("healing herb"))

        val device2 = state.world.getArea(AreaId("B")).device!!
        assertEquals(DeviceId("device2"), device2.id)
        assertEquals(0, device2.healthEffect)
    }

    @Test
    fun `load from non-existent file throws`() {
        val path = Path.of("/nonexistent/scenario.json")
        assertFailsWith<IllegalArgumentException> {
            loadScenario(path)
        }
    }

    @Test
    fun `area with unknown device reference throws`() {
        val tempDir = Files.createTempDirectory("scenario-test-device")
        try {
            Files.writeString(tempDir.resolve("devices.json"), "[]")
            Files.writeString(tempDir.resolve("bad_areas.json"), """[{
                "id": "A",
                "description": "Test.",
                "connections": [],
                "deviceId": "nonexistent"
            }]""")
            Files.writeString(tempDir.resolve("config.json"), """{
                "playerStart": {"health": 10, "maxHealth": 20, "startArea": "A"},
                "areasFile": "bad_areas.json",
                "devicesFile": "devices.json"
            }""")

            assertFailsWith<IllegalArgumentException> {
                loadScenario(tempDir.resolve("config.json"))
            }
        } finally {
            tempDir.toFile().deleteRecursively()
        }
    }

    @Test
    fun `area with unknown connection target throws`() {
        val tempDir = Files.createTempDirectory("scenario-test-conn")
        try {
            Files.writeString(tempDir.resolve("devices.json"), "[]")
            Files.writeString(tempDir.resolve("bad_areas.json"), """[{
                "id": "A",
                "description": "Test.",
                "connections": ["NonExistent"],
                "deviceId": null
            }]""")
            Files.writeString(tempDir.resolve("config.json"), """{
                "playerStart": {"health": 10, "maxHealth": 20, "startArea": "A"},
                "areasFile": "bad_areas.json",
                "devicesFile": "devices.json"
            }""")

            assertFailsWith<IllegalArgumentException> {
                loadScenario(tempDir.resolve("config.json"))
            }
        } finally {
            tempDir.toFile().deleteRecursively()
        }
    }
}
