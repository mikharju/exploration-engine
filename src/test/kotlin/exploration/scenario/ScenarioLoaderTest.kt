package exploration.scenario

import exploration.adapter.jsonloader.loadScenarioFiles
import exploration.core.model.AreaId
import exploration.core.model.DeviceId
import exploration.core.model.ExitState
import exploration.core.model.ExitId
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

    @Test
    fun `bad trigger item reference in condition throws`() {
        val dir = Files.createTempDirectory("scenario-test")
        try {
            Files.writeString(dir.resolve("items.json"), """[{"id":"key","description":"A key.","initialLocationType":"AREA","initialLocationId":"A"}]""")
            Files.writeString(dir.resolve("triggers.json"), """[{"id":"t1","ownerType":"AREA","ownerId":"A","conditions":[{"checkType":"itemCarried","itemId":"missingKey"}],"effects":[]}]""")
            Files.writeString(dir.resolve("areas.json"), """[{"id":"A","description":"Room.","connections":[],"deviceId":null}]""")
            Files.writeString(dir.resolve("devices.json"), "[]")
            Files.writeString(dir.resolve("config.json"), """{"playerStart":{"health":10,"maxHealth":20,"startArea":"A"},"areasFile":"areas.json","devicesFile":"devices.json","itemsFile":"items.json","triggersFile":"triggers.json"}""")

            val files = loadScenarioFiles(dir.resolve("config.json"))
            assertFailsWith<IllegalArgumentException> { assembleGame(files.config, files.areas, files.devices, files.triggers, files.items) }
        } finally { dir.toFile().deleteRecursively() }
    }

    @Test
    fun `bad trigger item reference in effect throws`() {
        val dir = Files.createTempDirectory("scenario-test")
        try {
            Files.writeString(dir.resolve("items.json"), """[{"id":"key","description":"A key.","initialLocationType":"AREA","initialLocationId":"A"}]""")
            Files.writeString(dir.resolve("triggers.json"), """[{"id":"t1","ownerType":"AREA","ownerId":"A","conditions":[],"effects":[{"type":"lockItem","itemId":"missingKey"}]}]""")
            Files.writeString(dir.resolve("areas.json"), """[{"id":"A","description":"Room.","connections":[],"deviceId":null}]""")
            Files.writeString(dir.resolve("devices.json"), "[]")
            Files.writeString(dir.resolve("config.json"), """{"playerStart":{"health":10,"maxHealth":20,"startArea":"A"},"areasFile":"areas.json","devicesFile":"devices.json","itemsFile":"items.json","triggersFile":"triggers.json"}""")

            val files = loadScenarioFiles(dir.resolve("config.json"))
            assertFailsWith<IllegalArgumentException> { assembleGame(files.config, files.areas, files.devices, files.triggers, files.items) }
        } finally { dir.toFile().deleteRecursively() }
    }

    @Test
    fun `exit initialState blocked is parsed`() {
        val areas = mapOf(
            "Forest" to AreaEntry("Forest", "F.", exits = listOf(ExitEntry("Ruins", "East", "blocked"))),
            "Ruins" to AreaEntry("Ruins", "R.", exits = emptyList())
        )
        val state = assembleGame(
            config = ScenarioConfig(PlayerStart(10, 20, "Forest"), "a.json", "d.json"),
            areaEntries = areas.values.toList(),
            deviceEntries = emptyList()
        )
        val data = state.exitStates[ExitId(AreaId("Forest"), AreaId("Ruins"))]!!
        assertEquals(ExitState.BLOCKED, data.state)
    }

    @Test
    fun `exit hidden flag is parsed`() {
        val areas = mapOf(
            "Forest" to AreaEntry("Forest", "F.", exits = listOf(ExitEntry("Secret", "East", null, true))),
            "Secret" to AreaEntry("Secret", "S.", exits = emptyList())
        )
        val state = assembleGame(
            config = ScenarioConfig(PlayerStart(10, 20, "Forest"), "a.json", "d.json"),
            areaEntries = areas.values.toList(),
            deviceEntries = emptyList()
        )
        val data = state.exitStates[ExitId(AreaId("Forest"), AreaId("Secret"))]!!
        assertTrue(data.hidden)
    }

    @Test
    fun `exit initialState blocked and hidden is parsed`() {
        val areas = mapOf(
            "Forest" to AreaEntry("Forest", "F.", exits = listOf(ExitEntry("HiddenDoor", "East", "blocked", true))),
            "HiddenDoor" to AreaEntry("HiddenDoor", "H.", exits = emptyList())
        )
        val state = assembleGame(
            config = ScenarioConfig(PlayerStart(10, 20, "Forest"), "a.json", "d.json"),
            areaEntries = areas.values.toList(),
            deviceEntries = emptyList()
        )
        val data = state.exitStates[ExitId(AreaId("Forest"), AreaId("HiddenDoor"))]!!
        assertEquals(ExitState.BLOCKED, data.state)
        assertTrue(data.hidden)
    }

    @Test
    fun `exit with unknown initialState defaults to open`() {
        val areas = mapOf(
            "Forest" to AreaEntry("Forest", "F.", exits = listOf(ExitEntry("Ruins", "East", "weird"))),
            "Ruins" to AreaEntry("Ruins", "R.", exits = emptyList())
        )
        val state = assembleGame(
            config = ScenarioConfig(PlayerStart(10, 20, "Forest"), "a.json", "d.json"),
            areaEntries = areas.values.toList(),
            deviceEntries = emptyList()
        )
        val data = state.exitStates[ExitId(AreaId("Forest"), AreaId("Ruins"))]!!
        assertEquals(ExitState.OPEN, data.state)
    }
}
