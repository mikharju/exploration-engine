package exploration.model

data class DeviceId(val name: String) {
    override fun toString(): String = name
}

data class Device(
    val id: DeviceId,
    val lookDescription: String,
    val activateDescription: String,
    val healthEffect: Int = 0
)
