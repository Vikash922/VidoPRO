import com.google.gms.googleservices.GoogleServicesPlugin.MissingGoogleServicesStrategy

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
  alias(libs.plugins.google.services)
}

val updateJsonFile = rootProject.file("update.json")
var currentVersionCode = 1
var currentVersionName = "1.0"

if (updateJsonFile.exists()) {
    val content = updateJsonFile.readText()
    val vcMatch = "\"versionCode\"\\s*:\\s*(\\d+)".toRegex().find(content)
    if (vcMatch != null) {
        currentVersionCode = vcMatch.groupValues[1].toInt()
    }
    val vnMatch = "\"versionName\"\\s*:\\s*\"([^\"]+)\"".toRegex().find(content)
    if (vnMatch != null) {
        currentVersionName = vnMatch.groupValues[1]
    }
}

android {
  namespace = "com.example"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.aistudio.videoeditor.vked"
    minSdk = 24
    targetSdk = 36
    versionCode = currentVersionCode
    versionName = currentVersionName

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  val releaseKeystorePath = System.getenv("KEYSTORE_FILE")
    ?: (project.findProperty("KEYSTORE_FILE") as? String)
    ?: (project.findProperty("keystoreFile") as? String)
  val releaseKeystorePassword = System.getenv("KEYSTORE_PASSWORD")
    ?: (project.findProperty("KEYSTORE_PASSWORD") as? String)
    ?: (project.findProperty("keystorePassword") as? String)
  val releaseKeyAlias = System.getenv("KEY_ALIAS")
    ?: (project.findProperty("KEY_ALIAS") as? String)
    ?: (project.findProperty("keyAlias") as? String)
  val releaseKeyPassword = System.getenv("KEY_PASSWORD")
    ?: (project.findProperty("KEY_PASSWORD") as? String)
    ?: (project.findProperty("keyPassword") as? String)

  val releaseKeystoreFile = releaseKeystorePath?.let { file(it) } ?: file("release.keystore")
  val isReleaseSigningConfigured = releaseKeystoreFile.exists() &&
      !releaseKeystorePassword.isNullOrBlank() &&
      !releaseKeyAlias.isNullOrBlank() &&
      !releaseKeyPassword.isNullOrBlank()

  signingConfigs {
    if (isReleaseSigningConfigured) {
      create("release") {
        storeFile = releaseKeystoreFile
        storePassword = releaseKeystorePassword
        keyAlias = releaseKeyAlias
        keyPassword = releaseKeyPassword
      }
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      if (isReleaseSigningConfigured) {
        signingConfig = signingConfigs.getByName("release")
      } else {
        // Fallback to debug signing for local/CI builds when production signing secrets are not supplied
        signingConfig = signingConfigs.getByName("debug")
      }
    }
    debug {
      signingConfig = signingConfigs.getByName("debug")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
  dependenciesInfo {
    includeInApk = false
    includeInBundle = true
  }
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
  ignoreList.add("FIREBASE_APPCHECK_DEBUG_TOKEN")
}

googleServices { missingGoogleServicesStrategy = MissingGoogleServicesStrategy.WARN }

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(project(":core:common"))
  implementation(project(":core:model"))
  implementation(project(":core:database"))
  implementation(project(":core:data"))
  implementation(project(":core:ui"))
  implementation(project(":core:media"))
  implementation(project(":feature:home"))
  implementation(project(":feature:editor"))
  implementation(project(":feature:timeline"))
  implementation(project(":feature:mediaPicker"))
  implementation(project(":feature:export"))
  implementation(project(":feature:settings"))

  implementation(platform(libs.androidx.compose.bom))
  implementation(platform(libs.firebase.bom))
  // implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  // implementation(libs.androidx.camera.camera2)
  // implementation(libs.androidx.camera.core)
  // implementation(libs.androidx.camera.lifecycle)
  // implementation(libs.androidx.camera.view)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  // implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation(libs.coil.compose)
  implementation(libs.coil.video)
  implementation(libs.media3.exoplayer)
  implementation(libs.media3.transformer)
  implementation(libs.media3.ui)
  implementation(libs.androidx.work.runtime.ktx)
  implementation(libs.converter.moshi)
  implementation(libs.firebase.ai)
  // Uncomment to use Firestore:
  // implementation(libs.firebase.firestore)

  // Uncomment ALL FOUR of the following dependencies together to use Firebase Auth and Google
  // Sign-In via Credential Manager:
  // implementation(libs.firebase.auth)
  // implementation(libs.androidx.credentials)
  // implementation(libs.androidx.credentials.play.services)
  // implementation(libs.googleid)
  implementation(libs.firebase.appcheck.recaptcha)
  implementation(libs.firebase.appcheck.debug)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  // implementation(libs.play.services.location)
  implementation(libs.retrofit)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
  "ksp"(libs.moshi.kotlin.codegen)
}
