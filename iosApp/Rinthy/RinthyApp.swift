import SwiftUI

@main
struct RinthyApp: App {
    @StateObject private var model = AppModel()

    var body: some Scene {
        WindowGroup {
            RootView()
                .environmentObject(model)
                .tint(Color.rinthyGreen)
        }
    }
}

extension Color {
    static let rinthyGreen = Color(red: 0.06, green: 0.74, blue: 0.31)
}
