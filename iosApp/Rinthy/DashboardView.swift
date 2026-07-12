import RinthyShared
import SwiftUI

struct DashboardView: View {
    @EnvironmentObject private var model: AppModel

    let dashboard: Dashboard
    var isRefreshing = false
    var errorMessage: String?
    @State private var selection = RinthyDestination.dashboard
    @State private var isProfileVisible = false
    @State private var selectedProject: Project?
    @State private var presentedError: String?

    var body: some View {
        Group {
            if let selectedProject {
                NavigationStack {
                    ProjectDetailView(project: selectedProject)
                        .rinthyChrome(
                            title: selectedProject.title,
                            dashboard: dashboard,
                            isRefreshing: false,
                            onAvatarTap: {},
                            showsBackButton: true,
                            onBack: { self.selectedProject = nil },
                            showsAvatar: false
                        )
                }
            } else if isProfileVisible {
                NavigationStack {
                    AccountView(
                        account: dashboard.account,
                        projectCount: dashboard.projects.count,
                        organizationCount: dashboard.organizations.count
                    )
                    .rinthyChrome(
                        title: "Profile",
                        dashboard: dashboard,
                        isRefreshing: false,
                        onAvatarTap: {},
                        showsBackButton: true,
                        onBack: { isProfileVisible = false },
                        showsAvatar: false
                    )
                }
            } else {
                dashboardTabs
            }
        }
        .onAppear { presentedError = errorMessage }
        .onChange(of: errorMessage) { presentedError = $0 }
        .alert("Could not refresh", isPresented: errorBinding) {
            Button("Retry") { model.refresh() }
        } message: {
            Text(errorMessage ?? "")
        }
    }

    private var dashboardTabs: some View {
        TabView(selection: $selection) {
            NavigationStack {
                OverviewView(
                    dashboard: dashboard,
                    onProjectTap: { selectedProject = $0 }
                )
                    .rinthyChrome(
                        title: "Dashboard",
                        dashboard: dashboard,
                        isRefreshing: isRefreshing,
                        onAvatarTap: { isProfileVisible = true }
                    )
            }
            .tag(RinthyDestination.dashboard)

            NavigationStack {
                ProjectsView(
                    projects: dashboard.projects,
                    onProjectTap: { selectedProject = $0 }
                )
                    .rinthyChrome(
                        title: "Projects",
                        dashboard: dashboard,
                        isRefreshing: isRefreshing,
                        onAvatarTap: { isProfileVisible = true }
                    )
            }
            .tag(RinthyDestination.projects)

            NavigationStack {
                OrganizationsView(organizations: dashboard.organizations)
                    .rinthyChrome(
                        title: "Teams",
                        dashboard: dashboard,
                        isRefreshing: isRefreshing,
                        onAvatarTap: { isProfileVisible = true }
                    )
            }
            .tag(RinthyDestination.teams)

            NavigationStack {
                AnalyticsView(dashboard: dashboard)
                    .rinthyChrome(
                        title: "Analytics",
                        dashboard: dashboard,
                        isRefreshing: isRefreshing,
                        onAvatarTap: { isProfileVisible = true }
                    )
            }
            .tag(RinthyDestination.analytics)
        }
        .toolbar(.hidden, for: .tabBar)
        .overlay(alignment: .bottom) {
            RinthyTabBar(selection: $selection)
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
        onAvatarTap: @escaping () -> Void,
        showsBackButton: Bool = false,
        onBack: @escaping () -> Void = {},
        showsAvatar: Bool = true
    ) -> some View {
        toolbar(.hidden, for: .navigationBar)
            .safeAreaInset(edge: .top, spacing: 0) {
                RinthyTopBar(
                    title: title,
                    avatarURL: dashboard.account.avatarUrl,
                    username: dashboard.account.username,
                    isRefreshing: isRefreshing,
                    onAvatarTap: onAvatarTap,
                    showsBackButton: showsBackButton,
                    onBack: onBack,
                    showsAvatar: showsAvatar
                )
            }
    }
}
