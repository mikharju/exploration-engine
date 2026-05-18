package exploration.core.model

enum class Direction(val key: String, val displayName: String) {
    North("w", "North"),
    West("a", "West"),
    South("s", "South"),
    East("d", "East");

    companion object {
        private val byKey = mapOf('w' to North, 'a' to West, 's' to South, 'd' to East)
        private val byName = mapOf("north" to North, "west" to West, "south" to South, "east" to East)

        fun fromKey(key: Char): Direction? = byKey[key]
        fun parse(name: String): Direction? = byName[name.lowercase()]
        internal fun fromIndex(index: Int): Direction? = values().getOrNull(index)
    }
}
