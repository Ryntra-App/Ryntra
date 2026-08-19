# Ryntra

<div align="center">
  <img src="./public/logo.png" width="128" height="128" alt="Ryntra" />
  <p><strong>A native mobile Modrinth dashboard for creators.</strong></p>
  <p>Manage projects, teams, versions, notifications, and analytics from your phone.</p>
</div>

> Ryntra is an unofficial app for Modrinth. It is not affiliated with, endorsed by, or maintained by Modrinth.

## Community

Join the Ryntra Discord server: https://discord.gg/6H5vDq2wk7

## Screenshots

<p align="center"><strong>Android · Material 3</strong></p>
<p align="center">
  <a href="./docs/screenshots/dashboard.png"><img src="./docs/screenshots/dashboard.png" width="210" alt="Ryntra dashboard on Android" /></a>
  <a href="./docs/screenshots/projects.png"><img src="./docs/screenshots/projects.png" width="210" alt="Ryntra projects on Android" /></a>
  <a href="./docs/screenshots/analytics.png"><img src="./docs/screenshots/analytics.png" width="210" alt="Ryntra analytics on Android" /></a>
  <a href="./docs/screenshots/teams.png"><img src="./docs/screenshots/teams.png" width="210" alt="Ryntra teams on Android" /></a>
</p>
<p align="center"><sub>Dashboard · Projects · Analytics · Teams</sub></p>

<p align="center"><strong>iOS · SwiftUI</strong></p>
<p align="center">
  <a href="./docs/screenshots/ios-dashboard.png"><img src="./docs/screenshots/ios-dashboard.png" width="210" alt="Ryntra dashboard on iOS" /></a>
  <a href="./docs/screenshots/ios-projects.png"><img src="./docs/screenshots/ios-projects.png" width="210" alt="Ryntra projects on iOS" /></a>
  <a href="./docs/screenshots/ios-analytics.png"><img src="./docs/screenshots/ios-analytics.png" width="210" alt="Ryntra analytics on iOS" /></a>
  <a href="./docs/screenshots/ios-teams.png"><img src="./docs/screenshots/ios-teams.png" width="210" alt="Ryntra teams on iOS" /></a>
</p>
<p align="center"><sub>Dashboard · Projects · Analytics · Teams</sub></p>

<p align="center"><sub>Click any screenshot to open the full-size capture.</sub></p>

## What Ryntra Can Do

- Sign in with Modrinth OAuth, with PAT login available as a fallback.
- View, search, and manage your Modrinth projects.
- Edit project metadata, links, descriptions, status, icons, banners, and gallery images.
- Manage versions, loaders, game versions, dependencies, files, and release metadata.
- Declare Modrinth content disclosures, including AI-generated content, telemetry, paid features, and derivative work.
- Work with teams and organizations, including members, permissions, invites, ownership, and organization projects.
- Open related projects directly from notifications and accept organization or project invitations.
- View analytics for downloads, views, playtime, revenue, trends, and per-project performance.
- Check balance and payout history where Modrinth exposes that data.
- Edit your Modrinth profile, avatar, bio, and account details.
- Customize the app with Material 3 Android themes and native iOS appearance, including light and dark modes.
- Use the app in English, Russian, and other community-contributed languages.
- Receive local background notifications or optional instant notifications through the limited relay.
- Get native update notices from GitHub Releases with the correct APK or IPA download for your platform.

## Downloads

