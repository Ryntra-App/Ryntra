import RyntraShared
import SwiftUI

struct NotificationsView: View {
    @EnvironmentObject private var model: AppModel
    @State private var isArchiveVisible = false

    private var visibleNotifications: [ModrinthNotification] {
        model.notifications.filter { isRead($0) == isArchiveVisible }
    }

    var body: some View {
        List {
            Section {
                if model.isNotificationsLoading && model.notifications.isEmpty {
                    HStack { Spacer(); ProgressView(); Spacer() }
                } else if visibleNotifications.isEmpty {
                    VStack(spacing: 10) {
                        Image(systemName: isArchiveVisible ? "archivebox" : "bell")
                            .font(.title2)
                            .foregroundStyle(.secondary)
                        Text(NSLocalizedString(
                            isArchiveVisible ? "No archived notifications" : "No new notifications",
                            comment: "Notifications empty state"
                        ))
                            .font(.headline)
                        Text(NSLocalizedString(
                            isArchiveVisible ? "Read notifications will appear here." : "Updates from Modrinth will appear here.",
                            comment: "Notifications empty hint"
                        ))
                            .font(.subheadline)
                            .foregroundStyle(.secondary)
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 34)
                } else {
                    ForEach(visibleNotifications, id: \.id) { notification in
                        Button {
                            Task { await model.markNotificationRead(notification) }
                            if let url = notification.modrinthURL {
                                UIApplication.shared.open(url)
                            }
                        } label: {
                            NotificationRow(
                                notification: notification,
                                isRead: isRead(notification)
                            )
                        }
                        .buttonStyle(.plain)
                    }
                }
            } header: {
                HStack {
                    Text(String(format: NSLocalizedString("%d unread", comment: "Notification unread count"), model.unreadNotificationCount))
                    Spacer()
                    Button {
                        withAnimation(.easeInOut(duration: 0.2)) { isArchiveVisible.toggle() }
                    } label: {
                        Image(systemName: isArchiveVisible ? "tray" : "archivebox")
                    }
                    .foregroundStyle(isArchiveVisible ? Color.ryntraGreen : .secondary)
                    .accessibilityLabel(NSLocalizedString(
                        isArchiveVisible ? "Show unread notifications" : "Show read notifications",
                        comment: "Notifications archive action"
                    ))
                    if !isArchiveVisible && model.unreadNotificationCount > 0 {
                        Button(NSLocalizedString("Mark all read", comment: "Notifications action")) {
                            Task { await model.markAllNotificationsRead() }
                        }
                    }
                }
            } footer: {
                Text(NSLocalizedString("Loaded directly from your Modrinth account.", comment: "Notification source"))
            }

            if let error = model.notificationsError {
                Section {
                    Text(error).foregroundStyle(.red)
                    Button(NSLocalizedString("Retry", comment: "Common retry")) {
                        Task { await model.refreshNotifications() }
                    }
                }
            }
        }
        .listStyle(.insetGrouped)
        .refreshable { await model.refreshNotifications() }
        .task { await model.refreshNotifications() }
    }

    private func isRead(_ notification: ModrinthNotification) -> Bool {
        notification.read || model.locallyReadNotificationIDs.contains(notification.id)
    }
}

private struct NotificationRow: View {
    let notification: ModrinthNotification
    let isRead: Bool

    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            Image(systemName: notification.symbol)
                .font(.system(size: 17, weight: .semibold))
                .foregroundStyle(Color.ryntraGreen)
                .frame(width: 34, height: 34)
                .background(Color.ryntraGreen.opacity(0.13), in: Circle())
            VStack(alignment: .leading, spacing: 4) {
                Text(notification.title.replacingOccurrences(of: "**", with: ""))
                    .font(.subheadline.weight(isRead ? .medium : .bold))
                    .foregroundStyle(.primary)
                Text(notification.text.replacingOccurrences(of: "**", with: ""))
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                    .lineLimit(4)
                Text(notification.localCreatedLabel)
                    .font(.caption2)
                    .foregroundStyle(.tertiary)
            }
            Spacer(minLength: 4)
            if !isRead {
                Circle().fill(Color.ryntraGreen).frame(width: 8, height: 8).padding(.top, 6)
            }
        }
        .contentShape(Rectangle())
    }
}

private let notificationISO8601WithFractionalSeconds: ISO8601DateFormatter = {
    let formatter = ISO8601DateFormatter()
    formatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
    return formatter
}()

private let notificationISO8601 = ISO8601DateFormatter()

private extension ModrinthNotification {
    var localCreatedLabel: String {
        guard let date = notificationISO8601WithFractionalSeconds.date(from: created)
                ?? notificationISO8601.date(from: created) else {
            return created
        }
        if Calendar.current.isDateInToday(date) {
            return date.formatted(date: .omitted, time: .shortened)
        }
        return date.formatted(date: .abbreviated, time: .shortened)
    }

    var symbol: String {
        switch type {
        case "project_update": return "shippingbox.and.arrow.backward"
        case "team_invite": return "person.badge.plus"
        case "status_change": return "checkmark.seal"
        case "moderator_message": return "text.bubble"
        default: return "bell"
        }
    }

    var modrinthURL: URL? {
        if link.hasPrefix("https://") || link.hasPrefix("http://") { return URL(string: link) }
        return URL(string: "https://modrinth.com/\(link.trimmingCharacters(in: CharacterSet(charactersIn: "/")))")
    }

}
