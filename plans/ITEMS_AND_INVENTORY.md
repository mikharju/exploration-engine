# Items & Inventory

## Core Design

Each item has one location at all times — single source of truth, no duplicates. Query by scanning locations map for matching type.

```
ItemLocationType enum: AREA | DEVICE | CARRIED | EQUIPPED

class Location
   type: ItemLocationType
   id: String?          # area name for AREA, device name for DEVICE, null for CARRIED/EQUIPPED
```

## Data Model

```
class ItemId
   name: String

class Item
   id: ItemId
   description: String
```

No triggers on items — all behavior through trigger activation conditions checking location state.

### GameState Additions

```
GameState extends:
   items: Map of ItemId → Item
   itemLocations: Map of ItemId → Location
   lockedItems: Set of ItemId          # runtime, only triggers modify
```

Separate locations map from immutable Item templates — only the smaller locations copy on state transitions.

### No Changes to Player or Area data classes.

Carried items = filter locations for CARRIED type. Room items = filter for AREA matching current area id. Device items = filter for DEVICE matching device id.

### Device — no item property

Device unchanged. Items sit at a device via location `(DEVICE, deviceId)`. Triggers move them on activation. No factory devices, no special device fields added.

## Commands & Events

New commands: TakeItem(itemName), DropItem(itemName), EquipItem(itemName), UnequipItem(itemName), Inventory

Text parser: `take <name>`, `drop <name>`, `equip <name>`, `unequip <name>`, `inv`/`inventory`. Named and numbered selection.

Corresponding InputEvent variants routed through GameEngineImpl to Commands.

## Trigger Activation Conditions

New condition types:

```
class ActivationCondition extends:
   checkType: CheckType     # STATUS (existing) | ITEM_CARRIED | ITEM_EQUIPPED
   optional statusName: String?       # for STATUS only
   optional itemId: String?           # for ITEM checks — which item to test
   optional op: ComparisonOp?         # for STATUS only
   optional threshold: Int?           # for STATUS only
```

Backward compatible — existing triggers use `checkType=STATUS`. New types only need itemId.

| Check Type | Fires When |
|---|---|
| ITEM_CARRIED | Location is exactly CARRIED (not EQUIPPED) |
| ITEM_EQUIPPED | Location is exactly EQUIPPED |

Any trigger owner type can use these conditions. A STATUS trigger with `ITEM_EQUIPPED(sword)` fires every turn while equipped. Device triggers can check item location when the device activates.

## Trigger Effects Added

```
class Effect extends:
   LockItem(itemId)       # add to lockedItems set
   UnlockItem(itemId)     # remove from lockedItems set
   SetLocation(item, type, id?)  # move item to new location — general purpose
```

One effect (`SetLocation`) replaces equip/unequip/spawn — covers all moves. A device activation trigger would have `SetLocation(ring, EQUIPPED)` or `SetLocation(potion, CARRIED)`. This eliminates the need for a special device property entirely.

## Command Processing

### TakeItem
1. Find item in current area: location == `(AREA, currentAreaId)`
2. Move to `(CARRIED, null)`
3. Turn++, status triggers, outcome check

### DropItem
1. Auto-unequip first if equipped (EQUIPPED → AREA directly)
2. Reject if locked
3. Move to `(AREA, currentAreaId)`
4. Turn++, status triggers, outcome check

### EquipItem
1. Find carried item: location == `(CARRIED, null)`
2. Reject if locked
3. Move to `(EQUIPPED, null)`
4. Turn++, status triggers, outcome check

### UnequipItem
1. Find equipped item: location == `(EQUIPPED, null)`
2. Reject if locked
3. Move to `(CARRIED, null)`
4. Turn++, status triggers, outcome check

### Activate (unchanged core)
Device effects apply normally as before. Device triggers fire — they may include `SetLocation` effects to move an item from the device to carried/equipped.

### Inventory / Look
- Inventory: list carried + equipped items with location label
- Look (extended): show room items via query + equipped summary
- Device look text can mention items present at `(DEVICE, deviceId)`

## Lock Mechanism

Locked items cannot be unequipped by player commands. Only trigger effects modify `lockedItems`. Unequip and drop reject if locked.

## Scenario JSON

New file `items.json`:
```json
[ { "id": "Ring of Warding", "description": "A silver ring that hums faintly." } ]
```

ScenarioConfig adds optional itemsFile reference. AreaEntry lists item IDs for initial AREA placement. Items placed at DEVICE set location directly in loader (or via a new field on AreaEntry or DeviceEntry specifying which items sit there — simplest: items.json includes an `initialLocation` and `initialLocationId` per item).

```
ItemEntry JSON extends:
   id: String
   description: String
   initialLocationType: ItemLocationType    # AREA | DEVICE default AREA
   optional initialLocationId: String?      # area name or device name
```

Trigger JSON gets new condition type strings (`itemCarried`, `itemEquipped`) and effect type (`setLocation`).

## ViewData Extension

```
class ViewData extends:
   carriedItems: List of ItemView
   equippedItems: List of ItemView

class ItemView
   name: String
   description: String
```

## Implementation Order

| Phase | Scope |
|---|---|
| 1. Models | ItemId, Item, Location types and enums |
| 2. GameState | Add items map, itemLocations map, lockedItems set |
| 3. Commands + Events | Take/Drop/Equip/Unequip command and InputEvent variants |
| 4. Trigger conditions | New checkType values, backward compat for STATUS |
| 5. Trigger effects | SetLocation effect type; LockItem/UnlockItem via SetLocation on lockedItems |
| 6. Command processor | Implement take/drop/equip/unequip with location transitions |
| 7. Trigger condition eval | Evaluate ITEM_CARRIED / ITEM_EQUIPPED against itemLocations |
| 8. Scenario loading | Load items.json, build initial locations map from item entries |
| 9. GameEngine + ViewData | Route new events, expose carried/equipped in view data |
| 10. Text parser | Parse take/drop/equip/unequip/inv (named and numbered) |
| 11. Look output | Show room items and equipped gear in look output |
| 12. Tests | Command processor tests for item actions, trigger integration |

## Important Details

- One instance per ItemId globally — if already somewhere accessible, `take` rejected
- Drop auto-unequips: EQUIPPED → AREA directly, skips intermediate triggers; status triggers fire via turn advance
- Items at DEVICE location move only via trigger effects (no device property needed)
- No factory devices — every item defined once in items.json with one initial location
