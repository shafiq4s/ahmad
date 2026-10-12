plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.ahmad.quran.svgnative"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.ahmad.quran.svgnative"
        minSdk = 23
        targetSdk = 36
        versionCode = 1
        versionName = "direct-svg-trial-only"
        testInstrumentationRunner = "com.ahmad.quran.svgnative.NativeSvgInstrumentation"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    sourceSets {
        getByName("debug") {
            java.srcDir("../app/src/main/java/com/ahmad/quran/data")
            // Reuse only the neutral polygon contracts/parser; no raster view/activity.
            java.srcDir("../app/src/main/java/com/ahmad/quran/mushaf")
            assets.srcDir("../app/src/main/assets")
            assets.srcDir("../tools/svg-native/local-fixtures/assets")
        }
    }
    testOptions {
        unitTests.isIncludeAndroidResources = true
        unitTests.all {
            it.systemProperty("nativeSvg.outputDir", rootProject.file("tools/svg-native").absolutePath)
            it.systemProperty("nativeSvg.referenceDir", rootProject.file("tools/quran-svg/local-fixtures").absolutePath)
        }
    }
}
androidComponents {
    beforeVariants(selector().withBuildType("release")) { it.enable = false }
}
kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }

tasks.register("checkNativeSvgFixtures") {
    doLast {
        check(rootProject.file("tools/svg-native/local-fixtures/assets/native-svg/pages.json").isFile) {
            "Run python3 tools/svg-native/prepare_fixtures.py first."
        }
    }
}
tasks.matching { it.name == "preDebugBuild" }.configureEach { dependsOn("checkNativeSvgFixtures") }

dependencies {
    // Authorized only in this isolated candidate; Apache-2.0, exact version.
    implementation("com.caverock:androidsvg-aar:1.4")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.16.1")
}
