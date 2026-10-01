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
    maxHeapSize = "1g"
}

// Renders the demo track, its kick list and the interface sounds into the app's generated
// assets and the logic tests' resources. Deterministic, so it only reruns when the code changes.
val generate by tasks.registering(JavaExec::class) {
    group = "neonlong"
    description = "Renders the demo track and interface sounds."
    val assetsDir = rootProject.layout.projectDirectory.dir("app/build/generated/neonlong/assets")
    val testResDir = rootProject.layout.projectDirectory.dir("game-logic/build/generated/testResources")
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set(application.mainClass)
    maxHeapSize = "1g"
    args(assetsDir.asFile.absolutePath, testResDir.asFile.absolutePath)
    inputs.files(sourceSets.main.get().runtimeClasspath)
    outputs.file(assetsDir.file("audio/demo_104bpm.wav"))
    outputs.file(assetsDir.file("audio/GEN_ui_hover.wav"))
    outputs.file(assetsDir.file("audio/GEN_ui_select.wav"))
    outputs.file(testResDir.file("demo_104bpm.wav"))
    outputs.file(testResDir.file("demo_104bpm.kicks.json"))
}
