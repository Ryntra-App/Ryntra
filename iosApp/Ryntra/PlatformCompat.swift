import SwiftUI

#if canImport(UIKit)
import UIKit
#elseif canImport(AppKit)
import AppKit
#endif

/// Cross-platform stand-ins for the UIKit APIs this app reaches for in more
/// than one place. One-off differences stay inline at their call site.

/// The native color type backing `Color` on the current platform.
#if canImport(UIKit)
typealias RyntraNativeColor = UIColor
#elseif canImport(AppKit)
typealias RyntraNativeColor = NSColor
#endif

/// Opens a URL outside the app.
@MainActor
func ryntraOpenExternalURL(_ url: URL) {
#if canImport(UIKit)
    UIApplication.shared.open(url)
#elseif canImport(AppKit)
    NSWorkspace.shared.open(url)
#endif
}

/// Replaces the pasteboard contents with `value`.
@MainActor
func ryntraCopyToPasteboard(_ value: String) {
#if canImport(UIKit)
    UIPasteboard.general.string = value
#elseif canImport(AppKit)
    NSPasteboard.general.clearContents()
    NSPasteboard.general.setString(value, forType: .string)
#endif
}

/// Asks the system to deliver a remote notification device token.
@MainActor
func ryntraRegisterForRemoteNotifications() {
#if canImport(UIKit)
    UIApplication.shared.registerForRemoteNotifications()
#elseif canImport(AppKit)
    NSApplication.shared.registerForRemoteNotifications()
#endif
}

extension ToolbarItemPlacement {
    /// Leading edge of the navigation bar on iOS, the navigation area on macOS.
    static var ryntraLeading: ToolbarItemPlacement {
#if os(macOS)
        .navigation
#else
        .navigationBarLeading
#endif
    }

    /// Trailing edge of the navigation bar on iOS, the primary action slot on macOS.
    static var ryntraTrailing: ToolbarItemPlacement {
#if os(macOS)
        .primaryAction
#else
        .navigationBarTrailing
#endif
    }
}

extension View {
    /// Keeps compact icon controls comfortably tappable without inflating
    /// their visual artwork. AppKit uses its native control metrics.
    @ViewBuilder
    func ryntraMinimumTouchTarget() -> some View {
#if os(macOS)
        self
#else
        frame(minWidth: 44, minHeight: 44)
            .contentShape(Rectangle())
#endif
    }

    /// Applies the inline navigation title style. macOS has a single title
    /// style, so this is a no-op there.
    @ViewBuilder
    func ryntraInlineNavigationTitle() -> some View {
#if os(macOS)
        self
#else
        navigationBarTitleDisplayMode(.inline)
#endif
    }

    /// The grouped list appearance, mapped to the closest macOS equivalent.
    @ViewBuilder
    func ryntraGroupedListStyle() -> some View {
#if os(macOS)
        listStyle(.inset)
#else
        listStyle(.insetGrouped)
#endif
    }

    /// Disables automatic capitalization. macOS has no software keyboard.
    @ViewBuilder
    func ryntraNoAutocapitalization() -> some View {
#if os(macOS)
        self
#else
        textInputAutocapitalization(.never)
#endif
    }

    /// Restores iOS-like controls inside settings rows. AppKit defaults a
    /// Toggle to a checkbox and gives every Button a bezel, which turns list
    /// rows into a wall of grey rectangles. Both styles propagate down the
    /// view tree, so applying this once at the list is enough.
    @ViewBuilder
    func ryntraSettingsRowControls() -> some View {
#if os(macOS)
        toggleStyle(.switch).buttonStyle(.plain)
#else
        self
#endif
    }

    /// Lifts a clickable row while the pointer is over it. Macs expect that
    /// feedback; touch platforms have no pointer, so this is a no-op there.
    @ViewBuilder
    func ryntraHoverHighlight() -> some View {
#if os(macOS)
        modifier(RyntraHoverHighlight())
#else
        self
#endif
    }

    /// Fills a scrolling screen with an opaque backdrop. On macOS the view is
    /// stretched to the full window first, so the translucent titlebar always
    /// samples this colour instead of whatever shows through a short page.
    @ViewBuilder
    func ryntraScreenBackground(_ color: Color) -> some View {
#if os(macOS)
        frame(maxWidth: .infinity, maxHeight: .infinity)
            .background(color)
#else
        background(color)
#endif
    }

    /// Replaces a List's own backdrop with the app background on macOS.
    /// The Mac titlebar is translucent and samples whatever sits beneath it,
    /// so a screen that keeps the lighter system list background shifts the
    /// titlebar's tone every time it appears — which reads as flickering.
    @ViewBuilder
    func ryntraOpaqueListBackground() -> some View {
#if os(macOS)
        scrollContentBackground(.hidden)
            .background(Color.ryntraBackground)
#else
        self
#endif
    }

    /// Keeps a segmented picker at its natural width. In a Mac list it
    /// otherwise stretches across the full row.
    @ViewBuilder
    func ryntraCompactSegments() -> some View {
#if os(macOS)
        fixedSize()
#else
        self
#endif
    }

    /// Requests a numeric software keyboard where one exists.
    @ViewBuilder
    func ryntraNumericKeyboard(decimal: Bool = false) -> some View {
#if os(macOS)
        self
#else
        keyboardType(decimal ? .decimalPad : .numberPad)
#endif
    }

    /// Requests the URL keyboard on iOS and remains a no-op on macOS.
    @ViewBuilder
    func ryntraURLKeyboard() -> some View {
#if os(macOS)
        self
#else
        keyboardType(.URL)
#endif
    }

    /// Lets the software keyboard follow a drag while preserving the native
    /// macOS scrolling behavior.
    @ViewBuilder
    func ryntraInteractiveKeyboardDismissal() -> some View {
#if os(macOS)
        self
#else
        scrollDismissesKeyboard(.interactively)
#endif
    }
}

#if os(macOS)
private struct RyntraHoverHighlight: ViewModifier {
    @State private var isHovering = false

    func body(content: Content) -> some View {
        content
            .background(
                RoundedRectangle(cornerRadius: 10, style: .continuous)
                    .fill(Color.primary.opacity(isHovering ? 0.06 : 0))
            )
            .onHover { isHovering = $0 }
            .animation(.easeOut(duration: 0.12), value: isHovering)
    }
}
#endif
