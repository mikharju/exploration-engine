package exploration.model

data class World(
    val areas: Map<AreaId, Area>,
    val startArea: AreaId
) {
    init {
        require(areas.containsKey(startArea)) { "startArea must exist in world" }
        for ((areaId, area) in areas) {
            for (target in area.connections) {
                require(target in areas) { "Area '$areaId' connects to '$target', but no such area exists" }
            }
        }
    }

    fun getArea(id: AreaId): Area =
        checkNotNull(areas[id]) { "Unknown area: $id" }
}
