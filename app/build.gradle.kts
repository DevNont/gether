import java.util.Properties

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
        versionCode = 7
        versionName = "0.2.0"
    }

    // Dedicated release keystore. Credentials come from env vars (CI) or
    // local.properties (dev machine) — never from the repo. LINE login uses a
    // browser OIDC flow, so the signing SHA-1 no longer affects sign-in.
    val localProps = Properties()
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { localProps.load(it) }

    fun releaseProp(name: String): String? = System.getenv(name) ?: localProps.getProperty(name)

    signingConfigs {
        create("release") {
            releaseProp("RELEASE_STORE_FILE")?.let { path ->
                storeFile = file(path)
                storePassword = releaseProp("RELEASE_STORE_PASSWORD")
                keyAlias = releaseProp("RELEASE_KEY_ALIAS")
                keyPassword = releaseProp("RELEASE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            // R8 on, with keep rules for the reflection-mapped Firestore DTOs.
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig =
                if (releaseProp("RELEASE_STORE_FILE") != null) {
                    signingConfigs.getByName("release")
                } else {
                    // No keystore configured (e.g. a fresh checkout): debug-signed build.
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
    implementation(libs.kotlinx.datetime)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.analytics)

    debugImplementation(libs.compose.ui.tooling)
}
