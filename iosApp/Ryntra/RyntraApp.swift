import SwiftUI

@main
struct RyntraApp: App {
#if os(macOS)
    @NSApplicationDelegateAdaptor(RyntraAppDelegate.self) private var appDelegate
#else
    @UIApplicationDelegateAdaptor(RyntraAppDelegate.self) private var appDelegate
#endif
    @Environment(\.scenePhase) private var scenePhase
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
#if os(macOS)
        // A single Window rather than a WindowGroup: the OAuth callback arrives
        // as a URL open, and a WindowGroup answers that by spawning a second
        // window instead of routing it to the one already signed in.
        Window("Ryntra", id: "main") {
            rootView
                .frame(minWidth: 820, minHeight: 600)
        }
        // SwiftUI's default window is small enough to clip the dashboard.
        .defaultSize(width: 1000, height: 700)
        .commands {
            // Without these the menu bar carries only SwiftUI's defaults, and
            // the app offers no keyboard route to anything.
            CommandGroup(after: .toolbar) {
                Button(NSLocalizedString("Refresh", comment: "Menu command")) {
                    model.refresh()
                }
                .keyboardShortcut("r", modifiers: .command)

                Button(NSLocalizedString("Notifications", comment: "Menu command")) {
                    Task { await model.refreshNotifications() }
                }
                .keyboardShortcut("0", modifiers: .command)
            }
            CommandGroup(replacing: .newItem) {}
        }
#else
        WindowGroup {
            rootView
        }
#endif
    }

    private var rootView: some View {
        RootView()
            .environmentObject(model)
            .tint(Color.ryntraGreen)
            .preferredColorScheme(
                storedThemeStyle == RyntraThemeStyle.ryntra.rawValue ? .dark : appearanceMode.colorScheme
            )
            .environment(\.locale, appLanguage.locale ?? Locale.current)
            .onAppear { RyntraAppLanguage.apply(storedAppLanguage) }
            .onOpenURL(perform: model.handleOpenURL)
            .task {
                appDelegate.onRemoteNotificationToken = { token in
                    Task { await model.updateInstantNotificationToken(token) }
                }
                appDelegate.onRemoteNotificationReceived = {
                    Task { await model.refreshNotifications() }
                }
            }
            .onChange(of: scenePhase) { phase in
                guard phase == .active else { return }
                Task { await model.refreshNotifications() }
            }
    }
}
