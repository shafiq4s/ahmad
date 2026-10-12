import java.security.MessageDigest
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.OutputDirectory

abstract class StagePageAssets : Exec() {
    @get:OutputDirectory abstract val outputDirectory: DirectoryProperty
}

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.ahmad.quran"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.ahmad.quran"
        minSdk = 23
        targetSdk = 36
        versionCode = 5
        versionName = "0.5.0"
        testInstrumentationRunner = if (providers.gradleProperty("pageAssetDeviceTests").orNull == "true")
            "com.ahmad.quran.PageAssetInstrumentation" else if (providers.gradleProperty("readerUiDeviceTests").orNull == "true")
            "com.ahmad.quran.ReaderUiInstrumentation" else "com.ahmad.quran.MushafInstrumentation"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }
    buildFeatures { buildConfig = true }
    testOptions {
        unitTests.isIncludeAndroidResources = true
        unitTests.all {
            it.systemProperty("quran.sourceDir", rootProject.file("tools/quran/sources").absolutePath)
        }
    }
    bundle {
        language {
            enableSplit = false
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation("com.caverock:androidsvg-aar:1.4")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.9.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-savedstate:2.9.4")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.16.1")
}

val checkMushafAssets = tasks.register<Exec>("checkMushafAssets") {
    workingDir(rootProject.projectDir)
    commandLine("python3", "tools/mushaf-svg/verify_assets.py")
}
val checkSvgDependency = tasks.register("checkSvgDependency") {
    doLast {
        val artifact = configurations.getByName("debugRuntimeClasspath").resolvedConfiguration.resolvedArtifacts
            .single { it.moduleVersion.id.group == "com.caverock" && it.name == "androidsvg-aar" }
        check(artifact.moduleVersion.id.version == "1.4")
        val digest = MessageDigest.getInstance("SHA-256").digest(artifact.file.readBytes())
            .joinToString("") { "%02x".format(it) }
        check(digest == "02a5b08a2b35d2d58eb2eaca9d84ac00fb341da725fdbd653ea3ed130437e95a") {
            "AndroidSVG AAR differs from the approved artifact."
        }
    }
}
tasks.matching { it.name == "preBuild" }.configureEach { dependsOn(checkMushafAssets, checkSvgDependency) }

for (variant in listOf("debug", "release")) {
    val baseUrl = providers.gradleProperty("quranPageBaseUrl").orElse("")
    val output = layout.buildDirectory.dir("generated/pageAssets/$variant")
    tasks.register<StagePageAssets>("stage${variant.replaceFirstChar { it.uppercase() }}PageAssets") {
        dependsOn(checkMushafAssets)
        inputs.file(rootProject.file("config/mushaf-page-assets.json"))
        inputs.dir(rootProject.file("tools/mushaf-svg/local-assets"))
        inputs.file(rootProject.file("tools/page-assets/stage_assets.py"))
        inputs.property("baseUrl", baseUrl)
        outputDirectory.set(output)
        workingDir(rootProject.projectDir)
        commandLine(listOf("python3", "tools/page-assets/stage_assets.py", "--destination", output.get().asFile.absolutePath,
            "--base-url", baseUrl.get()) + if (variant == "debug") listOf("--allow-local-http") else emptyList())
    }
}
androidComponents.onVariants { variant ->
    val stage = tasks.named<StagePageAssets>("stage${variant.name.replaceFirstChar { it.uppercase() }}PageAssets")
    variant.sources.assets?.addGeneratedSourceDirectory(stage) { it.outputDirectory }
}
