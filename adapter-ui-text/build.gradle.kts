plugins {
    kotlin("jvm") version "2.3.21"
    application
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":exploration-engine-core"))
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
