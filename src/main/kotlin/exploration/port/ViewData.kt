package exploration.port

data class ViewData(
    val outputLine: String,
    val health: Int,
    val maxHealth: Int,
    val currentAreaName: String,
    val exploredCount: Int,
    val totalAreas: Int,
    val activatedCount: Int,
    val totalDevices: Int,
    val exits: List<String>,
    val gameOver: Boolean = false,
    val win: Boolean? = null
)