import RinthyShared
import SwiftUI

struct ProjectDetailView: View {
    @EnvironmentObject private var model: AppModel

    @State private var project: Project
    @State private var selectedTab = ProjectDetailTab.overview
    @State private var versions: [ProjectVersion] = []
    @State private var dependencies: [ProjectDependency] = []
    @State private var members: [ProjectMember] = []
    @State private var organizationMembers: [ProjectMember] = []
    @State private var organizationName: String?
    @State private var markdownBlocks: [MarkdownBlock]
    @State private var isLoadingVersions = false
    @State private var isLoadingMembers = false
    @State private var versionError: String?
    @State private var memberError: String?

    init(project: Project) {
        _project = State(initialValue: project)
        _markdownBlocks = State(initialValue: [])
    }

    private var resources: [(String, URL)] {
        [
            (NSLocalizedString("Source", comment: "Project resource"), project.sourceUrl),
            (NSLocalizedString("Issues", comment: "Project resource"), project.issuesUrl),
            (NSLocalizedString("Wiki", comment: "Project resource"), project.wikiUrl),
            (NSLocalizedString("Discord", comment: "Project resource"), project.discordUrl),
        ].compactMap { label, url in
            guard let url, let parsed = URL(string: url), !url.isEmpty else { return nil }
            return (label, parsed)
        }
    }

    var body: some View {
        ScrollView {
            LazyVStack(alignment: .leading, spacing: 0) {
                identity
                projectTabs

                switch selectedTab {
                case .overview:
                    overviewContent
                case .versions:
                    ProjectVersionsManagementView(
                        project: project,
                        versions: versions,
                        isLoading: isLoadingVersions,
                        errorMessage: versionError,
                        onReload: { await loadVersions() }
                    )
                case .edit:
                    EditProjectView(project: project, onSaved: { await reloadProject() })
                case .members:
                    ProjectMembersManagementView(
                        project: project,
                        members: members,
                        organizationMembers: organizationMembers,
                        organizationName: organizationName,
                        isLoading: isLoadingMembers,
                        errorMessage: memberError,
                        onReload: { await loadMembers() }
                    )
                }
            }
            .padding(.horizontal, 16)
            .padding(.top, 12)
            .padding(.bottom, 36)
        }
        .background(Color.rinthyBackground)
        .task(id: project.id) {
            async let loadedVersions: Void = loadVersions()
            async let loadedMembers: Void = loadMembers()
            async let loadedMarkdown = parseMarkdown(project.body)
            markdownBlocks = await loadedMarkdown
            _ = await (loadedVersions, loadedMembers)
        }
    }

    @ViewBuilder
    private var overviewContent: some View {
        metrics
        section("Summary", value: project.description_.isEmpty ? "No summary provided." : project.description_)

        if !markdownBlocks.isEmpty {
            DetailHeading(title: "Description")
            VStack(alignment: .leading, spacing: 9) {
                ForEach(Array(markdownBlocks.enumerated()), id: \.offset) { _, block in
                    MarkdownBlockView(block: block)
                }
            }
        }

        environment
        dependenciesList
        categories
        gallery
        details
        resourcesList
    }

    private var projectTabs: some View {
        HStack(spacing: 0) {
            ForEach(ProjectDetailTab.allCases, id: \.self) { tab in
                Button {
                    selectedTab = tab
                } label: {
                    HStack(spacing: 5) {
                        Image(systemName: tab.symbol)
                            .font(.caption.weight(.semibold))
                        Text(tab.label)
                            .font(.caption.weight(selectedTab == tab ? .bold : .semibold))
                            .lineLimit(1)
                            .minimumScaleFactor(0.78)
                    }
                    .foregroundStyle(selectedTab == tab ? Color.rinthyGreen : Color.secondary)
                    .frame(maxWidth: .infinity)
                    .frame(height: 44)
                    .background(
                        selectedTab == tab ? Color.rinthySurfaceRaised : Color.clear,
                        in: RoundedRectangle(cornerRadius: 8)
                    )
                }
                .buttonStyle(.plain)
            }
        }
        .padding(3)
        .background(Color.rinthySurface, in: RoundedRectangle(cornerRadius: 11))
        .overlay {
            RoundedRectangle(cornerRadius: 11)
                .stroke(Color.primary.opacity(0.08), lineWidth: 0.5)
        }
        .padding(.bottom, 18)
    }

