import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    android {
        namespace = "com.ryntra.shared"
        compileSdk = 36
        minSdk = 26
        withHostTestBuilder {}
    }

    val frameworkName = "RyntraShared"
    val xcframework = XCFramework(frameworkName)
    listOf(iosArm64(), iosSimulatorArm64(), iosX64()).forEach { target ->
        target.binaries.framework {
            baseName = frameworkName
            binaryOption("bundleId", "com.ryntra.mobile.shared")
            isStatic = true
            xcframework.add(this)
        }
    }

    // macOS links dynamically. A static framework pulls the whole
    // platform.posix and platform.darwin cinterop caches into the app, and
    // those reference symbols (fdclosedir, thread_suspend2, vm_reallocate)
    // that the macOS SDK no longer exposes for linking. The XCFramework stays
    // iOS-only because it feeds the IPA builds.
    macosArm64().binaries.framework {
        baseName = frameworkName
        binaryOption("bundleId", "com.ryntra.mobile.shared")
        isStatic = false
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.json)
            implementation(libs.jetbrains.markdown)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.ktor.client.mock)
        }
        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
        }
        appleMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
    }
}
