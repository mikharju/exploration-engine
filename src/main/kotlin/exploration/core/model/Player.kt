package exploration.model

data class Player(
    val health: Int,
    val maxHealth: Int,
    val currentArea: AreaId
) {
    init {
        require(health >= 0) { "Player health must be non-negative" }
        require(maxHealth > 0) { "maxHealth must be positive" }
        require(maxHealth >= health) { "maxHealth must be >= health" }
    }

    fun adjustHealth(amount: Int): Player = copy(
        health = minOf(maxHealth, maxOf(0, health + amount))
    )
}
