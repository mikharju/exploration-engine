package exploration.model

enum class ItemLocationType { AREA, DEVICE, CARRIED, EQUIPPED }

sealed interface LocationTarget {
    data class InArea(val areaId: AreaId) : LocationTarget
    data class OnDevice(val deviceId: DeviceId) : LocationTarget
}

data class Location(
    val type: ItemLocationType,
    val target: LocationTarget? = null
) {
    companion object {
        fun fromTarget(target: Effect.TargetRef): Location = when (target) {
            is Effect.TargetRef.InArea -> Location(ItemLocationType.AREA, LocationTarget.InArea(target.areaId))
            is Effect.TargetRef.OnDevice -> Location(ItemLocationType.DEVICE, LocationTarget.OnDevice(target.deviceId))
            is Effect.TargetRef.Carried -> Location(ItemLocationType.CARRIED)
            is Effect.TargetRef.Equipped -> Location(ItemLocationType.EQUIPPED)
        }

        fun area(areaId: AreaId): Location = Location(ItemLocationType.AREA, LocationTarget.InArea(areaId))
        fun device(deviceId: DeviceId): Location = Location(ItemLocationType.DEVICE, LocationTarget.OnDevice(deviceId))
    }
}
