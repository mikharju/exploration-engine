plugins {
    kotlin("jvm") version "2.3.21"
    kotlin("plugin.serialization") version "2.3.21"
    application
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")
    implementation("org.fusesource.jansi:jansi:2.4.1")
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.12.2")
}

kotlin {
    jvmToolchain(25)
}

application {
    mainClass.set("exploration.cli.MainKt")
}

tasks.test {
    useJUnitPlatform()
}
