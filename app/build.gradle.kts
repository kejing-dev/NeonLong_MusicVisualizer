import java.util.Collections
import java.util.zip.ZipFile
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.internal.os.OperatingSystem

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.technomancer.neonlong"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.technomancer.neonlong"
        minSdk = 35
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        ndk { abiFilters.add("arm64-v8a") }

        // Logged by BootInfo as part of the [boot] line.
        buildConfigField("String", "SPATIAL_BOM_VERSION", "\"${libs.versions.spatialBom.get()}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    androidResources {
        noCompress += listOf(".bundle", ".glb", ".wav")
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }

    // Models, textures and audio made by :tools:assetgen and :tools:demosynth at build time.
    sourceSets["main"].assets.srcDir(layout.buildDirectory.dir("generated/neonlong/assets"))
}

tasks.named("preBuild") {
    dependsOn(":tools:assetgen:generate", ":tools:demosynth:generate")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(platform(libs.spatial.bom))
    implementation(libs.core)
    implementation(libs.platform)
    implementation(libs.foundation)
    implementation(libs.design)
    implementation(libs.sense)
    implementation(libs.tracking)
    implementation(libs.androidx.ui.tooling)
    implementation(libs.androidx.annotation)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.datastore.preferences)
    implementation(project(":editor-asset"))
    implementation(project(":game-logic"))
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    debugImplementation(libs.androidx.ui.tooling.preview)
}

configurations.all {
    resolutionStrategy {
        exclude("androidx.compose.ui", "ui")
        exclude("androidx.compose.ui", "ui-graphics")
        exclude("androidx.compose.ui", "ui-text")
        exclude("androidx.compose.foundation", "foundation")
    }
}

// ---------------------------------------------------------------------------------------------
// Native 3D only: fails the build if any web technology reaches the app.
// ---------------------------------------------------------------------------------------------
val verifyNoWeb by tasks.registering {
    group = "verification"
    description = "Fails if web files, web APIs or web/JS-engine dependencies are present."
    val appSrc = layout.projectDirectory.dir("src").asFile
    val logicSrc = rootProject.layout.projectDirectory.dir("game-logic/src").asFile
    inputs.files(fileTree(appSrc), fileTree(logicSrc))
    doLast {
        val webExtensions = setOf("html", "htm", "js", "mjs", "css", "wasm")
        val sourceTokens = listOf("android.webkit", "WebView", "WebSpatial", "evaluateJavascript")
        val artifactPattern = Regex("(?i)webkit|webspatial|webview|j2v8|quickjs|rhino|graaljs")
        val problems = mutableListOf<String>()

        appSrc.walkTopDown().filter { it.isFile && it.extension.lowercase() in webExtensions }
            .forEach { problems += "web file: ${it.relativeTo(rootDir)}" }

        listOf(appSrc, logicSrc).filter { it.exists() }.forEach { dir ->
            dir.walkTopDown().filter { it.isFile && (it.extension == "kt" || it.extension == "java") }
                .forEach { file ->
                    val text = file.readText()
                    sourceTokens.filter { text.contains(it) }
                        .forEach { problems += "web API '$it' in ${file.relativeTo(rootDir)}" }
                }
        }

        listOf("debugRuntimeClasspath", "releaseRuntimeClasspath").forEach { name ->
            configurations.getByName(name).incoming.resolutionResult.allComponents.mapNotNull { it.moduleVersion }
                .filter { id -> artifactPattern.containsMatchIn("${id.group}:${id.name}") }
                .forEach { id -> problems += "web dependency on $name: ${id.group}:${id.name}:${id.version}" }
        }

        if (problems.isNotEmpty()) {
            throw GradleException("verifyNoWeb failed:\n" + problems.joinToString("\n") { "  - $it" })
        }
        logger.lifecycle("verifyNoWeb: OK")
    }
}

tasks.named("preBuild") { dependsOn(verifyNoWeb) }

// ---------------------------------------------------------------------------------------------
// Offline SDK API index. Writes `javap -public` for every resolved com.pico.spatial*
// artifact to ../docs/sdk-api/<artifact>-<version>.txt, outside the repository.
// ---------------------------------------------------------------------------------------------
val dumpSdkApi by tasks.registering {
    group = "documentation"
    description = "Dumps the public API of every resolved com.pico.spatial* artifact to ../docs/sdk-api."
    val outDir = File(rootDir.parentFile, "docs/sdk-api")
    doLast {
        val javap = File(System.getProperty("java.home"), "bin/javap" + if (OperatingSystem.current().isWindows) ".exe" else "")
        require(javap.exists()) { "javap not found at $javap; run Gradle with a full JDK (e.g. Android Studio's jbr)." }
        val classesJars = configurations.getByName("debugRuntimeClasspath").incoming.artifactView {
            componentFilter { c -> c is ModuleComponentIdentifier && c.group.startsWith("com.pico.spatial") }
            attributes { attribute(Attribute.of("artifactType", String::class.java), "android-classes-jar") }
        }.artifacts
        val jarArtifacts = configurations.getByName("debugRuntimeClasspath").incoming.artifactView {
            componentFilter { c -> c is ModuleComponentIdentifier && c.group.startsWith("com.pico.spatial") }
            attributes { attribute(Attribute.of("artifactType", String::class.java), "jar") }
            lenient(true)
        }.artifacts
        outDir.mkdirs()
        val written = sortedSetOf<String>()
        (classesJars + jarArtifacts).forEach { artifact ->
            val id = artifact.id.componentIdentifier as? ModuleComponentIdentifier
                ?: return@forEach
            if (!id.group.startsWith("com.pico.spatial")) return@forEach
            val outName = "${id.group}.${id.module}-${id.version}.txt"
            if (!written.add(outName)) return@forEach
            val anonymous = Regex("""\$\d""")
            val entryNames: List<String> = ZipFile(artifact.file).use { zip ->
                Collections.list(zip.entries()).map { entry -> entry.name }
            }
            val classNames: List<String> = entryNames
                .filter { name -> name.endsWith(".class") && !name.startsWith("META-INF") && !anonymous.containsMatchIn(name) }
                .map { name -> name.removeSuffix(".class").replace('/', '.') }
                .sorted()
            val out = File(outDir, outName)
            out.writeText("# ${id.group}:${id.module}:${id.version} (${classNames.size} classes)\n")
            classNames.chunked(150).forEach { chunk ->
                val process = ProcessBuilder(listOf(javap.absolutePath, "-public", "-cp", artifact.file.absolutePath) + chunk)
                    .redirectErrorStream(true)
                    .start()
                val text = process.inputStream.bufferedReader().readText()
                process.waitFor()
                out.appendText(text.lines().filterNot { line -> line.startsWith("Compiled from") }.joinToString("\n"))
                out.appendText("\n")
            }
            logger.lifecycle("dumpSdkApi: ${out.relativeTo(rootDir)} (${classNames.size} classes)")
        }
        if (written.isEmpty()) throw GradleException("dumpSdkApi: no com.pico.spatial* artifacts resolved")
    }
}
