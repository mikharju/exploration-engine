plugins {
    kotlin("jvm") version "2.3.21"
}

repositories {
    mavenCentral()
}

val defaultUi = findProperty("defaultUI") as String? ?: "text"

dependencies {
    implementation(project(":exploration-engine-core"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")

    when (defaultUi) {
        "key" -> {
            implementation(project(":exploration-engine-ui-key"))
            implementation("org.fusesource.jansi:jansi:2.4.1")
        }
        "lanterna" -> {
            implementation(project(":exploration-engine-ui-lanterna"))
            implementation("com.googlecode.lanterna:lanterna:3.1.2")
        }
        else -> {
            implementation(project(":exploration-engine-ui-text"))
        }
    }

    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.12.2")
}

kotlin {
    jvmToolchain(21)
}

tasks.test {
    useJUnitPlatform()
}
