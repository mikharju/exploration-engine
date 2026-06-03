package exploration.adapter.ui.libgdx

import exploration.port.ItemView

/** Types used across the libGDX adapter modules. */

interface HistoryEntry {
    val text: String
}

data class OverlayState(
    override val text: String = "",
    val state: State,
    val message: String? = null,
    val selectionItems: List<ItemView>? = null,
    val selectionTarget: SelectionTarget? = null
) : HistoryEntry {
    
    enum class State {
        Playing,
        Inventory,
        StoryViewer,
        GameOver
    }
}

sealed interface SelectionTarget {
    object TAKE : SelectionTarget
    object DROP : SelectionTarget
    object EQUIP : SelectionTarget
    object UNEQUIP : SelectionTarget
}

data class SelectionState(
    val active: Boolean = false,
    val target: SelectionTarget? = null,
    val items: List<ItemView> = emptyList()
) {
    companion object {
        fun inactive(): SelectionState = SelectionState()
        fun forTake(items: List<ItemView>): SelectionState = 
            SelectionState(true, SelectionTarget.TAKE, items)
        fun forDrop(items: List<ItemView>): SelectionState = 
            SelectionState(true, SelectionTarget.DROP, items)
        fun forEquip(items: List<ItemView>): SelectionState = 
            SelectionState(true, SelectionTarget.EQUIP, items)
        fun forUnequip(items: List<ItemView>): SelectionState = 
            SelectionState(true, SelectionTarget.UNEQUIP, items)
    }
}

sealed interface ItemActionResult {
    data class Take(val itemName: String) : ItemActionResult
    data class Drop(val itemName: String) : ItemActionResult
    data class Equip(val itemName: String) : ItemActionResult
    data class Unequip(val itemName: String) : ItemActionResult
}
