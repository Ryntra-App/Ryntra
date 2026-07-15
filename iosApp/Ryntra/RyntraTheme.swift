import SwiftUI
import UIKit

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
    case system
    case english = "en"
    case russian = "ru"

    var id: String { rawValue }

    var label: String {
        switch self {
        case .system: return NSLocalizedString("System", comment: "Language")
        case .english: return "English"
        case .russian: return "Русский"
        }
    }

    var locale: Locale? {
        switch self {
        case .system: return nil
        case .english: return Locale(identifier: "en")
        case .russian: return Locale(identifier: "ru")
        }
    }

    static func apply(_ rawValue: String) {
        let language = RyntraAppLanguage(rawValue: rawValue) ?? .system
        switch language {
        case .system:
            UserDefaults.standard.removeObject(forKey: "AppleLanguages")
        case .english, .russian:
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

    static let ryntraGreen = adaptive(
        dark: UIColor(red: 0.28, green: 0.85, blue: 0.47, alpha: 1),
        light: UIColor(red: 0.08, green: 0.46, blue: 0.23, alpha: 1)
    )

    static let ryntraCyan = adaptive(
        dark: UIColor(red: 0.33, green: 0.78, blue: 0.91, alpha: 1),
        light: UIColor(red: 0.00, green: 0.40, blue: 0.49, alpha: 1)
    )

    static let ryntraBackground = adaptive(
        dark: UIColor(red: 0.0, green: 0.0, blue: 0.0, alpha: 1),
        light: .systemBackground
    )

    static let ryntraSurface = adaptive(
        dark: UIColor(red: 0.047, green: 0.047, blue: 0.055, alpha: 1),
        light: .secondarySystemBackground
    )

    static let ryntraSurfaceRaised = adaptive(
        dark: UIColor(red: 0.11, green: 0.11, blue: 0.118, alpha: 1),
        light: .tertiarySystemBackground
    )

    static let ryntraSeparator = adaptive(
        dark: UIColor(red: 0.173, green: 0.173, blue: 0.18, alpha: 1),
        light: .separator
    )

    private static func adaptive(dark: UIColor, light: UIColor) -> Color {
        Color(uiColor: UIColor { traits in
            traits.userInterfaceStyle == .dark ? dark : light
        })
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
