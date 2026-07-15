import RyntraShared
import SwiftUI

struct DashboardView: View {
    @EnvironmentObject private var model: AppModel
    @Environment(\.accessibilityReduceMotion) private var systemReduceMotion
    @AppStorage("reduceMotion") private var appReduceMotion = false
    @AppStorage("themeStyle") private var storedThemeStyle = RyntraThemeStyle.platform.rawValue

    let dashboard: Dashboard
    var isRefreshing = false
    var errorMessage: String?
    @State private var selection = RyntraDestination.dashboard
    @State private var isProfileVisible = false
    @State private var selectedProject: Project?
    @State private var presentedError: String?

    var body: some View {
        ZStack {
            dashboardTabs

            if isProfileVisible {
                NavigationStack {
                    AccountView(
                        account: dashboard.account,
                        projectCount: dashboard.projects.count,
                        organizationCount: dashboard.organizations.count
                    )
                    .ryntraChrome(
                        title: "Profile",
                        dashboard: dashboard,
                        isRefreshing: false,
                        onAvatarTap: {},
                        showsBackButton: true,
                        onBack: { isProfileVisible = false },
                        showsAvatar: false
                    )
                }
                .zIndex(1)
                .transition(RyntraMotion.navigationTransition(reduceMotion: reduceMotion))
            }

            if let selectedProject {
                NavigationStack {
                    ProjectDetailView(project: selectedProject)
                        .ryntraChrome(
                            title: selectedProject.title,
                            dashboard: dashboard,
                            isRefreshing: false,
                            onAvatarTap: {},
                            showsBackButton: true,
                            onBack: { self.selectedProject = nil },
                            showsAvatar: false
                        )
                }
                .zIndex(2)
                .transition(RyntraMotion.navigationTransition(reduceMotion: reduceMotion))
            }
        }
        .animation(RyntraMotion.resolved(.navigation, reduceMotion: reduceMotion), value: selectedProject?.id)
        .animation(RyntraMotion.resolved(.navigation, reduceMotion: reduceMotion), value: isProfileVisible)
        .onAppear { presentedError = errorMessage }
        .onChange(of: errorMessage) { presentedError = $0 }
        .alert("Could not refresh", isPresented: errorBinding) {
            Button("Retry") { model.refresh() }
        } message: {
            Text(errorMessage ?? "")
        }
    }

    @ViewBuilder
    private var dashboardTabs: some View {
        if isPlatformNative {
            dashboardTabView
        } else {
            dashboardTabView
                .toolbar(.hidden, for: .tabBar)
                .overlay(alignment: .bottom) {
                    RyntraTabBar(selection: $selection)
                }
        }
    }

    private var dashboardTabView: some View {
        TabView(selection: $selection) {
            NavigationStack {
                OverviewView(
                    dashboard: dashboard,
                    onProjectTap: { selectedProject = $0 }
                )
                    .ryntraChrome(
                        title: RyntraDestination.dashboard.label,
                        dashboard: dashboard,
                        isRefreshing: isRefreshing,
                        onAvatarTap: { isProfileVisible = true }
                    )
            }
            .tabItem { Label(RyntraDestination.dashboard.label, systemImage: RyntraDestination.dashboard.platformSymbol) }
            .tag(RyntraDestination.dashboard)

            NavigationStack {
                ProjectsView(
                    projects: dashboard.projects,
                    onProjectTap: { selectedProject = $0 }
                )
                    .ryntraChrome(
                        title: RyntraDestination.projects.label,
                        dashboard: dashboard,
                        isRefreshing: isRefreshing,
                        onAvatarTap: { isProfileVisible = true }
                    )
            }
            .tabItem { Label(RyntraDestination.projects.label, systemImage: RyntraDestination.projects.platformSymbol) }
            .tag(RyntraDestination.projects)

            NavigationStack {
                OrganizationsView(organizations: dashboard.organizations)
                    .ryntraChrome(
                        title: RyntraDestination.teams.label,
                        dashboard: dashboard,
                        isRefreshing: isRefreshing,
                        onAvatarTap: { isProfileVisible = true }
                    )
            }
            .tabItem { Label(RyntraDestination.teams.label, systemImage: RyntraDestination.teams.platformSymbol) }
            .tag(RyntraDestination.teams)

            NavigationStack {
                AnalyticsView(dashboard: dashboard, isActive: selection == .analytics)
                    .ryntraChrome(
                        title: RyntraDestination.analytics.label,
                        dashboard: dashboard,
                        isRefreshing: isRefreshing,
                        onAvatarTap: { isProfileVisible = true }
                    )
            }
            .tabItem { Label(RyntraDestination.analytics.label, systemImage: RyntraDestination.analytics.platformSymbol) }
            .tag(RyntraDestination.analytics)
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

    private var reduceMotion: Bool {
        systemReduceMotion || appReduceMotion
    }

    private var isPlatformNative: Bool {
        storedThemeStyle == RyntraThemeStyle.platform.rawValue
    }
}

private extension View {
    func ryntraChrome(
        title: String,
        dashboard: Dashboard,
        isRefreshing: Bool,
        onAvatarTap: @escaping () -> Void,
        showsBackButton: Bool = false,
        onBack: @escaping () -> Void = {},
        showsAvatar: Bool = true
    ) -> some View {
        modifier(
            RyntraChromeModifier(
                title: title,
                dashboard: dashboard,
                isRefreshing: isRefreshing,
                onAvatarTap: onAvatarTap,
                showsBackButton: showsBackButton,
                onBack: onBack,
                showsAvatar: showsAvatar
            )
        )
    }
}

private struct RyntraChromeModifier: ViewModifier {
    @AppStorage("themeStyle") private var storedThemeStyle = RyntraThemeStyle.platform.rawValue

    let title: String
    let dashboard: Dashboard
    let isRefreshing: Bool
    let onAvatarTap: () -> Void
    let showsBackButton: Bool
    let onBack: () -> Void
    let showsAvatar: Bool

    @ViewBuilder
    func body(content: Content) -> some View {
        if storedThemeStyle == RyntraThemeStyle.platform.rawValue {
            content
                .navigationTitle(title)
                .navigationBarTitleDisplayMode(showsBackButton ? .inline : .large)
                .toolbar {
                    if showsBackButton {
                        ToolbarItem(placement: .navigationBarLeading) {
                            Button(action: onBack) {
                                Image(systemName: "chevron.left")
                            }
                            .accessibilityLabel("Back")
                        }
                    }
                    ToolbarItemGroup(placement: .navigationBarTrailing) {
                        if isRefreshing { ProgressView() }
                        if showsAvatar {
                            Button(action: onAvatarTap) {
                                AsyncImage(url: URL(string: dashboard.account.avatarUrl ?? "")) { image in
                                    image.resizable().scaledToFill()
                                } placeholder: {
                                    Circle().fill(.quaternary)
                                }
                                .frame(width: 32, height: 32)
                                .clipShape(Circle())
                            }
                            .accessibilityLabel("Open \(dashboard.account.username)'s account")
                        }
                    }
                }
        } else {
            content
                .toolbar(.hidden, for: .navigationBar)
                .safeAreaInset(edge: .top, spacing: 0) {
                    RyntraTopBar(
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
}
