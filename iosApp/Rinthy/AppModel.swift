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

    private let controller = AppController()
    private let keychain = KeychainTokenStore()
    private let oauthCoordinator = OAuthCoordinator()
    private var observation: Observation?
    private var pendingToken: String?

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

    func loadProjectDetails(project: Project) async throws -> Project {
        try await controller.loadProjectDetails(projectIdOrSlug: project.slug ?? project.id)
    }

    func loadProjectVersions(project: Project) async throws -> [ProjectVersion] {
        try await controller.loadProjectVersions(projectIdOrSlug: project.slug ?? project.id)
    }

    func signOut() {
        pendingToken = nil
        oauthError = nil
        oauthCoordinator.clear()
        keychain.clear()
        controller.signOut()
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
            if failed.previousDashboard == nil {
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
