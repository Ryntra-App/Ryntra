import Foundation
import RyntraShared
import SwiftUI

struct ProjectsView: View {
    @EnvironmentObject private var model: AppModel
    @Environment(\.accessibilityReduceMotion) private var systemReduceMotion
    @AppStorage("showFavoriteProjects") private var showFavoriteProjects = true
    @AppStorage("reduceMotion") private var appReduceMotion = false
    @AppStorage("projectSortMode") private var storedSortMode = ProjectSortMode.popular.rawValue
    @AppStorage("favoriteProjectIds") private var storedFavoriteIds = "[]"
    @AppStorage("themeStyle") private var storedThemeStyle = RyntraThemeStyle.platform.rawValue

    let projects: [Project]
    var onProjectTap: (Project) -> Void = { _ in }

    @State private var query = ""

    private var sortMode: ProjectSortMode {
        ProjectSortMode(rawValue: storedSortMode) ?? .popular
    }

    private var favoriteIds: Set<String> {
        guard showFavoriteProjects,
              let data = storedFavoriteIds.data(using: .utf8),
              let decoded = try? JSONDecoder().decode([String].self, from: data) else { return [] }
        return Set(decoded)
    }

    private var filteredProjects: [Project] {
        let filtered = query.isEmpty ? projects : projects.filter {
            $0.title.localizedCaseInsensitiveContains(query) ||
                $0.description_.localizedCaseInsensitiveContains(query) ||
                ($0.slug?.localizedCaseInsensitiveContains(query) ?? false)
        }
        return filtered.sortedForDisplay(mode: sortMode, favoriteIds: favoriteIds)
    }

    var body: some View {
        List {
            Section {
                projectSummary
                    .listRowInsets(EdgeInsets(top: 8, leading: 16, bottom: 8, trailing: 16))
                    .listRowSeparator(.hidden)

                Picker("Sort projects", selection: $storedSortMode) {
                    ForEach(ProjectSortMode.allCases, id: \.self) { mode in
                        Text(mode.label).tag(mode.rawValue)
                    }
                }
                .pickerStyle(.segmented)
                .listRowInsets(EdgeInsets(top: 4, leading: 16, bottom: 10, trailing: 16))
                .listRowSeparator(.hidden)
            }

            if filteredProjects.isEmpty {
                EmptyStateView(
                    title: projects.isEmpty ? "No projects yet" : "No matching projects",
                    systemImage: "shippingbox",
                    message: projects.isEmpty
                        ? "Managed projects will appear here."
                        : "Try another title, slug, or summary."
                )
                .listRowSeparator(.hidden)
            } else {
                ForEach(filteredProjects, id: \.id) { project in
                    ProjectBannerCard(
                        project: project,
                        isFavorite: favoriteIds.contains(project.id),
                        onFavoriteTap: showFavoriteProjects ? { toggleFavorite(project.id) } : nil
                    )
                    .contentShape(Rectangle())
                    .onTapGesture { onProjectTap(project) }
                    .listRowInsets(EdgeInsets(top: 6, leading: 16, bottom: 6, trailing: 16))
                    .listRowSeparator(.hidden)
                    .listRowBackground(Color.clear)
                }
            }
            if !isPlatformNative {
                Color.clear
                    .frame(height: 90)
                    .listRowSeparator(.hidden)
            }
        }
        .listStyle(.plain)
        .searchable(text: $query, prompt: "Search projects")
        .refreshable { model.refresh() }
    }

    private var isPlatformNative: Bool {
        storedThemeStyle == RyntraThemeStyle.platform.rawValue
    }

    private var projectSummary: some View {
        HStack(spacing: 0) {
            summaryMetric("\(projects.count)", label: "Projects")
            summaryMetric(
                ryntraExactCount(projects.reduce(0) { $0 + $1.downloads }),
                label: "Downloads"
            )
            summaryMetric(
                ryntraExactCount(projects.reduce(0) { $0 + $1.followers }),
                label: "Followers"
            )
        }
        .padding(.vertical, 13)
        .background(Color.ryntraSurface, in: RoundedRectangle(cornerRadius: 10))
    }

    private func summaryMetric(_ value: String, label: String) -> some View {
        VStack(spacing: 1) {
            Text(value).font(.headline).monospacedDigit().lineLimit(1).minimumScaleFactor(0.7)
            Text(label).font(.caption2).foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("\(label), \(value)")
    }

    private func toggleFavorite(_ projectId: String) {
        var updated = favoriteIds
        let update = {
            if updated.contains(projectId) {
                updated.remove(projectId)
            } else {
                updated.insert(projectId)
            }
            guard let data = try? JSONEncoder().encode(updated.sorted()),
                  let encoded = String(data: data, encoding: .utf8) else { return }
            storedFavoriteIds = encoded
        }
        if systemReduceMotion || appReduceMotion {
            update()
        } else {
            withAnimation(RyntraMotion.control, update)
        }
    }
}

struct ProjectRow: View {
    let project: Project
    var showDescription = true
    var showStatus = true
    var isFavorite = false
    var onFavoriteTap: (() -> Void)?

