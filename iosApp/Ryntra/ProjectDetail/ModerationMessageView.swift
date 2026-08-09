import RyntraShared
import SwiftUI

struct ModerationMessageView: View {
    let message: ModerationMessage
    let author: Account?
    let isOwnMessage: Bool
    let isDeleting: Bool
    let onReply: () -> Void
    let onDelete: () -> Void

    @State private var blocks: [MarkdownBlock] = []
    @State private var confirmsDeletion = false

    var body: some View {
        Group {
            switch message.body.type {
            case "status_change":
                timelineEvent(
                    String(
                        format: NSLocalizedString("Status changed: %@ → %@", comment: "Moderation event"),
                        ProjectStatusSupport.statusLabel(message.body.oldStatus ?? "unknown"),
                        ProjectStatusSupport.statusLabel(message.body.newStatus ?? "unknown")
                    ),
                    symbol: "checkmark.circle.fill"
                )
            case "thread_closure":
                timelineEvent(
                    NSLocalizedString("The moderation thread was closed.", comment: "Moderation event"),
                    symbol: "checkmark.shield.fill"
                )
            case "deleted":
                timelineEvent(
                    NSLocalizedString("This message was deleted.", comment: "Moderation event"),
                    symbol: "trash"
                )
            default:
                textMessage
            }
        }
        .task(id: message.body.body) {
            blocks = await Task.detached(priority: .userInitiated) {
                MarkdownParser.shared.parse(markdown: message.body.body ?? "")
            }.value
        }
        .confirmationDialog(
            NSLocalizedString("Delete this reply?", comment: "Moderation delete"),
            isPresented: $confirmsDeletion,
            titleVisibility: .visible
        ) {
            Button(NSLocalizedString("Delete", comment: "Moderation delete"), role: .destructive, action: onDelete)
            Button(NSLocalizedString("Cancel", comment: "Common action"), role: .cancel) {}
        } message: {
            Text(NSLocalizedString(
                "The message will be removed from the Modrinth moderation thread.",
                comment: "Moderation delete"
            ))
        }
    }

    private var textMessage: some View {
        let isModerator = author?.role == "moderator" || author?.role == "admin"
        return VStack(alignment: .leading, spacing: 11) {
            messageHeader(isModerator: isModerator)
            messageBody
            Divider()
            messageActions
        }
        .padding(13)
        .background(
            isModerator ? Color.ryntraGreen.opacity(0.08) : Color.ryntraSurface,
            in: RoundedRectangle(cornerRadius: 11)
        )
        .overlay {
            RoundedRectangle(cornerRadius: 11)
                .stroke(Color.primary.opacity(0.10), lineWidth: 0.75)
        }
    }

    private func messageHeader(isModerator: Bool) -> some View {
        HStack(spacing: 9) {
            RemoteImage(url: URL(string: author?.avatarUrl ?? "")) { image in
                image.resizable().scaledToFill()
            } placeholder: {
                Circle().fill(Color.secondary.opacity(0.14))
            }
            .frame(width: 34, height: 34)
            .clipShape(Circle())

            VStack(alignment: .leading, spacing: 1) {
                HStack(spacing: 5) {
                    Text(author?.username ?? NSLocalizedString("Modrinth", comment: "Moderation author"))
                        .font(.subheadline.weight(.semibold))
                    if isModerator {
                        Image(systemName: "checkmark.shield.fill")
                            .font(.caption)
                            .foregroundStyle(Color.ryntraGreen)
                    }
                }
                Text(ModerationTimestampFormatter.string(from: message.created))
                    .font(.caption2)
                    .foregroundStyle(.secondary)
            }
            Spacer()
            if message.body.isPrivate {
                Text(NSLocalizedString("Private moderator message", comment: "Moderation visibility"))
                    .font(.caption2.weight(.semibold))
                    .foregroundStyle(.orange)
            }
        }
    }

    private var messageBody: some View {
        VStack(alignment: .leading, spacing: 7) {
            if blocks.isEmpty {
                Text(message.body.body ?? "").foregroundStyle(.secondary)
            } else {
                ForEach(Array(blocks.enumerated()), id: \.offset) { _, block in
                    MarkdownBlockView(block: block)
                }
            }
        }
    }

    private var messageActions: some View {
        HStack {
            Button(action: onReply) {
                Label(NSLocalizedString("Reply", comment: "Moderation action"), systemImage: "arrowshape.turn.up.left")
            }
            .buttonStyle(.borderless)
            Spacer()
            if isOwnMessage {
                Button(role: .destructive) {
                    confirmsDeletion = true
                } label: {
                    if isDeleting {
                        ProgressView().controlSize(.small)
                    } else {
                        Image(systemName: "trash").ryntraMinimumTouchTarget()
                    }
                }
                .disabled(isDeleting)
                .accessibilityLabel(NSLocalizedString("Delete reply", comment: "Moderation action"))
            }
        }
    }

    private func timelineEvent(_ text: String, symbol: String) -> some View {
        HStack(spacing: 11) {
            Image(systemName: symbol)
                .foregroundStyle(Color.ryntraGreen)
                .frame(width: 36, height: 36)
                .background(Color.ryntraGreen.opacity(0.14), in: Circle())
            VStack(alignment: .leading, spacing: 2) {
                Text(text).font(.subheadline.weight(.medium))
                Text(ModerationTimestampFormatter.string(from: message.created))
                    .font(.caption2)
                    .foregroundStyle(.secondary)
            }
            Spacer()
        }
        .padding(.vertical, 8)
    }
}

private enum ModerationTimestampFormatter {
    private static let iso8601 = ISO8601DateFormatter()

    static func string(from value: String) -> String {
        guard let date = iso8601.date(from: value) else {
            return value.components(separatedBy: "T").first ?? value
        }
        if Calendar.current.isDateInToday(date) {
            return date.formatted(date: .omitted, time: .shortened)
        }
        return date.formatted(date: .abbreviated, time: .shortened)
    }
}
