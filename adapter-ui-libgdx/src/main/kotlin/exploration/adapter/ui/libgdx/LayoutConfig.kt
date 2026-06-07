package exploration.adapter.ui.libgdx

/** Shared layout configuration for libGDX UI components. */
data class LayoutConfig(
    val viewportW: Float = 1800f,
    val viewportH: Float = 1100f,
    val bottomBarHeight: Float = 180f,
    val margin: Float = 20f,
    val messagePanelWidthRatio: Float = 0.58f,
    val panelGap: Float = 30f,
    val directionBtnSize: Float = 70f,
    val statusBarWidth: Float = 180f,
    val panelPadding: Float = 20f,
    val maxMessages: Int = 50,
    val storyBoxW: Float = 700f,
    val storyBoxH: Float = 500f,
    val gameOverBoxW: Float = 600f,
    val gameOverBoxH: Float = 300f,
    val quitConfirmBoxW: Float = 500f,
    val quitConfirmBoxH: Float = 200f,
    val touchButtonRadius: Float = 35f,
    val itemHitWidth: Float = 340f,
    val itemHitHeight: Float = 36f,
    val carriedItemStartY: Float = 500f,
    val areaItemStartY: Float = 380f,
    val itemSpacing: Float = 44f
)
