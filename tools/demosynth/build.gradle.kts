import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Dev-only build-time generator. Never packaged into the APK.
plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

kotlin {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_11) }
}

application {
    mainClass.set("com.technomancer.neonlong.tools.demosynth.MainKt")
}

dependencies {
    implementation(project(":game-logic"))
    testImplementation(platform(libs.junit.jupiter.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
}
