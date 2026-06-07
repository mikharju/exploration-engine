package exploration.adapter.ui.libgdx

import com.badlogic.gdx.Input as GdxInput
import exploration.model.Direction
import exploration.port.ExitInfo
import exploration.port.ItemView
import exploration.port.InputEvent
import exploration.port.ViewData

object InputMapper {

    fun mapKey(keycode: Int, viewData: ViewData): InputEvent? = when (keycode) {
        // Movement - arrow keys and WASD
        GdxInput.Keys.UP -> InputEvent.MoveDirection(Direction.North)
        GdxInput.Keys.LEFT -> InputEvent.MoveDirection(Direction.West)
        GdxInput.Keys.DOWN -> InputEvent.MoveDirection(Direction.South)
        GdxInput.Keys.RIGHT -> InputEvent.MoveDirection(Direction.East)
        GdxInput.Keys.W -> InputEvent.MoveDirection(Direction.North)
        GdxInput.Keys.A -> InputEvent.MoveDirection(Direction.West)
        GdxInput.Keys.S -> InputEvent.MoveDirection(Direction.South)
        GdxInput.Keys.D -> InputEvent.MoveDirection(Direction.East)

        // Actions
        GdxInput.Keys.L -> InputEvent.Look
        GdxInput.Keys.U -> InputEvent.Activate
        GdxInput.Keys.I -> InputEvent.Inventory

        // Item actions - delegate to handler for selection logic
        else -> null
    }

    sealed interface ItemAction {
        data class Event(val event: InputEvent) : ItemAction
        data class Message(val text: String) : ItemAction
        data class Selection(val target: SelectionTarget, val items: List<ItemView>) : ItemAction
    }

    fun handleItemKey(key: Char, viewData: ViewData): ItemAction? = when (key.lowercaseChar()) {
        'g' -> handleTake(viewData)
        'p' -> handleDrop(viewData)
        'e' -> handleEquip(viewData)
        'r' -> handleUnequip(viewData)
        else -> null
    }

    private fun handleTake(viewData: ViewData): ItemAction {
        val candidates = viewData.areaItems.filterNot { it.locked }
        return when {
            candidates.isEmpty() -> ItemAction.Message("Nothing to grab here.")
            candidates.size == 1 -> ItemAction.Event(InputEvent.TakeItem(candidates[0].name))
            else -> ItemAction.Selection(SelectionTarget.TAKE, candidates)
        }
    }

    private fun handleDrop(viewData: ViewData): ItemAction {
        val candidates = viewData.carriedItems + viewData.equippedItems
        return when {
            candidates.isEmpty() -> ItemAction.Message("Nothing to drop.")
            candidates.size == 1 -> ItemAction.Event(InputEvent.DropItem(candidates[0].name))
            else -> ItemAction.Selection(SelectionTarget.DROP, candidates)
        }
    }

    private fun handleEquip(viewData: ViewData): ItemAction {
        val candidates = viewData.carriedItems.filterNot { it.locked }
        return when {
            candidates.isEmpty() -> ItemAction.Message("Nothing to equip.")
            candidates.size == 1 -> ItemAction.Event(InputEvent.EquipItem(candidates[0].name))
            else -> ItemAction.Selection(SelectionTarget.EQUIP, candidates)
        }
    }

    private fun handleUnequip(viewData: ViewData): ItemAction {
        val candidates = viewData.equippedItems
        return when {
            candidates.isEmpty() -> ItemAction.Message("Nothing equipped.")
            candidates.size == 1 -> ItemAction.Event(InputEvent.UnequipItem(candidates[0].name))
            else -> ItemAction.Selection(SelectionTarget.UNEQUIP, candidates)
        }
    }

    fun mapTouchDirection(x: Float, y: Float, viewData: ViewData): InputEvent? {
        val centerX = GameScreen.VIEWPORT_W / 2f

        // WASD-style layout: W above, S/A/D on same row (matching LibgdxRenderBackend)
        val buttonY = GameScreen.DIRECTION_BTN_SIZE / 2f + 10f
        val spacing = GameScreen.DIRECTION_BTN_SIZE * 1.3f
        val southRowY = buttonY
        val northY = buttonY + spacing * 0.9f
        val directions: List<Pair<Direction, Pair<Float, Float>>> = listOf(
            Direction.North to (centerX to (northY)),
            Direction.West to ((centerX - spacing) to (southRowY)),
            Direction.South to (centerX to (southRowY)),
            Direction.East to ((centerX + spacing) to (southRowY))
        )

        for ((dir, pos) in directions) {
            val exitInfo = viewData.exits[dir] ?: continue
            if (!exitInfo.blocked && isCircleHit(pos.first, pos.second, x, y, 35f)) {
                return InputEvent.MoveDirection(dir)
            }
        }

        return null
    }

    fun mapTouchItem(x: Float, y: Float, viewData: ViewData): ItemActionResult? {
        val rightPanelLeft = calculateRightPanelLeft()
        
        // Carried items - clickable in the status panel area
        var itemY = 500f
        for (item in viewData.carriedItems) {
            if (isRectHit(x, y, rightPanelLeft + 20f, itemY, 340f, 36f)) {
                return ItemActionResult.Equip(item.name)
            }
            itemY -= 44f
        }

        // Equipped items
        for (item in viewData.equippedItems) {
            if (isRectHit(x, y, rightPanelLeft + 20f, itemY, 340f, 36f)) {
                return ItemActionResult.Unequip(item.name)
            }
            itemY -= 44f
        }

        // Area items - shown at bottom of message panel
        var areaItemY = 380f
        for (item in viewData.areaItems.filterNot { it.locked }) {
            if (isRectHit(x, y, GameScreen.MARGIN + 20f, areaItemY, 340f, 36f)) {
                return ItemActionResult.Take(item.name)
            }
            areaItemY -= 44f
        }

        return null
    }

    private fun calculateRightPanelLeft(): Float {
        val usableWidth = GameScreen.VIEWPORT_W - 2 * GameScreen.MARGIN
        return GameScreen.MARGIN + usableWidth * GameScreen.MESSAGE_PANEL_WIDTH_RATIO + GameScreen.PANEL_GAP / 2f
    }

    private fun isCircleHit(cx: Float, cy: Float, mx: Float, my: Float, r: Float): Boolean {
        val dx = mx - cx
        val dy = my - cy
        return dx * dx + dy * dy <= r * r
    }

    private fun isRectHit(x: Float, y: Float, rx: Float, ry: Float, rw: Float, rh: Float): Boolean {
        return x >= rx && x <= rx + rw && y >= ry && y <= ry + rh
    }
}
