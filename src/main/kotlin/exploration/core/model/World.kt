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
