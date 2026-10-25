plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.20"
}

val hermesBaseUrl = providers.gradleProperty("HERMES_BASE_URL")
    .orElse("https://hermes-oregon.onrender.com")
    .get()
val keystorePath = System.getenv("ANDROID_KEYSTORE_PATH")
val keystorePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
val keyAliasValue = System.getenv("ANDROID_KEY_ALIAS")
val keyPasswordValue = System.getenv("ANDROID_KEY_PASSWORD")

android {
    namespace = "com.hermes.assistant"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.vyom.assistant"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "0.2.0"
        buildConfigField("String", "HERMES_BASE_URL", "\"$hermesBaseUrl\"")
    }

    signingConfigs {
        create("release") {
            if (!keystorePath.isNullOrBlank()) storeFile = file(keystorePath)
            if (!keystorePassword.isNullOrBlank()) storePassword = keystorePassword
            if (!keyAliasValue.isNullOrBlank()) keyAlias = keyAliasValue
            if (!keyPasswordValue.isNullOrBlank()) keyPassword = keyPasswordValue
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions { jvmTarget = "17" }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

tasks.register("validateReleaseSigning") {
    doLast {
        if (keystorePath.isNullOrBlank() || keystorePassword.isNullOrBlank() ||
            keyAliasValue.isNullOrBlank() || keyPasswordValue.isNullOrBlank() ||
            !file(keystorePath).exists()) {
            throw GradleException("Release signing is not configured. Add all four Android signing secrets.")
        }
    }
}
tasks.matching { it.name == "assembleRelease" }.configureEach {
    dependsOn("validateReleaseSigning")
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2025.10.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.activity:activity-compose:1.11.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.4")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
