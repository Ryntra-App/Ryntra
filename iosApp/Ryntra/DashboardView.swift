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
    @State private var presentedError: String?
    @State private var routedProjects: [String: Project] = [:]
    @State private var routedOrganizations: [String: Organization] = [:]

    enum DashboardRoute: Hashable {
        case profile
        case notifications
        case project(String)
        case organization(String)
    }

#if os(macOS)
    /// Screens pushed over the tabs. On macOS every toolbar in the view tree
    /// contributes to the same window titlebar, so stacking these as ZStack
    /// layers piles up their back buttons and leaves the covered layer's
    /// controls live — pressing the bell behind the profile opened both at
    /// once. A real navigation stack shows one screen, and one toolbar, at a
    /// time.
    @State private var path: [DashboardRoute] = []
#else
    /// Each tab owns its history, matching the way UIKit-backed tab
    /// navigation behaves. Switching tabs never destroys a half-finished flow.
    @State private var tabPaths: [RyntraDestination: [DashboardRoute]] = [:]
#endif

    var body: some View {
        withDashboardState(dashboardTabs)
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

    @ViewBuilder
    private var dashboardTabs: some View {
        if isPlatformNative {
#if os(macOS)
            dashboardTabView
#else
            adaptiveDashboardTabView
#endif
        } else {
            dashboardTabView
#if !os(macOS)
                // macOS draws the tab bar inside the window chrome, where it is
                // neither hideable nor in the way.
                .toolbar(.hidden, for: .tabBar)
#endif
                .safeAreaInset(edge: .bottom, spacing: 0) {
                    if activePath.isEmpty {
                        RyntraTabBar(selection: customTabSelection)
                            .transition(.move(edge: .bottom).combined(with: .opacity))
                    }
                }
                .animation(RyntraMotion.resolved(RyntraMotion.navigation, reduceMotion: reduceMotion), value: activePath)
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
                onAvatarTap: { push(.profile) },
                showsBackButton: !path.isEmpty,
                onBack: pop,
                showsAvatar: path.isEmpty,
                onNotificationsTap: path.isEmpty ? { push(.notifications) } : nil,
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
        case .project(let projectID): routedProjects[projectID]?.title ?? ""
        case .organization(let organizationID): routedOrganizations[organizationID]?.name ?? ""
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
            NotificationsView(onOpenProject: { openNotificationProject($0) })
        case .project(let projectID):
            if let project = routedProjects[projectID] {
                ProjectDetailView(
                    project: project,
                    isReadOnly: !isManagedProject(project)
                )
            }
        case .organization(let organizationID):
            if let organization = routedOrganizations[organizationID] {
                OrganizationDetailView(organization: organization, onProjectTap: openProject)
            }
        }
    }

    private func openOrganization(_ organization: Organization) {
        routedOrganizations[organization.id] = organization
        push(.organization(organization.id))
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
    @ViewBuilder
    private var adaptiveDashboardTabView: some View {
        if #available(iOS 18.0, *) {
            dashboardTabView
                .tabViewStyle(.sidebarAdaptable)
        } else {
            dashboardTabView
        }
    }

    private var dashboardTabView: some View {
        TabView(selection: $selection) {
            dashboardNavigationStack(for: .dashboard) {
                OverviewView(
                    dashboard: dashboard,
                    onProjectTap: { openProject($0, in: .dashboard) }
                )
            }
            .tabItem { Label(RyntraDestination.dashboard.label, systemImage: RyntraDestination.dashboard.platformSymbol) }
            .tag(RyntraDestination.dashboard)

            dashboardNavigationStack(for: .projects) {
                ProjectsView(
                    projects: dashboard.projects,
                    onProjectTap: { openProject($0, in: .projects) }
                )
            }
            .tabItem { Label(RyntraDestination.projects.label, systemImage: RyntraDestination.projects.platformSymbol) }
            .tag(RyntraDestination.projects)

            dashboardNavigationStack(for: .teams) {
                OrganizationsView(
                    organizations: dashboard.organizations,
                    onOpenOrganization: { openOrganization($0, in: .teams) }
                )
            }
            .tabItem { Label(RyntraDestination.teams.label, systemImage: RyntraDestination.teams.platformSymbol) }
            .tag(RyntraDestination.teams)

            dashboardNavigationStack(for: .analytics) {
                AnalyticsView(dashboard: dashboard, isActive: selection == .analytics)
            }
            .tabItem { Label(RyntraDestination.analytics.label, systemImage: RyntraDestination.analytics.platformSymbol) }
            .tag(RyntraDestination.analytics)
        }
    }

    private func dashboardNavigationStack<Content: View>(
        for destination: RyntraDestination,
        @ViewBuilder content: () -> Content
    ) -> some View {
        NavigationStack(path: tabPathBinding(for: destination)) {
            content()
                .ryntraChrome(
                    title: destination.label,
                    dashboard: dashboard,
                    isRefreshing: isRefreshing,
                    onAvatarTap: { push(.profile, in: destination) },
                    onNotificationsTap: { push(.notifications, in: destination) },
                    unreadNotificationCount: model.unreadNotificationCount
                )
                .navigationDestination(for: DashboardRoute.self) { route in
                    pushedScreen(route, in: destination)
                        .toolbar(.hidden, for: .tabBar)
                }
        }
    }

    @ViewBuilder
    private func pushedScreen(_ route: DashboardRoute, in destination: RyntraDestination) -> some View {
        switch route {
        case .profile:
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
                onBack: { pop(in: destination) },
                showsAvatar: false,
                usesSystemBackButton: true
            )
        case .notifications:
            NotificationsView(onOpenProject: { openNotificationProject($0, in: destination) })
                .ryntraChrome(
                    title: NSLocalizedString("Notifications", comment: "Screen title"),
                    dashboard: dashboard,
                    isRefreshing: model.isNotificationsLoading,
                    onAvatarTap: {},
                    showsBackButton: true,
                    onBack: { pop(in: destination) },
                    showsAvatar: false,
                    usesSystemBackButton: true
                )
        case .project(let projectID):
            if let project = routedProjects[projectID] {
                ProjectDetailView(project: project, isReadOnly: !isManagedProject(project))
                    .ryntraChrome(
                        title: project.title,
                        dashboard: dashboard,
                        isRefreshing: false,
                        onAvatarTap: {},
                        showsBackButton: true,
                        onBack: { pop(in: destination) },
                        showsAvatar: false,
                        usesSystemBackButton: true
                    )
            }
        case .organization(let organizationID):
            if let organization = routedOrganizations[organizationID] {
                OrganizationDetailView(
                    organization: organization,
                    onProjectTap: { openProject($0, in: destination) }
                )
                .ryntraChrome(
                    title: organization.name,
                    dashboard: dashboard,
                    isRefreshing: false,
                    onAvatarTap: {},
                    showsBackButton: true,
                    onBack: { pop(in: destination) },
                    showsAvatar: false,
                    usesSystemBackButton: true
                )
            }
        }
    }

    private func tabPathBinding(for destination: RyntraDestination) -> Binding<[DashboardRoute]> {
        Binding(
            get: { tabPaths[destination] ?? [] },
            set: { tabPaths[destination] = $0 }
        )
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

    private var activePath: [DashboardRoute] {
#if os(macOS)
        path
#else
        tabPaths[selection] ?? []
#endif
    }

    private var customTabSelection: Binding<RyntraDestination> {
        Binding(
            get: { selection },
            set: { destination in
                if destination == selection {
#if os(macOS)
                    path.removeAll()
#else
                    tabPaths[destination] = []
#endif
                } else {
                    selection = destination
                }
            }
        )
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

    private func openProject(_ project: Project) {
        routedProjects[project.id] = project
#if os(macOS)
        push(.project(project.id))
#else
        push(.project(project.id), in: selection)
#endif
    }

    private func openOrganization(_ organization: Organization, in destination: RyntraDestination) {
        routedOrganizations[organization.id] = organization
        push(.organization(organization.id), in: destination)
    }

    private func openProject(_ project: Project, in destination: RyntraDestination) {
        routedProjects[project.id] = project
        push(.project(project.id), in: destination)
    }

    private func openNotificationProject(
        _ projectReference: String,
        in destination: RyntraDestination? = nil
    ) {
        if let managed = dashboard.projects.first(where: {
            $0.id == projectReference || $0.slug == projectReference
        }) {
            if let destination {
                openProject(managed, in: destination)
            } else {
                openProject(managed)
            }
            return
        }
        Task {
            do {
                let project = try await model.loadProjectDetails(projectIdOrSlug: projectReference)
                if let destination {
                    openProject(project, in: destination)
                } else {
                    openProject(project)
                }
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

#if os(macOS)
    private func push(_ route: DashboardRoute) {
        path.append(route)
    }

    private func push(_ route: DashboardRoute, in _: RyntraDestination) {
        path.append(route)
    }

    private func pop() {
        if !path.isEmpty { path.removeLast() }
    }
#else
    private func push(_ route: DashboardRoute, in destination: RyntraDestination) {
        tabPaths[destination, default: []].append(route)
    }

    private func pop(in destination: RyntraDestination) {
        guard !(tabPaths[destination] ?? []).isEmpty else { return }
        tabPaths[destination]?.removeLast()
    }
#endif
}

private extension String {
    var normalizedProjectReference: String {
        trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
    }
}
