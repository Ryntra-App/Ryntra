import Foundation
import RinthyShared

@MainActor
final class AppModel: ObservableObject {
    enum ViewState {
        case signedOut
        case loading(Dashboard?)
        case ready(Dashboard)
        case failed(String, Dashboard?)
    }

    @Published private(set) var state: ViewState = .signedOut
    @Published private(set) var oauthError: String?
    @Published private(set) var isProjectSaving = false
    @Published private(set) var projectUpdateSuccess = false
    @Published private(set) var projectUpdateError: String?
    @Published private(set) var isProjectActionRunning = false
    @Published private(set) var projectActionSuccess: String?
    @Published private(set) var projectActionError: String?
    @Published private(set) var analyticsReport: AnalyticsReport?
    @Published private(set) var walletReport: WalletReport?
    @Published private(set) var analyticsRangeDays = 30
    @Published private(set) var isAnalyticsLoading = false
    @Published private(set) var analyticsError: String?
    @Published private(set) var walletError: String?

    private let controller = AppController()
    private let keychain = KeychainTokenStore()
    private let oauthCoordinator = OAuthCoordinator()
    private var observation: Observation?
    private var pendingToken: String?
    private var activeAnalyticsKey: String?

    var currentAccountID: String? {
        switch state {
        case .ready(let dashboard):
            return dashboard.account.id
        case .loading(let dashboard):
            return dashboard?.account.id
        case .failed(_, let dashboard):
            return dashboard?.account.id
        case .signedOut:
            return nil
        }
    }

    init() {
        observation = controller.observe { [weak self] sharedState in
            DispatchQueue.main.async {
                self?.receive(sharedState)
            }
        }
        if let token = keychain.read() {
            pendingToken = token
            controller.signIn(token: token)
        }
    }

    func signIn(token: String) {
        oauthError = nil
        pendingToken = token.trimmingCharacters(in: .whitespacesAndNewlines)
        controller.signIn(token: token)
    }

    func startOAuth() -> URL? {
        oauthError = nil
        return oauthCoordinator.createAuthorizationURL()
    }

    func handleOAuthCallback(_ url: URL) {
        switch oauthCoordinator.consumeCallback(url) {
        case .ignored:
            break
        case .success(let token):
            signIn(token: token)
        case .failure(let message):
            oauthError = message
        }
    }

    func refresh() {
        controller.refresh()
    }

    func loadAnalytics(projects: [Project], rangeDays: Int) async {
        guard [7, 30, 90, 180].contains(rangeDays) else { return }
        let projectIDs = projects.map(\.id)
        let requestKey = "\(rangeDays):\(projectIDs.joined(separator: ","))"
        analyticsRangeDays = rangeDays
        if activeAnalyticsKey == requestKey, analyticsReport != nil, walletReport != nil { return }
        activeAnalyticsKey = requestKey
        isAnalyticsLoading = true
        analyticsError = nil
        if walletReport == nil { walletError = nil }
        let end = Date()
        let currentStart = Calendar(identifier: .gregorian).date(byAdding: .day, value: -rangeDays, to: end) ?? end
        let comparisonStart = Calendar(identifier: .gregorian).date(byAdding: .day, value: -(rangeDays * 2), to: end) ?? end
        let formatter = ISO8601DateFormatter()

        if projectIDs.isEmpty {
            let walletResult = await fetchWalletResult()
            guard activeAnalyticsKey == requestKey else { return }
            analyticsReport = nil
            applyWalletResult(walletResult)
            isAnalyticsLoading = false
            return
        }

        let query = AnalyticsQuery(
            startTime: formatter.string(from: comparisonStart),
            endTime: formatter.string(from: end),
            slices: Int32(rangeDays * 2),
            projectIds: projectIDs,
            currentStartTime: formatter.string(from: currentStart),
            currentSlices: Int32(rangeDays)
        )
        if walletReport == nil {
            async let analyticsResult = fetchAnalyticsResult(query: query)
            async let walletResult = fetchWalletResult()
            let results = await (analyticsResult, walletResult)
            guard activeAnalyticsKey == requestKey else { return }
            applyAnalyticsResult(results.0)
            applyWalletResult(results.1)
        } else {
            let analyticsResult = await fetchAnalyticsResult(query: query)
            guard activeAnalyticsKey == requestKey else { return }
            applyAnalyticsResult(analyticsResult)
        }
        if activeAnalyticsKey == requestKey { isAnalyticsLoading = false }
    }

