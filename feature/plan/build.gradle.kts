import java.util.Properties

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

// Google Maps Places API key. Read from the MAPS_API_KEY env var (CI) or local.properties (dev);
// empty when unset, in which case the place field falls back to a plain Google Maps search intent.
val mapsApiKey: String =
    System.getenv("MAPS_API_KEY")
        ?: rootProject.file("local.properties").takeIf { it.exists() }?.let {
            Properties().apply { it.inputStream().use(::load) }.getProperty("MAPS_API_KEY")
        }
        ?: ""

android {
    namespace = "com.triptogether.feature.plan"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
        buildConfigField("String", "MAPS_API_KEY", "\"$mapsApiKey\"")
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
    implementation(project(":core:ui"))

    implementation(platform(libs.compose.bom))
    implementation(libs.bundles.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.places)

    debugImplementation(libs.compose.ui.tooling)
}
