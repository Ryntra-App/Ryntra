import RyntraShared
import SwiftUI

struct OverviewView: View {
    @EnvironmentObject private var model: AppModel
    @AppStorage("themeStyle") private var storedThemeStyle = RyntraThemeStyle.platform.rawValue

    let dashboard: Dashboard
    var onProjectTap: (Project) -> Void = { _ in }

    private var totalDownloads: Int64 {
        dashboard.projects.reduce(0) { $0 + $1.downloads }
    }

    private var totalFollowers: Int64 {
        dashboard.projects.reduce(0) { $0 + $1.followers }
    }

    private var attentionProjects: [Project] {
        Array(
            dashboard.projects
                .filter { $0.needsAttention() }
                .sorted { attentionRank($0) < attentionRank($1) }
                .prefix(5)
        )
    }

    private var attentionCount: Int {
        dashboard.projects.filter { $0.needsAttention() }.count
    }

    private var inReviewProjects: [Project] {
        Array(
            dashboard.projects
                // Quiet processing only — moderator issues go under Needs attention.
                .filter { $0.isInReview() && !$0.needsAttention() }
                .sorted { ($0.queued ?? $0.updated ?? "") > ($1.queued ?? $1.updated ?? "") }
                .prefix(5)
        )
    }

    private var inReviewCount: Int {
        dashboard.projects.filter { $0.isInReview() && !$0.needsAttention() }.count
    }

    private var recentProjects: [Project] {
        Array(dashboard.projects.sorted { ($0.updated ?? "") > ($1.updated ?? "") }.prefix(4))
    }

    private var leadingProject: Project? {
        dashboard.projects.max { $0.downloads < $1.downloads }
    }

    private var projectTypes: [(name: String, count: Int)] {
        let grouped: [String: [Project]] = Dictionary(
            grouping: dashboard.projects,
            by: { project in project.displayTypeLabel }
        )
        let counts: [(name: String, count: Int)] = grouped.map { entry in
            (name: entry.key, count: entry.value.count)
        }
        let sorted = counts.sorted { left, right in
            if left.count == right.count {
                return left.name < right.name
            }
            return left.count > right.count
        }
        return Array(sorted.prefix(4))
    }

    var body: some View {
        ScrollView {
            LazyVStack(alignment: .leading, spacing: 0) {
                Text(String(format: NSLocalizedString("Welcome back, %@", comment: "Overview welcome"), dashboard.account.username))
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                    .padding(.top, 8)
                    .padding(.bottom, 14)

                portfolioSummary

                sectionHeader(
                    NSLocalizedString("Needs attention", comment: "Overview section"),
                    detail: attentionCount == 0
                        ? nil
                        : String(format: NSLocalizedString("%d open actions", comment: "Attention count"), attentionCount)
                )
                attentionContent

                if !inReviewProjects.isEmpty {
                    sectionHeader(
                        NSLocalizedString("In review", comment: "Overview section"),
                        detail: String(
                            format: NSLocalizedString("%d projects awaiting moderation", comment: "In review count"),
                            inReviewCount
                        )
                    )
                    ForEach(inReviewProjects, id: \.id) { project in
                        Button { onProjectTap(project) } label: { InReviewRow(project: project) }
                            .buttonStyle(.plain)
                        Divider()
                    }
                }

                if let leadingProject {
                    sectionHeader(
                        NSLocalizedString("Portfolio leader", comment: "Overview section"),
                        detail: NSLocalizedString("Your most downloaded project", comment: "Overview section detail")
                    )
                    leadingProjectRow(leadingProject)
                }

                sectionHeader(
                    NSLocalizedString("Recently updated", comment: "Overview section"),
                    detail: NSLocalizedString("Latest activity across your workspace", comment: "Overview section detail")
                )
                recentContent

                if !projectTypes.isEmpty {
                    sectionHeader(NSLocalizedString("Portfolio mix", comment: "Overview section"), detail: nil)
                    ForEach(Array(projectTypes.enumerated()), id: \.element.name) { index, type in
                        HStack {
                            Text(type.name)
                            Spacer()
                            Text(String(format: NSLocalizedString("%d of %d", comment: "Count of total"), type.count, dashboard.projects.count))
                                .font(.subheadline)
                                .foregroundStyle(.secondary)
                        }
                        .padding(.vertical, 10)
                        if index < projectTypes.count - 1 { Divider() }
                    }
                }
            }
            .padding(.horizontal, 16)
            .padding(.bottom, isPlatformNative ? 20 : 96)
        }
        .background(Color.ryntraBackground)
        .refreshable { model.refresh() }
    }

    private var isPlatformNative: Bool {
        storedThemeStyle == RyntraThemeStyle.platform.rawValue
    }

    private var portfolioSummary: some View {
        VStack(spacing: 0) {
            summaryRow(
                "Downloads",
                value: ryntraExactCount(totalDownloads),
                symbol: "arrow.down",
                tint: .ryntraGreen
            )
            Divider().padding(.leading, 44)
            summaryRow(
                "Followers",
                value: ryntraExactCount(totalFollowers),
                symbol: "heart",
                tint: .ryntraGreen
            )
            Divider().padding(.leading, 44)
            HStack(spacing: 16) {
                compactFact(
                    "\(dashboard.projects.count) \(dashboard.projects.count == 1 ? "project" : "projects")",
                    symbol: "shippingbox"
                )
                compactFact(
                    "\(dashboard.projects.filter { $0.status == "approved" }.count) approved",
                    symbol: "checkmark.seal"
                )
                compactFact(
                    "\(dashboard.organizations.count) \(dashboard.organizations.count == 1 ? "team" : "teams")",
                    symbol: "person.2"
                )
            }
            .padding(.horizontal, 14)
            .padding(.vertical, 12)
        }
        .background(Color.ryntraSurface, in: RoundedRectangle(cornerRadius: 10))
    }

