package exploration.core.model

data class ItemId(val name: String) {
    override fun toString(): String = name
}

data class Item(
    val id: ItemId,
    val description: String,
    val location: Location = Location(ItemLocationType.AREA),
    val locked: Boolean = false
)
