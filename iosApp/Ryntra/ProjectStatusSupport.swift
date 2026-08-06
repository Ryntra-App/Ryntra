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
    func ryntraProjectContextMenu(_ project: Project) -> some View {
        contextMenu {
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
        }
    }
}
