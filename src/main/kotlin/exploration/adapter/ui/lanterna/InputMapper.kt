package exploration.adapter.ui.lanterna

import com.googlecode.lanterna.input.KeyStroke
import com.googlecode.lanterna.input.KeyType
import exploration.port.InputEvent
import exploration.port.ItemView
import exploration.port.ViewData

interface KeyMapper {
    fun mapKey(
        key: KeyStroke,
        view: ViewData,
        selState: SelectionState?,
        overlay: Overlay,
        terminalColumns: Int = 0,
        terminalRows: Int = 0
    ): ReadInputResult
}

class LanternaKeyMapper : KeyMapper {

    override fun mapKey(
        key: KeyStroke,
        view: ViewData,
        selState: SelectionState?,
        overlay: Overlay,
        terminalColumns: Int,
        terminalRows: Int
    ): ReadInputResult {
        if (selState != null) {
            return handleSelectionDigit(key, selState, overlay)
        }

        var nextOverlay = overlay
        var handledInOverlay: Boolean

        when (overlay) {
            is Overlay.MessageViewer -> {
                val ov = overlay
                val textAreaWidth = UiUtils.calcOverlayWidth(terminalColumns)
                val totalWrappedLines = UiUtils.countWrappedDisplayLines(ov.messages, textAreaWidth)
                val visibleRows = UiUtils.calcOverlayMaxHeight(ov.maxHeight, terminalRows) - 3
                val maxScroll = maxOf(0, totalWrappedLines - visibleRows + 3)

                handledInOverlay = when (key.keyType) {
                    KeyType.PageDown -> {
                        nextOverlay = ov.copy(scrollOffset = minOf(maxScroll, ov.scrollOffset + maxOf(1, UiUtils.calcOverlayMaxHeight(ov.maxHeight, terminalRows) - 2)))
                        true
                    }
                    KeyType.ArrowDown -> {
                        nextOverlay = ov.copy(scrollOffset = minOf(maxScroll, ov.scrollOffset + 1))
                        true
                    }
                    KeyType.PageUp -> {
                        nextOverlay = ov.copy(scrollOffset = maxOf(0, ov.scrollOffset - maxOf(1, ov.maxHeight - 2)))
                        true
                    }
                    KeyType.ArrowUp -> {
                        nextOverlay = ov.copy(scrollOffset = maxOf(0, ov.scrollOffset - 1))
                        true
                    }
                    KeyType.ArrowLeft -> {
                        if (ov.focusedIndex > 0)
                            nextOverlay = ov.copy(focusedIndex = ov.focusedIndex - 1, scrollOffset = 0)
                        true
                    }
                    KeyType.ArrowRight -> {
                        if (ov.focusedIndex < ov.messages.size - 1)
                            nextOverlay = ov.copy(focusedIndex = ov.focusedIndex + 1, scrollOffset = 0)
                        true
                    }
                    KeyType.Escape -> {
                        nextOverlay = Overlay.None
                        true
                    }
                    else -> false
                }
            }
            Overlay.None -> handledInOverlay = false
        }

        if (handledInOverlay) return ReadInputResult(KeyAction.NoOp, null, nextOverlay)

        val displayOverlay = if (overlay !is Overlay.None && key.keyType != KeyType.Escape) {
            Overlay.None
        } else {
            overlay
        }

        return mapMainKey(key, view, selState, displayOverlay)
    }

    private fun mapMainKey(
        key: KeyStroke,
        view: ViewData,
        selState: SelectionState?,
        displayOverlay: Overlay
    ): ReadInputResult {
        return when (key.keyType) {
            KeyType.Escape -> return if (displayOverlay !is Overlay.None) {
                ReadInputResult(KeyAction.NoOp, null, Overlay.None)
            } else {
                ReadInputResult(KeyAction.Quit, null, displayOverlay)
            }
            KeyType.ArrowUp -> return ReadInputResult(KeyAction.Event(InputEvent.MoveDirection(InputEvent.Direction.North)), selState, displayOverlay)
            KeyType.ArrowDown -> return ReadInputResult(KeyAction.Event(InputEvent.MoveDirection(InputEvent.Direction.South)), selState, displayOverlay)
            KeyType.ArrowLeft -> return ReadInputResult(KeyAction.Event(InputEvent.MoveDirection(InputEvent.Direction.West)), selState, displayOverlay)
            KeyType.ArrowRight -> return ReadInputResult(KeyAction.Event(InputEvent.MoveDirection(InputEvent.Direction.East)), selState, displayOverlay)
            KeyType.Character -> {
                when (key.character.lowercaseChar()) {
                    'w' -> ReadInputResult(KeyAction.Event(InputEvent.MoveDirection(InputEvent.Direction.North)), null, displayOverlay)
                    'a' -> ReadInputResult(KeyAction.Event(InputEvent.MoveDirection(InputEvent.Direction.West)), null, displayOverlay)
                    's' -> ReadInputResult(KeyAction.Event(InputEvent.MoveDirection(InputEvent.Direction.South)), null, displayOverlay)
                    'd' -> ReadInputResult(KeyAction.Event(InputEvent.MoveDirection(InputEvent.Direction.East)), null, displayOverlay)
                    'l' -> ReadInputResult(KeyAction.Event(InputEvent.Look), null, displayOverlay)
                    'u' -> ReadInputResult(KeyAction.Event(InputEvent.Activate), null, displayOverlay)
                    'g' -> handleTakeItem(view, displayOverlay)
                    'p' -> handleDropItem(view, displayOverlay)
                    'e' -> handleEquipItem(view, displayOverlay)
                    'r' -> handleUnequipItem(view, displayOverlay)
                    'i' -> ReadInputResult(KeyAction.Event(InputEvent.Inventory), null, displayOverlay)
                    'j' -> handleStoryViewer(view)
                    'h' -> ReadInputResult(KeyAction.NoOp, null, displayOverlay)
                    'q' -> ReadInputResult(KeyAction.Quit, null, displayOverlay)
                    else -> ReadInputResult(KeyAction.Help, null, displayOverlay)
                }
            }
            else -> ReadInputResult(KeyAction.Help, selState, displayOverlay)
        }
    }