    private func fetchAnalyticsResult(query: AnalyticsQuery) async -> Result<AnalyticsReport, Error> {
        do {
            return .success(try await controller.loadAnalytics(query: query))
        } catch {
            return .failure(error)
        }
    }

    private func fetchWalletResult() async -> Result<WalletReport, Error> {
        do {
            return .success(try await controller.loadWallet())
        } catch {
            return .failure(error)
        }
    }

    private func applyAnalyticsResult(_ result: Result<AnalyticsReport, Error>) {
        switch result {
        case .success(let report):
            analyticsReport = report
        case .failure(let error):
            analyticsError = error.localizedDescription
        }
    }

    private func applyWalletResult(_ result: Result<WalletReport, Error>) {
        switch result {
        case .success(let wallet):
            walletReport = wallet
        case .failure(let error):
            walletError = error.localizedDescription
        }
    }

    func loadProjectDetails(project: Project) async throws -> Project {
        try await controller.loadProjectDetails(projectIdOrSlug: project.slug ?? project.id)
    }

    func loadProjectVersions(project: Project) async throws -> [ProjectVersion] {
        try await controller.loadProjectVersions(projectIdOrSlug: project.slug ?? project.id)
    }

    func loadProjectDependencies(versions: [ProjectVersion]) async throws -> [ProjectDependency] {
        try await controller.loadProjectDependencies(versions: versions)
    }

    func loadProjectMembers(project: Project) async throws -> [ProjectMember] {
        try await controller.loadProjectMembers(projectIdOrSlug: project.slug ?? project.id, teamId: project.team)
    }

    func loadProjectTeamRoster(project: Project) async throws -> ProjectTeamRoster {
        try await controller.loadProjectTeamRoster(project: project)
    }

    func loadOrganizationMembers(organization: Organization) async throws -> [ProjectMember] {
        let key = organization.slug.isEmpty ? organization.id : organization.slug
        return try await controller.loadOrganizationMembers(
            organizationIdOrSlug: key,
            teamId: organization.teamId
        )
    }

    func loadOrganizationProjects(organization: Organization) async throws -> [Project] {
        try await controller.loadOrganizationProjects(organizationIdOrSlug: organization.slug.isEmpty ? organization.id : organization.slug)
    }

    func loadOrganizationDetail(organization: Organization) async throws -> OrganizationDetail {
        try await controller.loadOrganizationDetail(organization: organization)
    }

    func updateProfile(userId: String, username: String, bio: String) async throws {
        try await controller.updateAccountProfile(
            userId: userId,
            username: username,
            bio: bio
        )
    }

    func updateProject(projectId: String, update: ProjectUpdate) async {
        projectUpdateError = nil
        isProjectSaving = true
        projectUpdateSuccess = false
        do {
            try await controller.updateProject(projectIdOrSlug: projectId, update: update)
            projectUpdateSuccess = true
        } catch {
            projectUpdateError = error.localizedDescription
        }
        isProjectSaving = false
    }

    func clearProjectUpdateStatus() {
        projectUpdateError = nil
        projectUpdateSuccess = false
    }

    func changeProjectIcon(project: Project, file: ProjectFileUpload) async throws {
        try await performProjectAction(success: "Icon updated") {
            try await controller.changeProjectIcon(projectIdOrSlug: project.id, file: file)
        }
    }

