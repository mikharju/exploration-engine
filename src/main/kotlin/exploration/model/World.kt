package exploration.model

data class World(
    val areas: Map<AreaId, Area>,
    val startArea: AreaId
) {
    init {
        require(areas.containsKey(startArea)) { "startArea must exist in world" }
        areas.keys.forEach { id ->
            require(id in areas) { "All connection targets must be valid AreaIds: $id" }
        }
    }

    fun getArea(id: AreaId): Area =
        checkNotNull(areas[id]) { "Unknown area: $id" }
}

fun buildDefaultWorld(): World {
    val forestId = AreaId("Forest")
    val caveId = AreaId("Cave")
    val ruinsId = AreaId("Ruins")
    val towerId = AreaId("Tower")

    val crystal = Device(
        id = DeviceId("Crystal"),
        lookDescription = "A glowing crystal pulses with soft blue light.",
        activateDescription = "The crystal flares brightly, warmth flooding through you. (+10 health)",
        healthEffect = 10
    )

    val glyphWall = Device(
        id = DeviceId("Glyph Wall"),
        lookDescription = "Ancient glyphs cover the cave wall, shimmering faintly.",
        activateDescription = "The glyphs rearrange into a clear message: 'All paths lead to understanding.'",
        healthEffect = 0
    )

    val ancientMachine = Device(
        id = DeviceId("Ancient Machine"),
        lookDescription = "A rusted machine hums with unstable energy.",
        activateDescription = "The machine whirs violently, discharging a burst of raw power. (-5 health)",
        healthEffect = -5
    )

    val orb = Device(
        id = DeviceId("Orb"),
        lookDescription = "A smooth obsidian orb sits on a stone pedestal.",
        activateDescription = "The orb cracks open, releasing warm light that strengthens you. (+3 health)",
        healthEffect = 3
    )

    return World(
        areas = mapOf(
            forestId to Area(forestId, "A dense forest with tall pines and dappled sunlight.", setOf(caveId), crystal),
            caveId to Area(caveId, "A dim cavern with stalactites and a cool breeze.", setOf(forestId, ruinsId, towerId), glyphWall),
            ruinsId to Area(ruinsId, "Crumbled stone ruins overgrown with vines.", setOf(caveId), ancientMachine),
            towerId to Area(towerId, "A tall stone tower interior with a spiral staircase.", setOf(caveId), orb)
        ),
        startArea = forestId
    )
}
