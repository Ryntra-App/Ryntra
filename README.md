# Ryntra Native

Ryntra is an unofficial native mobile workspace for Modrinth creators. Business logic is shared with Kotlin Multiplatform, while Android and iOS keep first-class native interfaces.

## Architecture

```text
androidApp/  Jetpack Compose + Material 3, Android Keystore
iosApp/      SwiftUI, Keychain, Xcode project
shared/      Kotlin models, Ktor, kotlinx.serialization, repository and state
```

The shared module has no UI dependency. `AppController` owns session-scoped loading and exposes the same state machine to Compose and SwiftUI. Platform code owns navigation, presentation, accessibility and secure token persistence.

## Implemented

- Modrinth OAuth through the Rinthy auth backend, with validated callback state.
- Personal access token authentication as a fallback.
- Encrypted local session storage on Android and iOS.
- Account, project and organization loading through the shared Ktor client.
- Project editing, gallery management, versions and member management.
- Project search, status, exact download and follower summaries.
- Creator analytics, project trends and wallet reporting.
- Native loading, retry, empty and refresh states.
- Dark and light Android themes; system-native iOS appearance.
- Shared tests using Ktor `MockEngine` plus Android lint and build checks.

## Requirements

- Android Studio with JBR 21 and Android SDK 36.
- Xcode 16 or newer for iOS builds.
- No Node.js, CocoaPods or web runtime is required.

## Android

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
./gradlew.bat :shared:testAndroidHostTest :androidApp:assembleDebug
```

Open the repository root in Android Studio and run the `androidApp` configuration. The debug APK is written to `androidApp/build/outputs/apk/debug/`.

## iOS

Open `iosApp/Rinthy.xcodeproj` on macOS and run the `Rinthy` scheme. Its build phase calls `:shared:embedAndSignAppleFrameworkForXcode`, so Xcode always consumes the matching Kotlin framework.

To validate the shared Apple framework from a terminal:

```bash
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64
```

Set an Apple Development Team in Xcode before installing on a physical device.

### Codemagic and Sideloadly

The `Ryntra Unsigned Device IPA` workflow in `codemagic.yaml` builds an unsigned ARM64 device archive and publishes `Ryntra-unsigned.ipa`. Download that artifact and open it in Sideloadly; Sideloadly signs it with the Apple ID selected there before installing it on the phone.

The unsigned workflow does not need App Store Connect credentials. A free Apple ID normally requires reinstalling after its development provisioning expires; a paid developer account follows Apple's longer provisioning rules.

## Security

Android encrypts the token with an AES/GCM key held by Android Keystore. iOS stores it as a `ThisDeviceOnly` Keychain item. Tokens are held in shared memory only for the active session and are never included in logs or error messages.

Ryntra is not affiliated with, endorsed by, or maintained by Modrinth.
