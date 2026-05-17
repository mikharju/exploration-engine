package exploration.port

import exploration.model.StatusRange

data class ItemView(
    val name: String,
    val description: String,
    val locked: Boolean = false
)

data class ExitInfo(
    val name: String?,
    val blocked: Boolean = false
)

data class ViewData(
    val outputLine: String,
    val commandText: String,
    val triggerTexts: List<String>,
    val storyMessages: List<String> = emptyList(),
    val health: Int,
    val maxHealth: Int,
    val currentAreaName: String,
    val exploredCount: Int,
    val totalAreas: Int,
    val activatedCount: Int,
    val totalDevices: Int,
    val exits: List<ExitInfo?>,
    val statuses: Map<String, Int> = emptyMap(),
    val statusBounds: Map<String, StatusRange> = emptyMap(),
    val gameOver: Boolean = false,
    val win: Boolean? = null,
    val areaItems: List<ItemView> = emptyList(),
    val carriedItems: List<ItemView> = emptyList(),
    val equippedItems: List<ItemView> = emptyList()
)