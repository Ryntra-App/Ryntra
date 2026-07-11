import SwiftUI
import UIKit

@main
struct RinthyApp: App {
    @StateObject private var model = AppModel()

    var body: some Scene {
        WindowGroup {
            RootView()
                .environmentObject(model)
                .tint(Color.rinthyGreen)
                .onOpenURL(perform: model.handleOAuthCallback)
        }
    }
}

extension Color {
    static let rinthyGreen = Color(uiColor: UIColor { traits in
        traits.userInterfaceStyle == .dark
            ? UIColor(red: 0.39, green: 0.87, blue: 0.48, alpha: 1)
            : UIColor(red: 0.08, green: 0.46, blue: 0.23, alpha: 1)
    })

    static let rinthyCyan = Color(uiColor: UIColor { traits in
        traits.userInterfaceStyle == .dark
            ? UIColor(red: 0.28, green: 0.79, blue: 0.94, alpha: 1)
            : UIColor(red: 0.00, green: 0.40, blue: 0.49, alpha: 1)
    })
}
