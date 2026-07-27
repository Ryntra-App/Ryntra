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
#if os(macOS)
    /// Screens pushed over the tabs. On macOS every toolbar in the view tree
    /// contributes to the same window titlebar, so stacking these as ZStack
    /// layers piles up their back buttons and leaves the covered layer's
    /// controls live — pressing the bell behind the profile opened both at
    /// once. A real navigation stack shows one screen, and one toolbar, at a
    /// time.
    @State private var path: [DashboardRoute] = []

    enum DashboardRoute: Hashable {
        case profile
        case notifications
        case project
        case organization
    }

    @State private var selectedOrganization: Organization?
#endif

    var body: some View {
#if os(macOS)
        withDashboardState(dashboardTabs)
#else
        withDashboardState(overlayStack)
#endif
    }

    /// State wiring shared by both navigation styles.
    private func withDashboardState(_ content: some View) -> some View {
        content
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

#if !os(macOS)
    private var overlayStack: some View {
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
        .animation(RyntraMotion.resolved(RyntraMotion.navigation, reduceMotion: reduceMotion), value: selectedProject?.id)
        .animation(RyntraMotion.resolved(RyntraMotion.navigation, reduceMotion: reduceMotion), value: isProfileVisible)
        .animation(RyntraMotion.resolved(RyntraMotion.navigation, reduceMotion: reduceMotion), value: isNotificationsVisible)
    }
#endif

    @ViewBuilder
    private var dashboardTabs: some View {
        if isPlatformNative {
            dashboardTabView
        } else {
            dashboardTabView
#if !os(macOS)
                // macOS draws the tab bar inside the window chrome, where it is
                // neither hideable nor in the way.
                .toolbar(.hidden, for: .tabBar)
#endif
                .overlay(alignment: .bottom) {
                    RyntraTabBar(selection: $selection)
                }
        }
    }

#if os(macOS)
    /// macOS hoists TabView's tabs into the titlebar but swaps their content
    /// with no transition, and a transition applied inside TabView is ignored —
    /// the best it allows is fading the whole thing out and back in, which
    /// reads as a flash. Driving the content directly and putting the same
    /// segmented control in the toolbar keeps the titlebar tabs while making a
    /// real cross-fade possible.
    private var dashboardTabView: some View {
        NavigationStack {
            ZStack {
                if let route = path.last {
                    pushedScreen(route)
                        .transition(RyntraMotion.navigationTransition(reduceMotion: reduceMotion))
                } else {
                    selectedTab
                }
            }
            .animation(RyntraMotion.resolved(RyntraMotion.navigation, reduceMotion: reduceMotion), value: selection)
            .animation(RyntraMotion.resolved(RyntraMotion.navigation, reduceMotion: reduceMotion), value: path)
            // One chrome for the whole stack. The pushed screens contribute
            // content only — if each carried its own toolbar, macOS would merge
            // them into the single window titlebar and stack up back buttons.
            .ryntraChrome(
                title: path.isEmpty ? selection.label : pushedTitle,
                dashboard: dashboard,
                isRefreshing: isRefreshing,
                onAvatarTap: { path = [.profile] },
                showsBackButton: !path.isEmpty,
                onBack: { path.removeAll() },
                showsAvatar: path.isEmpty,
                onNotificationsTap: path.isEmpty ? { path = [.notifications] } : nil,
                unreadNotificationCount: model.unreadNotificationCount,
                windowTitle: "Ryntra"
            )
            .toolbar {
                if path.isEmpty {
                    ToolbarItem(placement: .principal) {
                        Picker("", selection: tabSelection) {
                            ForEach(RyntraDestination.allCases, id: \.self) { destination in
                                Text(destination.label).tag(RyntraDestination?.some(destination))
                            }
                        }
                        .pickerStyle(.segmented)
                        .labelsHidden()
                    }
                }
                // Macs have no pull-to-refresh gesture, so `refreshable` on the
                // screens below is unreachable here — this is the only way to
                // reload by hand.
                ToolbarItem(placement: .ryntraTrailing) {
                    Button(action: refreshAll) {
                        Label(NSLocalizedString("Refresh", comment: "Toolbar action"), systemImage: "arrow.clockwise")
                    }
                    .keyboardShortcut("r", modifiers: .command)
                    .disabled(isRefreshing)
                    .help(NSLocalizedString("Refresh", comment: "Toolbar action"))
                }
            }
        }
    }

    private var pushedTitle: String {
        switch path.last {
        case .profile: "Profile"
        case .notifications: NSLocalizedString("Notifications", comment: "Screen title")
        case .project: selectedProject?.title ?? ""
        case .organization: selectedOrganization?.name ?? ""
        case nil: selection.label
        }
    }

    /// Screens reached from the tabs, supplying content only.
    @ViewBuilder
    private func pushedScreen(_ route: DashboardRoute) -> some View {
        switch route {
        case .profile:
            AccountView(
                account: dashboard.account,
                projectCount: dashboard.projects.count,
                organizationCount: dashboard.organizations.count
            )
        case .notifications:
            NotificationsView(onOpenProject: openNotificationProject)
        case .project:
            if let selectedProject {
                ProjectDetailView(
                    project: selectedProject,
                    isReadOnly: !isManagedProject(selectedProject)
                )
            }
        case .organization:
            if let selectedOrganization {
                OrganizationDetailView(organization: selectedOrganization)
            }
        }
    }

    private func openOrganization(_ organization: Organization) {
        selectedOrganization = organization
        path = [.organization]
    }

    /// Reloads whatever is on screen. Notifications have their own endpoint and
    /// are not part of the dashboard payload.
    private func refreshAll() {
        if path.last == .notifications {
            Task { await model.refreshNotifications() }
        } else {
            model.refresh()
        }
    }

    /// Selection for the titlebar tabs, reported as nil while a screen is
    /// pushed. Picking the tab that is already current would otherwise be no
    /// change at all, so the setter would never run and the pushed screen would
    /// stay put — the case of opening Settings from Dashboard and then pressing
    /// Dashboard again. Reporting nil also stops a tab from looking active
    /// while its content is covered.
    private var tabSelection: Binding<RyntraDestination?> {
        Binding(
            get: { path.isEmpty ? selection : nil },
            set: { destination in
                guard let destination else { return }
                path.removeAll()
                selection = destination
            }
        )
    }

    @ViewBuilder
    private var selectedTab: some View {
        switch selection {
        case .dashboard:
            OverviewView(dashboard: dashboard, onProjectTap: openProject)
                .transition(.opacity)
        case .projects:
            ProjectsView(projects: dashboard.projects, onProjectTap: openProject)
                .transition(.opacity)
        case .teams:
            OrganizationsView(
                organizations: dashboard.organizations,
                onOpenOrganization: openOrganization
            )
            .transition(.opacity)
        case .analytics:
            AnalyticsView(dashboard: dashboard, isActive: true)
                .transition(.opacity)
        }
    }
#else
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
#endif

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
        if dashboard.projects.contains(where: { managed in
            managed.id == project.id || (!(managed.slug?.isEmpty ?? true) && managed.slug == project.slug)
        }) {
            return true
        }
        guard let reference = project.organization?.normalizedProjectReference, !reference.isEmpty else {
            return false
        }
        return dashboard.organizations.contains { organization in
            [organization.id, organization.slug, organization.name]
                .map(\.normalizedProjectReference)
                .contains(reference)
        }
    }

    /// Shows a project, replacing whatever screen is currently open.
    private func openProject(_ project: Project) {
        selectedProject = project
#if os(macOS)
        path = [.project]
#else
        isNotificationsVisible = false
#endif
    }

    private func openNotificationProject(_ projectReference: String) {
        if let managed = dashboard.projects.first(where: {
            $0.id == projectReference || $0.slug == projectReference
        }) {
            openProject(managed)
            return
        }
        Task {
            do {
                let project = try await model.loadProjectDetails(projectIdOrSlug: projectReference)
                openProject(project)
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

private extension String {
    var normalizedProjectReference: String {
        trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
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
        unreadNotificationCount: Int = 0,
        windowTitle: String? = nil
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
                unreadNotificationCount: unreadNotificationCount,
                windowTitle: windowTitle
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
    /// Window title to use instead of `title`. Changing the navigation title
    /// makes SwiftUI rebuild the whole toolbar, and on macOS that rebuild is
    /// visible as the titlebar flickering on every tab switch. The tab screens
    /// pass a constant here so the toolbar stays put; `title` still drives the
    /// Ryntra theme's own top bar.
    var windowTitle: String?

    @ViewBuilder
    func body(content: Content) -> some View {
        if storedThemeStyle == RyntraThemeStyle.platform.rawValue {
            content
                .navigationTitle(windowTitle ?? title)
#if !os(macOS)
                .navigationBarTitleDisplayMode(showsBackButton ? .inline : .large)
#endif
                .toolbar {
                    if showsBackButton {
                        ToolbarItem(placement: .ryntraLeading) {
                            Button(action: onBack) {
                                Image(systemName: "chevron.left")
                            }
                            .accessibilityLabel("Back")
                        }
                    }
                    ToolbarItemGroup(placement: .ryntraTrailing) {
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
                                RemoteImage(url: URL(string: dashboard.account.avatarUrl ?? "")) { image in
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
#if !os(macOS)
                // On macOS the equivalent placement is the window toolbar, and
                // hiding that takes the whole titlebar — window controls
                // included — with it.
                .toolbar(.hidden, for: .navigationBar)
#endif
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
