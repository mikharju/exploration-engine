package exploration.model

enum class OwnerType { AREA, DEVICE, STATUS }

enum class CheckType { STATUS, ITEM_CARRIED, ITEM_EQUIPPED }

data class ActivationCondition(
    val checkType: CheckType = CheckType.STATUS,
    val statusName: String? = null,
    val op: ComparisonOp = ComparisonOp.GT,
    val threshold: Int = 0,
    val itemId: ItemId? = null
)

enum class ComparisonOp { GT, LT }

sealed interface TriggerOwner {
    data class Area(val areaId: AreaId) : TriggerOwner
    data class Device(val deviceId: DeviceId) : TriggerOwner
    object Status : TriggerOwner
}

sealed class Effect {
    data class DisplayText(val text: String) : Effect()
    data class ChangeHealth(val amount: Int) : Effect()
    data class AdjustStatus(val statusName: String, val amount: Int) : Effect()
    data class SetStatus(val statusName: String, val value: Int) : Effect()

    sealed interface TargetRef {
        data class InArea(val areaId: AreaId) : TargetRef
        data class OnDevice(val deviceId: DeviceId) : TargetRef
        object Carried : TargetRef
        object Equipped : TargetRef
    }

    data class SetLocation(
        val itemId: ItemId,
        val target: TargetRef
    ) : Effect()

    data class LockItem(val itemId: ItemId) : Effect()
    data class UnlockItem(val itemId: ItemId) : Effect()
    data class StoryMessage(val text: String) : Effect()

    // Exit effects — target area and direction
    data class SetExitBlocked(val from: AreaId, val to: AreaId, val blocked: Boolean = true) : Effect()
    data class HideExit(val from: AreaId, val to: AreaId) : Effect()
    data class ShowExit(val from: AreaId, val to: AreaId) : Effect()

    data class EndGame(val text: String) : Effect()
}

data class Trigger(
    val id: String,
    val ownerType: OwnerType,
    val ownerId: String,
    val conditions: List<ActivationCondition> = emptyList(),
    val effects: List<Effect>,
    val singleUse: Boolean = true,
    val intervalTurns: Int? = null,
    val remainingActivations: Int = if (singleUse) 1 else Int.MAX_VALUE,
    val lastFireTurn: Int? = null
) {
    val owner: TriggerOwner
        get() = when (ownerType) {
            OwnerType.AREA -> TriggerOwner.Area(AreaId(ownerId))
            OwnerType.DEVICE -> TriggerOwner.Device(DeviceId(ownerId))
            OwnerType.STATUS -> TriggerOwner.Status
        }

    fun areaId(): AreaId? = if (ownerType == OwnerType.AREA) AreaId(ownerId) else null
    fun deviceId(): DeviceId? = if (ownerType == OwnerType.DEVICE) DeviceId(ownerId) else null
}
