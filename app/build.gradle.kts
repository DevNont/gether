plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.google.services)
    alias(libs.plugins.crashlytics)
}

android {
    namespace = "com.triptogether"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.triptogether"
        minSdk = 26
        targetSdk = 35
        versionCode = 6
        versionName = "0.1.5"
    }

    signingConfigs {
        // CI signs with the exact keystore whose SHA-1 is registered in Firebase, restored to a
        // known path via the SIGNING_KEYSTORE env var. Relying on AGP's default debug-keystore
        // location failed on the runner (it generated a fresh random key), breaking sign-in.
        create("ci") {
            System.getenv("SIGNING_KEYSTORE")?.let { path ->
                storeFile = file(path)
                storePassword = "android"
                keyAlias = "androiddebugkey"
                keyPassword = "android"
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // Beta APK signed with the debug key (SHA-1 registered in Firebase) so Google/LINE
            // sign-in works for testers. On CI use the explicit restored keystore; locally fall
            // back to the machine debug keystore. Swap to a real release key before Play Store.
            signingConfig =
                if (System.getenv("SIGNING_KEYSTORE") != null) {
                    signingConfigs.getByName("ci")
                } else {
                    signingConfigs.getByName("debug")
                }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:data"))
    implementation(project(":core:ui"))
    implementation(project(":feature:auth"))
    implementation(project(":feature:trip"))
    implementation(project(":feature:plan"))
    implementation(project(":feature:expense"))
    implementation(project(":feature:settlement"))
    implementation(project(":feature:extras"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.bundles.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.analytics)

    debugImplementation(libs.compose.ui.tooling)
}
