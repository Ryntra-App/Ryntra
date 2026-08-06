import Foundation
import RyntraShared
import UserNotifications

#if os(macOS)
import AppKit
#else
import BackgroundTasks
import UIKit
#endif

final class LocalNotificationManager {
    static let shared = LocalNotificationManager()

    static let refreshIdentifier = "com.ryntra.mobile.notifications.refresh"
    static let enabledKey = "localNotificationsEnabled"

    private let knownIDsKey = "knownModrinthNotificationIDs"
    private let initialSyncKey = "modrinthNotificationInitialSync"
    private let maximumKnownIDs = 300

#if os(macOS)
    /// macOS has no BGTaskScheduler; NSBackgroundActivityScheduler owns a
    /// repeating activity and re-arms itself.
    private var backgroundActivity: NSBackgroundActivityScheduler?
#endif

    private init() {}

    func registerBackgroundTask() {
#if !os(macOS)
        BGTaskScheduler.shared.register(forTaskWithIdentifier: Self.refreshIdentifier, using: nil) { task in
            guard let refreshTask = task as? BGAppRefreshTask else {
                task.setTaskCompleted(success: false)
                return
            }
            self.handle(refreshTask)
        }
#endif
    }

    func restoreScheduleIfNeeded() {
        guard UserDefaults.standard.bool(forKey: Self.enabledKey) else { return }
        scheduleRefresh()
    }

    @MainActor
    func setEnabled(_ isEnabled: Bool) async -> Bool {
        if !isEnabled {
            UserDefaults.standard.set(false, forKey: Self.enabledKey)
            cancelRefresh()
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
#if os(macOS)
        backgroundActivity?.invalidate()
        let scheduler = NSBackgroundActivityScheduler(identifier: Self.refreshIdentifier)
        scheduler.repeats = true
        scheduler.interval = 15 * 60
        scheduler.tolerance = 5 * 60
        scheduler.qualityOfService = .utility
        scheduler.schedule { [weak self] completion in
            Task {
                _ = await self?.pollAndPresent()
                completion(.finished)
            }
        }
        backgroundActivity = scheduler
#else
        BGTaskScheduler.shared.cancel(taskRequestWithIdentifier: Self.refreshIdentifier)
        let request = BGAppRefreshTaskRequest(identifier: Self.refreshIdentifier)
        request.earliestBeginDate = Date(timeIntervalSinceNow: 15 * 60)
        try? BGTaskScheduler.shared.submit(request)
#endif
    }

    private func cancelRefresh() {
#if os(macOS)
        backgroundActivity?.invalidate()
        backgroundActivity = nil
#else
        BGTaskScheduler.shared.cancel(taskRequestWithIdentifier: Self.refreshIdentifier)
#endif
    }

#if !os(macOS)
    private func handle(_ task: BGAppRefreshTask) {
        // iOS grants one run per request, so queue the next one right away.
        scheduleRefresh()
        let operation = Task {
            let success = await pollAndPresent()
            task.setTaskCompleted(success: success)
        }
        task.expirationHandler = { operation.cancel() }
    }
#endif

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
        if !newUnread.isEmpty {
            let cachedCount = defaults.integer(forKey: "cachedUnreadNotificationCount")
            defaults.set(cachedCount + newUnread.count, forKey: "cachedUnreadNotificationCount")
        }
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

/// The app delegate protocol for the current platform.
#if os(macOS)
typealias RyntraPlatformAppDelegate = NSApplicationDelegate
#else
typealias RyntraPlatformAppDelegate = UIApplicationDelegate
#endif

final class RyntraAppDelegate: NSObject, RyntraPlatformAppDelegate, UNUserNotificationCenterDelegate {
    var onRemoteNotificationToken: ((String) -> Void)?
    var onRemoteNotificationReceived: (() -> Void)?

    /// Shared launch work behind each platform's delegate callback.
    private func finishLaunching() {
        UNUserNotificationCenter.current().delegate = self
        LocalNotificationManager.shared.registerBackgroundTask()
        LocalNotificationManager.shared.restoreScheduleIfNeeded()
    }

    private func handleDeviceToken(_ deviceToken: Data) {
        Task { @MainActor in
            RemoteNotificationRegistration.shared.didRegister(deviceToken: deviceToken)
            let token = deviceToken.map { String(format: "%02x", $0) }.joined()
            onRemoteNotificationToken?(token)
        }
    }

    private func handleRegistrationFailure(_ error: Error) {
        Task { @MainActor in RemoteNotificationRegistration.shared.didFail(error: error) }
    }

#if os(macOS)
    func applicationDidFinishLaunching(_ notification: Notification) {
        finishLaunching()
        // Launched from a build directory rather than /Applications, the app
        // otherwise comes up behind whatever was already frontmost.
        NSApplication.shared.activate(ignoringOtherApps: true)
    }

    func application(_ application: NSApplication, didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data) {
        handleDeviceToken(deviceToken)
    }

    func application(_ application: NSApplication, didFailToRegisterForRemoteNotificationsWithError error: Error) {
        handleRegistrationFailure(error)
    }
#else
    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        finishLaunching()
        return true
    }

    func application(_ application: UIApplication, didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data) {
        handleDeviceToken(deviceToken)
    }

    func application(_ application: UIApplication, didFailToRegisterForRemoteNotificationsWithError error: Error) {
        handleRegistrationFailure(error)
    }
#endif

    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification
    ) async -> UNNotificationPresentationOptions {
        onRemoteNotificationReceived?()
        return [.banner, .sound, .badge]
    }

    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse
    ) async {
        onRemoteNotificationReceived?()
        let userInfo = response.notification.request.content.userInfo
        guard let link = (userInfo["modrinthLink"] ?? userInfo["modrinth_link"]) as? String else { return }
        let path = link
            .replacingOccurrences(of: "https://modrinth.com/", with: "")
            .replacingOccurrences(of: "http://modrinth.com/", with: "")
            .trimmingCharacters(in: CharacterSet(charactersIn: "/"))
        guard let url = URL(string: "ryntra://modrinth/\(path)") else { return }
        await ryntraOpenExternalURL(url)
    }
}
