package exploration.adapter.ui.libgdx

import com.badlogic.gdx.Input as GdxInput
import exploration.model.Direction
import exploration.port.GameEngine
import exploration.port.GameRef
import exploration.port.InputEvent
import exploration.port.ViewData

/**
 * Pure game logic extracted from GameScreen. Handles input processing, state transitions,
 * engine interaction, and story management. No libGDX dependencies — fully unit testable.
 */
class GamePresenter(
    private val engine: GameEngine,
    private val scenarioPath: String
) {

    var viewData: ViewData? = null

    var overlayState: OverlayState = OverlayState.Playing
        private set

    var selectionState: SelectionState = SelectionState.inactive()
        private set

    var pendingStories: List<String> = emptyList()

    var currentStoryIndex: Int = 0

    var storyScrollLines: Int = 0
        private set

    private var gameRef: GameRef? = null
    private var storedStoryCount: Int = 0

    init {
        val ref = engine.start(scenarioPath)
        gameRef = ref
        viewData = engine.tick(ref, InputEvent.Look)
        storedStoryCount = viewData!!.storyMessages.size
    }

    /** Process non-printable key events (arrows, J, Escape, Page Up/Down). */
    fun handleKeyDown(keycode: Int, backend: RenderBackend): Boolean {
        val vd = viewData ?: return false

        // F12 is handled by GameScreen for screenshots — skip here

        // StoryViewer scrolling — Page Up/Down, arrows only (W/S handled in keyTyped)
        if (overlayState == OverlayState.StoryViewer && pendingStories.isNotEmpty()) {
            when (keycode) {
                GdxInput.Keys.PAGE_UP -> storyScrollLines = (storyScrollLines - 5).coerceAtLeast(0)
                GdxInput.Keys.PAGE_DOWN -> storyScrollLines = (storyScrollLines + 1).coerceAtMost(getMaxStoryScroll(backend))
                GdxInput.Keys.UP -> storyScrollLines = (storyScrollLines - 1).coerceAtLeast(0)
                GdxInput.Keys.DOWN -> storyScrollLines = (storyScrollLines + 1).coerceAtMost(getMaxStoryScroll(backend))
                GdxInput.Keys.LEFT -> {
                    currentStoryIndex = (currentStoryIndex - 1).coerceAtLeast(0)
                    storyScrollLines = 0
                }
                GdxInput.Keys.RIGHT -> {
                    currentStoryIndex = (currentStoryIndex + 1).coerceAtMost(pendingStories.size - 1)
                    storyScrollLines = 0
                }
            }
        }

        // Arrow keys — non-printable, only fire keyDown (but not when StoryViewer is active)
        val event = when {
            overlayState == OverlayState.StoryViewer && pendingStories.isNotEmpty() -> null
            keycode == GdxInput.Keys.UP -> InputEvent.MoveDirection(Direction.North)
            keycode == GdxInput.Keys.LEFT -> InputEvent.MoveDirection(Direction.West)
            keycode == GdxInput.Keys.DOWN -> InputEvent.MoveDirection(Direction.South)
            keycode == GdxInput.Keys.RIGHT -> InputEvent.MoveDirection(Direction.East)
            else -> null
        }

        // J — story viewer (non-printable key)
        if (event == null && keycode == GdxInput.Keys.J) {
            val allStories = vd.storyMessages.filter { it.isNotBlank() }
            if (allStories.isNotEmpty()) {
                overlayState = OverlayState.StoryViewer
                pendingStories = allStories
                currentStoryIndex = 0
                storyScrollLines = 0
            }
        }

        // Escape — non-printable key
        if (event == null && keycode == GdxInput.Keys.ESCAPE) {
            when (overlayState) {
                OverlayState.StoryViewer -> {
                    overlayState = OverlayState.Playing
                    pendingStories = emptyList()
                }
                OverlayState.Inventory, OverlayState.GameOver -> { /* handled by GameScreen */ }
                OverlayState.QuitConfirm -> overlayState = OverlayState.Playing
                else -> overlayState = OverlayState.QuitConfirm
            }
        }

        if (event != null && gameRef != null) {
            viewData = engine.tick(gameRef!!, event)
            checkGameOver(viewData!!)
            updateStoryOverlay()
        }
        return true
    }

    /** Process printable key events (WASD, L/U/I, g/p/e/r, 0-9, Y/N). */
    fun handleKeyTyped(character: Char, backend: RenderBackend): Boolean {
        val vd = viewData ?: return false

        // Y/N — quit confirmation dialog
        if (overlayState == OverlayState.QuitConfirm) {
            when (character.lowercaseChar()) {
                'y' -> { /* handled by GameScreen */ }
                'n' -> overlayState = OverlayState.Playing
            }
            return true
        }

        var event: InputEvent? = null

        // Movement — printable chars only fire keyTyped (not keyDown)
        when (character.lowercaseChar()) {
            'w' -> if (overlayState == OverlayState.StoryViewer && pendingStories.isNotEmpty()) {
                storyScrollLines = (storyScrollLines - 1).coerceAtLeast(0)
            } else {
                event = InputEvent.MoveDirection(Direction.North)
            }
            'a' -> if (overlayState == OverlayState.StoryViewer && pendingStories.isNotEmpty()) {
                currentStoryIndex = (currentStoryIndex - 1).coerceAtLeast(0)
                storyScrollLines = 0
            } else {
                event = InputEvent.MoveDirection(Direction.West)
            }
            's' -> if (overlayState == OverlayState.StoryViewer && pendingStories.isNotEmpty()) {
                storyScrollLines = (storyScrollLines + 1).coerceAtMost(getMaxStoryScroll(backend))
            } else {
                event = InputEvent.MoveDirection(Direction.South)
            }
            'd' -> if (overlayState == OverlayState.StoryViewer && pendingStories.isNotEmpty()) {
                currentStoryIndex = (currentStoryIndex + 1).coerceAtMost(pendingStories.size - 1)
                storyScrollLines = 0
            } else {
                event = InputEvent.MoveDirection(Direction.East)
            }
            'l' -> event = InputEvent.Look
            'u' -> event = InputEvent.Activate
            'i' -> event = InputEvent.Inventory
        }

        // g/p/e/r — item actions (may trigger selection overlay)
        if (event == null) {
            val action = InputMapper.handleItemKey(character, vd)
            when (action) {
                is InputMapper.ItemAction.Event -> {
                    viewData = engine.tick(gameRef!!, action.event)
                    checkGameOver(viewData!!)
                    updateStoryOverlay()
                }
                is InputMapper.ItemAction.Selection -> {
                    selectionState = SelectionState(true, action.target, action.items)
                    overlayState = OverlayState.Inventory
                }
                is InputMapper.ItemAction.Message -> {} // no-op for now
                null -> {}
            }
        }

        // 0-9 — digit selection for multi-item prompts
        if (event == null && character in '0'..'9') {
            event = handleDigitSelection(character, vd)
        }

        if (event != null && gameRef != null) {
            viewData = engine.tick(gameRef!!, event)
            checkGameOver(viewData!!)
            updateStoryOverlay()
        }
        return true
    }

    /** Process touch input for movement and item interactions. */
    fun handleTouchDown(x: Float, y: Float): Boolean {
        val vd = viewData ?: return false

        var event = InputMapper.mapTouchDirection(x, y, vd)
        if (event != null && gameRef != null) {
            viewData = engine.tick(gameRef!!, event)
            checkGameOver(viewData!!)
            updateStoryOverlay()
            return true
        }

        val actionResult = InputMapper.mapTouchItem(x, y, vd)
        actionResult?.let { result ->
            val inputEvent = when (result) {
                is ItemActionResult.Take -> InputEvent.TakeItem(result.itemName)
                is ItemActionResult.Drop -> InputEvent.DropItem(result.itemName)
                is ItemActionResult.Equip -> InputEvent.EquipItem(result.itemName)
                is ItemActionResult.Unequip -> InputEvent.UnequipItem(result.itemName)
            }
            if (gameRef != null) {
                viewData = engine.tick(gameRef!!, inputEvent)
                checkGameOver(viewData!!)
                updateStoryOverlay()
            }
        }

        return true
    }

    /** Close any active selection overlay. */
    fun closeSelection() {
        selectionState = SelectionState.inactive()
        overlayState = OverlayState.Playing
    }

    /** Calculate maximum scroll lines for the current story viewer content. */
    fun getMaxStoryScroll(backend: RenderBackend): Int {
        if (pendingStories.isEmpty() || currentStoryIndex < 0 || currentStoryIndex >= pendingStories.size) return 0
        val story = pendingStories[currentStoryIndex]
        val boxW = 700f
        val maxWidthPx = (boxW - RenderBackend.MARGIN * 2).toInt()
        val maxCharsPerLine = if (maxWidthPx > 0) {
            val testStr = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
            val testWidth = backend.textWidth(testStr)
            ((testStr.length.toFloat() / testWidth) * maxWidthPx).toInt().coerceIn(1, 200)
        } else 40

        var lineCount = 0
        if (story.isBlank()) return 0
        for (segment in story.split("\n")) {
            if (segment.isBlank()) {
                lineCount += 1
            } else {
                lineCount += backend.splitText(segment, maxCharsPerLine).size
            }
        }
        val boxH = 500f
        val availableHeight = boxH - RenderBackend.MARGIN * 2 - backend.fontLineHeight
        val maxVisibleLines = (availableHeight / (backend.fontLineHeight * 1.3f)).toInt().coerceAtLeast(1)
        return maxOf(lineCount - maxVisibleLines, 0)
    }

    /** Calculate visible lines for a given height using the backend's font metrics. */
    fun getVisibleLinesInHeight(height: Float, backend: RenderBackend): Int {
        return (height / backend.fontLineHeight).toInt().coerceAtLeast(1)
    }

    private fun handleDigitSelection(digit: Char, vd: ViewData): InputEvent? {
        if (!selectionState.active) return null

        val index = digit - '1'
        if (index < 0 || index >= selectionState.items.size) {
            closeSelection()
            return null
        }

        val selectedItem = selectionState.items[index]
        val event = when (selectionState.target) {
            SelectionTarget.UNEQUIP -> InputEvent.UnequipItem(selectedItem.name)
            SelectionTarget.DROP -> InputEvent.DropItem(selectedItem.name)
            SelectionTarget.TAKE -> InputEvent.TakeItem(selectedItem.name)
            SelectionTarget.EQUIP -> InputEvent.EquipItem(selectedItem.name)
            else -> null
        }

        closeSelection()
        return event
    }

    private fun checkGameOver(vd: ViewData) {
        if (vd.endGameMessage != null) {
            overlayState = OverlayState.GameOver
        }
    }

    private fun updateStoryOverlay() {
        val vd = viewData ?: return
        if (vd.storyMessages.size > storedStoryCount) {
            overlayState = OverlayState.StoryViewer
            pendingStories = vd.storyMessages.drop(storedStoryCount).filter { it.isNotBlank() }
            currentStoryIndex = 0
            storyScrollLines = 0
        }
        storedStoryCount = vd.storyMessages.size
    }

    /** Check if a new story message was generated. */
    fun hasNewStories(): Boolean {
        val vd = viewData ?: return false
        return vd.storyMessages.size > storedStoryCount
    }
}
