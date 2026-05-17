package exploration.model

enum class Direction(val index: Int, val key: String, val displayName: String) {
    North(0, "w", "North"),
    West(1, "a", "West"),
    South(2, "s", "South"),
    East(3, "d", "East");

    companion object {
        fun fromIndex(index: Int): Direction? = values().getOrNull(index)
        fun parse(name: String): Direction? = entries.find { it.name.equals(name, ignoreCase = true) }
    }
}