    func deleteProjectIcon(project: Project) async throws {
        try await performProjectAction(success: "Icon removed") {
            try await controller.deleteProjectIcon(projectIdOrSlug: project.id)
        }
    }

    func addGalleryImage(project: Project, file: ProjectFileUpload) async throws {
        try await performProjectAction(success: "Gallery image added") {
            try await controller.addGalleryImage(
                projectIdOrSlug: project.id,
                file: file,
                featured: false,
                title: "Gallery image",
                description: ""
            )
        }
    }

    func deleteGalleryImage(project: Project, imageURL: String) async throws {
        try await performProjectAction(success: "Gallery image removed") {
            try await controller.deleteGalleryImage(projectIdOrSlug: project.id, imageUrl: imageURL)
        }
    }

    func createVersion(project: Project, request: CreateVersionRequest) async throws {
        try await performProjectAction(success: "Version created") {
            _ = try await controller.createVersion(projectId: project.id, request: request)
        }
    }

    func updateVersion(versionID: String, update: VersionUpdate) async throws {
        try await performProjectAction(success: "Version updated") {
            try await controller.updateVersion(versionId: versionID, update: update)
        }
    }

    func deleteVersion(versionID: String) async throws {
        try await performProjectAction(success: "Version deleted") {
            try await controller.deleteVersion(versionId: versionID)
        }
    }

    func findUser(username: String) async throws -> Account? {
        try await controller.findUser(username: username)
    }

    func inviteMember(teamID: String, userID: String) async throws {
        try await performProjectAction(success: "Invitation sent") {
            try await controller.addTeamMember(teamId: teamID, userId: userID)
        }
    }

    func updateMember(teamID: String, userID: String, update: ProjectMemberUpdate) async throws {
        try await performProjectAction(success: "Member updated") {
            try await controller.updateTeamMember(teamId: teamID, userId: userID, update: update)
        }
    }

    func removeMember(teamID: String, userID: String) async throws {
        try await performProjectAction(success: "Member removed") {
            try await controller.deleteTeamMember(teamId: teamID, userId: userID)
        }
    }

    func joinTeam(teamID: String) async throws {
        try await performProjectAction(success: "Invitation accepted") {
            try await controller.joinTeam(teamId: teamID)
        }
    }

    func transferOwnership(teamID: String, userID: String) async throws {
        try await performProjectAction(success: "Ownership transferred") {
            try await controller.transferTeamOwnership(teamId: teamID, userId: userID)
        }
    }

    func clearProjectActionStatus() {
        projectActionSuccess = nil
        projectActionError = nil
    }

    func signOut() {
        pendingToken = nil
        oauthError = nil
        oauthCoordinator.clear()
        clearProjectActionStatus()
        analyticsReport = nil
        walletReport = nil
        walletError = nil
        analyticsError = nil
        activeAnalyticsKey = nil
        isAnalyticsLoading = false
        keychain.clear()
        controller.signOut()
    }

    private func performProjectAction(
        success: String,
        operation: () async throws -> Void
    ) async throws {
        projectActionError = nil
        projectActionSuccess = nil
        isProjectActionRunning = true
        defer { isProjectActionRunning = false }
        do {
            try await operation()
            projectActionSuccess = success
        } catch {
            projectActionError = error.localizedDescription
            throw error
        }
    }

    private func receive(_ sharedState: AppState) {
        switch sharedState {
        case _ as AppStateSignedOut:
            state = .signedOut
        case let loading as AppStateLoading:
            state = .loading(loading.previousDashboard)
        case let ready as AppStateReady:
            if let token = pendingToken {
                keychain.write(token)
                pendingToken = nil
            }
            state = .ready(ready.dashboard)
        case let failed as AppStateFailed:
            if failed.isAuthenticationFailure {
                keychain.clear()
                pendingToken = nil
            }
            state = .failed(failed.message, failed.previousDashboard)
        default:
            state = .failed("Unsupported application state.", nil)
        }
    }

    deinit {
        observation?.cancel()
        controller.close()
    }
}