    private func summaryRow(_ label: String, value: String, symbol: String, tint: Color) -> some View {
        HStack(spacing: 12) {
            Image(systemName: symbol)
                .foregroundStyle(tint)
                .frame(width: 18)
            Text(label)
            Spacer()
            Text(value).fontWeight(.semibold).monospacedDigit()
        }
        .font(.subheadline)
        .padding(.horizontal, 14)
        .padding(.vertical, 12)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("\(label), \(value)")
    }

    private func compactFact(_ label: String, symbol: String) -> some View {
        HStack(spacing: 5) {
            Image(systemName: symbol)
            Text(label).lineLimit(1).minimumScaleFactor(0.75)
        }
        .font(.caption2)
        .foregroundStyle(.secondary)
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private func sectionHeader(_ title: String, detail: String?) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            RyntraSectionLabel(text: title)
            if let detail {
                Text(detail).font(.caption).foregroundStyle(.secondary)
            }
        }
        .padding(.top, 20)
        .padding(.bottom, 8)
    }

    @ViewBuilder
    private var attentionContent: some View {
        if attentionProjects.isEmpty {
            HStack(spacing: 12) {
                Image(systemName: "checkmark.circle.fill").foregroundStyle(Color.ryntraGreen)
                VStack(alignment: .leading, spacing: 2) {
                    Text(NSLocalizedString("All clear", comment: "Overview all clear")).fontWeight(.semibold)
                    Text(NSLocalizedString("No moderation issues right now. Projects waiting for review are listed separately.", comment: "Overview all clear hint"))
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(14)
            .background(Color.ryntraSurface, in: RoundedRectangle(cornerRadius: 10))
        } else {
            ForEach(attentionProjects, id: \.id) { project in
                Button { onProjectTap(project) } label: { AttentionRow(project: project) }
                    .buttonStyle(.plain)
                Divider()
            }
        }
    }

    private func leadingProjectRow(_ project: Project) -> some View {
        let share = totalDownloads == 0 ? 0 : Double(project.downloads) / Double(totalDownloads)
        return Button { onProjectTap(project) } label: {
            VStack(alignment: .leading, spacing: 12) {
                HStack(spacing: 12) {
                    ProjectArtwork(project: project)
                    VStack(alignment: .leading, spacing: 3) {
                        Text(project.title).fontWeight(.semibold).lineLimit(1)
                        Text("\(ryntraExactCount(project.downloads)) downloads")
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    }
                    Spacer()
                    Image(systemName: "arrow.down").foregroundStyle(Color.ryntraGreen)
                }
                ProgressView(value: share).tint(Color.ryntraGreen)
                Text("\(Int(share * 100))% of portfolio downloads")
                    .font(.caption2)
                    .foregroundStyle(.secondary)
            }
            .padding(14)
            .background(Color.ryntraSurface, in: RoundedRectangle(cornerRadius: 10))
        }
        .buttonStyle(.plain)
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
                Button { onProjectTap(project) } label: {
                    ProjectRow(project: project, showDescription: false, showStatus: false)
                        .padding(.vertical, 4)
                }
                .buttonStyle(.plain)
                Divider()
            }
        }
    }
}

private struct AttentionRow: View {
    let project: Project

    var body: some View {
        HStack(spacing: 12) {
            ProjectArtwork(project: project)
            VStack(alignment: .leading, spacing: 3) {
                Text(project.title).fontWeight(.semibold).lineLimit(1)
                Text(project.attentionMessageText)
                    .font(.caption)
                    .foregroundStyle(.secondary)
                    .lineLimit(2)
            }
            Spacer(minLength: 8)
            Image(systemName: "exclamationmark.triangle.fill")
                .foregroundStyle(.red)
                .accessibilityLabel(NSLocalizedString("Attention required", comment: "Attention a11y"))
        }
        .padding(.vertical, 10)
    }
}

private struct InReviewRow: View {
    let project: Project

    var body: some View {
        HStack(spacing: 12) {
            ProjectArtwork(project: project)
            VStack(alignment: .leading, spacing: 3) {
                Text(project.title).fontWeight(.semibold).lineLimit(1)
                Text(NSLocalizedString("Submitted for publication · awaiting moderation", comment: "In review"))
                    .font(.caption)
                    .foregroundStyle(.secondary)
                    .lineLimit(2)
            }
            Spacer(minLength: 8)
            Image(systemName: "clock")
                .foregroundStyle(.orange)
                .accessibilityLabel(NSLocalizedString("In review", comment: "In review a11y"))
        }
        .padding(.vertical, 10)
    }
}

private func attentionRank(_ project: Project) -> Int {
    switch project.attentionState().kind {
    case .rejected: return 0
    case .withheld: return 1
    default: return 2
    }
}
