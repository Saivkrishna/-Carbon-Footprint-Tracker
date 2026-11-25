plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

// Redirect build artifacts outside OneDrive to avoid Windows file-locking issues
layout.buildDirectory.set(file("${System.getProperty("user.home")}/.cache/CarbonPrint/app-build"))

android {
    namespace = "com.carbonprint.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.carbonprint.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)

    // Room Database
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // WorkManager
    implementation(libs.androidx.work.runtime.ktx)

    // Google Play Services: Activity Recognition & Fused Location
    implementation(libs.google.play.services.location)
    implementation(libs.kotlinx.coroutines.play.services)

    // MPAndroidChart
    implementation(libs.mp.android.chart)

    // OpenStreetMap osmdroid
    implementation(libs.osmdroid.android)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
}

// Copy finalized APK and test results into workspace directory
tasks.register<Copy>("copyApkToWorkspace") {
    from(layout.buildDirectory.dir("outputs/apk/debug"))
    into(file("${rootDir}/app/build/outputs/apk/debug"))
    include("*.apk")
}

tasks.register<Copy>("copyTestResultsToWorkspace") {
    from(layout.buildDirectory.dir("test-results/testDebugUnitTest"))
    into(file("${rootDir}/app/build/test-results/testDebugUnitTest"))
}

afterEvaluate {
    tasks.findByName("assembleDebug")?.finalizedBy("copyApkToWorkspace")
    tasks.findByName("testDebugUnitTest")?.finalizedBy("copyTestResultsToWorkspace")
}
