import SwiftUI

@main
struct RyntraApp: App {
    @UIApplicationDelegateAdaptor(RyntraAppDelegate.self) private var appDelegate
    @StateObject private var model = AppModel()
    @AppStorage("themeStyle") private var storedThemeStyle = RyntraThemeStyle.platform.rawValue
    @AppStorage("appearanceMode") private var storedAppearanceMode = RyntraAppearanceMode.system.rawValue
    @AppStorage("appLanguage") private var storedAppLanguage = RyntraAppLanguage.system.rawValue

    private var appearanceMode: RyntraAppearanceMode {
        RyntraAppearanceMode(rawValue: storedAppearanceMode) ?? .system
    }

    private var appLanguage: RyntraAppLanguage {
        RyntraAppLanguage(rawValue: storedAppLanguage) ?? .system
    }

    var body: some Scene {
        WindowGroup {
            RootView()
                .environmentObject(model)
                .tint(Color.ryntraGreen)
                .preferredColorScheme(
                    storedThemeStyle == RyntraThemeStyle.ryntra.rawValue ? .dark : appearanceMode.colorScheme
                )
                .environment(\.locale, appLanguage.locale ?? Locale.current)
                .onAppear { RyntraAppLanguage.apply(storedAppLanguage) }
                .onOpenURL(perform: model.handleOAuthCallback)
        }
    }
}