    private func loadVersions() async {
        isLoadingVersions = true
        versionError = nil
        do {
            let loaded = try await model.loadProjectVersions(project: project)
            versions = loaded
            dependencies = try await model.loadProjectDependencies(versions: loaded)
        } catch {
            versionError = error.localizedDescription
        }
        isLoadingVersions = false
    }

    private func reloadProject() async {
        do {
            project = try await model.loadProjectDetails(project: project)
            markdownBlocks = await parseMarkdown(project.body)
        } catch {
            versionError = error.localizedDescription
        }
    }

    private func parseMarkdown(_ source: String) async -> [MarkdownBlock] {
        await Task.detached(priority: .userInitiated) {
            MarkdownParser.shared.parse(markdown: source)
        }.value
    }

    private func loadMembers() async {
        isLoadingMembers = true
        memberError = nil
        do {
            // Refresh project first so team_id / organization are present for org projects.
            if project.team == nil || project.organization == nil {
                project = try await model.loadProjectDetails(project: project)
            }
            let roster = try await model.loadProjectTeamRoster(project: project)
            members = roster.projectMembers
            organizationMembers = roster.organizationMembers
            organizationName = roster.organization?.name
        } catch {
            memberError = error.localizedDescription
        }
        isLoadingMembers = false
    }

    private var identity: some View {
        HStack(alignment: .center, spacing: 14) {
            ProjectArtwork(project: project)
                .frame(width: 76, height: 76)
                .clipShape(RoundedRectangle(cornerRadius: 12))

            VStack(alignment: .leading, spacing: 4) {
                Text(project.title)
                    .font(.title2.bold())
                Text(project.displayTypeLabel)
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                if project.status != "approved" {
                    Text(project.localizedStatusLabel)
                        .font(.caption2.weight(.semibold))
                        .foregroundStyle(project.status == "rejected" ? Color.red : Color.orange)
                        .padding(.top, 3)
                }
            }
        }
        .padding(.bottom, 22)
    }

    private var metrics: some View {
        HStack(spacing: 0) {
            metric("Downloads", value: rinthyExactCount(project.downloads), systemImage: "arrow.down")
            Divider().frame(height: 50)
            metric("Followers", value: rinthyExactCount(project.followers), systemImage: "heart")
        }
        .padding(.vertical, 14)
        .background(Color.rinthySurface, in: RoundedRectangle(cornerRadius: 8))
        .overlay {
            RoundedRectangle(cornerRadius: 8)
                .stroke(Color.rinthySeparator, lineWidth: 0.5)
        }
    }

    private func metric(_ label: String, value: String, systemImage: String) -> some View {
        HStack(spacing: 8) {
            Image(systemName: systemImage)
                .font(.caption.weight(.semibold))
                .foregroundStyle(Color.rinthyGreen)
            VStack(alignment: .leading, spacing: 1) {
                Text(LocalizedStringKey(label)).font(.caption2).foregroundStyle(.secondary)
                Text(value).font(.headline).monospacedDigit()
            }
        }
        .frame(maxWidth: .infinity)
    }

