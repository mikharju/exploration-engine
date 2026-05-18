package exploration.core.model

data class Player(
    val health: Int,
    val maxHealth: Int,
    val currentArea: AreaId,
    val statuses: Map<String, Int> = emptyMap()
) {
    init {
        require(health >= 0) { "Player health must be non-negative" }
        require(maxHealth > 0) { "maxHealth must be positive" }
        require(maxHealth >= health) { "maxHealth must be >= health" }
    }

    fun adjustHealth(amount: Int): Player = copy(
        health = minOf(maxHealth, maxOf(0, health + amount))
    )

    fun adjustStatus(name: String, amount: Int, range: StatusRange?): Player {
        val current = statuses.getOrElse(name) { 0 }
        val newValue = if (range != null) {
            minOf(range.max, maxOf(range.min, current + amount))
        } else {
            current + amount
        }
        return copy(statuses = statuses + (name to newValue))
    }

    fun setStatus(name: String, value: Int, range: StatusRange?): Player {
        val newValue = if (range != null) {
            minOf(range.max, maxOf(range.min, value))
        } else {
            value
        }
        return copy(statuses = statuses + (name to newValue))
    }
}
