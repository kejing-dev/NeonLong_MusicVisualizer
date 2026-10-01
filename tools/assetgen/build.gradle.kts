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
    mainClass.set("com.technomancer.neonlong.tools.assetgen.MainKt")
}

dependencies {
    implementation(project(":game-logic"))
    testImplementation(platform(libs.junit.jupiter.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
    systemProperty("java.awt.headless", "true")
}

// Writes the generated models and textures into the app's generated assets, and the
// triangle/texture report next to the other planning docs (outside the repository).
val generate by tasks.registering(JavaExec::class) {
    group = "neonlong"
    description = "Generates the dragon's models and textures."
    val assetsDir = rootProject.layout.projectDirectory.dir("app/build/generated/neonlong/assets")
    val reportFile = File(rootDir.parentFile, "docs/asset-report.md")
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set(application.mainClass)
    jvmArgs("-Djava.awt.headless=true")
    args(assetsDir.asFile.absolutePath, reportFile.absolutePath)
    inputs.files(sourceSets.main.get().runtimeClasspath)
    outputs.dir(assetsDir.dir("models"))
    outputs.dir(assetsDir.dir("textures"))
    outputs.file(reportFile)
}
