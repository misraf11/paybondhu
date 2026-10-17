plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Signing: CI passes a private keystore through these variables (GitHub secrets).
// Without them the build falls back to keystore/paybondhu-public.jks so that every
// build still installs over the previous one.
val ksFile = System.getenv("PB_KEYSTORE_FILE")?.takeIf { it.isNotBlank() }
    ?: rootProject.file("keystore/paybondhu-public.jks").path
val ksPass = System.getenv("PB_KEYSTORE_PASSWORD")?.takeIf { it.isNotBlank() } ?: "paybondhu-public"
val ksAlias = System.getenv("PB_KEY_ALIAS")?.takeIf { it.isNotBlank() } ?: "paybondhu"
val ksKeyPass = System.getenv("PB_KEY_PASSWORD")?.takeIf { it.isNotBlank() } ?: "paybondhu-public"

android {
    namespace = "com.paybondhu.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.paybondhu.app"
        minSdk = 24
        targetSdk = 34
        versionCode = 20
        versionName = "2.0.0"
    }

    signingConfigs {
        create("release") {
            storeFile = file(ksFile)
            storePassword = ksPass
            keyAlias = ksAlias
            keyPassword = ksKeyPass
            enableV1Signing = true
            enableV2Signing = true
            enableV3Signing = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            signingConfig = signingConfigs.getByName("release")
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
    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.work:work-runtime-ktx:2.9.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
}
