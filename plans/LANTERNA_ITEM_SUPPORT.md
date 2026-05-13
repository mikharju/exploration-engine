# Lanterna Item Key Support

## Problem

Item commands (take/drop/equip/unequip/inventory) exist in core but are not accessible from the Lanterna UI adapter. Only WASD movement, look (`l`), and activate (`u`) are wired up.

## Solution

### Key Bindings

| Action | Key | Active when |
|--------|-----|-------------|
| Grab item in area | `g` | `areaItems` not empty |
| Drop carried item | `p` | `carriedItems` or `equippedItems` not empty |
| Equip carried item | `e` | `carriedItems` not empty |
| Unequip equipped item | `r` | `equippedItems` not empty |
| Toggle inventory panel | `i` | always active |

### Ambiguous Selection

When multiple items match the action (e.g., 3 items on ground for `g`), show a numbered prompt in message log and enter a wait state. Next digit key (1-9) resolves by index, then clears the wait state.

```kotlin
private enum class SelectionTarget { TAKE, DROP, EQUIP, UNEQUIP }
private var selectionState: Pair<SelectionTarget, List<ItemView>>? = null
```

### Bottom Bar Layout

WASD cross stays centered and unchanged. Item keys added below as a new row:

```
         [w] North    [i] inv
[a] West   [s] South   [d] East
[g] grab   [p] drop  [e] equip  [r] uneq
```

Each hint is dimmed when no matching item exists (`[g] grab` dims if `areaItems.isEmpty()`).

### Files Modified

| File | Changes |
|------|---------|
| `port/ViewData.kt` | Add `areaItems: List<ItemView>` field |
| `core/engine/GameEngineImpl.kt` | Populate `areaItems` in `view()` from items at current area with location type AREA |
| `adapter/ui/lanterna/LanternaUiAdapter.kt` | Key bindings, digit-prompt state machine, bottom bar layout rewrite (WASD + item hints), help text update |

### Render Geometry

Bottom bar expands to 3 rows (`h-4`, `h-3`, `h-2`). Separator at `h-5`. Panels above unchanged.

`drawDirectionPad(g, view.exits, w - 2, h - 3)` → `drawBottomBar(g, view, w - 2, h - 5)`
