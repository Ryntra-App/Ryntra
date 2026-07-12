import RinthyShared
import SwiftUI

struct OverviewView: View {
    @EnvironmentObject private var model: AppModel

    let dashboard: Dashboard

    private var attentionProjects: [Project] {
        Array(dashboard.projects.filter { !$0.isHealthy }.prefix(3))
    }

    private var recentProjects: [Project] {
        Array(dashboard.projects.sorted { ($0.updated ?? "") > ($1.updated ?? "") }.prefix(4))
    }

    var body: some View {
        ScrollView {
            LazyVStack(alignment: .leading, spacing: 0) {
                creatorHeader
                metricStrip
                sectionHeader(
                    "Needs attention",
                    detail: attentionProjects.isEmpty
                        ? nil
                        : "\(attentionProjects.count) open actions"
                )
                attentionContent
                sectionHeader("Recent projects", detail: "Updated across your workspace")
                recentContent
            }
            .padding(.horizontal, 16)
            .padding(.bottom, 120)
        }
        .background(Color(uiColor: .systemBackground))
        .refreshable { model.refresh() }
    }

    private var creatorHeader: some View {
        Text("Welcome back, \(dashboard.account.username)")
            .font(.subheadline)
            .foregroundStyle(.secondary)
            .padding(.top, 8)
            .padding(.bottom, 18)
    }

    private var metricStrip: some View {
        HStack(spacing: 0) {
            metric(
                "Downloads",
                value: compact(dashboard.projects.reduce(0) { $0 + $1.downloads }),
                symbol: "arrow.down"
            )
            Divider().frame(height: 52)
            metric(
                "Followers",
                value: compact(dashboard.projects.reduce(0) { $0 + $1.followers }),
                symbol: "heart"
            )
            Divider().frame(height: 52)
            metric("Projects", value: "\(dashboard.projects.count)", symbol: "shippingbox")
        }
        .padding(.vertical, 14)
        .background(Color(uiColor: .secondarySystemBackground), in: RoundedRectangle(cornerRadius: 8))
        .overlay {
            RoundedRectangle(cornerRadius: 8)
                .stroke(Color(uiColor: .separator), lineWidth: 0.5)
        }
    }

    private func metric(_ label: String, value: String, symbol: String) -> some View {
        HStack(spacing: 7) {
            Image(systemName: symbol)
                .font(.caption.weight(.semibold))
                .foregroundStyle(.secondary)
            VStack(alignment: .leading, spacing: 1) {
                Text(label)
                    .font(.caption2)
                    .foregroundStyle(.secondary)
                Text(value)
                    .font(.headline)
                    .monospacedDigit()
            }
        }
        .frame(maxWidth: .infinity)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("\(label), \(value)")
    }

    private func sectionHeader(_ title: String, detail: String?) -> some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(title).font(.title3.bold())
            if let detail {
                Text(detail).font(.caption).foregroundStyle(.secondary)
            }
        }
        .padding(.top, 28)
        .padding(.bottom, 8)
    }

    @ViewBuilder
    private var attentionContent: some View {
        if attentionProjects.isEmpty {
            HStack(spacing: 12) {
                Image(systemName: "checkmark.circle.fill")
                    .foregroundStyle(Color.rinthyGreen)
                VStack(alignment: .leading, spacing: 2) {
                    Text("All clear").fontWeight(.semibold)
                    Text("No projects currently require action.")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
            }
            .padding(14)
            .background(Color(uiColor: .secondarySystemBackground), in: RoundedRectangle(cornerRadius: 10))
            .overlay {
                RoundedRectangle(cornerRadius: 10)
                    .stroke(Color(uiColor: .separator), lineWidth: 0.5)
            }
        } else {
            ForEach(attentionProjects, id: \.id) { project in
                AttentionRow(project: project)
                Divider()
            }
        }
    }

    @ViewBuilder
    private var recentContent: some View {
        if recentProjects.isEmpty {
            EmptyStateView(
                title: "No projects yet",
                systemImage: "shippingbox",
                message: "Projects you own or manage will appear here."
            )
        } else {
            ForEach(recentProjects, id: \.id) { project in
                ProjectRow(project: project, showDescription: false, showStatus: false)
                    .padding(.vertical, 8)
                Divider()
            }
        }
    }

    private func compact(_ value: Int64) -> String {
        if value >= 1_000_000 { return String(format: "%.1fM", Double(value) / 1_000_000) }
        if value >= 1_000 { return String(format: "%.1fK", Double(value) / 1_000) }
        return "\(value)"
    }
}

private struct AttentionRow: View {
    let project: Project

    var body: some View {
        HStack(spacing: 12) {
            ProjectArtwork(project: project)
            VStack(alignment: .leading, spacing: 3) {
                Text(project.title).fontWeight(.semibold).lineLimit(1)
                Text(project.statusMessage)
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
            Spacer(minLength: 8)
            Image(systemName: "exclamationmark.triangle")
                .foregroundStyle(.orange)
                .accessibilityLabel("Attention required")
        }
        .padding(.vertical, 10)
    }
}

private extension Project {
    var isHealthy: Bool {
        status == "approved" || status == "archived"
    }

    var statusMessage: String {
        switch status {
        case "processing": return "Modrinth is processing this project"
        case "rejected": return "Review the moderation response"
        case "withheld": return "Project is withheld from publishing"
        case "scheduled": return "Publication is scheduled"
        case "private": return "Project is currently private"
        case "draft": return "Draft is waiting to be finished"
        default: return "Status: \(status.capitalized)"
        }
    }
}
