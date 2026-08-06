# Ryntra

Unofficial Modrinth dashboard. Kotlin Multiplatform core with native UI per platform.

```
shared/      Kotlin: models, Modrinth API, repositories, app controller
androidApp/  Android, Jetpack Compose
iosApp/      iOS + macOS, SwiftUI, single Xcode target
```

`shared` is the only place business logic lives. The Swift and Compose layers
render it and nothing more — if a rule about Modrinth data is being written in
Swift, it probably belongs in `commonMain` instead.

## Platforms

| Platform | Status | Notes |
|---|---|---|
| Android | native | minSdk 26 |
| iOS / iPadOS | native | 16.0+ |
| macOS | native | 14.0+, **arm64 only** |
| visionOS | compatibility | runs the iPad build, no native target |

macOS and visionOS both have hard limits imposed by Kotlin/Native — see
[agents/kotlin-shared.md](agents/kotlin-shared.md) before assuming a target can
be added.

## Detailed guides

| File | Covers |
|---|---|
| [agents/swift-style.md](agents/swift-style.md) | Swift conventions, comments, when to add a file |
| [agents/platform-compat.md](agents/platform-compat.md) | What belongs in `PlatformCompat.swift` and what does not |
| [agents/kotlin-shared.md](agents/kotlin-shared.md) | KMP targets, source sets, framework linkage |
| [agents/apple-build.md](agents/apple-build.md) | Xcode project layout, schemes, entitlements, build commands |
| [agents/macos-pitfalls.md](agents/macos-pitfalls.md) | macOS behaviours that repeatedly caused bugs |

## Working agreements

Build to verify a change compiles, then hand it over — **do not launch the app**.
The maintainer runs and inspects it, on every platform.

Talk to the maintainer in Russian. Everything committed — code, comments, docs,
commit messages — is English.

Do not add third-party Swift dependencies. The iOS target links only the KMP
framework; keep it that way unless explicitly asked.
