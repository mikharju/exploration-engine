# Plan: adapter-ui-libgdx — Graphical UI Adapter

## Goal

New UI variant using libGDX instead of terminal text rendering. Same game data and interfaces as lanterna/text/key variants, but with graphical rendering, clickable controls, and animated visual elements.

## Module Structure (matching existing convention)

```
adapter-ui-libgdx/
├── build.gradle.kts
└── src/
    ├── main/kotlin/exploration/cli/Main.kt                          # entry point in exploration.cli/
    └── main/kotlin/exploration/adapter/ui/libgdx/
        ├── LibgdxUiAdapter.kt                                       # ApplicationAdapter — creates GameScreen, handles lifecycle
        ├── GameScreen.kt                                            # Screen — game loop, resize, input dispatch, delegates rendering to Renderer
        ├── Renderer.kt                                              # all draw logic via ShapeRenderer + batch.draw()
        ├── InputMapper.kt                                           # maps keyboard + mouse → InputEvent (same pattern as lanterna's)
        ├── UiUtils.kt                                               # text wrapping, HP bar building, exit labels, status formatting
        └── components/
            ├── DirectionSlots.kt                                    # 4 directional button definitions (shape + label)
            ├── HealthBar.kt                                         # animated progress bar state + draw logic
            ├── ItemSprite.kt                                        # colored shape + text for items in panels
            └── WorldMap.kt                                          # progressive discovery graph render logic
```

**settings.gradle.kts:** `include("adapter-ui-libgdx")` → project name `exploration-engine-ui-libgdx`

## Screen / Renderer Split

GameScreen owns game state, lifecycle, and input dispatch. Renderer owns all pixel drawing — no actor hierarchy needed for a text-heavy UI. Each frame: stage processes input → GameScreen dispatches event to engine → new ViewData → Renderer.draw(viewData, delta).

```
GameScreen (implements Screen)
├── viewport: FitViewport(1600, 900) — letterboxes on non-matching aspect ratios
├── stage: Stage(viewport + inputListener) — handles mouse/keyboard events only
├── renderer: Renderer (receives viewData + delta, draws everything)
├── overlayState: OverlayState enum
├── loop state: history, selectionState, storyCount tracking
└── methods: render(delta), resize(w, h), handleInput(), changeOverlay()

Renderer
├── drawBackground(viewport, bgColor)
├── drawBorderAndTitle(batch, viewport, width)
├── drawMessagePanel(batch, history, viewport, bounds)
├── drawStatusPanel(batch, viewData, viewport, bounds)
├── drawBottomBar(batch, viewData, viewport, bounds)
├── drawOverlay(batch, overlayState, viewData, viewport)
└── each component draws itself via batch API
```

## Viewport & Layout Strategy

- **FitViewport(1600, 900)** — black letterboxes on non-matching aspect ratios. All coordinates in logical pixels, no distortion. Asset-ready: when textures are added later they map to same logical coordinates.
- **Split layout** mirrors lanterna: left ~60% message panel, right ~40% status panel, bottom bar full-width with directional slots and action hints.
- Overlays rendered on a separate top-level Stage (modal behavior).

## World Map Design (Progressive Discovery)

Tracks three sets derived from GameState for what to render:

| Set | Condition | Render style |
|---|---|---|
| **Explored** | `player.currentArea` or was in exploredAreas | Filled circle, full opacity, cyan border |
| **Known unexplored** | Has a visible or previously-visible connection to an explored area but not yet visited | Dimmed circle (30% alpha), gray border |
| **Unknown** | No known connection exists | Not rendered at all |

Connections drawn as lines between circles. Currently-connected exits highlighted green; blocked connections dimmer. Positioned top-right corner (~120x90 logical units).

Areas are added to the map when a connection becomes visible (player is in an area with that exit, or has visited a neighboring area). Previously-visited but unexplored areas remain shown as dimmed circles even after moving away.

## Render Architecture

