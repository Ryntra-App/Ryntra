import SwiftUI

enum RyntraThemeStyle: String, CaseIterable, Identifiable {
    case platform
    case ryntra

    var id: String { rawValue }
    var label: String { self == .platform ? "Platform" : "Ryntra" }
}

enum RyntraAppearanceMode: String, CaseIterable, Identifiable {
    case system
    case light
    case dark

    var id: String { rawValue }
    var label: String {
        switch self {
        case .system: return NSLocalizedString("System", comment: "Appearance mode")
        case .light: return NSLocalizedString("Light", comment: "Appearance mode")
        case .dark: return NSLocalizedString("Dark", comment: "Appearance mode")
        }
    }

    var colorScheme: ColorScheme? {
        switch self {
        case .system: return nil
        case .light: return .light
        case .dark: return .dark
        }
    }
}

enum RyntraAppLanguage: String, CaseIterable, Identifiable {
    // <localization-tool:ios-language-cases>
    case system
    case english = "en"
    case russian = "ru"
    // </localization-tool:ios-language-cases>

    var id: String { rawValue }

    var label: String {
        switch self {
        // <localization-tool:ios-language-labels>
        case .system: return NSLocalizedString("System", comment: "Language")
        case .english: return "English"
        case .russian: return "Русский"
        // </localization-tool:ios-language-labels>
        }
    }

    var locale: Locale? {
        self == .system ? nil : Locale(identifier: rawValue)
    }

    static func apply(_ rawValue: String) {
        let language = RyntraAppLanguage(rawValue: rawValue) ?? .system
        if language == .system {
            UserDefaults.standard.removeObject(forKey: "AppleLanguages")
        } else {
            UserDefaults.standard.set([language.rawValue], forKey: "AppleLanguages")
        }
        UserDefaults.standard.synchronize()
    }
}

enum RyntraMotion {
    static let navigation = Animation.interactiveSpring(
        response: 0.38,
        dampingFraction: 0.88,
        blendDuration: 0.08
    )
    static let control = Animation.interactiveSpring(
        response: 0.26,
        dampingFraction: 0.9,
        blendDuration: 0.05
    )

    static func resolved(_ animation: Animation, reduceMotion: Bool) -> Animation? {
        reduceMotion ? nil : animation
    }

    static func navigationTransition(reduceMotion: Bool) -> AnyTransition {
        guard !reduceMotion else { return .identity }
        return .asymmetric(
            insertion: .move(edge: .trailing).combined(with: .opacity),
            removal: .move(edge: .trailing).combined(with: .opacity)
        )
    }
}

func ryntraExactCount(_ value: Int64) -> String {
    value.formatted(.number.grouping(.automatic))
}

extension Color {
    static let analyticsBlue = Color(red: 0.18, green: 0.55, blue: 0.96)
    static let analyticsOrange = Color(red: 0.95, green: 0.49, blue: 0.18)
    static let analyticsGreen = Color(red: 0.22, green: 0.72, blue: 0.39)
    static let analyticsPink = Color(red: 0.88, green: 0.32, blue: 0.62)
    static let analyticsCyan = Color(red: 0.18, green: 0.70, blue: 0.74)
    static let analyticsViolet = Color(red: 0.48, green: 0.39, blue: 0.88)
    static let analyticsRed = Color(red: 0.91, green: 0.30, blue: 0.34)
    static let analyticsGold = Color(red: 0.78, green: 0.61, blue: 0.12)

    static let analyticsSeries: [Color] = [
        .analyticsBlue, .analyticsOrange, .analyticsGreen, .analyticsPink,
        .analyticsCyan, .analyticsViolet, .analyticsRed, .analyticsGold,
    ]

    /// Modrinth's revenue page colours each dated payout in this order; matching it lets a
    /// creator read the app's balance bar the same way as the site's
    static let ryntraPayoutEstimates: [Color] = [
        Color(red: 0.31, green: 0.61, blue: 1.00),
        Color(red: 0.75, green: 0.52, blue: 0.99),
        Color(red: 0.98, green: 0.57, blue: 0.24),
        Color(red: 0.97, green: 0.44, blue: 0.44),
    ]

    static let ryntraGreen = adaptive(
        dark: RyntraNativeColor(red: 0.28, green: 0.85, blue: 0.47, alpha: 1),
        light: RyntraNativeColor(red: 0.08, green: 0.46, blue: 0.23, alpha: 1)
    )

    static let ryntraCyan = adaptive(
        dark: RyntraNativeColor(red: 0.33, green: 0.78, blue: 0.91, alpha: 1),
        light: RyntraNativeColor(red: 0.00, green: 0.40, blue: 0.49, alpha: 1)
    )

    static let ryntraOnAccent = adaptive(
        dark: RyntraNativeColor(red: 0.02, green: 0.02, blue: 0.02, alpha: 1),
        light: RyntraNativeColor(red: 0.98, green: 0.98, blue: 0.98, alpha: 1)
    )

    static let ryntraBackground = adaptive(
        dark: RyntraNativeColor(red: 0.0, green: 0.0, blue: 0.0, alpha: 1),
        light: .ryntraLightBackground
    )

