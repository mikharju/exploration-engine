package exploration.model

enum class ItemLocationType { AREA, DEVICE, CARRIED, EQUIPPED }

data class Location(
    val type: ItemLocationType,
    val id: String? = null
)
