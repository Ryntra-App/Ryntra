# Swift conventions

## Comments

Comment **why**, never what. The code already says what it does; the comment
exists for the reader who wonders why it was written that oddly.

```swift
// Good — the reason is invisible from the code.
// macOS has no BGTaskScheduler; NSBackgroundActivityScheduler owns a
// repeating activity and re-arms itself.

// Useless — restates the line below it.
// Set the background colour
.background(Color.ryntraBackground)
```

Non-obvious platform behaviour deserves a comment every time. Someone will
eventually "simplify" a workaround that looks pointless — the comment is what
stops them. Most of `agents/macos-pitfalls.md` exists because these were not
obvious to anyone.

Write comments as prose, in English, no trailing periods on single fragments.

## Files

Add a file when it carries a self-contained concept (`RemoteImage`,
`PlatformCompat`). Do not add one to hold a platform branch or a couple of
helpers — put those next to what they serve.

Every new Swift file must be registered in `project.pbxproj` by hand, in three
places. See [apple-build.md](apple-build.md).

## Views

Screens take data and callbacks; they do not reach into navigation state. The
dashboard owns routing and passes `onProjectTap` / `onOpenOrganization` down.
This is what lets macOS route through its navigation path while iOS keeps its
overlay stack, with no change to the screen itself.

Keep platform branches at the smallest scope that works. A whole duplicated
`body` per platform is a last resort — `DashboardView` does it only because the
navigation containers genuinely differ.

## Theme

Colors come from `Color.ryntra*` in `RyntraTheme.swift`, never literal
`Color(red:green:blue:)` at a call site. Adaptive colors resolve at draw time
through `adaptive(dark:light:)` so they keep following the system appearance.

Motion goes through `RyntraMotion` and must respect reduce-motion: read both
`@Environment(\.accessibilityReduceMotion)` and the in-app `reduceMotion`
setting, and pass the pair through `RyntraMotion.resolved(_:reduceMotion:)`.

## Images

Use `RemoteImage`, never `AsyncImage`. `AsyncImage` has no cache and restarts
its request on every view rebuild, so avatars and icons blink back to their
placeholder on each scroll tick and toolbar update. `RemoteImage` keeps decoded
images in an `NSCache`, reads it synchronously in `body`, and coalesces
concurrent requests for the same URL.

## Localization

User-facing strings go through `NSLocalizedString` with a `comment:` that says
where the string appears. Add new keys to both `en.lproj` and `ru.lproj`.
