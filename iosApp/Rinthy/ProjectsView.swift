import RinthyShared
import SwiftUI

struct ProjectsView: View {
    @EnvironmentObject private var model: AppModel
    let projects: [Project]
    @State private var query = ""

    private var filteredProjects: [Project] {
        guard !query.isEmpty else { return projects }
        return projects.filter {
            $0.title.localizedCaseInsensitiveContains(query) ||
                $0.description_.localizedCaseInsensitiveContains(query)
        }
    }

    var body: some View {
        List {
            Section {
                HStack {
                    Text("\(projects.count) projects")
                    Spacer()
                    Text("\(compact(projects.reduce(0) { $0 + $1.downloads })) downloads")
                }
                .font(.subheadline)
                .foregroundStyle(.secondary)
            }

            if filteredProjects.isEmpty {
                EmptyStateView(
                    title: projects.isEmpty ? "No projects yet" : "No matching projects",
                    systemImage: "shippingbox",
                    message: projects.isEmpty ? "Managed projects will appear here." : "Try another search term."
                )
            } else {
                ForEach(filteredProjects, id: \.id) { project in
                    ProjectRow(project: project)
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
}

struct ProjectRow: View {
    let project: Project
    var showDescription = true
    var showStatus = true

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
