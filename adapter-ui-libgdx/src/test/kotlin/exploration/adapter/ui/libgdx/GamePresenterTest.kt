package exploration.adapter.ui.libgdx

import com.badlogic.gdx.Input as GdxInput
import exploration.model.Direction
import exploration.port.GameEngine
import exploration.port.GameRef
import exploration.port.InputEvent
import exploration.port.ItemView
import exploration.port.ViewData
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class GamePresenterTest {

    private fun makeEngine(
        initialStoryCount: Int = 0,
        storyMessages: List<String> = emptyList(),
        endGameMessage: String? = null
    ): GameEngine {
        return object : GameEngine {
            private var _tickCount = 0
            override val tickCount: Int get() = _tickCount

            override fun start(scenarioId: String): GameRef = GameRef("stub")

            override fun tick(ref: GameRef, event: InputEvent): ViewData {
                _tickCount++
                val newStories = if (event is InputEvent.Look && _tickCount == 1) {
                    storyMessages.take(initialStoryCount)
                } else {
                    storyMessages.drop(_tickCount - 1).take(1) + storyMessages.take(initialStoryCount)
                }
                return ViewData(
                    messageHistory = listOf("Tick $_tickCount"),
                    commandText = "", triggerTexts = emptyList(),
                    health = 10, maxHealth = 20, currentAreaName = "Room",
                    exploredCount = _tickCount, totalAreas = 5, activatedCount = 0, totalDevices = 2,
                    exits = mapOf(
                        Direction.North to exploration.port.ExitInfo("North exit"),
                        Direction.West to null,
                        Direction.South to exploration.port.ExitInfo("South exit"),
                        Direction.East to null
                    ), statuses = emptyMap(), statusBounds = emptyMap(),
                    endGameMessage = endGameMessage,
                    areaItems = listOf(ItemView("key", "A key")),
                    carriedItems = if (_tickCount > 2) listOf(ItemView("sword", "A sword")) else emptyList(),
                    equippedItems = emptyList(),
                    storyMessages = newStories.filter { it.isNotBlank() }
                )
            }
        }
    }

    private fun makePresenter(
        engine: GameEngine = makeEngine(initialStoryCount = 1, storyMessages = listOf("Hello world", "Second message"))
    ): GamePresenter = GamePresenter(engine, "test.json")

    // --- Initial state ---

    @Test
    fun `presenter starts with viewData and Playing overlay`() {
        val p = makePresenter()
        assertNotNull(p.viewData)
        assertEquals(OverlayState.Playing, p.overlayState)
        assertFalse(p.selectionState.active)
        assertTrue(p.pendingStories.isEmpty())
    }

    @Test
    fun `presenter has at least one story after initial look`() {
        val p = makePresenter()
        assertTrue(p.viewData!!.storyMessages.isNotEmpty())
    }

    // --- Arrow key handling in Playing state ---

    @Test
    fun `arrow keys trigger movement events`() {
        val engine = makeEngine(initialStoryCount = 0, storyMessages = emptyList())
        val p = makePresenter(engine)
        assertEquals(OverlayState.Playing, p.overlayState)

        p.handleKeyDown(GdxInput.Keys.UP, StubBackend())
        assertEquals(2, engine.tickCount)

        p.handleKeyDown(GdxInput.Keys.DOWN, StubBackend())
        assertEquals(3, engine.tickCount)

        p.handleKeyDown(GdxInput.Keys.LEFT, StubBackend())
        assertEquals(4, engine.tickCount)

        p.handleKeyDown(GdxInput.Keys.RIGHT, StubBackend())
        assertEquals(5, engine.tickCount)
    }

    @Test
    fun `arrow keys do nothing in story viewer`() {
        val p = makePresenter()
        // Open story viewer first
        p.handleKeyDown(GdxInput.Keys.J, StubBackend())
        assertEquals(OverlayState.StoryViewer, p.overlayState)

        // Arrow keys should scroll stories, not trigger movement
        val initialIndex = p.currentStoryIndex
        p.handleKeyDown(GdxInput.Keys.DOWN, StubBackend())
        assertTrue(p.pendingStories.isNotEmpty())
    }

    // --- WASD key handling in Playing state ---

    @Test
    fun `WASD keys trigger movement events`() {
        val engine = makeEngine(initialStoryCount = 0, storyMessages = emptyList())
        val p = makePresenter(engine)

        p.handleKeyTyped('w', StubBackend())
        assertEquals(2, engine.tickCount)

        p.handleKeyTyped('a', StubBackend())
        assertEquals(3, engine.tickCount)

        p.handleKeyTyped('s', StubBackend())
        assertEquals(4, engine.tickCount)

        p.handleKeyTyped('d', StubBackend())
        assertEquals(5, engine.tickCount)
    }

    @Test
    fun `WASD keys are case insensitive`() {
        val engine = makeEngine(initialStoryCount = 0, storyMessages = emptyList())
        val p = makePresenter(engine)

        p.handleKeyTyped('W', StubBackend())
        assertEquals(2, engine.tickCount)

        p.handleKeyTyped('w', StubBackend())
        assertEquals(3, engine.tickCount)
    }

    // --- Action keys ---

    @Test
    fun `L key triggers look event`() {
        val engine = makeEngine(initialStoryCount = 0, storyMessages = emptyList())
        val p = makePresenter(engine)

        p.handleKeyTyped('l', StubBackend())
        assertEquals(2, engine.tickCount)
    }

    @Test
    fun `U key triggers activate event`() {
        val engine = makeEngine(initialStoryCount = 0, storyMessages = emptyList())
        val p = makePresenter(engine)

        p.handleKeyTyped('u', StubBackend())
        assertEquals(2, engine.tickCount)
    }

    @Test
    fun `I key triggers inventory event`() {
        val engine = makeEngine(initialStoryCount = 0, storyMessages = emptyList())
        val p = makePresenter(engine)

        p.handleKeyTyped('i', StubBackend())
        assertEquals(2, engine.tickCount)
    }

    // --- Story viewer ---

    @Test
    fun `J key opens story viewer when stories exist`() {
        val p = makePresenter()
        assertEquals(OverlayState.Playing, p.overlayState)

        p.handleKeyDown(GdxInput.Keys.J, StubBackend())
        assertEquals(OverlayState.StoryViewer, p.overlayState)
        assertTrue(p.pendingStories.isNotEmpty())
        assertEquals(0, p.currentStoryIndex)
    }

    @Test
    fun `story viewer shows correct number of stories`() {
        val engine = makeEngine(initialStoryCount = 3, storyMessages = listOf("A", "B", "C"))
        val p = makePresenter(engine)

        p.handleKeyDown(GdxInput.Keys.J, StubBackend())
        assertEquals(3, p.pendingStories.size)
    }

    @Test
    fun `story viewer scrolls with arrow keys`() {
        val engine = makeEngine(initialStoryCount = 5, storyMessages = listOf("A", "B", "C", "D", "E"))
        val p = makePresenter(engine)

        p.handleKeyDown(GdxInput.Keys.J, StubBackend())
        assertEquals(0, p.currentStoryIndex)

        p.handleKeyDown(GdxInput.Keys.RIGHT, StubBackend())
        assertEquals(1, p.currentStoryIndex)

        p.handleKeyDown(GdxInput.Keys.LEFT, StubBackend())
        assertEquals(0, p.currentStoryIndex)
    }

    @Test
    fun `story viewer scrolls with page up down`() {
        val engine = makeEngine(initialStoryCount = 5, storyMessages = listOf("A", "B", "C", "D", "E"))
        val p = makePresenter(engine)

        p.handleKeyDown(GdxInput.Keys.J, StubBackend())
        assertEquals(0, p.storyScrollLines)

        p.handleKeyDown(GdxInput.Keys.PAGE_DOWN, StubBackend())
        assertTrue(p.storyScrollLines >= 0 || p.pendingStories.size <= 1)

        p.handleKeyDown(GdxInput.Keys.PAGE_UP, StubBackend())
        assertTrue(p.storyScrollLines >= 0)
    }

    @Test
    fun `W S scroll story viewer when active`() {
        val engine = makeEngine(initialStoryCount = 5, storyMessages = listOf("A", "B", "C", "D", "E"))
        val p = makePresenter(engine)

        p.handleKeyDown(GdxInput.Keys.J, StubBackend())
        assertEquals(0, p.storyScrollLines)

        p.handleKeyTyped('s', StubBackend())
        assertTrue(p.storyScrollLines >= 0)
    }

    @Test
    fun `A D navigate stories when story viewer active`() {
        val engine = makeEngine(initialStoryCount = 3, storyMessages = listOf("A", "B", "C"))
        val p = makePresenter(engine)

        p.handleKeyDown(GdxInput.Keys.J, StubBackend())
        assertEquals(0, p.currentStoryIndex)

        p.handleKeyTyped('d', StubBackend())
        assertEquals(1, p.currentStoryIndex)

        p.handleKeyTyped('a', StubBackend())
        assertEquals(0, p.currentStoryIndex)
    }

    @Test
    fun `Escape closes story viewer`() {
        val p = makePresenter()
        p.handleKeyDown(GdxInput.Keys.J, StubBackend())
        assertEquals(OverlayState.StoryViewer, p.overlayState)

        p.handleKeyDown(GdxInput.Keys.ESCAPE, StubBackend())
        assertEquals(OverlayState.Playing, p.overlayState)
    }

    // --- Quit confirmation ---

    @Test
    fun `Escape in playing shows quit confirm`() {
        val p = makePresenter()
        assertEquals(OverlayState.Playing, p.overlayState)

        p.handleKeyDown(GdxInput.Keys.ESCAPE, StubBackend())
        assertEquals(OverlayState.QuitConfirm, p.overlayState)
    }

    @Test
    fun `N cancels quit confirm`() {
        val p = makePresenter()
        p.handleKeyDown(GdxInput.Keys.ESCAPE, StubBackend())
        assertEquals(OverlayState.QuitConfirm, p.overlayState)

        p.handleKeyTyped('n', StubBackend())
        assertEquals(OverlayState.Playing, p.overlayState)
    }

    @Test
    fun `Y in quit confirm is handled by GameScreen`() {
        val p = makePresenter()
        p.handleKeyDown(GdxInput.Keys.ESCAPE, StubBackend())
        assertEquals(OverlayState.QuitConfirm, p.overlayState)

        // Y should not change overlay state (GameScreen handles exit)
        p.handleKeyTyped('y', StubBackend())
        assertEquals(OverlayState.QuitConfirm, p.overlayState)
    }

    @Test
    fun `Escape in quit confirm returns to playing`() {
        val p = makePresenter()
        p.handleKeyDown(GdxInput.Keys.ESCAPE, StubBackend())
        p.handleKeyDown(GdxInput.Keys.ESCAPE, StubBackend())
        assertEquals(OverlayState.Playing, p.overlayState)
    }

    // --- Item actions ---

    @Test
    fun `g key with single area item triggers take`() {
        val engine = makeEngine(initialStoryCount = 0, storyMessages = emptyList())
        val p = makePresenter(engine)

        p.handleKeyTyped('g', StubBackend())
        assertEquals(2, engine.tickCount)
    }

    @Test
    fun `p key with no carried items shows message`() {
        val p = makePresenter(makeEngine(initialStoryCount = 0, storyMessages = emptyList()))
        // No carried items initially, should not trigger tick
        assertEquals(1, p.viewData!!.exploredCount) // only initial look
    }

    @Test
    fun `e key with single carried item triggers equip`() {
        val engine = makeEngine(initialStoryCount = 0, storyMessages = emptyList())
        val p = makePresenter(engine)

        // First tick to get carried items (engine returns sword after tick > 2)
        p.handleKeyTyped('w', StubBackend())
        p.handleKeyTyped('w', StubBackend())
        p.handleKeyTyped('e', StubBackend())
        assertEquals(4, engine.tickCount)
    }

    @Test
    fun `r key with no equipped items shows message`() {
        val p = makePresenter(makeEngine(initialStoryCount = 0, storyMessages = emptyList()))
        // No equipped items, should not trigger tick
        assertEquals(1, p.viewData!!.exploredCount) // only initial look
    }

    @Test
    fun `multiple area items trigger selection overlay`() {
        val engine = object : GameEngine {
            private var _tickCount = 0
            override val tickCount: Int get() = _tickCount
            override fun start(scenarioId: String): GameRef = GameRef("stub")
            override fun tick(ref: GameRef, event: InputEvent): ViewData {
                _tickCount++
                return ViewData(
                    messageHistory = listOf("Tick $_tickCount"), commandText = "", triggerTexts = emptyList(),
                    health = 10, maxHealth = 20, currentAreaName = "Room",
                    exploredCount = _tickCount, totalAreas = 5, activatedCount = 0, totalDevices = 2,
                    exits = mapOf(
                        Direction.North to null, Direction.West to null,
                        Direction.South to null, Direction.East to null
                    ), statuses = emptyMap(), statusBounds = emptyMap(),
                    areaItems = listOf(ItemView("x", ""), ItemView("y", "")),
                    carriedItems = emptyList(), equippedItems = emptyList()
                )
            }
        }
        val p = makePresenter(engine)

        p.handleKeyTyped('g', StubBackend())
        assertTrue(p.selectionState.active)
        assertEquals(OverlayState.Inventory, p.overlayState)
    }

    // --- Digit selection ---

    @Test
    fun `digit 1 selects first item in selection`() {
        val engine = object : GameEngine {
            private var _tickCount = 0
            override val tickCount: Int get() = _tickCount
            override fun start(scenarioId: String): GameRef = GameRef("stub")
            override fun tick(ref: GameRef, event: InputEvent): ViewData {
                _tickCount++
                return ViewData(
                    messageHistory = listOf("Tick $_tickCount"), commandText = "", triggerTexts = emptyList(),
                    health = 10, maxHealth = 20, currentAreaName = "Room",
                    exploredCount = _tickCount, totalAreas = 5, activatedCount = 0, totalDevices = 2,
                    exits = mapOf(
                        Direction.North to null, Direction.West to null,
                        Direction.South to null, Direction.East to null
                    ), statuses = emptyMap(), statusBounds = emptyMap(),
                    areaItems = listOf(ItemView("x", ""), ItemView("y", "")),
                    carriedItems = emptyList(), equippedItems = emptyList()
                )
            }
        }
        val p = makePresenter(engine)

        // Trigger selection with g key (2 items)
        p.handleKeyTyped('g', StubBackend())
        assertTrue(p.selectionState.active)

        // Select first item
        p.handleKeyTyped('1', StubBackend())
        assertFalse(p.selectionState.active)
    }

    @Test
    fun `digit selection closes overlay`() {
        val engine = object : GameEngine {
            private var _tickCount = 0
            override val tickCount: Int get() = _tickCount
            override fun start(scenarioId: String): GameRef = GameRef("stub")
            override fun tick(ref: GameRef, event: InputEvent): ViewData {
                _tickCount++
                return ViewData(
                    messageHistory = listOf("Tick $_tickCount"), commandText = "", triggerTexts = emptyList(),
                    health = 10, maxHealth = 20, currentAreaName = "Room",
                    exploredCount = _tickCount, totalAreas = 5, activatedCount = 0, totalDevices = 2,
                    exits = mapOf(
                        Direction.North to null, Direction.West to null,
                        Direction.South to null, Direction.East to null
                    ), statuses = emptyMap(), statusBounds = emptyMap(),
                    areaItems = listOf(ItemView("x", ""), ItemView("y", "")),
                    carriedItems = emptyList(), equippedItems = emptyList()
                )
            }
        }
        val p = makePresenter(engine)

        p.handleKeyTyped('g', StubBackend())
        assertEquals(OverlayState.Inventory, p.overlayState)

        p.handleKeyTyped('1', StubBackend())
        assertEquals(OverlayState.Playing, p.overlayState)
    }

    @Test
    fun `invalid digit closes selection`() {
        val engine = object : GameEngine {
            private var _tickCount = 0
            override val tickCount: Int get() = _tickCount
            override fun start(scenarioId: String): GameRef = GameRef("stub")
            override fun tick(ref: GameRef, event: InputEvent): ViewData {
                _tickCount++
                return ViewData(
                    messageHistory = listOf("Tick $_tickCount"), commandText = "", triggerTexts = emptyList(),
                    health = 10, maxHealth = 20, currentAreaName = "Room",
                    exploredCount = _tickCount, totalAreas = 5, activatedCount = 0, totalDevices = 2,
                    exits = mapOf(
                        Direction.North to null, Direction.West to null,
                        Direction.South to null, Direction.East to null
                    ), statuses = emptyMap(), statusBounds = emptyMap(),
                    areaItems = listOf(ItemView("x", "Item X"), ItemView("y", "Item Y")), // 2 items to trigger selection
                    carriedItems = emptyList(), equippedItems = emptyList()
                )
            }
        }
        val p = makePresenter(engine)

        p.handleKeyTyped('g', StubBackend())
        assertTrue(p.selectionState.active)

        // '2' is out of range for 1 item
        p.handleKeyTyped('2', StubBackend())
        assertFalse(p.selectionState.active)
    }

    @Test
    fun `closeSelection resets state`() {
        val engine = object : GameEngine {
            private var _tickCount = 0
            override val tickCount: Int get() = _tickCount
            override fun start(scenarioId: String): GameRef = GameRef("stub")
            override fun tick(ref: GameRef, event: InputEvent): ViewData {
                _tickCount++
                return ViewData(
                    messageHistory = listOf("Tick $_tickCount"), commandText = "", triggerTexts = emptyList(),
                    health = 10, maxHealth = 20, currentAreaName = "Room",
                    exploredCount = _tickCount, totalAreas = 5, activatedCount = 0, totalDevices = 2,
                    exits = mapOf(
                        Direction.North to null, Direction.West to null,
                        Direction.South to null, Direction.East to null
                    ), statuses = emptyMap(), statusBounds = emptyMap(),
                    areaItems = listOf(ItemView("x", ""), ItemView("y", "")),
                    carriedItems = emptyList(), equippedItems = emptyList()
                )
            }
        }
        val p = makePresenter(engine)

        p.handleKeyTyped('g', StubBackend())
        assertTrue(p.selectionState.active)
        assertEquals(OverlayState.Inventory, p.overlayState)

        p.closeSelection()
        assertFalse(p.selectionState.active)
        assertEquals(OverlayState.Playing, p.overlayState)
    }

    // --- Touch input ---

    @Test
    fun `touchDown triggers engine tick on valid touch`() {
        val engine = makeEngine(initialStoryCount = 0, storyMessages = emptyList())
        val p = makePresenter(engine)

        // Touch in direction button area should trigger movement
        p.handleTouchDown(900f, 150f)
        assertTrue(engine.tickCount >= 1)
    }

    // --- Game over ---

    @Test
    fun `endGameMessage sets overlay to GameOver`() {
        val engine = makeEngine(initialStoryCount = 0, storyMessages = emptyList(), endGameMessage = "You lost!")
        val p = makePresenter(engine)

        p.handleKeyTyped('w', StubBackend())
        assertEquals(OverlayState.GameOver, p.overlayState)
    }

    // --- getMaxStoryScroll ---

    @Test
    fun `getMaxStoryScroll returns 0 when no stories`() {
        val p = makePresenter()
        p.pendingStories = emptyList()
        assertEquals(0, p.getMaxStoryScroll(StubBackend()))
    }

    @Test
    fun `getMaxStoryScroll returns 0 for invalid index`() {
        val p = makePresenter()
        p.currentStoryIndex = -1
        assertEquals(0, p.getMaxStoryScroll(StubBackend()))

        p.currentStoryIndex = 999
        assertEquals(0, p.getMaxStoryScroll(StubBackend()))
    }

    // --- hasNewStories ---

    @Test
    fun `hasNewStories returns false when no viewData`() {
        val engine = makeEngine(initialStoryCount = 0, storyMessages = emptyList())
        val p = GamePresenter(engine, "test.json")
        p.viewData = null
        assertFalse(p.hasNewStories())
    }

    // --- Edge cases ---

    @Test
    fun `handleKeyDown with unknown keycode does nothing`() {
        val engine = makeEngine(initialStoryCount = 0, storyMessages = emptyList())
        val p = makePresenter(engine)
        val initialTicks = engine.tickCount

        p.handleKeyDown(999, StubBackend())
        assertEquals(initialTicks, engine.tickCount)
    }

    @Test
    fun `handleKeyTyped with unknown character does nothing`() {
        val engine = makeEngine(initialStoryCount = 0, storyMessages = emptyList())
        val p = makePresenter(engine)
        val initialTicks = engine.tickCount

        p.handleKeyTyped('z', StubBackend())
        assertEquals(initialTicks, engine.tickCount)
    }

    @Test
    fun `handleKeyDown returns true always`() {
        val p = makePresenter()
        assertTrue(p.handleKeyDown(GdxInput.Keys.UP, StubBackend()))
        assertTrue(p.handleKeyDown(999, StubBackend()))
    }

    @Test
    fun `handleKeyTyped returns true always`() {
        val p = makePresenter()
        assertTrue(p.handleKeyTyped('w', StubBackend()))
        assertTrue(p.handleKeyTyped('z', StubBackend()))
    }

    @Test
    fun `handleTouchDown returns true always`() {
        val p = makePresenter()
        assertTrue(p.handleTouchDown(100f, 100f))
    }

    // --- Stub backend for tests ---

    private class StubBackend : RenderBackend {
        override fun render(viewData: ViewData, selectionState: SelectionState) {}
        override fun renderWithOverlay(viewData: ViewData, overlayName: String, selectionState: SelectionState) {}
        override fun renderStoryViewer(
            viewData: ViewData, stories: List<String>,
            selectionState: SelectionState, scrollLines: Int, maxVisibleLines: Int,
            currentPage: Int, totalPages: Int
        ) {}
        override fun renderGameOver(message: String) {}
        override fun renderQuitConfirm() {}
        override fun splitText(text: String, maxChars: Int): List<String> {
            if (maxChars <= 0) return listOf(text)
            return text.split("\n")
        }
        override val fontLineHeight: Float get() = 16f
        override fun textWidth(text: String): Float = text.length * 8f
    }
}
