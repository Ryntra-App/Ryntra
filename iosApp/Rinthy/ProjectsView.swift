import RinthyShared
import SwiftUI

struct ProjectsView: View {
    @EnvironmentObject private var model: AppModel
    let projects: [Project]
    var onProjectTap: (Project) -> Void = { _ in }
    @State private var query = ""
    @State private var sortMode = ProjectSortMode.popular
    @State private var favoriteIds = Set<String>()

    private var filteredProjects: [Project] {
        let filtered = query.isEmpty ? projects : projects.filter {
            $0.title.localizedCaseInsensitiveContains(query) || $0.description_.localizedCaseInsensitiveContains(query)
        }
        return filtered.sortedForDisplay(mode: sortMode, favoriteIds: favoriteIds)
    }

    var body: some View {
        List {
            Section {
                VStack(alignment: .leading, spacing: 12) {
                    HStack {
                        Text("\(projects.count) projects")
                        Spacer()
                        Text("\(compact(projects.reduce(0) { $0 + $1.downloads })) downloads")
                    }
                    .font(.subheadline)
                    .foregroundStyle(.secondary)

                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 8) {
                            ForEach(ProjectSortMode.allCases, id: \.self) { mode in
                                SortChip(
                                    label: mode.label,
                                    isSelected: mode == sortMode,
                                    onTap: { sortMode = mode }
                                )
                            }
                        }
                    }
                }
            }

            if filteredProjects.isEmpty {
                EmptyStateView(
                    title: projects.isEmpty ? "No projects yet" : "No matching projects",
                    systemImage: "shippingbox",
                    message: projects.isEmpty ? "Managed projects will appear here." : "Try another search term."
                )
            } else {
                ForEach(filteredProjects, id: \.id) { project in
                    ProjectRow(
                        project: project,
                        isFavorite: favoriteIds.contains(project.id),
                        onFavoriteTap: { toggleFavorite(project.id) }
                    )
                    .contentShape(Rectangle())
                    .onTapGesture { onProjectTap(project) }
                }
            }
            Color.clear
                .frame(height: 90)
                .listRowSeparator(.hidden)
        }
        .listStyle(.plain)
        .searchable(text: $query, prompt: "Search projects")
        .refreshable { model.refresh() }
    }

    private func compact(_ value: Int64) -> String {
        if value >= 1_000_000 { return String(format: "%.1fM", Double(value) / 1_000_000) }
        if value >= 1_000 { return String(format: "%.1fK", Double(value) / 1_000) }
        return "\(value)"
    }

    private func toggleFavorite(_ projectId: String) {
        if favoriteIds.contains(projectId) {
            favoriteIds.remove(projectId)
        } else {
            favoriteIds.insert(projectId)
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
        HStack(alignment: .top, spacing: 12) {
            ProjectArtwork(project: project)

            VStack(alignment: .leading, spacing: 5) {
                HStack {
                    Text(project.title).fontWeight(.bold).lineLimit(1)
                    Spacer()
                    if showStatus, project.status != "approved" {
                        Text(project.status.capitalized)
                            .font(.caption2.weight(.semibold))
                            .foregroundStyle(project.status == "rejected" ? Color.red : Color.orange)
                    }
                    if let onFavoriteTap {
                        Button(action: onFavoriteTap) {
                            Image(systemName: isFavorite ? "star.fill" : "star")
                                .font(.caption.weight(.semibold))
                                .foregroundStyle(isFavorite ? Color.rinthyGreen : Color.secondary)
                                .frame(width: 32, height: 32)
                        }
                        .buttonStyle(.plain)
                        .accessibilityLabel(isFavorite ? "Remove favorite" : "Add favorite")
                    }
                }
                Text(project.projectType.capitalized)
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(.secondary)
                if showDescription, !project.description_.isEmpty {
                    Text(project.description_)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                        .lineLimit(2)
                }
                Label("\(project.downloads)", systemImage: "arrow.down.circle")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
        }
        .padding(.vertical, 4)
    }
}

private enum ProjectSortMode: CaseIterable {
    case popular
    case updated
    case title
    case followers

    var label: String {
        switch self {
        case .popular: return "Popular"
        case .updated: return "Updated"
        case .title: return "A-Z"
        case .followers: return "Followers"
        }
    }
}

private struct SortChip: View {
    let label: String
    let isSelected: Bool
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            Text(label)
                .font(.caption.weight(.semibold))
                .foregroundStyle(isSelected ? Color.rinthyGreen : Color.secondary)
                .padding(.horizontal, 10)
                .padding(.vertical, 7)
                .background(
                    isSelected ? Color.rinthyGreen.opacity(0.13) : Color(uiColor: .secondarySystemBackground),
                    in: RoundedRectangle(cornerRadius: 8)
                )
        }
        .buttonStyle(.plain)
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
            RoundedRectangle(cornerRadius: 8)
                .fill(.quaternary)
                .overlay(Text(String(project.title.prefix(1))).fontWeight(.black))
        }
        .frame(width: 52, height: 52)
        .clipShape(RoundedRectangle(cornerRadius: 8))
        .accessibilityHidden(true)
    }
}
