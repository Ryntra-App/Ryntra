import Foundation
import UserNotifications

@MainActor
final class RemoteNotificationRegistration {
    static let shared = RemoteNotificationRegistration()

    private let tokenKey = "apnsDeviceToken"
    private var pending: (id: UUID, continuation: CheckedContinuation<String, Error>)?

    private init() {}

    func requestToken() async throws -> String {
        let granted = try await UNUserNotificationCenter.current()
            .requestAuthorization(options: [.alert, .sound, .badge])
        guard granted else { throw RegistrationError.permissionDenied }
        if let token = UserDefaults.standard.string(forKey: tokenKey), !token.isEmpty {
            ryntraRegisterForRemoteNotifications()
            return token
        }

        let requestID = UUID()
        return try await withCheckedThrowingContinuation { continuation in
            pending = (requestID, continuation)
            ryntraRegisterForRemoteNotifications()
            Task {
                try? await Task.sleep(for: .seconds(15))
                guard pending?.id == requestID else { return }
                pending?.continuation.resume(throwing: RegistrationError.timedOut)
                pending = nil
            }
        }
    }

    func didRegister(deviceToken: Data) {
        let token = deviceToken.map { String(format: "%02x", $0) }.joined()
        UserDefaults.standard.set(token, forKey: tokenKey)
        pending?.continuation.resume(returning: token)
        pending = nil
    }

    func didFail(error: Error) {
        pending?.continuation.resume(throwing: error)
        pending = nil
    }

    enum RegistrationError: LocalizedError {
        case permissionDenied
        case timedOut

        var errorDescription: String? {
            switch self {
            case .permissionDenied:
                return NSLocalizedString("Notification permission was not granted.", comment: "Push setup error")
            case .timedOut:
                return NSLocalizedString("APNs registration timed out. Try again.", comment: "Push setup error")
            }
        }
    }
}
