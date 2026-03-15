plugins {
 alias(libs.plugins.android.library)
 alias(libs.plugins.kotlin.android)
}

android {
 namespace = "com.naigebao.features.sessions"
 compileSdk = 34

 defaultConfig {
  minSdk = 26
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
}

dependencies {
 implementation(project(":core:model"))
 implementation(project(":core:storage"))
 implementation(project(":core:ui"))
 implementation(libs.androidx.lifecycle.viewmodel.compose)
 implementation(platform(libs.androidx.compose.bom))
 implementation(libs.androidx.compose.ui)
 implementation(libs.androidx.compose.ui.graphics)
 implementation(libs.androidx.compose.ui.tooling.preview)
 implementation(libs.androidx.compose.material3)
 implementation(libs.androidx.compose.material)
 implementation(libs.coil.compose)
}