# Rinthy Design System

## Direction

Rinthy uses the Creator Cockpit direction. The interface is dense enough for routine creator work, but project artwork and clear hierarchy keep it expressive. Discord informs activity density, Spotify informs content identity, and neither product is copied literally.

## Theme Scene

A creator checks project health one-handed between tasks, sometimes outdoors and sometimes late at night. The app follows the system appearance so contrast stays appropriate without adding a separate theme decision.

## Color Strategy

Restrained, with a vivid Rinthy green reserved for primary actions, selection, and positive movement. Cyan is an informational accent. Amber and red are semantic only. Neutral surfaces carry a subtle green cast and never use pure black or pure white.

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

Use a 4-point base: 4, 8, 12, 16, 24, 32, and 48. Primary page gutters are 16 points on phones. Content groups are separated by 24 points; dense rows use 12 points. Corners are 8 points or smaller. Touch targets are at least 44 points on iOS and 48 dp on Android.

## Layout

The default destination is Overview. It answers three questions in order: what changed, what needs attention, and which projects were recently active. Projects, Teams, and Account remain dedicated destinations. Lists use dividers and spacing instead of repeated floating cards.

## Materials

Translucency communicates chrome and layers only. SwiftUI uses system bar materials. Compose uses tonal, slightly translucent navigation surfaces without placing blur behind dense content. Content surfaces remain opaque for predictable contrast and scroll performance.

## States

- Loading preserves known content and shows a compact progress affordance.
- Refresh errors preserve content and expose a retry action.
- Empty states explain what will appear and where the user can act.
- Status always combines color with a label or icon.
- Reduced-motion settings suppress nonessential transitions.

## Platform Expression

Android uses Material 3 interaction behavior, edge-to-edge layout, Compose navigation bars, and Android iconography. iOS uses NavigationStack, TabView, SF Symbols, native materials, and system list/scroll behavior. Information architecture and semantic color roles are shared; component geometry is not forced to match pixel-for-pixel.
