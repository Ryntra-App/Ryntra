# Contributing

Use JDK 21 and Android SDK 37. Keep business rules and transport models in `shared`; add only platform presentation and platform services to `androidApp` or `iosApp`.

Before opening a pull request, run:

```bash
./gradlew :shared:testAndroidHostTest :androidApp:assembleDebug
```

On macOS, also build the `Rinthy` Xcode scheme. New API behavior should include a `MockEngine` test, and tokens or personal data must never appear in fixtures, logs or screenshots.

Contributors must accept the agreement in [CLA.md](CLA.md).