    private fun handleTakeItem(view: ViewData, overlay: Overlay): ReadInputResult {
        val candidates = view.areaItems
        return if (candidates.isEmpty()) ReadInputResult(KeyAction.Help, null, overlay)
        else if (candidates.size == 1) ReadInputResult(KeyAction.Event(InputEvent.TakeItem(candidates[0].name)), null, overlay)
        else ReadInputResult(KeyAction.WaitSelection, SelectionState(SelectionTarget.TAKE, candidates), overlay)
    }

    private fun handleDropItem(view: ViewData, overlay: Overlay): ReadInputResult {
        val candidates = view.carriedItems + view.equippedItems
        return if (candidates.isEmpty()) ReadInputResult(KeyAction.Help, null, overlay)
        else if (candidates.size == 1) ReadInputResult(KeyAction.Event(InputEvent.DropItem(candidates[0].name)), null, overlay)
        else ReadInputResult(KeyAction.WaitSelection, SelectionState(SelectionTarget.DROP, candidates), overlay)
    }

    private fun handleEquipItem(view: ViewData, overlay: Overlay): ReadInputResult {
        val candidates = view.carriedItems
        return if (candidates.isEmpty()) ReadInputResult(KeyAction.Help, null, overlay)
        else if (candidates.size == 1) ReadInputResult(KeyAction.Event(InputEvent.EquipItem(candidates[0].name)), null, overlay)
        else ReadInputResult(KeyAction.WaitSelection, SelectionState(SelectionTarget.EQUIP, candidates), overlay)
    }

    private fun handleUnequipItem(view: ViewData, overlay: Overlay): ReadInputResult {
        val candidates = view.equippedItems
        return if (candidates.isEmpty()) ReadInputResult(KeyAction.Help, null, overlay)
        else if (candidates.size == 1) ReadInputResult(KeyAction.Event(InputEvent.UnequipItem(candidates[0].name)), null, overlay)
        else ReadInputResult(KeyAction.WaitSelection, SelectionState(SelectionTarget.UNEQUIP, candidates), overlay)
    }

    private fun handleStoryViewer(view: ViewData): ReadInputResult {
        val msgs = view.storyMessages.filter { it.isNotBlank() }
        return if (msgs.isEmpty()) ReadInputResult(KeyAction.Help, null, Overlay.None)
        else ReadInputResult(KeyAction.NoOp, null, Overlay.MessageViewer(msgs, focusedIndex = msgs.lastIndex))
    }

    private fun handleSelectionDigit(key: KeyStroke, selState: SelectionState, overlay: Overlay): ReadInputResult {
        if (key.keyType == KeyType.Escape) return ReadInputResult(KeyAction.Help, null, overlay)
        if (key.keyType != KeyType.Character) return ReadInputResult(KeyAction.WaitSelection, selState, overlay)

        val idx = key.character.digitToIntOrNull() ?: return ReadInputResult(KeyAction.WaitSelection, selState, overlay)
        val itemIdx = idx - 1
        if (itemIdx < 0 || itemIdx >= selState.items.size) return ReadInputResult(KeyAction.Help, null, overlay)

        val event = when (selState.target) {
            SelectionTarget.TAKE -> InputEvent.TakeItem(selState.items[itemIdx].name)
            SelectionTarget.DROP -> InputEvent.DropItem(selState.items[itemIdx].name)
            SelectionTarget.EQUIP -> InputEvent.EquipItem(selState.items[itemIdx].name)
            SelectionTarget.UNEQUIP -> InputEvent.UnequipItem(selState.items[itemIdx].name)
        }
        return ReadInputResult(KeyAction.Event(event), null, overlay)
    }
}

sealed class KeyAction {
    object Quit : KeyAction()
    object Help : KeyAction()
    object WaitSelection : KeyAction()
    object NoOp : KeyAction()
    data class Event(val event: InputEvent) : KeyAction()
}

data class ReadInputResult(
    val action: KeyAction,
    val selectionState: SelectionState?,
    val overlay: Overlay
)