    var body: some View {
        HStack(alignment: .center, spacing: 12) {
            ProjectArtwork(project: project)

            VStack(alignment: .leading, spacing: 4) {
                HStack(spacing: 8) {
                    Text(project.title).fontWeight(.semibold).lineLimit(1)
                    Spacer(minLength: 0)
                    if showStatus, project.status != "approved" {
                        statusLabel
                    }
                }
                Text(project.slug.map { "\($0)  ·  \(project.displayTypeLabel)" }
                     ?? project.displayTypeLabel)
                    .font(.caption.weight(.medium))
                    .foregroundStyle(.secondary)
                    .lineLimit(1)
                if showDescription, !project.description_.isEmpty {
                    Text(project.description_)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                        .lineLimit(2)
                        .padding(.top, 1)
                }
                HStack(spacing: 13) {
                    metric(ryntraExactCount(project.downloads), symbol: "arrow.down", tint: Color.ryntraGreen)
                    metric(ryntraExactCount(project.followers), symbol: "heart", tint: Color.ryntraGreen)
                    if let updated = ryntraProjectDate(project.updated) {
                        Text(updated).font(.caption2).foregroundStyle(.secondary)
                    }
                }
                .padding(.top, 2)
            }

            if let onFavoriteTap {
                Button(action: onFavoriteTap) {
                    Image(systemName: isFavorite ? "star.fill" : "star")
                        .font(.caption.weight(.semibold))
                        .foregroundStyle(isFavorite ? Color.ryntraGreen : Color.secondary)
                        .frame(width: 36, height: 44)
                }
                .buttonStyle(.plain)
                .accessibilityLabel(isFavorite ? "Remove favorite" : "Add favorite")
            } else {
                Image(systemName: "chevron.right")
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(.tertiary)
            }
        }
        .padding(.vertical, 6)
    }

    private var statusLabel: some View {
        return Text(project.localizedStatusLabel)
            .font(.caption2.weight(.semibold))
            .foregroundStyle(statusColor)
            .padding(.horizontal, 6)
            .padding(.vertical, 3)
            .background(statusColor.opacity(0.12), in: RoundedRectangle(cornerRadius: 6))
    }

    private var statusColor: Color {
        switch project.status {
        case "rejected", "withheld": return .red
        case "processing", "scheduled", "draft": return .orange
        default: return .secondary
        }
    }

    private func metric(_ value: String, symbol: String, tint: Color) -> some View {
        HStack(spacing: 4) {
            Image(systemName: symbol).foregroundStyle(tint)
            Text(value).foregroundStyle(.secondary)
        }
        .font(.caption2)
    }
}

private enum ProjectSortMode: String, CaseIterable {
    case popular
    case updated
    case title
    case followers

    var label: String {
        switch self {
        case .popular: return NSLocalizedString("Popular", comment: "Project sort")
        case .updated: return NSLocalizedString("Updated", comment: "Project sort")
        case .title: return NSLocalizedString("A-Z", comment: "Project sort")
        case .followers: return NSLocalizedString("Followers", comment: "Project sort")
        }
    }
}

private extension Array where Element == Project {
    func sortedForDisplay(mode: ProjectSortMode, favoriteIds: Set<String>) -> [Project] {
        let sorted: [Project]
        switch mode {
        case .popular:
            sorted = self.sorted { $0.downloads > $1.downloads }
        case .updated:
            sorted = self.sorted { ($0.updated ?? "") > ($1.updated ?? "") }
        case .title:
            sorted = self.sorted { $0.title.localizedCaseInsensitiveCompare($1.title) == .orderedAscending }
        case .followers:
            sorted = self.sorted { $0.followers > $1.followers }
        }
        return sorted.sorted { favoriteIds.contains($0.id) && !favoriteIds.contains($1.id) }
    }
}

func ryntraProjectDate(_ value: String?) -> String? {
    guard let value, let date = projectISODateFormatter.date(from: value) else { return nil }
    return date.formatted(.dateTime.month(.abbreviated).day())
}

private let projectISODateFormatter = ISO8601DateFormatter()

struct EmptyStateView: View {
    let title: String
    let systemImage: String
    let message: String

    var body: some View {
        VStack(spacing: 8) {
            Image(systemName: systemImage)
                .font(.title2)
                .foregroundStyle(.secondary)
            Text(title).font(.headline)
            Text(message)
                .font(.subheadline)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 40)
    }
}

struct ProjectArtwork: View {
    let project: Project

    var body: some View {
        AsyncImage(url: URL(string: project.iconUrl ?? "")) { image in
            image.resizable().scaledToFill()
        } placeholder: {
            RoundedRectangle(cornerRadius: 10)
                .fill(.quaternary)
                .overlay(Text(String(project.title.prefix(1))).fontWeight(.bold))
        }
        .frame(width: 56, height: 56)
        .clipShape(RoundedRectangle(cornerRadius: 10))
        .accessibilityHidden(true)
    }
}
