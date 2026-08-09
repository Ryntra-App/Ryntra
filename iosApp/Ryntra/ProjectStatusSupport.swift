import Foundation
import RyntraShared
import SwiftUI

enum ProjectStatusSupport {
    static func statusLabel(_ status: String) -> String {
        switch status.lowercased() {
        case "approved": return NSLocalizedString("Approved", comment: "Project status")
        case "archived": return NSLocalizedString("Archived", comment: "Project status")
        case "rejected": return NSLocalizedString("Rejected", comment: "Project status")
        case "draft": return NSLocalizedString("Draft", comment: "Project status")
        case "unlisted": return NSLocalizedString("Unlisted", comment: "Project status")
        case "processing": return NSLocalizedString("In review", comment: "Project status")
        case "withheld": return NSLocalizedString("Withheld", comment: "Project status")
        case "scheduled": return NSLocalizedString("Scheduled", comment: "Project status")
        case "private": return NSLocalizedString("Private", comment: "Project status")
        case "unknown": return NSLocalizedString("Unknown", comment: "Project status")
        default: return status.capitalized
        }
    }

    static func typeLabel(for project: Project) -> String {
        switch project.projectType.lowercased() {
        case "mod": return NSLocalizedString("Mod", comment: "Project type")
        case "plugin": return NSLocalizedString("Plugin", comment: "Project type")
        case "hybrid": return NSLocalizedString("Mod / Plugin", comment: "Project type")
        case "modpack": return NSLocalizedString("Modpack", comment: "Project type")
        case "resourcepack", "resource_pack": return NSLocalizedString("Resource Pack", comment: "Project type")
        case "shader": return NSLocalizedString("Shader", comment: "Project type")
        case "datapack", "data_pack": return NSLocalizedString("Data Pack", comment: "Project type")
        case "server": return NSLocalizedString("Server", comment: "Project type")
        default: return project.projectType.capitalized
        }
    }

    static func attentionMessage(for project: Project) -> String {
        let state = project.status.lowercased()
        let base: String
        switch state {
        case "processing": base = NSLocalizedString("In moderation review", comment: "Attention")
        case "rejected": base = NSLocalizedString("Rejected by moderation · check the review notes", comment: "Attention")
        case "withheld": base = NSLocalizedString("Withheld from publishing · check the review notes", comment: "Attention")
        case "scheduled": base = NSLocalizedString("Publication is scheduled", comment: "Attention")
        case "draft": base = NSLocalizedString("Draft · not submitted for review yet", comment: "Attention")
        case "unlisted": base = NSLocalizedString("Unlisted · not shown in search", comment: "Attention")
        case "private": base = NSLocalizedString("Private · only visible to the team", comment: "Attention")
        case "archived": base = NSLocalizedString("Archived", comment: "Attention")
        case "approved": base = NSLocalizedString("Approved", comment: "Attention")
        default: base = NSLocalizedString("Needs a status check on Modrinth", comment: "Attention")
        }
        return base
    }
}

extension Project {
    var displayTypeLabel: String { ProjectStatusSupport.typeLabel(for: self) }
    var localizedStatusLabel: String { ProjectStatusSupport.statusLabel(status) }
    var attentionMessageText: String { ProjectStatusSupport.attentionMessage(for: self) }

    /// Public page for this project on Modrinth.
    var modrinthPageURL: URL? {
        let reference = (slug?.isEmpty ?? true) ? id : (slug ?? id)
        return URL(string: "https://modrinth.com/project/\(reference)")
    }
}

extension View {
    /// Right-click actions for a project row. Macs expect a context menu on
    /// anything clickable; on iOS the same menu appears on long press.
    func ryntraProjectContextMenu(
        _ project: Project,
        onOpen: (() -> Void)? = nil,
        canDelete: Bool = false,
        onDelete: (() -> Void)? = nil,
        onRequestDelete: (() -> Void)? = nil
    ) -> some View {
        contextMenu {
            if let onOpen {
                Button(action: onOpen) {
                    Label(
                        NSLocalizedString("Open project", comment: "Project context action"),
                        systemImage: "folder"
                    )
                }
            }
            if let url = project.modrinthPageURL {
                Button {
                    ryntraOpenExternalURL(url)
                } label: {
                    Label(
                        NSLocalizedString("Open on Modrinth", comment: "Project context action"),
                        systemImage: "arrow.up.right.square"
                    )
                }
                Button {
                    ryntraCopyToPasteboard(url.absoluteString)
                } label: {
                    Label(
                        NSLocalizedString("Copy link", comment: "Project context action"),
                        systemImage: "link"
                    )
                }
            }
            if canDelete, let onDelete {
                Divider()
                Button(role: .destructive, action: onDelete) {
                    Label(
                        NSLocalizedString("Delete this project", comment: "Project context action"),
                        systemImage: "trash"
                    )
                }
            } else if let onRequestDelete {
                Divider()
                Button(role: .destructive, action: onRequestDelete) {
                    Label(
                        NSLocalizedString("Delete this project", comment: "Project context action"),
                        systemImage: "trash"
                    )
                }
            }
        }
    }
}

struct ProjectDeleteSheet: View {
    @EnvironmentObject private var model: AppModel
    @Environment(\.dismiss) private var dismiss

    let project: Project
    var onDeleted: () -> Void = {}

    @State private var confirmation = ""
    @State private var localError: String?

    private var isConfirmed: Bool {
        confirmation.trimmingCharacters(in: .whitespacesAndNewlines) == project.title
    }

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    Label {
                        Text(NSLocalizedString(
                            "All versions and attached data will be permanently removed. Links and dependent projects may stop working.",
                            comment: "Project deletion warning"
                        ))
                    } icon: {
                        Image(systemName: "exclamationmark.triangle.fill")
                            .foregroundStyle(.red)
                    }
                }

                Section {
                    TextField(
                        NSLocalizedString("Project name", comment: "Project deletion confirmation"),
                        text: $confirmation
                    )
                    .disabled(model.isProjectActionRunning)
                } header: {
                    Text(String.localizedStringWithFormat(
                        NSLocalizedString("Type “%@” to confirm", comment: "Project deletion confirmation hint"),
                        project.title
                    ))
                }

                if let error = localError ?? model.projectActionError {
                    Section {
                        Text(error).foregroundStyle(.red)
                    }
                }

                Section {
                    Button(role: .destructive) {
                        Task { await deleteProject() }
                    } label: {
                        HStack {
                            if model.isProjectActionRunning { ProgressView().controlSize(.small) }
                            Text(model.isProjectActionRunning
                                ? NSLocalizedString("Deleting…", comment: "Project deletion progress")
                                : NSLocalizedString("Delete project permanently", comment: "Project deletion action"))
                        }
                        .frame(maxWidth: .infinity)
                    }
                    .disabled(!isConfirmed || model.isProjectActionRunning)
                }
            }
            .navigationTitle(NSLocalizedString("Delete project?", comment: "Project deletion title"))
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(NSLocalizedString("Cancel", comment: "Cancel")) { dismiss() }
                        .disabled(model.isProjectActionRunning)
                }
            }
        }
        .interactiveDismissDisabled(model.isProjectActionRunning)
        .onAppear { model.clearProjectActionStatus() }
    }

    @MainActor private func deleteProject() async {
        localError = nil
        do {
            try await model.deleteProject(project: project)
            onDeleted()
            dismiss()
        } catch {
            localError = error.localizedDescription
        }
    }
}