```
Stage root group
├── MessagePanel — scrollable text area: outputLine + commandText + trigger texts (blue for triggers)
├── StatusPanel  — HealthBar actor + progress stats + status bars + equipped items
├── BottomBar    — DirectionSlots row + action hint labels
└── OverlayStage — modal popups: InventoryViewer, StoryViewer, GameOver
```

**Overlay states:** `Playing`, `Inventory` (select take/drop/equip/unequip), `StoryViewer` (scrollable story messages), `GameOver` (end message + summary).

## Enhancements Over Lanterna TUI

1. **Clickable directional buttons** — Shape-rendered arrow shapes with hover highlight (green tint glow). w/a/s/d keyboard shortcuts still work simultaneously.

2. **Animated health bars** — Real ProgressBar-style rendering, color interpolates green→yellow→red as ratio drops, smooth transitions via libGDX Timer. Lanterna uses text characters; this is actual animated progress with styled borders.

3. **Procedural world map** — ShapeRenderer node graph showing explored + known-but-unexplored areas with progressive discovery. Player sees the "shape" of their world without knowing what's behind unvisited doors.

4. **Item sprites via colored shapes** — Small colored rectangles with item name labels:
   - Green fill = area item to take
   - Blue fill = carried in inventory
   - Purple fill = equipped
   - Red outline (no fill) = locked/unavailable
   Clicking an area item triggers Take flow; clicking a carried item shows Drop/Equip options.

5. **Smooth screen transitions** — Semi-transparent fade overlay when moving between areas, animated via Timer for opacity change.

6. **Mouse-driven inventory management** — Click items in status panel to interact with them instead of digit-selection prompts. Context-aware action buttons appear below the selected item group.

## Input Handling (InputMapper.kt)

Same object-based pattern as lanterna's LanternaKeyMapper, but for libGDX input:

```kotlin
// Keyboard: w/a/s/d → MoveDirection, l → Look, u → Activate, i → Inventory toggle
//           g/p/e/r → item actions (take/drop/equip/unequip), j → story viewer, h → help, q/esc → quit
// Mouse: click directional buttons → move to connected area
//        click items in panels → interaction flow
//        scroll wheel → navigate message history
//        close overlays with escape or click outside content
```

## Build & Run

```sh
./gradlew :adapter-ui-libgdx:installDist
./adapter-ui-libgdx/build/install/exploration-engine-ui-libgdx/bin/exploration-engine-ui-libgdx <scenario-file>
```

**Dependencies:** `com.badlogicgames.gdx:gdx:1.13.1` (core only). Desktop backend handled by ApplicationAdapter — no separate desktop module needed for now.

## Asset-Ready Design Decisions

All rendering is procedural today but structured to swap in assets later:

- **ItemSprite.kt** renders colored rectangles now; would draw sprite textures instead later via batch.draw(texRegion, ...)
- **DirectionSlots** arrow shapes are ShapeRenderer-drawn; could be texture regions (TextureRegion) with same positioning logic
- **WorldMap.kt** uses circles/lines via ShapeRenderer; area icons could replace circles with small sprites
- No asset manager or file loading today — keeps the module self-contained and build-fast, but component draw contracts are clear for future replacement

## Architecture for graphics rendering

- Make rendering hierarchical and composable.

### GfxArea
- Denotes square by leftX, bottomY, height, width
- Has derived property topY calculated from height + bottomY

### Basic element of rendering: GfxElement
- Has properties gfxArea, subElements, internalPadding
- Shapes and graphics render from bottom left corner toward right and up already in LibGDX, but text renders right and downward instead, so it needs to be started from top left corner
- GfxElement draws it's own background first, then calls sub elements to draw themselves and finally draws it's own foreground
- Single GfxElement draws everything it owns in relation to it's own coordinates
- InternalPadding is used in calculating internalUsableArea
- Parent element gives it's sub elements coordinates bottomLeftX, bottomLeftY, height, width in relation to it's own coordinates
- All draw actions should begin and end their own draw cycle. Do not leave drawing open for sub elements, they should begin and end their own draw cycles.