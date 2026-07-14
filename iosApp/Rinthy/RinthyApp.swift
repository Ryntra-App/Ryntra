import SwiftUI

@main
struct RinthyApp: App {
    @StateObject private var model = AppModel()
    @AppStorage("themeStyle") private var storedThemeStyle = RinthyThemeStyle.platform.rawValue
    @AppStorage("appearanceMode") private var storedAppearanceMode = RinthyAppearanceMode.system.rawValue
    @AppStorage("appLanguage") private var storedAppLanguage = RinthyAppLanguage.system.rawValue

    private var appearanceMode: RinthyAppearanceMode {
        RinthyAppearanceMode(rawValue: storedAppearanceMode) ?? .system
    }

    private var appLanguage: RinthyAppLanguage {
        RinthyAppLanguage(rawValue: storedAppLanguage) ?? .system
    }

    var body: some Scene {
        WindowGroup {
            RootView()
                .environmentObject(model)
                .tint(Color.rinthyGreen)
                .preferredColorScheme(
                    storedThemeStyle == RinthyThemeStyle.rinthy.rawValue ? .dark : appearanceMode.colorScheme
                )
                .environment(\.locale, appLanguage.locale ?? Locale.current)
                .onAppear { RinthyAppLanguage.apply(storedAppLanguage) }
                .onOpenURL(perform: model.handleOAuthCallback)
        }
    }
}
