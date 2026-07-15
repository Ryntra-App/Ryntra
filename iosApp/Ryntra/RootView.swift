import RyntraShared
import SwiftUI

struct RootView: View {
    @EnvironmentObject private var model: AppModel

    var body: some View {
        switch model.state {
        case .signedOut:
            LoginView()
        case .loading(let dashboard):
            if let dashboard {
                DashboardView(dashboard: dashboard, isRefreshing: true)
            } else {
                LoginView(isLoading: true)
            }
        case .ready(let dashboard):
            DashboardView(dashboard: dashboard)
        case .failed(let message, let dashboard):
            if let dashboard {
                DashboardView(dashboard: dashboard, errorMessage: message)
            } else {
                LoginView(errorMessage: message)
            }
        }
    }
}
