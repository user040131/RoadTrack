plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Unit tests fail to load classes from a non-ASCII build path on Windows; point ROADTRACK_BUILD_DIR at an ASCII folder there.
System.getenv("ROADTRACK_BUILD_DIR")?.let { layout.buildDirectory.set(file("$it/app")) }

android {
    namespace = "ai.roadtrack.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "ai.roadtrack.app"
        minSdk = 28 // Pleos requirement (signing key rotation)
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // shortcut: release is signed with the debug key so the contest APK installs; switch to a real keystore before App Market submission
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
}

dependencies {
    implementation("ai.pleos.playground:Vehicle:2.3.2")

    implementation(platform("androidx.compose:compose-bom:2025.06.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.1")

    testImplementation("junit:junit:4.13.2")
}
