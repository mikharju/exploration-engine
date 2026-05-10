package exploration.model

data class AreaId(val name: String) {
    override fun toString(): String = name
}

data class Area(
    val id: AreaId,
    val description: String,
    val connections: Set<AreaId>,
    val device: Device? = null
)
