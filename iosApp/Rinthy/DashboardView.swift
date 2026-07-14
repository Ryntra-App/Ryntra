import RinthyShared
import SwiftUI

struct DashboardView: View {
    @EnvironmentObject private var model: AppModel
    @Environment(\.accessibilityReduceMotion) private var systemReduceMotion
    @AppStorage("reduceMotion") private var appReduceMotion = false
    @AppStorage("themeStyle") private var storedThemeStyle = RinthyThemeStyle.platform.rawValue

    let dashboard: Dashboard
    var isRefreshing = false
    var errorMessage: String?
    @State private var selection = RinthyDestination.dashboard
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
                .zIndex(1)
                .transition(RinthyMotion.navigationTransition(reduceMotion: reduceMotion))
            }

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
                .zIndex(2)
                .transition(RinthyMotion.navigationTransition(reduceMotion: reduceMotion))
            }
        }
        .animation(RinthyMotion.resolved(.navigation, reduceMotion: reduceMotion), value: selectedProject?.id)
        .animation(RinthyMotion.resolved(.navigation, reduceMotion: reduceMotion), value: isProfileVisible)
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
                    RinthyTabBar(selection: $selection)
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
                    .rinthyChrome(
                        title: RinthyDestination.dashboard.label,
                        dashboard: dashboard,
                        isRefreshing: isRefreshing,
                        onAvatarTap: { isProfileVisible = true }
                    )
            }
            .tabItem { Label(RinthyDestination.dashboard.label, systemImage: RinthyDestination.dashboard.platformSymbol) }
            .tag(RinthyDestination.dashboard)

            NavigationStack {
                ProjectsView(
                    projects: dashboard.projects,
                    onProjectTap: { selectedProject = $0 }
                )
                    .rinthyChrome(
                        title: RinthyDestination.projects.label,
                        dashboard: dashboard,
                        isRefreshing: isRefreshing,
                        onAvatarTap: { isProfileVisible = true }
                    )
            }
            .tabItem { Label(RinthyDestination.projects.label, systemImage: RinthyDestination.projects.platformSymbol) }
            .tag(RinthyDestination.projects)

            NavigationStack {
                OrganizationsView(organizations: dashboard.organizations)
                    .rinthyChrome(
                        title: RinthyDestination.teams.label,
                        dashboard: dashboard,
                        isRefreshing: isRefreshing,
                        onAvatarTap: { isProfileVisible = true }
                    )
            }
            .tabItem { Label(RinthyDestination.teams.label, systemImage: RinthyDestination.teams.platformSymbol) }
            .tag(RinthyDestination.teams)

            NavigationStack {
                AnalyticsView(dashboard: dashboard, isActive: selection == .analytics)
                    .rinthyChrome(
                        title: RinthyDestination.analytics.label,
                        dashboard: dashboard,
                        isRefreshing: isRefreshing,
                        onAvatarTap: { isProfileVisible = true }
                    )
            }
            .tabItem { Label(RinthyDestination.analytics.label, systemImage: RinthyDestination.analytics.platformSymbol) }
            .tag(RinthyDestination.analytics)
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
        storedThemeStyle == RinthyThemeStyle.platform.rawValue
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
        modifier(
            RinthyChromeModifier(
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

private struct RinthyChromeModifier: ViewModifier {
    @AppStorage("themeStyle") private var storedThemeStyle = RinthyThemeStyle.platform.rawValue

    let title: String
    let dashboard: Dashboard
    let isRefreshing: Bool
    let onAvatarTap: () -> Void
    let showsBackButton: Bool
    let onBack: () -> Void
    let showsAvatar: Bool

    @ViewBuilder
    func body(content: Content) -> some View {
        if storedThemeStyle == RinthyThemeStyle.platform.rawValue {
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
}
