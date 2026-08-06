# PlatformCompat.swift

`iosApp/Ryntra/PlatformCompat.swift` holds cross-platform stand-ins for APIs
that differ between UIKit and AppKit.

## The rule

**Only abstract what is used more than once. A one-off difference stays inline
at its call site as `#if os(macOS)`.**

Wrapping a single call in a named helper adds indirection and hides the platform
difference from the one place that cares about it. Wrapping the same difference
scattered across ten files does the opposite — it makes the divergence
reviewable in one spot.

Current members earn their place: `ryntraOpenExternalURL` (5 call sites),
`ryntraInlineNavigationTitle` (8), `ryntraNoAutocapitalization` (3),
`ryntraCopyToPasteboard` (2), `RyntraNativeColor` (6 in the theme alone).

Counter-examples that are deliberately **not** here:

- `.toolbar(.hidden, for: .tabBar)` — one call site, `#if` in `DashboardView`
- `Color(uiColor: .systemGroupedBackground)` — one screen, a private computed
  property on that view
- `NSApplicationDelegate` vs `UIApplicationDelegate` — one class, a typealias
  next to it in `LocalNotificationManager.swift`

Related exception: `ToolbarItemPlacement.ryntraLeading` / `.ryntraTrailing` are
a pair describing one concept. Splitting them by call count would be worse than
keeping them together.

## Naming

Prefix with `ryntra` so completion groups them and they never collide with a
future SwiftUI API of the same name. Free functions for actions
(`ryntraCopyToPasteboard`), `View` extensions for modifiers
(`ryntraGroupedListStyle`).

## Shape

Every helper is `@ViewBuilder` with the platform branch inside, so call sites
stay flat:

```swift
@ViewBuilder
func ryntraGroupedListStyle() -> some View {
#if os(macOS)
    listStyle(.inset)
#else
    listStyle(.insetGrouped)
#endif
}
```

Prefer `#if canImport(UIKit)` when the split is genuinely UIKit-vs-AppKit
(colors, pasteboard), and `#if os(macOS)` when it is about macOS behaviour
(toolbars, hover, keyboards). visionOS imports UIKit, so `canImport` keeps it on
the iOS path automatically — which is what the compatibility build needs.

## Do not create a new file for one platform difference

`BackgroundTasks` on iOS versus `NSBackgroundActivityScheduler` on macOS lives
inside `LocalNotificationManager`, not in a separate scheduler file. The
difference is three methods; a file would scatter one feature across two places.
