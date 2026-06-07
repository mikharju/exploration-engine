package exploration.adapter.ui.libgdx

import exploration.port.ViewData

/** Backend abstraction for rendering game UI. Enables testable GameScreen by decoupling from libGDX. */
interface RenderBackend {
    fun render(viewData: ViewData, selectionState: SelectionState)
    fun renderWithOverlay(viewData: ViewData, overlayName: String, selectionState: SelectionState)
    fun renderStoryViewer(
        viewData: ViewData,
        stories: List<String>,
        selectionState: SelectionState,
        scrollLines: Int,
        maxVisibleLines: Int,
        currentPage: Int,
        totalPages: Int
    )
    fun renderGameOver(message: String)
    fun renderQuitConfirm()
    fun splitText(text: String, maxChars: Int): List<String>
    val fontLineHeight: Float
    fun textWidth(text: String): Float

    companion object {
        const val MARGIN = 40f
    }
}
