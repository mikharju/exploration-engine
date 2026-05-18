# Item locked status (completed)

- Show in lanterna item status, which items are locked. 
- Change color and append (Locked) suffix.

# Story message (completed)

- Add new trigger effect called story message. It may be very long, several paragraphs.
- Add a temporary view to lanterna UI that displays this long message with a scroll bar
and uses page up and page down key functionality. 
- Add also journal action to lanterna UI, allows displaying previous story messages.
- Add minimal support for story messages to other UIs.
- Journal action should not go to core at all. No time passing or triggers firing on it.

# Doors (completed)

- Connections have states: locked, open, closed
- Connections can be hidden (apart from state)
- Triggers may change state or hide/show connections

# Consistent exits tied to directions (completed)

- Give area connections direction property
- Example: cave exits North Forest, East Ruins
- Allows scenario files to determine which way each exit points and makes it easier to design maps with sensible directions and return routes

# Win/lose trigger (completed)

- Trigger can end the game and set final message
- Scenario file should set welcome message and default final message
- Game no longer ends if all areas are explored and all devices activated

# Reviews needed

- LanternaUiAdapter
- CommandProcessor
- ScenarioLoader
- Qualified name use in files, for ex. ScenarioFile, ScenarioLoader
- setStatus health confusion

# Investigate sandbox sbx!!

# New time costs

- Add time cost to exits and devices
- Load from json
- Moving through exits and activating devices advances turn counter by time cost amount
- Status triggers will fire potentially multiple times on high cost actions
- Consider simplified turn loop that only fires status triggers during resolution of high cost actions, or propose better alternative
- Look, pickup, drop, equip, unequip no longer advance turn counter

# Use items

- Add action use item
- Add trigger CheckType ITEM_USED

# Presentation hints

- Info on how to render this object in UI
- May be ignored
- Core will only transmit the hints to UI
- Load from scenario
- Key value pairs
- Color, image filename, font, bold/italic