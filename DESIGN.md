# Rinthy Design System

## Direction

Rinthy is platform-native first. Android follows current Material 3 interaction, navigation, dynamic color, motion, and accessibility conventions. iOS follows Apple Human Interface Guidelines with native SwiftUI navigation, lists, forms, controls, materials, and motion. Information architecture is shared; component geometry and chrome are not forced to match across platforms.

## Theme Scene

A creator checks project health one-handed between tasks, sometimes outdoors and sometimes late at night. The default Platform theme follows the device appearance. The preserved Rinthy theme provides the existing dark glass interface as an explicit alternative.

## Theme Modes

- `Platform` is the default. Android uses Material 3 dynamic color where supported, system top bars, system navigation, and native controls. iOS uses standard `TabView`, `NavigationStack`, grouped lists, forms, and system materials.
- `Rinthy` preserves the current dark, green-accented glass design on both platforms.
- `System`, `Light`, and `Dark` appearance choices apply to the Platform theme. Rinthy remains dark to preserve its intended contrast and material treatment.
- Theme and appearance preferences are stored locally, included in Android preference export/import, and survive restarts.

## Color Strategy

In Platform mode, Android derives its full primary palette from Material dynamic color when available; do not hardcode Rinthy green over Android system color roles. iOS uses semantic system colors with Rinthy green as its app tint. The Rinthy theme keeps green as its explicit brand accent and preserves its existing neutral dark surfaces. Cyan is informational; amber and red are semantic only.

Green must not color passive metadata, project types, decorative metrics, or every approved row. A screen should still read as neutral when viewed at a distance; tint identifies interaction and meaning rather than filling the composition.

### Semantic Roles

- `brand`: Android dynamic primary in Platform mode; Rinthy green on iOS and in the Rinthy theme.
- `info`: cyan, links and informational states.
- `success`: green, approved and healthy states.
- `warning`: amber, review and processing states.
- `error`: red, failed and rejected states.
- `background`: the lowest, quietest app plane.
- `surface`: list rows and content groupings.
- `chrome`: top bars, bottom navigation, and sheets only.

## Typography

Use each platform's system font. Headings use bold weight and a compact fixed scale; body and metadata retain Dynamic Type or Compose font scaling. Data uses tabular figures where the platform supports them. Letter spacing remains zero except short uppercase metadata labels.

## Spatial System

Use a 4-point base: 4, 8, 12, 16, 24, 32, and 48. Primary page gutters are 16 points on phones. Content groups are separated by 24 points; dense rows use 12 points. Content corners are 8 to 12 points. Floating chrome may use a capsule silhouette. Touch targets are at least 44 points on iOS and 48 dp on Android.

## Layout

The default destination is Dashboard. It answers three questions in order: what changed, what needs attention, and which projects were recently active. Projects, Teams, and Analytics are the other tab destinations. Profile is intentionally outside the tab model and opens through the avatar with a standard back path. A single large title and avatar frame each tab without a top container. Lists use dividers and spacing instead of repeated floating cards.

## Materials

Platform mode delegates chrome and elevation to the operating system. Android uses Material surfaces and navigation components; iOS uses the system tab bar, navigation bar, grouped-list background, and platform materials. Custom blur is not layered on top of system chrome.

Rinthy mode keeps translucency for navigation layering only. On iOS 26 and newer its floating bottom tab bar uses Liquid Glass; iOS 16 through 25 use system material. Compose uses live backdrop blur where the Android version supports it and a translucent tonal fallback elsewhere.

## Iconography

iOS uses SF Symbols. Their license does not permit using the Apple symbol set as Android application artwork. Android uses Lucide glyphs selected to match the same metaphors, optical size, and stroke weight. A destination keeps one metaphor across platforms; filled selection is expressed by the containing selection material rather than mixing unrelated filled and outline families.

## States

- Loading preserves known content and shows a compact progress affordance.
- Refresh errors preserve content and expose a retry action.
- Empty states explain what will appear and where the user can act.
- Status always combines color with a label or icon.
- Reduced-motion settings suppress nonessential transitions.

## Platform Expression

Android and iOS share content priorities, terminology, data accuracy, and feature coverage. They deliberately use different platform components. Android keeps Material 3, system back behavior, edge-to-edge insets, ripple feedback, accessibility semantics, and Android system bars. iOS keeps `NavigationStack`, native `TabView`, grouped lists and forms, SF Symbols, Dynamic Type, and system materials. The Rinthy theme is the only mode that intentionally shares custom visual geometry.
