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

    private let controller = AppController()
    private let keychain = KeychainTokenStore()
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
        pendingToken = token.trimmingCharacters(in: .whitespacesAndNewlines)
        controller.signIn(token: token)
    }

    func refresh() {
        controller.refresh()
    }

    func signOut() {
        pendingToken = nil
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
