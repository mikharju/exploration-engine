package exploration.scenario

import exploration.core.engine.fireAreaTriggers
import exploration.model.*
import exploration.state.GameState

fun assembleGame(
    config: ScenarioConfig,
    areaEntries: List<AreaEntry>,
    deviceEntries: List<DeviceEntry>,
    triggerEntries: List<TriggerEntry> = emptyList(),
    itemEntries: List<ItemEntry> = emptyList()
): GameState {
    val statusBounds = config.statuses.mapValues { (_, v) -> StatusRange(v.min, v.max) }
    val statusInitial = config.statuses.entries.associate { (k, v) -> k to v.initial }

    val deviceMap = buildDevices(deviceEntries)

    val areaIds = areaEntries.map { it.id }.toSet()
    val areasMap = buildAreas(areaEntries, deviceMap, areaIds)

    val startId = AreaId(config.playerStart.startArea)
    if (startId !in areasMap.keys) {
        throw IllegalArgumentException("Start area '${config.playerStart.startArea}' not found in areas")
    }

    val world = World(areas = areasMap, startArea = startId)

    val player = Player(
        health = config.playerStart.health,
        maxHealth = config.playerStart.maxHealth,
        currentArea = startId,
        statuses = statusInitial
    )

    val items = buildItems(itemEntries)

    val triggers = buildTriggers(triggerEntries)

    validateTriggers(triggers, areaIds, deviceMap.keys.toSet(), items.map { it.id }.toSet())

    val state = GameState(
        world = world,
        player = player,
        statusBounds = statusBounds,
        triggers = triggers,
        items = items
    )

    return fireAreaTriggers(state, startId)
}

private fun buildDevices(deviceEntries: List<DeviceEntry>): Map<DeviceId, Device> = deviceEntries.associate { entry ->
    DeviceId(entry.id) to Device(
        id = DeviceId(entry.id),
        lookDescription = entry.lookDescription,
        activateDescription = entry.activateDescription,
        healthEffect = entry.healthEffect,
        statusEffects = entry.statusEffects
    )
}

private fun buildAreas(areaEntries: List<AreaEntry>, deviceMap: Map<DeviceId, Device>, areaIds: Set<String>): Map<AreaId, Area> = areaEntries.associateTo(mutableMapOf()) { entry ->
    if (entry.deviceId != null && !deviceMap.keys.contains(DeviceId(entry.deviceId))) {
        throw IllegalArgumentException("Area '${entry.id}' references unknown device: ${entry.deviceId}")
    }

    val connections = entry.connections.mapNotNull { connName ->
        require(connName in areaIds) { "Area '${entry.id}' connects to unknown area: $connName" }
        AreaId(connName)
    }.toSet()

    AreaId(entry.id) to Area(
        id = AreaId(entry.id),
        description = entry.description,
        connections = connections,
        device = entry.deviceId?.let { deviceMap[DeviceId(it)]!! }
    )
}

private fun buildItems(itemEntries: List<ItemEntry>): List<Item> = itemEntries.map { entry ->
    Item(
        id = ItemId(entry.id),
        description = entry.description,
        location = entry.toLocation(),
        locked = false
    )
}

private fun buildTriggers(triggerEntries: List<TriggerEntry>): List<Trigger> = triggerEntries.map { entry ->
    Trigger(
        id = entry.id,
        ownerType = OwnerType.valueOf(entry.ownerType),
        ownerId = entry.ownerId,
        conditions = entry.conditions.map { cond ->
            ActivationCondition(
                checkType = when (cond.checkType) {
                    "itemCarried" -> CheckType.ITEM_CARRIED
                    "itemEquipped" -> CheckType.ITEM_EQUIPPED
                    null -> CheckType.STATUS
                    else -> CheckType.STATUS
                },
                statusName = cond.statusName,
                op = when (cond.op) {
                    ">" -> ComparisonOp.GT
                    "<" -> ComparisonOp.LT
                    null -> ComparisonOp.GT
                    else -> error("Unknown operator: ${cond.op}")
                },
                threshold = cond.threshold ?: 0,
                itemId = cond.itemId?.let { ItemId(it) }
            )
        },
        effects = entry.effects.map { ef ->
            when (ef.type) {
                "displayText" -> Effect.DisplayText(checkNotNull(ef.text))
                "changeHealth" -> Effect.ChangeHealth(checkNotNull(ef.amount))
                "adjustStatus" -> Effect.AdjustStatus(checkNotNull(ef.statusName), checkNotNull(ef.amount))
                "setStatus" -> Effect.SetStatus(checkNotNull(ef.statusName), checkNotNull(ef.value))
                "setLocation" -> Effect.SetLocation(
                    ItemId(checkNotNull(ef.itemId)),
                    when (ef.locationType?.uppercase()) {
                        "AREA" -> Effect.TargetRef.InArea(AreaId(checkNotNull(ef.locationId)))
                        "DEVICE" -> Effect.TargetRef.OnDevice(DeviceId(checkNotNull(ef.locationId)))
                        "CARRIED" -> Effect.TargetRef.Carried
                        "EQUIPPED" -> Effect.TargetRef.Equipped
                        else -> error("Unknown location type: ${ef.locationType}")
                    }
                )
                "lockItem" -> Effect.LockItem(ItemId(checkNotNull(ef.itemId)))
                "unlockItem" -> Effect.UnlockItem(ItemId(checkNotNull(ef.itemId)))
                "storyMessage" -> Effect.StoryMessage(checkNotNull(ef.text))
                else -> error("Unknown effect type: ${ef.type}")
            }
        },
        singleUse = entry.singleUse,
        intervalTurns = entry.intervalTurns
    )
}

private fun validateTriggers(triggers: List<Trigger>, areaIds: Set<String>, deviceIds: Set<DeviceId>, itemIds: Set<ItemId>) {
    for (t in triggers) {
        when (t.ownerType) {
            OwnerType.AREA -> require(t.ownerId in areaIds) { "Trigger '${t.id}' references unknown area: ${t.ownerId}" }
            OwnerType.DEVICE -> require(DeviceId(t.ownerId) in deviceIds) { "Trigger '${t.id}' references unknown device: ${t.ownerId}" }
            OwnerType.STATUS -> {}
        }

        for (c in t.conditions) {
            c.itemId?.let { require(it in itemIds) { "Trigger '${t.id}' references unknown item: ${it.name}" } }
        }
        for (e in t.effects) {
            when (e) {
                is Effect.SetLocation -> require(e.itemId in itemIds) { "Trigger '${t.id}' references unknown item: ${e.itemId.name}" }
                is Effect.LockItem -> require(e.itemId in itemIds) { "Trigger '${t.id}' references unknown item: ${e.itemId.name}" }
                is Effect.UnlockItem -> require(e.itemId in itemIds) { "Trigger '${t.id}' references unknown item: ${e.itemId.name}" }
                else -> {}
            }
        }
    }
}
