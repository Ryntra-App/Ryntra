import RyntraShared
import SwiftUI

struct ProjectModerationView: View {
    @EnvironmentObject private var model: AppModel

    let project: Project
    let currentUserID: String?
    let versionCount: Int32
    let canSubmitProject: Bool
    let isSubmittingProject: Bool
    let submissionError: String?
    let onSubmitProject: () -> Void

    @State private var thread: ModerationThread?
    @State private var isLoading = false
    @State private var isSending = false
    @State private var deletingMessageID: String?
    @State private var errorMessage: String?
    @State private var draft = ""
    @State private var isPreviewing = false
    @State private var previewBlocks: [MarkdownBlock] = []
    @State private var replyingToMessageID: String?

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            submissionStatus
            header

            if project.threadId?.isEmpty != false {
                emptyState(
                    title: "No moderation thread",
                    message: "A thread appears after the project is submitted for review."
                )
            } else if isLoading && thread == nil {
                ProgressView()
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 44)
            } else {
                if let errorMessage {
                    errorCard(errorMessage)
                }
                if let thread {
                    messages(thread)
                    if hasModerationSubmission {
                        composer(thread)
                    }
                }
            }
        }
        .task(id: project.threadId) {
            await loadThread(force: false)
        }
    }

    private var submissionReadiness: ProjectSubmissionReadiness {
        project.moderationReadiness(versionCount: versionCount)
    }

    private var hasModerationSubmission: Bool {
        project.threadId?.isEmpty == false || project.queued?.isEmpty == false ||
            ["processing", "rejected", "withheld", "approved"].contains(project.status.lowercased())
    }

    private var submissionStatus: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text(NSLocalizedString("Publication", comment: "Project submission section"))
                .font(.headline)

            HStack(spacing: 8) {
                Text(NSLocalizedString("Project status", comment: "Moderation status"))
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                Text(project.localizedStatusLabel)
                    .font(.caption.weight(.semibold))
            }

            if let queued = project.queued, let submitted = moderationSubmissionDate(queued) {
                Label(
                    String.localizedStringWithFormat(
                        NSLocalizedString("Submitted %@", comment: "Moderation submission date"),
                        submitted
                    ),
                    systemImage: "clock"
                )
                .font(.caption)
                .foregroundStyle(.secondary)
            }

            if project.canSubmitForModeration() {
                if submissionReadiness.canSubmit {
                    requirementRow(
                        NSLocalizedString(
                            "This project is ready to be sent to Modrinth moderation.",
                            comment: "Project ready for review"
                        ),
                        isComplete: true
                    )

                    Button(action: onSubmitProject) {
                        if isSubmittingProject {
                            ProgressView().frame(maxWidth: .infinity)
                        } else {
                            Label(
                                project.status == "draft"
                                    ? NSLocalizedString("Submit for review", comment: "Project submission action")
                                    : NSLocalizedString("Resubmit for review", comment: "Project submission action"),
                                systemImage: "paperplane.fill"
                            )
                            .frame(maxWidth: .infinity)
                        }
                    }
                    .buttonStyle(.borderedProminent)
                    .tint(.ryntraGreen)
                    .disabled(!canSubmitProject || isSubmittingProject)
                } else {
                    Text(NSLocalizedString("Before submitting, add:", comment: "Missing submission requirements"))
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                    ForEach(submissionReadiness.missingRequirementKeys, id: \.self) { key in
                        requirementRow(submissionRequirementLabel(key), isComplete: false)
                    }
                }

                if !canSubmitProject && submissionReadiness.canSubmit {
                    Text(NSLocalizedString(
                        "You need permission to edit project details before you can submit it.",
                        comment: "Project submission permission"
                    ))
                    .font(.caption)
                    .foregroundStyle(.secondary)
                }
            } else if project.status == "processing" {
                requirementRow(
                    NSLocalizedString(
                        "This project is currently in Modrinth review.",
                        comment: "Project review status"
                    ),
                    isComplete: true
                )
                Button {} label: {
                    Label(NSLocalizedString("In review", comment: "Project status"), systemImage: "hourglass")
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.borderedProminent)
                .disabled(true)
            }

            if let submissionError {
                Text(submissionError)
                    .font(.caption)
                    .foregroundStyle(.red)
            }
        }
        .padding(.bottom, 10)
        .overlay(alignment: .bottom) {
            Divider()
        }
    }

    private func requirementRow(_ text: String, isComplete: Bool) -> some View {
        Label(text, systemImage: isComplete ? "checkmark.circle.fill" : "circle")
            .font(.subheadline)
            .foregroundStyle(isComplete ? Color.ryntraGreen : Color.secondary)
    }

    private func submissionRequirementLabel(_ key: String) -> String {
        switch key {
        case "version": return NSLocalizedString("At least one version", comment: "Submission requirement")
        case "icon": return NSLocalizedString("Project icon", comment: "Submission requirement")
        case "summary": return NSLocalizedString("Short summary", comment: "Submission requirement")
        case "description": return NSLocalizedString("Full description", comment: "Submission requirement")
        case "license": return NSLocalizedString("Project license", comment: "Submission requirement")
        default: return key
        }
    }

    private var header: some View {
        HStack(alignment: .center, spacing: 12) {
            VStack(alignment: .leading, spacing: 6) {
                Text(NSLocalizedString("Moderation messages", comment: "Moderation title"))
                    .font(.headline)
            }
            Spacer()
            Button {
                Task { await loadThread(force: true) }
            } label: {
                if isLoading {
                    ProgressView().controlSize(.small)
                } else {
                    Image(systemName: "arrow.clockwise")
                }
            }
            .buttonStyle(.borderless)
            .accessibilityLabel(NSLocalizedString("Refresh moderation", comment: "Moderation action"))
            .disabled(isLoading)
        }
    }

    @ViewBuilder
    private func messages(_ thread: ModerationThread) -> some View {
        let messages = thread.messages.sorted { $0.created < $1.created }
        if messages.isEmpty {
            emptyState(
                title: "No moderation messages yet",
                message: "Updates from the review team and your replies will appear here."
            )
        } else {
            ForEach(messages, id: \.id) { message in
                ModerationMessageView(
                    message: message,
                    author: thread.members.first { $0.id == message.authorId },
                    isOwnMessage: message.authorId == currentUserID,
                    isDeleting: deletingMessageID == message.id,
                    onReply: { replyingToMessageID = message.id },
                    onDelete: { Task { await deleteMessage(message.id) } }
                )
            }
        }
    }

    private func composer(_ thread: ModerationThread) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            if let target = thread.messages.first(where: { $0.id == replyingToMessageID }) {
                let username = thread.members.first(where: { $0.id == target.authorId })?.username
                    ?? NSLocalizedString("Moderator", comment: "Moderation author")
                HStack {
                    Text(String(
                        format: NSLocalizedString("Replying to %@", comment: "Moderation reply target"),
                        username
                    ))
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(Color.ryntraGreen)
                    Spacer()
                    Button {
                        replyingToMessageID = nil
                    } label: {
                        Image(systemName: "xmark")
                    }
                    .accessibilityLabel(NSLocalizedString("Cancel reply", comment: "Moderation action"))
                }
                .padding(.horizontal, 12)
                .frame(minHeight: 40)
                .background(Color.ryntraGreen.opacity(0.10), in: RoundedRectangle(cornerRadius: 8))
            }

            Picker("", selection: $isPreviewing) {
                Text(NSLocalizedString("Write", comment: "Markdown editor")).tag(false)
                Text(NSLocalizedString("Preview", comment: "Markdown editor")).tag(true)
            }
            .pickerStyle(.segmented)

            if isPreviewing {
                VStack(alignment: .leading, spacing: 8) {
                    if previewBlocks.isEmpty {
                        Text(NSLocalizedString("Nothing to preview", comment: "Markdown editor"))
                            .foregroundStyle(.secondary)
                    } else {
                        ForEach(Array(previewBlocks.enumerated()), id: \.offset) { _, block in
                            MarkdownBlockView(block: block)
                        }
                    }
                }
                .frame(maxWidth: .infinity, minHeight: 150, alignment: .topLeading)
                .padding(12)
                .background(Color.ryntraSurface, in: RoundedRectangle(cornerRadius: 10))
            } else {
                TextField(
                    NSLocalizedString("Reply to the moderation team…", comment: "Moderation editor"),
                    text: $draft,
                    axis: .vertical
                )
                    .lineLimit(4...12)
                    .padding(12)
                    .background(Color.ryntraSurface, in: RoundedRectangle(cornerRadius: 10))
                    .onChange(of: draft) { value in
                        if value.count > 10_000 { draft = String(value.prefix(10_000)) }
                    }
            }

            Button {
                Task { await sendReply(thread.id) }
            } label: {
                if isSending {
                    ProgressView().frame(maxWidth: .infinity)
                } else {
                    Label(NSLocalizedString("Send reply", comment: "Moderation action"), systemImage: "paperplane.fill")
                        .frame(maxWidth: .infinity)
                }
            }
            .buttonStyle(.borderedProminent)
            .disabled(draft.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || isSending)
        }
        .padding(.top, 8)
        .onChange(of: isPreviewing) { previewing in
            guard previewing else { return }
            Task {
                previewBlocks = await Task.detached(priority: .userInitiated) {
                    MarkdownParser.shared.parse(markdown: draft)
                }.value
            }
        }
    }

    private func emptyState(title: String, message: String) -> some View {
        VStack(spacing: 7) {
            Text(NSLocalizedString(title, comment: "Moderation empty state"))
                .font(.headline)
            Text(NSLocalizedString(message, comment: "Moderation empty state"))
                .font(.subheadline)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 36)
    }

    private func errorCard(_ message: String) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            Text(message)
                .font(.subheadline)
            Button {
                Task { await loadThread(force: true) }
            } label: {
                Label(NSLocalizedString("Retry", comment: "Common action"), systemImage: "arrow.clockwise")
                    .frame(maxWidth: .infinity)
            }
            .buttonStyle(.bordered)
        }
        .padding(14)
        .background(Color.ryntraSurface, in: RoundedRectangle(cornerRadius: 10))
        .overlay {
            RoundedRectangle(cornerRadius: 10)
                .stroke(Color.red.opacity(0.42), lineWidth: 0.75)
        }
    }

    private func loadThread(force: Bool) async {
        guard let threadID = project.threadId, !threadID.isEmpty else { return }
        if !force, thread?.id == threadID { return }
        isLoading = true
        errorMessage = nil
        defer { isLoading = false }
        do {
            thread = try await model.loadModerationThread(threadID: threadID)
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    private func sendReply(_ threadID: String) async {
        let body = draft.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !body.isEmpty else { return }
        isSending = true
        errorMessage = nil
        defer { isSending = false }
        do {
            try await model.sendModerationReply(
                threadID: threadID,
                body: body,
                replyingTo: replyingToMessageID
            )
            thread = try await model.loadModerationThread(threadID: threadID)
            draft = ""
            previewBlocks = []
            isPreviewing = false
            replyingToMessageID = nil
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    private func deleteMessage(_ messageID: String) async {
        guard let threadID = project.threadId else { return }
        deletingMessageID = messageID
        errorMessage = nil
        defer { deletingMessageID = nil }
        do {
            try await model.deleteModerationMessage(messageID: messageID)
            thread = try await model.loadModerationThread(threadID: threadID)
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    private func moderationSubmissionDate(_ value: String) -> String? {
        guard let date = moderationISODateFormatters.lazy.compactMap({ $0.date(from: value) }).first else {
            return nil
        }
        return date.formatted(date: .abbreviated, time: .shortened)
    }
}

private let moderationISODateFormatters: [ISO8601DateFormatter] = {
    let standard = ISO8601DateFormatter()
    let fractional = ISO8601DateFormatter()
    fractional.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
    return [standard, fractional]
}()
