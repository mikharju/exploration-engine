package exploration.model

data class ExitId(
    val from: AreaId,
    val to: AreaId
) {
    override fun toString(): String = "$from → $to"
}

enum class ExitState { OPEN, BLOCKED }

data class ExitStateData(
    val state: ExitState = ExitState.OPEN,
    val hidden: Boolean = false
) {
    val visible: Boolean = !hidden && state == ExitState.OPEN
}

data class Exit(
    val targetArea: AreaId,
    val direction: Direction
) {
    fun exitId(from: AreaId): ExitId = ExitId(from, targetArea)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Exit) return false
        return targetArea == other.targetArea && direction == other.direction
    }

    override fun hashCode(): Int = targetArea.hashCode() * 31 + direction.hashCode()
}

data class AreaId(val name: String) {
    override fun toString(): String = name
}

data class Area(
    val id: AreaId,
    val description: String,
    val exits: Set<Exit> = emptySet(),
    val device: Device? = null
) {
    val connections: Set<AreaId> = exits.map { it.targetArea }.toSet()


}
