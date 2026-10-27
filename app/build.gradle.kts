plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.20"
}

val gitCommitShort = System.getenv("GITHUB_SHA")?.take(7) ?: "local"
val androidVersionCode = System.getenv("ANDROID_VERSION_CODE")?.toIntOrNull() ?: 3
val androidVersionName = System.getenv("ANDROID_VERSION_NAME") ?: "0.3.0"
val hermesBaseUrl = (System.getenv("HERMES_BASE_URL")?.takeIf { it.isNotBlank() } ?: "https://hermes-oregon.onrender.com").trimEnd('/')

android {
    namespace = "com.hermes.assistant"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.vyom.assistant"
        minSdk = 26
        targetSdk = 35
        versionCode = androidVersionCode
        versionName = androidVersionName
        buildConfigField("String", "HERMES_BASE_URL", "\"$hermesBaseUrl\"")
    }

    signingConfigs {
        create("release") {
            storeFile = file(System.getenv("ANDROID_KEYSTORE_PATH") ?: "release-keystore.jks")
            storePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD") ?: ""
            keyAlias = System.getenv("ANDROID_KEY_ALIAS") ?: ""
            keyPassword = System.getenv("ANDROID_KEY_PASSWORD") ?: ""
        }
    }

    buildTypes {
        debug {
            buildConfigField("String", "GIT_COMMIT_SHORT", "\"$gitCommitShort\"")
        }
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
            buildConfigField("String", "GIT_COMMIT_SHORT", "\"$gitCommitShort\"")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2025.10.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation("androidx.activity:activity-compose:1.11.0")
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.security:security-crypto:1.1.0")
    testImplementation("junit:junit:4.13.2")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.4")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
