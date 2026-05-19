package exploration.core.model

data class ItemId(val name: String) {
    override fun toString(): String = name
}

data class Item(
    val id: ItemId,
    val description: String,
    val location: Location = Location(ItemLocationType.AREA),
    val locked: Boolean = false
) {
    fun isInArea(areaId: AreaId): Boolean =
        location.type == ItemLocationType.AREA && (location.target as? LocationTarget.InArea)?.areaId == areaId

    fun isCarried(): Boolean = location.type == ItemLocationType.CARRIED

    fun isEquipped(): Boolean = location.type == ItemLocationType.EQUIPPED

    fun isOnDevice(deviceId: DeviceId): Boolean =
        location.type == ItemLocationType.DEVICE && (location.target as? LocationTarget.OnDevice)?.deviceId == deviceId
}
