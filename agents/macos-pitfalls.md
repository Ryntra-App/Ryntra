# macOS behaviours that keep biting

The Mac build shares its SwiftUI code with iOS, but several AppKit behaviours
have no iOS equivalent. Each item below caused a real bug.

## The toolbar belongs to the window, not to a view

On iOS a `ZStack` layer covers the one beneath it, navigation bar included. On
macOS **every** `.toolbar` in the view tree feeds the single window titlebar, so
stacked layers merge their toolbars: back buttons pile up, and controls of the
covered layer stay clickable. Opening the profile and then pressing the bell
behind it opened both screens at once.

Therefore: on macOS the dashboard shows one screen at a time through `path`, and
`ryntraChrome` is applied once for the whole stack. Pushed screens supply
content only — they never add their own toolbar. Anything opened as a new screen
must join `DashboardRoute`, not become another overlay.

## Never hide the window toolbar to hide a bar

`.toolbar(.hidden, for: .windowToolbar)` removes the entire titlebar — traffic
lights included — leaving no way out of the screen. `.navigationBar` and
`.tabBar` placements do not exist on macOS at all, so those calls are wrapped in
`#if !os(macOS)`.

## The titlebar is translucent and samples what is under it

If two screens have different background colors, the titlebar changes tone as
one fades into the other. It reads as the tab bar flickering.

Every screen must therefore paint the same backdrop: `ryntraScreenBackground(_:)`
for scrolling screens, `ryntraOpaqueListBackground()` for `List`-based ones — a
`List` otherwise keeps its own lighter system background.

## Changing navigationTitle rebuilds the whole toolbar

Which is visible, because the segmented tab control is in that toolbar. The tab
screens pass a constant `windowTitle: "Ryntra"` so switching tabs does not
rebuild it; `title` still drives the Ryntra theme's own top bar.

## TabView cannot animate

macOS `TabView` swaps content instantly and ignores any `transition` applied
inside it. Fading the whole `TabView` out and back in only produces a flash.

The macOS build drives the content itself — `switch selection` inside a `ZStack`
with `.transition(.opacity)` — and puts the same segmented control in the
toolbar via `.principal`. That keeps titlebar tabs and gives a real cross-fade.

## Selecting the already-selected tab is not a change

So a setter bound to it never runs, and a covering screen never closes. The tab
binding reports `nil` while a screen is pushed, which makes any tab press —
including the current one — a real change. It also stops a tab from looking
active while its content is covered.

## Controls default differently in lists

AppKit renders `Toggle` as a checkbox and gives every `Button` a bezel, turning
settings rows into grey rectangles. `ryntraSettingsRowControls()` restores
switch and plain styles; both propagate down the tree, so it is applied once at
the list. Segmented pickers stretch across the row unless given
`ryntraCompactSegments()`, and `Menu` renders as a full-width bordered popup
unless styled `.borderlessButton` with a hidden indicator.

## No pull-to-refresh

`.refreshable` is unreachable — there is no gesture. macOS needs an explicit
refresh affordance; the toolbar button with ⌘R is it.

## Keychain

Use the legacy file-based keychain. `kSecUseDataProtectionKeychain` fails every
write with `-34018` (`errSecMissingEntitlement`) because it needs a
keychain-access-group derived from a Team ID, which this project has none of.
Always check the `OSStatus` of `SecItemAdd` — swallowing it is how "login is not
saved" went unnoticed.

## Window and launch

`WindowGroup` opens a **second window** when a URL arrives, so the OAuth
callback landed in a fresh window instead of the signed-in one. macOS uses a
single `Window` scene.

An app launched outside `/Applications` comes up unfocused; the delegate calls
`NSApplication.shared.activate(ignoringOtherApps: true)` on launch.
