package exploration.core.model

enum class Direction(val key: String) {
    North("w"),
    West("a"),
    South("s"),
    East("d");

    companion object {
        private val byName = entries.associateBy { it.name.lowercase() }

        fun parse(name: String): Direction? = byName[name.lowercase()]
    }
}
