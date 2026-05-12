package exploration.port

import exploration.model.StatusRange

data class ViewData(
    val outputLine: String,
    val commandText: String,
    val triggerTexts: List<String>,
    val health: Int,
    val maxHealth: Int,
    val currentAreaName: String,
    val exploredCount: Int,
    val totalAreas: Int,
    val activatedCount: Int,
    val totalDevices: Int,
    val exits: List<String>,
    val statuses: Map<String, Int> = emptyMap(),
    val statusBounds: Map<String, StatusRange> = emptyMap(),
    val gameOver: Boolean = false,
    val win: Boolean? = null
)