import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Pure Kotlin/JVM game logic: no Android or PICO imports.
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

kotlin {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_11) }
}

dependencies {
    api(libs.kotlinx.serialization.json)
    testImplementation(platform(libs.junit.jupiter.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
    // The beat-detection test reads the generated demo track and its kick list.
    dependsOn(":tools:demosynth:generate")
    testLogging { showStandardStreams = true }
}