    /// Cards sit one step above `ryntraBackground`, the way a grouped list's
    /// rows sit above its own backdrop.
    static let ryntraSurface = adaptive(
        dark: RyntraNativeColor(red: 0.047, green: 0.047, blue: 0.055, alpha: 1),
        light: .ryntraLightSurface
    )

    static let ryntraSurfaceRaised = adaptive(
        dark: RyntraNativeColor(red: 0.11, green: 0.11, blue: 0.118, alpha: 1),
        light: .ryntraLightSurfaceRaised
    )

    static let ryntraSeparator = adaptive(
        dark: RyntraNativeColor(red: 0.173, green: 0.173, blue: 0.18, alpha: 1),
        light: .ryntraLightSeparator
    )

    /// Resolves against the active appearance at draw time so the color keeps
    /// following the system when the user switches light and dark.
    private static func adaptive(dark: RyntraNativeColor, light: RyntraNativeColor) -> Color {
#if canImport(UIKit)
        Color(uiColor: UIColor { traits in
            traits.userInterfaceStyle == .dark ? dark : light
        })
#elseif canImport(AppKit)
        Color(nsColor: NSColor(name: nil) { appearance in
            appearance.bestMatch(from: [.aqua, .darkAqua]) == .darkAqua ? dark : light
        })
#endif
    }
}

/// Light-mode system colors, named per platform.
///
/// The three UIKit values are one ladder, and they have to stay in the same
/// family as the backdrop every screen paints. Since that is
/// `systemGroupedBackground`, these are its grouped steps — mixing in a
/// `systemBackground` step would put white on white.
private extension RyntraNativeColor {
    static var ryntraLightBackground: RyntraNativeColor {
#if canImport(UIKit)
        .systemGroupedBackground
#elseif canImport(AppKit)
        .windowBackgroundColor
#endif
    }

    static var ryntraLightSurface: RyntraNativeColor {
#if canImport(UIKit)
        .secondarySystemGroupedBackground
#elseif canImport(AppKit)
        .underPageBackgroundColor
#endif
    }

    static var ryntraLightSurfaceRaised: RyntraNativeColor {
#if canImport(UIKit)
        .tertiarySystemGroupedBackground
#elseif canImport(AppKit)
        .controlBackgroundColor
#endif
    }

    static var ryntraLightSeparator: RyntraNativeColor {
#if canImport(UIKit)
        .separator
#elseif canImport(AppKit)
        .separatorColor
#endif
    }
}

/// The backdrop every screen paints.
///
/// The navigation bar, the tab bar and the Ryntra glass bar are all translucent
/// and sample whatever sits behind them. A screen that paints a different
/// backdrop from the one it replaces therefore shifts their tone while a push
/// animates, which reads as the bars flashing. Grouped screens and scrolling
/// screens both come through here so there is only ever one tone in flight.
///
/// Both themes land on `ryntraBackground`, because it is already the tone each
/// one wants: pure black in dark, and in light the grouped step below
/// `ryntraSurface` that keeps a card readable against the page.
struct RyntraScreenBackdrop: ViewModifier {
    @AppStorage("themeStyle") private var storedThemeStyle = RyntraThemeStyle.platform.rawValue

    /// True for a grouped `List`, whose system-drawn rows only read correctly
    /// against the system's own backdrop. Every other screen — scrolling pages
    /// and plain lists with clear rows alike — gives its backdrop up.
    let keepsSystemListBackground: Bool

    private var isPlatformNative: Bool {
        storedThemeStyle == RyntraThemeStyle.platform.rawValue
    }

    func body(content: Content) -> some View {
#if os(macOS)
        // The Mac titlebar samples the window, so every screen — List or not —
        // gives up its own background for the app's. The view is stretched to
        // the full window first, or a short page would let the desktop through
        // under the titlebar.
        content
            .scrollContentBackground(.hidden)
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .background(Color.ryntraBackground)
#else
        content
            .scrollContentBackground(keepsSystemListBackground && isPlatformNative ? .visible : .hidden)
            .background(Color.ryntraBackground)
#endif
    }
}

extension View {
    /// Backdrop for a scrolling screen, or a plain `List` whose rows are clear.
    func ryntraScreenBackdrop() -> some View {
        modifier(RyntraScreenBackdrop(keepsSystemListBackground: false))
    }

    /// Backdrop for a grouped `List`, which keeps its system row backgrounds.
    func ryntraGroupedListBackdrop() -> some View {
        modifier(RyntraScreenBackdrop(keepsSystemListBackground: true))
    }
}

struct RyntraSectionLabel: View {
    @AppStorage("themeStyle") private var storedThemeStyle = RyntraThemeStyle.platform.rawValue
    let text: String

    private var isPlatformNative: Bool {
        storedThemeStyle == RyntraThemeStyle.platform.rawValue
    }

    var body: some View {
        Text(LocalizedStringKey(text))
            .textCase(isPlatformNative ? nil : .uppercase)
            .font(isPlatformNative ? .subheadline.weight(.semibold) : .caption.weight(.bold))
            .foregroundStyle(isPlatformNative ? Color.secondary : Color.ryntraGreen)
            .accessibilityAddTraits(.isHeader)
    }
}
