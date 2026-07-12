# Rinthy Design System

## Direction

Rinthy uses a Human Interface direction based on Apple HIG. Clear hierarchy, direct manipulation, system typography, restrained materials, and content-first layouts are shared across platforms. Project artwork and Rinthy color preserve creator identity, so the result does not become a generic system-app clone.

## Theme Scene

A creator checks project health one-handed between tasks, sometimes outdoors and sometimes late at night. The app follows the system appearance so contrast stays appropriate without adding a separate theme decision.

## Color Strategy

Restrained, with a vivid Rinthy green reserved for primary actions, selection, and positive movement. Cyan is an informational accent. Amber and red are semantic only. Neutral surfaces carry a subtle green cast and never use pure black or pure white.

Green must not color passive metadata, project types, decorative metrics, or every approved row. A screen should still read as neutral when viewed at a distance; tint identifies interaction and meaning rather than filling the composition.

### Semantic Roles

- `brand`: Rinthy green, primary actions and active navigation.
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

Translucency communicates navigation layering only. The top title and avatar belong directly to the content layer and never sit inside glass. On iOS 26 and newer, the floating bottom tab bar uses Liquid Glass. iOS 16 through 25 use system material with the same silhouette. Compose reproduces the bottom material using live backdrop blur, neutral tint, a subtle border, and a darker neutral selected capsule. Content scrolls beneath the tab bar so the material has something meaningful to refract.

Android 12 and newer use real backdrop blur for floating chrome. Android 8 through 11 use a translucent tonal fallback because platform rendering cannot provide the same live blur reliably. Both variants keep the same geometry, border, tint, and contrast.

## Iconography

iOS uses SF Symbols. Their license does not permit using the Apple symbol set as Android application artwork. Android uses Lucide glyphs selected to match the same metaphors, optical size, and stroke weight. A destination keeps one metaphor across platforms; filled selection is expressed by the containing selection material rather than mixing unrelated filled and outline families.

## States

- Loading preserves known content and shows a compact progress affordance.
- Refresh errors preserve content and expose a retry action.
- Empty states explain what will appear and where the user can act.
- Status always combines color with a label or icon.
- Reduced-motion settings suppress nonessential transitions.

## Platform Expression

The visible component geometry is intentionally shared across iOS and Android. Android keeps system back behavior, edge-to-edge insets, ripple feedback, accessibility semantics, and Android status/navigation bars. iOS keeps NavigationStack, TabView state, SF Symbols, Dynamic Type, and native materials. Material 3 may provide internal behavior on Android, but no default Material component styling is allowed to leak into the interface.
