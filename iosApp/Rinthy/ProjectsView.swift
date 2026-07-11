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
                ContentUnavailableView(
                    projects.isEmpty ? "No projects yet" : "No matching projects",
                    systemImage: "shippingbox",
                    description: Text(projects.isEmpty ? "Managed projects will appear here." : "Try another search term.")
                )
            } else {
                ForEach(filteredProjects, id: \.id) { project in
                    ProjectRow(project: project)
                }
            }
        }
        .listStyle(.insetGrouped)
        .searchable(text: $query, prompt: "Search projects")
        .refreshable { model.refresh() }
    }

    private func compact(_ value: Int64) -> String {
        if value >= 1_000_000 { return String(format: "%.1fM", Double(value) / 1_000_000) }
        if value >= 1_000 { return String(format: "%.1fK", Double(value) / 1_000) }
        return "\(value)"
    }
}

private struct ProjectRow: View {
    let project: Project

    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            AsyncImage(url: URL(string: project.iconUrl ?? "")) { image in
                image.resizable().scaledToFill()
            } placeholder: {
                RoundedRectangle(cornerRadius: 8)
                    .fill(.quaternary)
                    .overlay(Text(String(project.title.prefix(1))).fontWeight(.black))
            }
            .frame(width: 52, height: 52)
            .clipShape(RoundedRectangle(cornerRadius: 8))

            VStack(alignment: .leading, spacing: 5) {
                HStack {
                    Text(project.title).fontWeight(.bold).lineLimit(1)
                    Spacer()
                    Text(project.status.capitalized)
                        .font(.caption2.weight(.semibold))
                        .foregroundStyle(project.status == "approved" ? Color.rinthyGreen : .orange)
                }
                Text(project.projectType.capitalized)
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(Color.rinthyGreen)
                if !project.description_.isEmpty {
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