    private func section(_ title: String, value: String) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            DetailHeading(title: title)
            Text(value)
                .font(.body)
                .foregroundStyle(.secondary)
        }
    }

    private var environment: some View {
        VStack(alignment: .leading, spacing: 10) {
            DetailHeading(title: "Environment")
            HStack(spacing: 10) {
                environmentValue("Client", value: project.clientSide, systemImage: "desktopcomputer")
                environmentValue("Server", value: project.serverSide, systemImage: "server.rack")
            }
        }
    }

    @ViewBuilder
    private var dependenciesList: some View {
        if !dependencies.isEmpty {
            DetailHeading(title: "Dependencies")
            VStack(spacing: 8) {
                ForEach(Array(dependencies.enumerated()), id: \.offset) { _, dependency in
                    HStack(spacing: 10) {
                        AsyncImage(url: URL(string: dependency.iconUrl ?? "")) { image in
                            image.resizable().scaledToFill()
                        } placeholder: {
                            RoundedRectangle(cornerRadius: 8).fill(.quaternary)
                        }
                        .frame(width: 40, height: 40)
                        .clipShape(RoundedRectangle(cornerRadius: 8))
                        VStack(alignment: .leading, spacing: 2) {
                            Text(
                                dependency.title ?? dependency.projectId ?? dependency.fileName ??
                                    NSLocalizedString("External dependency", comment: "Project dependency")
                            )
                                .fontWeight(.semibold)
                            Text(NSLocalizedString(
                                dependency.dependencyType.capitalized,
                                comment: "Project dependency type"
                            ))
                                .font(.caption)
                                .foregroundStyle(.secondary)
                        }
                        Spacer()
                    }
                    .padding(.vertical, 5)
                }
            }
        }
    }

    private func environmentValue(_ label: String, value: String, systemImage: String) -> some View {
        HStack(spacing: 9) {
            Image(systemName: systemImage)
                .font(.caption.weight(.semibold))
                .foregroundStyle(Color.rinthyGreen)
            VStack(alignment: .leading, spacing: 2) {
                Text(LocalizedStringKey(label)).font(.caption2).foregroundStyle(.secondary)
                Text(NSLocalizedString(value.capitalized, comment: "Project environment value"))
                    .fontWeight(.semibold)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    @ViewBuilder
    private var categories: some View {
        if !project.categories.isEmpty {
            DetailHeading(title: "Categories")
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    ForEach(project.categories, id: \.self) { category in
                        Text(category.capitalized)
                            .font(.caption.weight(.medium))
                            .padding(.horizontal, 10)
                            .padding(.vertical, 7)
                            .background(Color.rinthySurface, in: RoundedRectangle(cornerRadius: 7))
                    }
                }
            }
        }
    }

    @ViewBuilder
    private var gallery: some View {
        if !project.gallery.isEmpty {
            DetailHeading(title: "Gallery")
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 10) {
                    ForEach(project.gallery, id: \.url) { image in
                        AsyncImage(url: URL(string: image.url)) { loaded in
                            loaded.resizable().scaledToFill()
                        } placeholder: {
                            RoundedRectangle(cornerRadius: 8).fill(.quaternary)
                        }
                        .frame(width: 172, height: 108)
                        .clipShape(RoundedRectangle(cornerRadius: 8))
                    }
                }
            }
        }
    }

    @ViewBuilder
    private var details: some View {
        if project.license != nil || project.published != nil {
            DetailHeading(title: "Details")
            if let license = project.license {
                detailValue("License", value: license.name ?? license.id, systemImage: "scale.3d")
            }
            if let published = project.published {
                detailValue("Published", value: String(published.prefix(10)), systemImage: "calendar")
            }
        }
    }

    @ViewBuilder
    private var resourcesList: some View {
        if !resources.isEmpty {
            DetailHeading(title: "Resources")
            ForEach(resources, id: \.0) { label, url in
                Link(destination: url) {
                    HStack {
                        Image(systemName: "globe")
                            .foregroundStyle(Color.rinthyGreen)
                        Text(label).fontWeight(.semibold)
                        Spacer()
                        Image(systemName: "arrow.up.right")
                            .foregroundStyle(.secondary)
                    }
                    .padding(.vertical, 14)
                }
                Divider()
            }
        }
    }

    private func detailValue(_ label: String, value: String, systemImage: String) -> some View {
        HStack(spacing: 10) {
            Image(systemName: systemImage)
                .foregroundStyle(Color.rinthyGreen)
            Text(LocalizedStringKey(label)).foregroundStyle(.secondary)
            Spacer()
            Text(value).fontWeight(.semibold).lineLimit(1)
        }
        .padding(.vertical, 9)
    }

}

private struct DetailHeading: View {
    let title: String
    var body: some View {
        Text(NSLocalizedString(title, comment: "Project section").uppercased())
            .font(.caption.weight(.bold))
            .foregroundStyle(Color.rinthyGreen)
            .padding(.top, 30)
            .padding(.bottom, 11)
    }
}

private enum ProjectDetailTab: CaseIterable {
    case overview
    case versions
    case edit
    case members

    var label: String {
        switch self {
        case .overview: return NSLocalizedString("Overview", comment: "Project tab")
        case .versions: return NSLocalizedString("Versions", comment: "Project tab")
        case .edit: return NSLocalizedString("Edit", comment: "Project tab")
        case .members: return NSLocalizedString("Members", comment: "Project tab")
        }
    }

    var symbol: String {
        switch self {
        case .overview: return "square.grid.2x2.fill"
        case .versions: return "arrow.down.circle.fill"
        case .edit: return "slider.horizontal.3"
        case .members: return "person.3.fill"
        }
    }
}
