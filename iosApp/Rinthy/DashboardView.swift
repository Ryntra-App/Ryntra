import RinthyShared
import SwiftUI

struct DashboardView: View {
    @EnvironmentObject private var model: AppModel

    let dashboard: Dashboard
    var isRefreshing = false
    var errorMessage: String?
    @State private var selection = RinthyDestination.overview
    @State private var presentedError: String?

    var body: some View {
        TabView(selection: $selection) {
            NavigationStack {
                OverviewView(dashboard: dashboard)
                    .rinthyChrome(
                        title: "Rinthy",
                        dashboard: dashboard,
                        isRefreshing: isRefreshing,
                        canRefresh: true,
                        onRefresh: model.refresh,
                        onAvatarTap: { selection = .account }
                    )
            }
            .tag(RinthyDestination.overview)

            NavigationStack {
                ProjectsView(projects: dashboard.projects)
                    .rinthyChrome(
                        title: "Projects",
                        dashboard: dashboard,
                        isRefreshing: isRefreshing,
                        canRefresh: true,
                        onRefresh: model.refresh,
                        onAvatarTap: { selection = .account }
                    )
            }
            .tag(RinthyDestination.projects)

            NavigationStack {
                OrganizationsView(organizations: dashboard.organizations)
                    .rinthyChrome(
                        title: "Teams",
                        dashboard: dashboard,
                        isRefreshing: isRefreshing,
                        canRefresh: true,
                        onRefresh: model.refresh,
                        onAvatarTap: { selection = .account }
                    )
            }
            .tag(RinthyDestination.teams)

            NavigationStack {
                AccountView(
                    account: dashboard.account,
                    projectCount: dashboard.projects.count,
                    organizationCount: dashboard.organizations.count
                )
                .rinthyChrome(
                    title: "Account",
                    dashboard: dashboard,
                    isRefreshing: false,
                    canRefresh: false,
                    onRefresh: {},
                    onAvatarTap: {}
                )
            }
            .tag(RinthyDestination.account)
        }
        .toolbar(.hidden, for: .tabBar)
        .safeAreaInset(edge: .bottom, spacing: 0) {
            RinthyTabBar(selection: $selection)
        }
        .onAppear { presentedError = errorMessage }
        .onChange(of: errorMessage) { presentedError = $0 }
        .alert("Could not refresh", isPresented: errorBinding) {
            Button("Retry") { model.refresh() }
        } message: {
            Text(errorMessage ?? "")
        }
    }

    private var errorBinding: Binding<Bool> {
        Binding(
            get: { presentedError != nil },
            set: { isPresented in
                if !isPresented { presentedError = nil }
            }
        )
    }

}

private extension View {
    func rinthyChrome(
        title: String,
        dashboard: Dashboard,
        isRefreshing: Bool,
        canRefresh: Bool,
        onRefresh: @escaping () -> Void,
        onAvatarTap: @escaping () -> Void
    ) -> some View {
        toolbar(.hidden, for: .navigationBar)
            .safeAreaInset(edge: .top, spacing: 0) {
                RinthyTopBar(
                    title: title,
                    avatarURL: dashboard.account.avatarUrl,
                    username: dashboard.account.username,
                    isRefreshing: isRefreshing,
                    canRefresh: canRefresh,
                    onRefresh: onRefresh,
                    onAvatarTap: onAvatarTap
                )
            }
    }
}
