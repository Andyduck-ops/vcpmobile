import java.util.Properties

plugins {
 alias(libs.plugins.android.application)
 alias(libs.plugins.kotlin.android)
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
 if (keystorePropertiesFile.exists()) {
  load(keystorePropertiesFile.inputStream())
 }
}

android {
 namespace = "com.naigebao.app"
 compileSdk = 34

 defaultConfig {
  applicationId = "com.naigebao.app"
  minSdk = 26
  targetSdk = 34
  versionCode = 1
  versionName = "0.1.0"

  testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
 }

 composeOptions {
  kotlinCompilerExtensionVersion = libs.versions.compose.compiler.get()
 }

 signingConfigs {
  create("release") {
   if (keystorePropertiesFile.exists()) {
    storeFile = file(keystoreProperties["storeFile"] as String)
    storePassword = keystoreProperties["storePassword"] as String
    keyAlias = keystoreProperties["keyAlias"] as String
    keyPassword = keystoreProperties["keyPassword"] as String
   }
  }
 }

 buildTypes {
  release {
   isMinifyEnabled = true
   isShrinkResources = true
   proguardFiles(
    getDefaultProguardFile("proguard-android-optimize.txt"),
    "proguard-rules.pro"
   )
   signingConfig = if (keystorePropertiesFile.exists()) {
    signingConfigs.getByName("release")
   } else {
    signingConfigs.getByName("debug")
   }
  }
 }
}

dependencies {
 implementation(project(":core:common"))
 implementation(project(":core:model"))
 implementation(project(":core:network"))
 implementation(project(":core:storage"))
 implementation(project(":core:ui"))
 implementation(project(":features:auth"))
 implementation(project(":features:chat"))
 implementation(project(":features:sessions"))
 implementation(project(":features:push"))

 implementation(libs.androidx.core.ktx)
 implementation(libs.androidx.lifecycle.runtime.ktx)
 implementation(libs.androidx.lifecycle.runtime.compose)
 implementation(libs.androidx.lifecycle.viewmodel.compose)
 implementation(libs.androidx.activity.compose)
 implementation(platform(libs.androidx.compose.bom))
 implementation(libs.androidx.compose.ui)
 implementation(libs.androidx.compose.ui.graphics)
 implementation(libs.androidx.compose.ui.tooling.preview)
 implementation(libs.androidx.compose.material3)
 implementation(libs.androidx.navigation.compose)
 implementation(libs.androidx.work.runtime.ktx)
 implementation(libs.firebase.messaging.ktx)
 implementation(libs.material)
 implementation(libs.kotlinx.coroutines.android)
 implementation(libs.okhttp)
 implementation(libs.kotlinx.serialization.json)
 implementation(libs.androidx.room.runtime)

 testImplementation(libs.junit)
 androidTestImplementation(libs.androidx.junit)
 androidTestImplementation(libs.androidx.espresso.core)
 androidTestImplementation(platform(libs.androidx.compose.bom))
 debugImplementation(libs.androidx.compose.ui.tooling)
}