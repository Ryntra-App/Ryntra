import Foundation
import Security

struct KeychainTokenStore {
    private let storage = KeychainValueStore(
        service: "com.ryntra.mobile.session",
        account: "modrinth-access-token"
    )

    func read() -> String? { storage.read() }

    func write(_ token: String) { storage.write(token) }

    func clear() { storage.clear() }
}

struct KeychainValueStore {
    let service: String
    let account: String

    func read() -> String? {
        var query = baseQuery
        query[kSecReturnData as String] = true
        query[kSecMatchLimit as String] = kSecMatchLimitOne

        var result: AnyObject?
        guard SecItemCopyMatching(query as CFDictionary, &result) == errSecSuccess,
              let data = result as? Data else {
            return nil
        }
        return String(data: data, encoding: .utf8)
    }

    func write(_ value: String) {
        clear()
        var query = baseQuery
        query[kSecValueData as String] = Data(value.utf8)
        query[kSecAttrAccessible as String] = kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
        let status = SecItemAdd(query as CFDictionary, nil)
        if status != errSecSuccess {
            NSLog("[Ryntra] keychain write failed: OSStatus %d", status)
        }
    }

    func clear() {
        SecItemDelete(baseQuery as CFDictionary)
    }

    private var baseQuery: [String: Any] {
        // macOS keeps the legacy file-based keychain here. The modern
        // data-protection keychain would avoid its password prompts, but it
        // requires a keychain-access-group entitlement derived from a Team ID,
        // and without one every write fails with errSecMissingEntitlement
        // (-34018). Signing the app with a real team is what removes the
        // prompts: they appear because an ad-hoc signature changes on every
        // rebuild, so each build looks like a different app to the ACL.
        [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: account,
        ]
    }
}
