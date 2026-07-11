import RinthyShared
import SwiftUI

struct DashboardView: View {
    @EnvironmentObject private var model: AppModel

    let dashboard: Dashboard
    var isRefreshing = false
    var errorMessage: String?

    var body: some View {
        TabView {
            NavigationStack {
                OverviewView(dashboard: dashboard)
                    .navigationTitle("Rinthy")
                    .toolbar { refreshToolbar }
            }
            .tabItem { Label("Overview", systemImage: "square.grid.2x2.fill") }

            NavigationStack {
                ProjectsView(projects: dashboard.projects)
                    .navigationTitle("Projects")
                    .toolbar { refreshToolbar }
            }
            .tabItem { Label("Projects", systemImage: "shippingbox.fill") }

            NavigationStack {
                OrganizationsView(organizations: dashboard.organizations)
                    .navigationTitle("Teams")
                    .toolbar { refreshToolbar }
            }
            .tabItem { Label("Teams", systemImage: "person.3.fill") }

            NavigationStack {
                AccountView(
                    account: dashboard.account,
                    projectCount: dashboard.projects.count,
                    organizationCount: dashboard.organizations.count
                )
                .navigationTitle("Account")
            }
            .tabItem { Label("Account", systemImage: "person.crop.circle.fill") }
        }
        .alert("Could not refresh", isPresented: .constant(errorMessage != nil)) {
            Button("Retry") { model.refresh() }
        } message: {
            Text(errorMessage ?? "")
        }
    }

    @ToolbarContentBuilder
    private var refreshToolbar: some ToolbarContent {
        ToolbarItem(placement: .topBarTrailing) {
            if isRefreshing {
                ProgressView()
            } else {
                Button(action: model.refresh) {
                    Image(systemName: "arrow.clockwise")
                }
                .accessibilityLabel("Refresh")
            }
        }
    }
}
