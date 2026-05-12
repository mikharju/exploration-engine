package exploration.model

enum class OwnerType { AREA, DEVICE, STATUS }

data class ActivationCondition(
    val statusName: String,
    val op: ComparisonOp,
    val threshold: Int
)

enum class ComparisonOp { GT, LT }

sealed class Effect {
    data class DisplayText(val text: String) : Effect()
    data class ChangeHealth(val amount: Int) : Effect()
    data class AdjustStatus(val statusName: String, val amount: Int) : Effect()
    data class SetStatus(val statusName: String, val value: Int) : Effect()
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
)
