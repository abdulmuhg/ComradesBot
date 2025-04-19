import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "2.1.20"
    application
}

group = "org.comradesbot"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://m2.dv8tion.net/releases") // JDA repository
}

dependencies {
    // JDA (Java Discord API)
    implementation("net.dv8tion:JDA:5.0.0-beta.18")

    // Kotlin coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    // Logging
    implementation("ch.qos.logback:logback-classic:1.4.11")

    // Testing
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
}


// Creates a fat JAR with all dependencies
tasks.jar {
    manifest {
        attributes["Main-Class"] = "MainKt"
    }

    // Include all dependencies in the JAR
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })

    // Handle duplicate files
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

application {
    mainClass.set("MainKt")
}

kotlin {
    jvmToolchain(17)
}