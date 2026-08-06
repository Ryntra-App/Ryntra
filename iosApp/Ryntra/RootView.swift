import RyntraShared
import SwiftUI

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
                        ryntraOpenExternalURL(url)
                    }
                    model.dismissAppUpdate()
                } onDismiss: {
                    model.dismissAppUpdate()
                }
                .presentationDetents([.medium, .large])
                .presentationDragIndicator(.visible)
            }
        }
    }
}

private struct AppUpdateView: View {
    let update: AppUpdate
    let onDownload: () -> Void
    let onDismiss: () -> Void
    @State private var notesBlocks: [MarkdownBlock] = []

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 18) {
                    Label("Update available", systemImage: "arrow.down.circle.fill")
                        .font(.subheadline.weight(.semibold))
                        .foregroundStyle(Color.ryntraGreen)
                    VStack(alignment: .leading, spacing: 4) {
                        Text(update.title)
                            .font(.title2.bold())
                        Text("Ryntra \(update.version)")
                            .font(.subheadline)
                            .foregroundStyle(.secondary)
                    }
                    Divider()
                    if !notesBlocks.isEmpty {
                        VStack(alignment: .leading, spacing: 10) {
                            ForEach(Array(notesBlocks.enumerated()), id: \.offset) { _, block in
                                MarkdownBlockView(block: block)
                            }
                        }
                    } else {
                        Text("A new release is ready to download.")
                            .foregroundStyle(.secondary)
                    }
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(20)
            }
            .navigationTitle("Update available")
            .ryntraInlineNavigationTitle()
            .task(id: update.notes) {
                notesBlocks = await Task.detached(priority: .userInitiated) {
                    MarkdownParser.shared.parse(markdown: update.notes)
                }.value
            }
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
