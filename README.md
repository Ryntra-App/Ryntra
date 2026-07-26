# Ryntra Native

Ryntra is an unofficial native mobile workspace for Modrinth creators. It brings project management, teams, releases, notifications, analytics and account tools to Android and iOS.

Ryntra is not affiliated with, endorsed by, or maintained by Modrinth.

## Architecture

```text
androidApp/  Jetpack Compose + Material 3, Android Keystore, FCM
iosApp/      SwiftUI, Keychain, APNs, Xcode project
shared/      Kotlin Multiplatform models, Ktor API clients, repositories and state
```

Business logic and API contracts live in `shared`. Each platform owns its navigation, presentation, secure storage, notifications and update UI.

## Features

- Modrinth OAuth sign-in with validated callbacks, plus PAT fallback.
- Project search, status, exact download/follower totals, editing, gallery and banners.
- Version creation and management, loaders, game versions, dependencies and files.
- Organization and team members, permissions, invitations and organization projects.
- In-app notifications with read/archive state and project deep links.
- Optional background checks and instant notifications through the limited relay service.
- Creator analytics for downloads, views, playtime, revenue, trends and project breakdowns.
- Wallet balance and payout history where Modrinth exposes the data.
- Native profile, avatar, bio, appearance, language and notification settings.
- Automatic GitHub Release checks. When a newer version is available, the app shows native release notes and opens the matching APK or IPA asset for download.
- Android Material 3 UI and iOS system-native SwiftUI UI, with light/dark appearance and adaptive iPad layouts.

## Downloads and updates

Stable builds are published in [Ryntra Releases](https://github.com/Ryntra-App/Ryntra/releases).

The app checks the latest GitHub Release against its bundled version. Android selects the `.apk` asset, while iOS selects the `.ipa` asset. The download action opens the actual release asset URL; it never guesses a filename.

For sideloading iOS:

1. Download the IPA from GitHub Releases.
2. Sign it with Sideloadly, AltStore, or another trusted sideloading tool.
3. Install it on the iPhone or iPad and trust the developer profile if iOS asks.

The iOS Appetize artifact is a separate unsigned Simulator build and cannot be installed on a physical device.

## Requirements

- Android Studio with JBR 21 and Android SDK 36.
- Xcode 16 or newer on macOS for iOS.
- No Node.js, CocoaPods or web runtime is required.

## Android

Run shared tests and build the debug APK:

```powershell
./gradlew.bat :shared:testAndroidHostTest :androidApp:assembleDebug
```

The debug APK is written to `androidApp/build/outputs/apk/debug/`.

For a release build:

```powershell
./gradlew.bat :androidApp:assembleRelease
```

Configure signing in Android Studio or Gradle before distributing a release APK.

## iOS

Open `iosApp/Ryntra.xcodeproj` on macOS and run the `Ryntra` scheme. The Xcode build phase embeds the matching Kotlin framework automatically.

The deployment target is iOS 16.0. The target supports both iPhone and iPad. Release builds use optimized whole-module Swift compilation.

### Codemagic

`codemagic.yaml` contains three workflows:

- `android-native`, which runs shared tests and builds the Android debug artifact.
- `ios-native`, which builds a Release iOS Simulator app and packages `Ryntra-simulator.zip` for Appetize.
- `ios-unsigned-ipa`, which builds a Release ARM64 device app and packages `Ryntra-unsigned.ipa` for sideload signing.

The unsigned device workflow intentionally disables Codemagic signing. It is still a Release build; Sideloadly or another signing tool applies the provisioning profile later.

## Notifications

The notification architecture and APNs/FCM relay deployment are documented in [docs/NOTIFICATIONS.md](docs/NOTIFICATIONS.md). The normal Modrinth session token never goes to the relay. Instant delivery uses a separate limited authorization.

## Translations

See [docs/TRANSLATING.md](docs/TRANSLATING.md) for the Android/iOS translation workflow and notification locale requirements.

## Security

Android stores session secrets using Android Keystore. iOS uses a `ThisDeviceOnly` Keychain item. Tokens are not written to logs or committed files.

## Community

Join the Ryntra Discord: <https://discord.gg/6H5vDq2wk7>
