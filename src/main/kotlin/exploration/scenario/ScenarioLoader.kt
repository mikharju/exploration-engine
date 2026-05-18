package exploration.scenario

import exploration.core.engine.fireAreaTriggers
import exploration.core.model.*
import exploration.core.state.GameState

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

    val initialExitStates = buildInitialExitStates(areaEntries, areaIds)

    val triggers = buildTriggers(triggerEntries)

    validateTriggers(triggers, areaIds, deviceMap.keys.toSet(), items.map { it.id }.toSet())

    val state = GameState(
        world = world,
        player = player,
        statusBounds = statusBounds,
        triggers = triggers,
        items = items,
        exitStates = initialExitStates,
        storyMessages = if (config.welcomeMessage.isNotBlank()) listOf(config.welcomeMessage) else emptyList()
    )

    return fireAreaTriggers(state, startId)
}

private fun buildInitialExitStates(areaEntries: List<AreaEntry>, areaIds: Set<String>): Map<ExitId, ExitStateData> {
    val result = mutableMapOf<ExitId, ExitStateData>()
    for (entry in areaEntries) {
        val from = AreaId(entry.id)
        if ((entry.exits ?: emptyList()).isNotEmpty()) {
            for (exitEntry in entry.exits!!) {
                require(exitEntry.areaId in areaIds) { "Area '${entry.id}' references unknown exit target: ${exitEntry.areaId}" }
                val to = AreaId(exitEntry.areaId)
                val id = ExitId(from, to)
                val state = when (exitEntry.initialState?.lowercase()) {
                    "blocked" -> ExitState.BLOCKED
                    else -> ExitState.OPEN  // default and any unknown value
                }
                result[id] = ExitStateData(state, exitEntry.hidden)
            }
        }
    }
    return result
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

    val exits: Set<Exit> = if ((entry.exits ?: emptyList()).isNotEmpty()) {
        (entry.exits ?: emptyList()).mapNotNull { exitEntry ->
            require(exitEntry.areaId in areaIds) { "Area '${entry.id}' references unknown exit target area: ${exitEntry.areaId}" }
            val dir = Direction.parse(exitEntry.direction)
                ?: error("Area '${entry.id}' has invalid direction '${exitEntry.direction}'. Must be one of North, West, South, East")
            Exit(AreaId(exitEntry.areaId), dir)
        }.toSet()
    } else {
        (entry.connections ?: emptyList()).mapNotNull { connName ->
            require(connName in areaIds) { "Area '${entry.id}' connects to unknown area: $connName" }
            AreaId(connName)
        }.sortedBy { it.name }.mapIndexed { idx, areaId -> Exit(areaId, Direction.fromIndex(idx) ?: Direction.North) }.toSet()
    }

    AreaId(entry.id) to Area(
        id = AreaId(entry.id),
        description = entry.description,
        exits = exits,
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
                    "<=" -> ComparisonOp.LTE
                    ">=" -> ComparisonOp.GTE
                    "==" -> ComparisonOp.EQ
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
                "setExitBlocked" -> Effect.SetExitBlocked(AreaId(checkNotNull(ef.fromId)), AreaId(checkNotNull(ef.toId)), checkNotNull(ef.blocked ?: false))
                "hideExit" -> Effect.HideExit(AreaId(checkNotNull(ef.fromId)), AreaId(checkNotNull(ef.toId)))
                "showExit" -> Effect.ShowExit(AreaId(checkNotNull(ef.fromId)), AreaId(checkNotNull(ef.toId)))
                "endGame" -> Effect.EndGame(checkNotNull(ef.text))
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
                is Effect.SetExitBlocked -> {
                    require(e.from.name in areaIds) { "Trigger '${t.id}' exit from area '${e.from.name}' not found" }
                    require(e.to.name in areaIds) { "Trigger '${t.id}' exit to area '${e.to.name}' not found" }
                }
                is Effect.HideExit -> {
                    require(e.from.name in areaIds) { "Trigger '${t.id}' exit from area '${e.from.name}' not found" }
                    require(e.to.name in areaIds) { "Trigger '${t.id}' exit to area '${e.to.name}' not found" }
                }
                is Effect.ShowExit -> {
                    require(e.from.name in areaIds) { "Trigger '${t.id}' exit from area '${e.from.name}' not found" }
                    require(e.to.name in areaIds) { "Trigger '${t.id}' exit to area '${e.to.name}' not found" }
                }
                else -> {}
            }
        }
    }
}
