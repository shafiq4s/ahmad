import java.security.MessageDigest

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
        testInstrumentationRunner = "com.ahmad.quran.MushafInstrumentation"
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
    sourceSets {
        getByName("main").assets.srcDir("../tools/mushaf-svg/local-assets")
    }
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