Android builds are published as APK files in [GitHub Releases](https://github.com/Ryntra-App/Ryntra/releases).

iOS builds are distributed as an unsigned IPA for sideloading. To install the iOS app, use Sideloadly on a computer, sign in to iCloud on Apple's official iCloud app or website, connect your iPhone or iPad with a cable, install the IPA, then trust the developer profile in device settings and enable Developer Mode if iOS asks.

The app checks GitHub Releases for newer versions and opens the matching APK or IPA asset when an update is available. The Appetize ZIP is an unsigned Simulator build and cannot be installed on a physical device.

## Authentication

Ryntra uses Modrinth OAuth for normal sign-in. PAT login is still available as a fallback for development or recovery.

Tokens are stored locally on your device using Android Keystore or the iOS Keychain.

## Local Development

### Requirements

- Android Studio with JBR 21 and Android SDK 36.
- Xcode 16 or newer for iOS builds.
- No Node.js, CocoaPods, or web runtime is required.

### Android Build

Run shared tests and build the Android app:

```bash
./gradlew.bat :shared:testAndroidHostTest :androidApp:assembleDebug
```

The debug APK is written to `androidApp/build/outputs/apk/debug/`.

For a release build:

```bash
./gradlew.bat :androidApp:assembleRelease
```

Configure a signing key in Android Studio or Gradle before distributing a release APK.

### iOS Build

Open `iosApp/Ryntra.xcodeproj` on macOS and run the `Ryntra` scheme. The Xcode build phase embeds the matching Kotlin Multiplatform framework automatically.

The app supports iOS 16.0 and newer on iPhone and iPad. Release builds use optimized whole-module Swift compilation.

For sideloading, build or download the unsigned IPA and install it with Sideloadly or another trusted sideloading tool.

### macOS Build

The `Ryntra` scheme is multiplatform. Pick **My Mac** as the run destination to build a native Mac app on macOS 14.0 or newer, Apple Silicon only.

```bash
xcodebuild -project iosApp/Ryntra.xcodeproj -scheme Ryntra \
  -destination 'platform=macOS,arch=arm64' build
```

macOS links the shared Kotlin framework dynamically, unlike the static one used for iOS: a static build pulls in the `platform.posix` and `platform.darwin` cinterop caches, which reference symbols the macOS SDK no longer exposes for linking. A build phase embeds and signs that framework into the app bundle.

Intel Macs are not covered. Kotlin/Native deprecated `macosX64` in 2.3.20, so the Mac build is pinned to `arm64`; adding `macosX64()` in `shared/build.gradle.kts` would restore it for as long as Kotlin keeps the target.

### visionOS

Kotlin/Native has no visionOS target, so there is no native visionOS build. Apple Vision Pro runs the iPad build in compatibility mode instead — select the **Apple Vision Pro (Designed for iPad)** destination:

```bash
xcodebuild -project iosApp/Ryntra.xcodeproj -scheme Ryntra \
  -destination 'platform=visionOS Simulator,name=Apple Vision Pro' build
```

The product lands in `Debug-iphonesimulator`, since the compatibility layer runs an iOS binary. Install it with `xcrun simctl install`.

### Codemagic

The repository includes three Codemagic workflows:

- `android-native`, which runs shared tests and builds the Android debug APK.
- `ios-native`, which builds a Release iOS Simulator app and packages `Ryntra-simulator.zip` for Appetize.
- `ios-unsigned-ipa`, which builds a Release ARM64 device app and packages `Ryntra-unsigned.ipa` for sideload signing.

The unsigned device workflow intentionally disables Codemagic signing. It is still a Release build; Sideloadly applies the provisioning profile later.

## Notifications

Notification privacy, Firebase/APNs setup, relay deployment, and troubleshooting are documented in [Notification setup](docs/NOTIFICATIONS.md).

The normal Ryntra session token never goes to the notification relay. Instant notifications use a separate limited Modrinth authorization with only the permissions required to read notifications.

## Translations

See [Translating Ryntra](docs/TRANSLATING.md) for the Android and iOS translation workflow, validation command, language rules, and notification locale requirements.

---

# Русский

Ryntra — неофициальное нативное мобильное приложение для авторов на Modrinth.

С ним можно управлять проектами, версиями, командами, организациями, уведомлениями и аналитикой прямо с телефона или планшета.

## Скриншоты

Актуальные экраны Android и iOS собраны в [галерее выше](#screenshots). Нажми на любой скриншот, чтобы открыть его в полном размере.

## Возможности

- Вход через Modrinth OAuth и запасной вход по PAT.
- Просмотр, поиск и управление проектами Modrinth.
- Редактирование метаданных, ссылок, описаний, статуса, иконок, баннеров и галереи.
- Управление версиями, загрузчиками, версиями игры, зависимостями, файлами и метаданными релизов.
- Раскрытие содержимого Modrinth: контент с ИИ, телеметрия, платные функции и производный контент.
- Работа с командами и организациями: участники, права, приглашения, владелец и проекты организации.
- Переход в связанные проекты прямо из уведомлений и принятие приглашений.
- Аналитика по загрузкам, просмотрам, playtime, доходу, трендам и отдельным проектам.
- Просмотр баланса и истории выплат, если эти данные доступны через Modrinth.
- Редактирование профиля, аватара, био и данных аккаунта.
- Нативные темы Android Material 3 и iOS, светлый и тёмный режимы.
- Поддержка английского, русского и других языков, которые добавляет сообщество.
- Локальные фоновые уведомления и необязательная мгновенная доставка через relay-сервис.
- Нативное уведомление о новых версиях с правильной ссылкой на APK или IPA.

## Установка

Android-версия публикуется APK-файлом в [GitHub Releases](https://github.com/Ryntra-App/Ryntra/releases).

iOS-версия доступна как unsigned IPA для sideloading. Чтобы установить приложение на iPhone или iPad, скачай IPA, подпиши его через Sideloadly или другой trusted sideloading-инструмент, установи приложение и доверь профиль разработчика в настройках устройства, если iOS попросит.

Приложение само проверяет GitHub Releases и показывает новую версию, когда она доступна. Кнопка скачивания открывает настоящий asset-файл нужной платформы. ZIP для Appetize предназначен только для iOS Simulator.

## Авторизация

Основной вход работает через Modrinth OAuth. PAT-вход оставлен как запасной вариант для разработки или восстановления доступа.

Токены хранятся локально в Android Keystore или iOS Keychain.

## Локальный запуск

### Требования

- Android Studio с JBR 21 и Android SDK 36.
- Xcode 16 или новее для iOS-сборок.
- Node.js, CocoaPods и web-runtime не требуются.

### Android-сборка

```bash
./gradlew.bat :shared:testAndroidHostTest :androidApp:assembleDebug
```

Debug APK появится в `androidApp/build/outputs/apk/debug/`.

Для Release-сборки:

```bash
./gradlew.bat :androidApp:assembleRelease
```

Перед публикацией настрой signing key в Android Studio или Gradle.

### iOS-сборка

Открой `iosApp/Ryntra.xcodeproj` на macOS и запусти схему `Ryntra`. Build phase Xcode автоматически подключает подходящий Kotlin Multiplatform framework.

Приложение поддерживает iOS 16.0 и новее на iPhone и iPad. Release-сборка использует оптимизированную whole-module компиляцию Swift.

Для sideloading можно скачать unsigned IPA и установить его через Sideloadly или другой trusted sideloading-инструмент.

### macOS-сборка

Схема `Ryntra` мультиплатформенная. Выбери destination **My Mac**, чтобы собрать нативное приложение для macOS 14.0 и новее, только Apple Silicon.

```bash
xcodebuild -project iosApp/Ryntra.xcodeproj -scheme Ryntra \
  -destination 'platform=macOS,arch=arm64' build
```

На macOS общий Kotlin-фреймворк линкуется динамически, в отличие от статического на iOS: статическая сборка тянет cinterop-кэши `platform.posix` и `platform.darwin`, которые ссылаются на символы, отсутствующие в macOS SDK. Отдельная build phase встраивает и подписывает этот фреймворк внутри бандла.

Intel Mac не поддерживается. Kotlin/Native объявил `macosX64` устаревшим в 2.3.20, поэтому Mac-сборка закреплена за `arm64`; добавление `macosX64()` в `shared/build.gradle.kts` вернёт поддержку, пока Kotlin сохраняет этот таргет.

### visionOS

У Kotlin/Native нет таргета visionOS, поэтому нативной visionOS-сборки не существует. Apple Vision Pro запускает iPad-сборку в режиме совместимости — выбери destination **Apple Vision Pro (Designed for iPad)**:

```bash
xcodebuild -project iosApp/Ryntra.xcodeproj -scheme Ryntra \
  -destination 'platform=visionOS Simulator,name=Apple Vision Pro' build
```

Продукт появится в `Debug-iphonesimulator`, потому что слой совместимости запускает iOS-бинарник. Установить его можно через `xcrun simctl install`.

### Codemagic

В `codemagic.yaml` есть три workflow:

- `android-native` запускает shared-тесты и собирает Android debug APK.
- `ios-native` собирает Release-приложение для iOS Simulator и пакует `Ryntra-simulator.zip` для Appetize.
- `ios-unsigned-ipa` собирает Release ARM64-приложение и пакует `Ryntra-unsigned.ipa` для последующей подписи.

Unsigned workflow намеренно отключает подпись Codemagic. Это всё равно Release-сборка, а provisioning profile добавляется через Sideloadly.

## Уведомления

Настройка уведомлений, Firebase/APNs, relay-сервера и диагностика описаны в [docs/NOTIFICATIONS.md](docs/NOTIFICATIONS.md).

Обычный токен сессии Ryntra не отправляется в relay. Мгновенные уведомления используют отдельную ограниченную авторизацию Modrinth.

## Переводы

Смотри [docs/TRANSLATING.md](docs/TRANSLATING.md), чтобы добавить язык одновременно для Android, iOS и серверных уведомлений.

## Сообщество

Discord-сервер Ryntra: https://discord.gg/6H5vDq2wk7
