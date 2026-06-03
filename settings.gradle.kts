rootProject.name = "exploration-engine"

include("core")
project(":core").name = "exploration-engine-core"

include("adapter-ui-text")
project(":adapter-ui-text").name = "exploration-engine-ui-text"

include("adapter-ui-key")
project(":adapter-ui-key").name = "exploration-engine-ui-key"

include("adapter-ui-lanterna")
project(":adapter-ui-lanterna").name = "exploration-engine-ui-lanterna"

include("app")
project(":app").name = "exploration-engine"
