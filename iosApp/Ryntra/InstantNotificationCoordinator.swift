import Foundation
import RyntraShared
import Security

struct InstantNotificationStatus {
    var isAvailable = true
    var isConnected = false
    var isLoading = false
    var errorMessage: String?
}

@MainActor
final class InstantNotificationCoordinator {
    enum CallbackResult {
        case ignored
        case success
        case failure(String)
    }

    private let relay: NotificationRelayClient
    private let store = InstantNotificationStore()

    init() {
        relay = NotificationRelayClient(baseUrl: Self.backendURL.absoluteString)
    }

    var isConnected: Bool { store.isConnected }

    func createAuthorizationURL() async throws -> URL {
        let pushToken = try await RemoteNotificationRegistration.shared.requestToken()
        let credentials = try await ensureRegistration(pushToken: pushToken)
        let clientState = randomState()
        store.pendingState = clientState
        let enrollment = try await relay.createEnrollment(
            installationId: credentials.installationID,
            secret: credentials.secret,
            clientState: clientState
        )
        guard let url = URL(string: enrollment.authorizationUrl) else {
            throw CoordinatorError.invalidAuthorizationURL
        }
        return url
    }

    func consumeCallback(_ url: URL) -> CallbackResult {
        guard let scheme = url.scheme,
              ["ryntra", "rinthy"].contains(scheme),
              url.host == "notifications",
              url.path == "/callback" else {
            return .ignored
        }
        let components = URLComponents(url: url, resolvingAgainstBaseURL: false)
        let returnedState = components?.value(for: "state")
        let expectedState = store.pendingState
        store.pendingState = nil
        guard statesMatch(expectedState, returnedState) else {
            return .failure(NSLocalizedString("Instant notification authorization expired. Try again.", comment: "Push setup error"))
        }
        guard components?.value(for: "error") == nil,
              components?.value(for: "status") == "connected" else {
            return .failure(NSLocalizedString("Instant notification authorization was cancelled.", comment: "Push setup error"))
        }
        store.isConnected = true
        return .success
    }

    func disconnect() async throws {
        if let secret = store.secret {
            try await relay.disconnect(installationId: store.installationID, secret: secret)
        }
        store.resetRegistration()
    }

    func updatePushToken(_ pushToken: String) async {
        guard let secret = store.secret else { return }
        _ = try? await relay.registerInstallation(
            installationId: store.installationID,
            platform: "ios",
            pushToken: pushToken,
            secret: secret
        )
    }

    func close() { relay.close() }

    private func ensureRegistration(pushToken: String) async throws -> (installationID: String, secret: String) {
        if let secret = store.secret {
            do {
                _ = try await relay.registerInstallation(
                    installationId: store.installationID,
                    platform: "ios",
                    pushToken: pushToken,
                    secret: secret
                )
                return (store.installationID, secret)
            } catch let error as ApiException where error.statusCode == 401 {
                store.resetRegistration()
            }
        }

        let installationID = store.installationID
        let registration = try await relay.registerInstallation(
            installationId: installationID,
            platform: "ios",
            pushToken: pushToken,
            secret: nil
        )
        guard let secret = registration.installationSecret, !secret.isEmpty else {
            throw CoordinatorError.missingInstallationSecret
        }
        store.secret = secret
        return (installationID, secret)
    }

    private func randomState() -> String {
        var bytes = [UInt8](repeating: 0, count: 32)
        guard SecRandomCopyBytes(kSecRandomDefault, bytes.count, &bytes) == errSecSuccess else {
            return UUID().uuidString.replacingOccurrences(of: "-", with: "").lowercased()
        }
        return bytes.map { String(format: "%02x", $0) }.joined()
    }

    private func statesMatch(_ expected: String?, _ returned: String?) -> Bool {
        guard let expected, let returned else { return false }
        let left = Array(expected.utf8)
        let right = Array(returned.utf8)
        guard left.count == right.count else { return false }
        return zip(left, right).reduce(UInt8(0)) { $0 | ($1.0 ^ $1.1) } == 0
    }

    private static var backendURL: URL {
        let configured = Bundle.main.object(forInfoDictionaryKey: "RyntraBackendURL") as? String
        return URL(string: configured ?? "") ?? URL(string: "https://authrinthy.sawiq.org")!
    }

    enum CoordinatorError: LocalizedError {
        case invalidAuthorizationURL
        case missingInstallationSecret

        var errorDescription: String? {
            NSLocalizedString("The notification service returned an invalid response.", comment: "Push setup error")
        }
    }
}

private final class InstantNotificationStore {
    private let defaults = UserDefaults.standard
    private let secretStore = KeychainValueStore(
        service: "com.ryntra.mobile.notifications",
        account: "installation-secret"
    )

    var installationID: String {
        if let existing = defaults.string(forKey: Keys.installationID), !existing.isEmpty { return existing }
        let created = UUID().uuidString.replacingOccurrences(of: "-", with: "").lowercased()
        defaults.set(created, forKey: Keys.installationID)
        return created
    }

    var secret: String? {
        get { secretStore.read() }
        set {
            if let newValue { secretStore.write(newValue) } else { secretStore.clear() }
        }
    }

    var pendingState: String? {
        get { defaults.string(forKey: Keys.pendingState) }
        set { defaults.set(newValue, forKey: Keys.pendingState) }
    }

    var isConnected: Bool {
        get { defaults.bool(forKey: Keys.isConnected) }
        set { defaults.set(newValue, forKey: Keys.isConnected) }
    }

    func resetRegistration() {
        secretStore.clear()
        defaults.removeObject(forKey: Keys.installationID)
        defaults.removeObject(forKey: Keys.pendingState)
        defaults.set(false, forKey: Keys.isConnected)
    }

    private enum Keys {
        static let installationID = "instantNotificationInstallationID"
        static let pendingState = "instantNotificationOAuthState"
        static let isConnected = "instantNotificationsConnected"
    }
}

private extension URLComponents {
    func value(for name: String) -> String? {
        queryItems?.first(where: { $0.name == name })?.value
    }
}
