# Story Messages, Overlay System, and Locked Item Display

## What was done

### 1. `Effect.StoryMessage` — new trigger effect type

- Added `StoryMessage(val text: String)` to `Effect` sealed class in `Trigger.kt`
- Added `storyMessages: List<String>` field to `GameState`
- Added `"storyMessage"` parsing branch in `ScenarioLoader.kt` (maps JSON `effect: "storyMessage"` → `Effect.StoryMessage`)

### 2. TriggerEngine support for StoryMessage effect

- Added `EffectsAccumulator.storyMessages` collection
- Handles `StoryMessage` by appending text to the accumulator
- Applies accumulated story messages to GameState in the return copy (alongside health/status/item changes)

### 3. ViewData integration

- Added `storyMessages: List<String>` field to `ViewData`
- Extended `ItemView` with `locked: Boolean = false`
- `GameEngineImpl` populates both fields from GameState when building view

### 4. Text and Key UI adapters — inline story message display

Both `TextUiAdapter.render()` and `KeyUiAdapter.render()` now iterate over `viewData.storyMessages` and print them inline before the status bar, keeping behavior consistent across all three UIs.

### 5. Lanterna Overlay System (sealed interface)

Added `Overlay` sealed interface in `LanternaUiAdapter.kt`:
- **None** — no overlay active (object)
- **MessageViewer(messages, scrollOffset, maxHeight, focusedIndex)** — shows story messages; `focusedIndex` tracks which message is currently displayed for side-arrow navigation

### 6. Lanterna Story Message Viewer Overlay

After each tick that produces trigger text or story messages:
- Creates `Overlay.MessageViewer` with only the newest (non-displayed) messages, focused on the last one
- **Vertical scroll**: PageUp/PageDown/Arrow Up/Arrow Down keys navigate within a message
- **Side arrows**: ArrowLeft shows previous story message (scroll resets to top), ArrowRight shows next newer message (scroll resets to top)
- Footer shows position counter: `"X / Y"` (e.g., "1 / 3")
- Escape closes viewer and returns to normal game play

### 7. Lanterna Journal — removed, replaced by 'j' key in MessageViewer

Journal overlay removed entirely. `'j'` now opens `MessageViewer` with the latest story message from the current view (same as auto-open after gameplay events). This avoids keeping a separate message accumulation system and keeps all story messages accessible through one unified viewer.

### 8. NoOp action — prevents history flooding during overlay navigation

Added `KeyAction.NoOp` to the sealed class:
- Re-renders without calling `engine.tick()` or adding anything to history
- All overlay navigation keys (arrows, PageUp/Down) return `NoOp` instead of `Help`
- Escape from overlay returns `NoOp` with `Overlay.None`, leaving history exactly as it was before opening
- 'h' key also uses `NoOp` — no more help messages flooding the log

### 9. Locked Item Display in Lanterna UI

Locked items (those with a trigger effect that moves them into the player's inventory while they're not yet on the correct area) are displayed:
- In red color (`TextColor.ANSI.RED`)
- With "(Locked)" suffix appended to the item name
- The "grab" prompt for area items checks `ItemView.locked` and skips locked items

### 10. Scroll bounds and paragraph rendering fixes

- **Scroll max corrected**: `maxScroll` now uses `countWrappedDisplayLines()` (accounts for text wrapping in narrow terminals) instead of `countDisplayLines()` (unwrapped line count ≈ number of messages). This allows scrolling to the very end of long wrapped content.
- **+3 padding at bottom**: maxScroll includes +3 so players can clearly see blank space below the last line, confirming "end of message."
- **Paragraph/blank-line support**: Messages are split by `\n` before wrapping, rendering empty paragraphs as padded blank rows that count toward scroll bounds. Multi-paragraph story messages display with proper spacing.

## Files changed

| File | Change |
|---|---|
| `core/model/Trigger.kt` | Added `Effect.StoryMessage(val text: String)` |
| `core/state/GameState.kt` | Added `storyMessages: List<String>` field |
| `core/engine/TriggerEngine.kt` | StoryMessage handling in EffectsAccumulator and return copy |
| `port/ViewData.kt` | Added `storyMessages`, extended `ItemView.locked` |
| `core/engine/GameEngineImpl.kt` | Populates locked + storyMessages in view building |
| `scenario/ScenarioLoader.kt` | `"storyMessage"` JSON effect parser |
| `adapter/ui/lanterna/LanternaUiAdapter.kt` | Overlay system, MessageViewer with focusedIndex/side arrows, NoOp action, paragraph rendering, scroll bounds fix, locked item display, journal removal |
| `adapter/ui/text/TextUiAdapter.kt` | Inline story message printing in render() |
| `adapter/ui/key/KeyUiAdapter.kt` | Inline story message printing in render() |
