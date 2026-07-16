plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

fun configurationValue(name: String, defaultValue: String = ""): String =
    providers.gradleProperty(name)
        .orElse(providers.environmentVariable(name))
        .orElse(defaultValue)
        .get()

fun String.asBuildConfigString(): String =
    "\"${replace("\\", "\\\\").replace("\"", "\\\"")}\""

android {
    namespace = "com.ryntra.mobile"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.ryntra.mobile"
        minSdk = 26
        targetSdk = 36
        versionCode = 30000
        versionName = "3.0.0-alpha01"
        buildConfigField(
            "String",
            "BACKEND_URL",
            configurationValue("RYNTRA_BACKEND_URL", "https://authrinthy.sawiq.org").asBuildConfigString(),
        )
        buildConfigField(
            "String",
            "FIREBASE_API_KEY",
            configurationValue("RYNTRA_FIREBASE_API_KEY", "AIzaSyCRwUOEsU9MtSFndV7M_UiBH2ogGdAjcCo").asBuildConfigString(),
        )
        buildConfigField(
            "String",
            "FIREBASE_APPLICATION_ID",
            configurationValue(
                "RYNTRA_FIREBASE_APPLICATION_ID",
                "1:788775992736:android:aebde0b3b557d613d5f8c5",
            ).asBuildConfigString(),
        )
        buildConfigField(
            "String",
            "FIREBASE_PROJECT_ID",
            configurationValue("RYNTRA_FIREBASE_PROJECT_ID", "ryntra-mobile").asBuildConfigString(),
        )
        buildConfigField(
            "String",
            "FIREBASE_SENDER_ID",
            configurationValue("RYNTRA_FIREBASE_SENDER_ID", "788775992736").asBuildConfigString(),
        )
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(project(":shared"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.browser)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.work.runtime)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.coil.svg)
    implementation(libs.haze)
    implementation(libs.icons.lucide)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
