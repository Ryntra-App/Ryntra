import RyntraShared
import SwiftUI
import UIKit

struct RootView: View {
    @EnvironmentObject private var model: AppModel

    var body: some View {
        ZStack {
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
        .task { model.checkForUpdates() }
        .sheet(
            isPresented: Binding(
                get: { model.appUpdate != nil },
                set: { isPresented in
                    if !isPresented { model.dismissAppUpdate() }
                }
            )
        ) {
            if let update = model.appUpdate {
                AppUpdateView(update: update) {
                    let destination = update.downloadUrl ?? update.releaseUrl
                    if let url = URL(string: destination) {
                        UIApplication.shared.open(url)
                    }
                    model.dismissAppUpdate()
                } onDismiss: {
                    model.dismissAppUpdate()
                }
            }
        }
    }
}

private struct AppUpdateView: View {
    let update: AppUpdate
    let onDownload: () -> Void
    let onDismiss: () -> Void

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 18) {
                    Label("New version available", systemImage: "arrow.down.circle.fill")
                        .font(.title3.weight(.semibold))
                        .foregroundStyle(Color.ryntraGreen)
                    Text("Ryntra \(update.version)")
                        .font(.largeTitle.bold())
                    if !update.notes.isEmpty {
                        Text(update.notes)
                            .foregroundStyle(.secondary)
                            .textSelection(.enabled)
                    } else {
                        Text("A new release is ready to download.")
                            .foregroundStyle(.secondary)
                    }
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(20)
            }
            .navigationTitle("Update available")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Later", action: onDismiss)
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Download", action: onDownload)
                }
            }
        }
    }
}
