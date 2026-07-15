import Foundation
import Security

final class OAuthCoordinator {
    enum CallbackResult {
        case ignored
        case success(String)
        case failure(String)
    }

    private let defaults = UserDefaults.standard
    private let stateKey = "modrinth_oauth_state"

    func createAuthorizationURL() -> URL? {
        let state = randomState()
        defaults.set(state, forKey: stateKey)

        // The auth deployment still uses its original hostname and callback scheme.
        var components = URLComponents(string: "https://rinthy-auth.vercel.app/api/modrinth/start")
        components?.queryItems = [URLQueryItem(name: "state", value: state)]
        return components?.url
    }

    func consumeCallback(_ url: URL) -> CallbackResult {
        guard let scheme = url.scheme,
              Self.callbackSchemes.contains(scheme),
              url.host == "auth",
              url.path == "/callback" else {
            return .ignored
        }

        let expectedState = defaults.string(forKey: stateKey)
        defaults.removeObject(forKey: stateKey)
        let components = URLComponents(url: url, resolvingAgainstBaseURL: false)
        let returnedState = components?.value(for: "state")
        guard statesMatch(expectedState, returnedState) else {
            return .failure("Sign-in failed because the OAuth state did not match.")
        }

        if let error = components?.value(for: "error"), !error.isEmpty {
            return .failure("Modrinth sign-in was cancelled.")
        }

        guard let token = components?.value(for: "token")?.trimmingCharacters(in: .whitespacesAndNewlines),
              !token.isEmpty,
              token.count <= 4_096 else {
            return .failure("OAuth did not return a valid access token.")
        }
        return .success(token)
    }

    func clear() {
        defaults.removeObject(forKey: stateKey)
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
        return zip(left, right).reduce(UInt8(0)) { difference, pair in
            difference | (pair.0 ^ pair.1)
        } == 0
    }

    private static let callbackSchemes: Set<String> = ["ryntra", "rinthy"]
}

private extension URLComponents {
    func value(for name: String) -> String? {
        queryItems?.first(where: { $0.name == name })?.value
    }
}
