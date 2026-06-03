plugins {
    kotlin("jvm") version "2.3.21"
    application
}

repositories {
    mavenCentral()
    maven("https://repo.libgdx.com/releases")
}

dependencies {
    implementation(project(":exploration-engine-core"))
    implementation(project(":exploration-engine"))
    implementation("com.badlogicgames.gdx:gdx:1.13.1")
    implementation("com.badlogicgames.gdx:gdx-backend-lwjgl3:1.13.1")
    implementation("com.badlogicgames.gdx:gdx-platform:1.13.1:natives-desktop")
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.12.2")
}

kotlin {
    jvmToolchain(21)
}

application {
    mainClass.set("exploration.cli.MainKt")
}

tasks.test {
    useJUnitPlatform()
}
