plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.example.core.media"
    compileSdk { version = release(36) { minorApiLevel = 1 } }

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:model"))
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.transformer)
    implementation(libs.media3.effect)
    implementation(libs.media3.ui)
    implementation(libs.media3.common)
    implementation(libs.coil.compose)
    implementation(libs.coil.video)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.core)
}
