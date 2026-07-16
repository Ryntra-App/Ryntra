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
    @State private var isNotificationsVisible = false
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

            if isNotificationsVisible {
                NavigationStack {
                    NotificationsView(onOpenProject: openNotificationProject)
                        .ryntraChrome(
                            title: NSLocalizedString("Notifications", comment: "Screen title"),
                            dashboard: dashboard,
                            isRefreshing: model.isNotificationsLoading,
                            onAvatarTap: {},
                            showsBackButton: true,
                            onBack: { isNotificationsVisible = false },
                            showsAvatar: false
                        )
                }
                .zIndex(1)
                .transition(RyntraMotion.navigationTransition(reduceMotion: reduceMotion))
            }

            if let selectedProject {
                NavigationStack {
                    ProjectDetailView(
                        project: selectedProject,
                        isReadOnly: !isManagedProject(selectedProject)
                    )
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
        .animation(RyntraMotion.resolved(.navigation, reduceMotion: reduceMotion), value: isNotificationsVisible)
        .onAppear { presentedError = errorMessage }
        .onAppear { openPendingNotificationProject() }
        .onChange(of: errorMessage) { presentedError = $0 }
        .onChange(of: model.pendingNotificationProjectReference) { _ in openPendingNotificationProject() }
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
                        onAvatarTap: { isProfileVisible = true },
                        onNotificationsTap: { isNotificationsVisible = true },
                        unreadNotificationCount: model.unreadNotificationCount
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
                        onAvatarTap: { isProfileVisible = true },
                        onNotificationsTap: { isNotificationsVisible = true },
                        unreadNotificationCount: model.unreadNotificationCount
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
                        onAvatarTap: { isProfileVisible = true },
                        onNotificationsTap: { isNotificationsVisible = true },
                        unreadNotificationCount: model.unreadNotificationCount
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
                        onAvatarTap: { isProfileVisible = true },
                        onNotificationsTap: { isNotificationsVisible = true },
                        unreadNotificationCount: model.unreadNotificationCount
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

    private func isManagedProject(_ project: Project) -> Bool {
        dashboard.projects.contains { managed in
            managed.id == project.id || (!(managed.slug?.isEmpty ?? true) && managed.slug == project.slug)
        }
    }

    private func openNotificationProject(_ projectReference: String) {
        if let managed = dashboard.projects.first(where: {
            $0.id == projectReference || $0.slug == projectReference
        }) {
            isNotificationsVisible = false
            selectedProject = managed
            return
        }
        Task {
            do {
                let project = try await model.loadProjectDetails(projectIdOrSlug: projectReference)
                isNotificationsVisible = false
                selectedProject = project
            } catch {
                presentedError = error.localizedDescription
            }
        }
    }

    private func openPendingNotificationProject() {
        guard let reference = model.pendingNotificationProjectReference else { return }
        model.consumeNotificationProjectReference()
        openNotificationProject(reference)
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
        showsAvatar: Bool = true,
        onNotificationsTap: (() -> Void)? = nil,
        unreadNotificationCount: Int = 0
    ) -> some View {
        modifier(
            RyntraChromeModifier(
                title: title,
                dashboard: dashboard,
                isRefreshing: isRefreshing,
                onAvatarTap: onAvatarTap,
                showsBackButton: showsBackButton,
                onBack: onBack,
                showsAvatar: showsAvatar,
                onNotificationsTap: onNotificationsTap,
                unreadNotificationCount: unreadNotificationCount
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
    let onNotificationsTap: (() -> Void)?
    let unreadNotificationCount: Int

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
                        if let onNotificationsTap {
                            Button(action: onNotificationsTap) {
                                Image(systemName: "bell")
                                    .overlay(alignment: .topTrailing) {
                                        if unreadNotificationCount > 0 {
                                            Circle().fill(Color.ryntraGreen).frame(width: 7, height: 7)
                                        }
                                    }
                            }
                            .accessibilityLabel(NSLocalizedString("Notifications", comment: "Navigation action"))
                        }
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
                        showsAvatar: showsAvatar,
                        onNotificationsTap: onNotificationsTap,
                        unreadNotificationCount: unreadNotificationCount
                    )
                }
        }
    }
}
