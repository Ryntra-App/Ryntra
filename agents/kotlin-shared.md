# Shared Kotlin module

`shared/` holds every model, endpoint and repository. Android and Apple both
consume it; nothing platform-specific belongs in `commonMain`.

## Source sets

```
commonMain/   everything
appleMain/    iOS + macOS — currently one file, the Darwin Ktor engine
androidMain/  OkHttp engine
```

`appleMain` (not `iosMain`) is deliberate: macOS is a real Kotlin/Native target
and shares the same Darwin engine. The default hierarchy gives
`commonMain → nativeMain → appleMain → {iosMain, macosMain}`, so anything valid
for all Apple platforms goes in `appleMain`.

## Targets and framework linkage

iOS links a **static** framework, bundled into an XCFramework for IPA builds.

macOS links a **dynamic** one, and is excluded from the XCFramework. This is not
a preference. A static Kotlin framework drags the whole `platform.posix` and
`platform.darwin` cinterop caches into the app, and those reference symbols the
macOS SDK no longer exposes for linking:

```
Undefined symbols for architecture arm64:
  "_fdclosedir",   "_fdscandir",    "_scandirat",
  "_thread_suspend2", "_thread_resume2", "_vm_reallocate",
  "_mach_memory_info_redacted", "_thread_set_x86_64_compat"
```

Linking dynamically resolves them inside the dylib. The trade-off is that the
framework must be embedded into the bundle — the Kotlin plugin's own
`embedAndSignAppleFrameworkForXcode` task is disabled in this setup ("Task is
enabled is false"), so a shell build phase does it. See [apple-build.md](apple-build.md).

## Targets that cannot be added

**visionOS** — Kotlin/Native has no visionOS target at all. Not experimental,
absent: `ktor-*-visionosarm64`, `kotlinx-coroutines-core-visionosarm64` and
friends return 404 on Maven Central, and visionOS appears in no tier of the
Kotlin/Native support table. Apple Vision Pro therefore runs the iPad build in
compatibility mode, and a native visionOS UI would require reimplementing all of
`commonMain` in Swift.

**Intel macOS** — `macosX64` is deprecated as of Kotlin 2.3.20 and pending
removal, so the Mac build is pinned to `arm64` via `ARCHS[sdk=macosx*]`. Adding
`macosX64()` back would work for as long as Kotlin keeps the target; Xcode
otherwise fails with "Xcode Requested Architecture Not Configured in Gradle".

## Building

```bash
./gradlew :shared:testAndroidHostTest      # shared tests
./gradlew :shared:linkDebugFrameworkMacosArm64
```

Xcode invokes Gradle itself through a build phase; there is no need to build the
framework by hand before an Xcode build.
