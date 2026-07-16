import BackgroundTasks
import Foundation
import RyntraShared
import UIKit
import UserNotifications

final class LocalNotificationManager {
    static let shared = LocalNotificationManager()

    static let refreshIdentifier = "com.ryntra.mobile.notifications.refresh"
    static let enabledKey = "localNotificationsEnabled"

    private let knownIDsKey = "knownModrinthNotificationIDs"
    private let initialSyncKey = "modrinthNotificationInitialSync"
    private let maximumKnownIDs = 300

    private init() {}

    func registerBackgroundTask() {
        BGTaskScheduler.shared.register(forTaskWithIdentifier: Self.refreshIdentifier, using: nil) { task in
            guard let refreshTask = task as? BGAppRefreshTask else {
                task.setTaskCompleted(success: false)
                return
            }
            self.handle(refreshTask)
        }
    }

    func restoreScheduleIfNeeded() {
        guard UserDefaults.standard.bool(forKey: Self.enabledKey) else { return }
        scheduleRefresh()
    }

    @MainActor
    func setEnabled(_ isEnabled: Bool) async -> Bool {
        if !isEnabled {
            UserDefaults.standard.set(false, forKey: Self.enabledKey)
            BGTaskScheduler.shared.cancel(taskRequestWithIdentifier: Self.refreshIdentifier)
            clearHistory()
            return false
        }

        do {
            let granted = try await UNUserNotificationCenter.current()
                .requestAuthorization(options: [.alert, .sound, .badge])
            UserDefaults.standard.set(granted, forKey: Self.enabledKey)
            if granted {
                scheduleRefresh()
                _ = await pollAndPresent()
            }
            return granted
        } catch {
            UserDefaults.standard.set(false, forKey: Self.enabledKey)
            return false
        }
    }

    func scheduleRefresh() {
        BGTaskScheduler.shared.cancel(taskRequestWithIdentifier: Self.refreshIdentifier)
        let request = BGAppRefreshTaskRequest(identifier: Self.refreshIdentifier)
        request.earliestBeginDate = Date(timeIntervalSinceNow: 15 * 60)
        try? BGTaskScheduler.shared.submit(request)
    }

    private func handle(_ task: BGAppRefreshTask) {
        scheduleRefresh()
        let operation = Task {
            let success = await pollAndPresent()
            task.setTaskCompleted(success: success)
        }
        task.expirationHandler = { operation.cancel() }
    }

    private func pollAndPresent() async -> Bool {
        guard !Task.isCancelled, let token = KeychainTokenStore().read() else { return true }
        let client = NotificationPollingClient()
        defer { client.close() }
        do {
            let notifications = try await client.load(token: token)
            guard !Task.isCancelled else { return false }
            await presentNewNotifications(notifications)
            return true
        } catch {
            return false
        }
    }

    private func presentNewNotifications(_ notifications: [ModrinthNotification]) async {
        let defaults = UserDefaults.standard
        let currentIDs = notifications.map(\.id)
        guard defaults.bool(forKey: initialSyncKey) else {
            updateKnownIDs(currentIDs)
            defaults.set(true, forKey: initialSyncKey)
            return
        }

        let knownIDs = Set(defaults.stringArray(forKey: knownIDsKey) ?? [])
        let newUnread = notifications.filter { !$0.read && !knownIDs.contains($0.id) }.prefix(5)
        for notification in newUnread {
            let localized = notification.localizedContent
            let content = UNMutableNotificationContent()
            content.title = localized.title
            content.body = localized.body
            content.sound = .default
            content.userInfo = ["modrinthLink": notification.link]
            let request = UNNotificationRequest(identifier: notification.id, content: content, trigger: nil)
            try? await UNUserNotificationCenter.current().add(request)
        }
        updateKnownIDs(Array(Set(currentIDs).union(knownIDs)))
    }

    private func updateKnownIDs(_ ids: [String]) {
        UserDefaults.standard.set(Array(ids.prefix(maximumKnownIDs)), forKey: knownIDsKey)
    }

    private func clearHistory() {
        UserDefaults.standard.removeObject(forKey: knownIDsKey)
        UserDefaults.standard.removeObject(forKey: initialSyncKey)
    }
}

final class RyntraAppDelegate: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate {
    var onRemoteNotificationToken: ((String) -> Void)?

    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        UNUserNotificationCenter.current().delegate = self
        LocalNotificationManager.shared.registerBackgroundTask()
        LocalNotificationManager.shared.restoreScheduleIfNeeded()
        return true
    }

    func application(_ application: UIApplication, didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data) {
        Task { @MainActor in
            RemoteNotificationRegistration.shared.didRegister(deviceToken: deviceToken)
            let token = deviceToken.map { String(format: "%02x", $0) }.joined()
            onRemoteNotificationToken?(token)
        }
    }

    func application(_ application: UIApplication, didFailToRegisterForRemoteNotificationsWithError error: Error) {
        Task { @MainActor in RemoteNotificationRegistration.shared.didFail(error: error) }
    }

    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification
    ) async -> UNNotificationPresentationOptions {
        [.banner, .sound, .badge]
    }

    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse
    ) async {
        let userInfo = response.notification.request.content.userInfo
        guard let link = (userInfo["modrinthLink"] ?? userInfo["modrinth_link"]) as? String else { return }
        let rawURL = link.hasPrefix("http") ? link : "https://modrinth.com/\(link.trimmingCharacters(in: CharacterSet(charactersIn: "/")))"
        guard let url = URL(string: rawURL) else { return }
        await UIApplication.shared.open(url)
    }
}
