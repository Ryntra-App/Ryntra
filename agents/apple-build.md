# Xcode project

One target, `Ryntra`, builds iOS, macOS and the visionOS compatibility app from
the same sources. `project.pbxproj` is maintained by hand and is deliberately
compact — keep edits in the existing style rather than letting Xcode rewrite it.

## Building

```bash
xcodebuild -project iosApp/Ryntra.xcodeproj -scheme Ryntra -configuration Debug \
  -destination 'platform=macOS,arch=arm64' build

xcodebuild ... -destination 'platform=iOS Simulator,name=iPhone 17' build
xcodebuild ... -destination 'platform=visionOS Simulator,name=Apple Vision Pro' build
```

Build to check it compiles, then stop — the maintainer runs the app.

Verify a change on **both** iOS and macOS. The two diverge in navigation and
chrome, and a `#if` that compiles on one can break the other.

## Multi-platform settings

`SDKROOT = iphoneos` plus `SUPPORTED_PLATFORMS = "iphoneos iphonesimulator macosx"`.

Do **not** switch `SDKROOT` to `auto`. It is Apple's documented value for
multiplatform targets and builds fine, but JetBrains IDEs cannot read it and
report *"Some targets do not have their base SDK configured properly"* on every
project load. With an explicit `iphoneos` and a destination passed at build
time, macOS builds correctly anyway.

Per-SDK overrides carry the differences:

```
ARCHS[sdk=macosx*]                    = arm64
INFOPLIST_FILE[sdk=macosx*]           = Ryntra/Info-macOS.plist
CODE_SIGN_ENTITLEMENTS[sdk=macosx*]   = Ryntra/Ryntra-macOS.entitlements
LD_RUNPATH_SEARCH_PATHS[sdk=macosx*]  = $(inherited) @executable_path/../Frameworks
TARGETED_DEVICE_FAMILY[sdk=iphone*]   = 1,2
SUPPORTS_MACCATALYST                  = NO
SUPPORTS_MAC_DESIGNED_FOR_IPHONE_IPAD = NO
SUPPORTS_XR_DESIGNED_FOR_IPHONE_IPAD  = YES
```

The last two are what make "My Mac" a native destination while Vision Pro stays
on the iPad compatibility path.

## Build phases

1. **Build shared Kotlin framework** — runs Gradle
2. Sources / Frameworks / Resources
3. **Embed shared Kotlin framework (macOS)** — copies and signs the dylib into
   `Contents/Frameworks`, guarded by `[ "$PLATFORM_NAME" = "macosx" ]`

Phase 3 exists because the Kotlin plugin's embed task is disabled here. Without
it the app links but cannot launch: no `Frameworks` directory, no rpath match.

## Adding a Swift file

Three edits in `project.pbxproj`, all by hand:

1. `PBXBuildFile` entry (`A0xx`)
2. `PBXFileReference` entry (`B0xx`)
3. the id in both the `PBXGroup` children and the `Sources` phase

Then `plutil -lint iosApp/Ryntra.xcodeproj/project.pbxproj` before building.

## Scheme

`xcshareddata/xcschemes/Ryntra.xcscheme` is committed on purpose. Auto-generated
schemes rename themselves between `Ryntra` and `iosApp.Ryntra` depending on how
Xcode last touched the project, and `codemagic.yaml` hardcodes `-scheme Ryntra`.

## Signing and entitlements

There is no Team ID and no signing certificate — builds are ad-hoc signed. Two
consequences that look like bugs but are not:

- **Keychain prompts on every rebuild.** Ad-hoc signatures change their cdhash
  each build, so the ACL sees a different app. Real signing removes this.
- **TCC prompt for Desktop access.** A sandboxed app inside `~/Desktop` — where
  this repo lives — needs permission to read its own bundle. Running the bundle
  from elsewhere avoids it.

`Entitlements file "…" was modified during the build` is almost always **stale
DerivedData**, not a broken file. Diagnose by building into a fresh
`-derivedDataPath`: if that succeeds and the shared one fails, clean it
(`xcodebuild clean`, or ⇧⌘K in Xcode). Do not reach for
`CODE_SIGN_ALLOW_ENTITLEMENTS_MODIFICATION` — it silences the check, not the
cause. It also fires genuinely if the file is saved while a build runs.
