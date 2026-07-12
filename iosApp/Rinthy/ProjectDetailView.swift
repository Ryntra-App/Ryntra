import RinthyShared
import SwiftUI

struct ProjectDetailView: View {
    @EnvironmentObject private var model: AppModel

    let project: Project
    @State private var selectedTab = ProjectDetailTab.overview
    @State private var versions: [ProjectVersion] = []
    @State private var isLoadingVersions = false
    @State private var versionError: String?

    private var resources: [(String, URL)] {
        [
            ("Source", project.sourceUrl),
            ("Issues", project.issuesUrl),
            ("Wiki", project.wikiUrl),
            ("Discord", project.discordUrl),
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
                    versionsContent
                case .edit:
                    pendingTab(
                        title: "Edit tools are next",
                        message: "Metadata, status, links, icon, and gallery editing will move here from the old app."
                    )
                case .members:
                    pendingTab(
                        title: "Members are next",
                        message: "Team roles, permissions, payout split, invitations, and ownership transfer belong in this tab."
                    )
                }
            }
            .padding(.horizontal, 16)
            .padding(.top, 12)
            .padding(.bottom, 36)
        }
        .background(Color(uiColor: .systemBackground))
        .task(id: project.id) {
            await loadVersions()
        }
    }

    @ViewBuilder
    private var overviewContent: some View {
        metrics
        section("Summary", value: project.description_.isEmpty ? "No summary provided." : project.description_)
        let cleanedBody = project.body.cleanMarkdown()
        if !cleanedBody.isEmpty, cleanedBody != project.description_.cleanMarkdown() {
            markdownSection("Description", value: cleanedBody)
        }
        environment
        categories
        gallery
        details
        resourcesList
    }

    @ViewBuilder
    private var versionsContent: some View {
        if isLoadingVersions {
            HStack(spacing: 10) {
                ProgressView()
                Text("Loading releases")
                    .foregroundStyle(.secondary)
            }
            .padding(.vertical, 28)
        } else if let versionError, versions.isEmpty {
            pendingTab(title: "Versions unavailable", message: versionError)
        } else if versions.isEmpty {
            pendingTab(title: "No versions yet", message: "Published releases for this project will appear here.")
        } else {
            LazyVStack(spacing: 10) {
                ForEach(versions, id: \.id) { version in
                    VersionCard(version: version)
                }
            }
        }
    }

    private var projectTabs: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                ForEach(ProjectDetailTab.allCases, id: \.self) { tab in
                    Button {
                        selectedTab = tab
                    } label: {
                        HStack(spacing: 7) {
                            Image(systemName: tab.symbol)
                                .font(.caption.weight(.semibold))
                            Text(tab.label)
                                .font(.caption.weight(selectedTab == tab ? .bold : .semibold))
                        }
                        .foregroundStyle(selectedTab == tab ? Color.primary : Color.secondary)
                        .padding(.horizontal, 12)
                        .padding(.vertical, 9)
                        .background(
                            selectedTab == tab ? Color.primary.opacity(0.095) : Color(uiColor: .secondarySystemBackground),
                            in: RoundedRectangle(cornerRadius: 12)
                        )
                        .overlay {
                            RoundedRectangle(cornerRadius: 12)
                                .stroke(selectedTab == tab ? Color.primary.opacity(0.16) : Color(uiColor: .separator), lineWidth: 0.5)
                        }
                    }
                    .buttonStyle(.plain)
                }
            }
        }
        .padding(.bottom, 18)
    }

    private func loadVersions() async {
        isLoadingVersions = true
        versionError = nil
        do {
            versions = try await model.loadProjectVersions(project: project)
        } catch {
            versionError = error.localizedDescription
        }
        isLoadingVersions = false
    }

    private var identity: some View {
        HStack(alignment: .center, spacing: 14) {
            ProjectArtwork(project: project)
                .frame(width: 76, height: 76)
                .clipShape(RoundedRectangle(cornerRadius: 14))

            VStack(alignment: .leading, spacing: 4) {
                Text(project.title)
                    .font(.title2.bold())
                Text(project.projectType.capitalized)
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                if project.status != "approved" {
                    Text(project.status.capitalized)
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
            metric("Downloads", value: compact(project.downloads), systemImage: "arrow.down")
            Divider().frame(height: 50)
            metric("Followers", value: compact(project.followers), systemImage: "heart")
        }
        .padding(.vertical, 14)
        .background(Color(uiColor: .secondarySystemBackground), in: RoundedRectangle(cornerRadius: 8))
        .overlay {
            RoundedRectangle(cornerRadius: 8)
                .stroke(Color(uiColor: .separator), lineWidth: 0.5)
        }
    }

    private func metric(_ label: String, value: String, systemImage: String) -> some View {
        HStack(spacing: 8) {
            Image(systemName: systemImage)
                .font(.caption.weight(.semibold))
                .foregroundStyle(Color.rinthyGreen)
            VStack(alignment: .leading, spacing: 1) {
                Text(label).font(.caption2).foregroundStyle(.secondary)
                Text(value).font(.headline).monospacedDigit()
            }
        }
        .frame(maxWidth: .infinity)
    }

    private func section(_ title: String, value: String) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            sectionTitle(title)
            Text(value)
                .font(.body)
                .foregroundStyle(.secondary)
        }
    }

    private func markdownSection(_ title: String, value: String) -> some View {
        VStack(alignment: .leading, spacing: 9) {
            sectionTitle(title)
            ForEach(value.markdownLines, id: \.self) { line in
                if line.isHeading {
                    Text(line.text)
                        .font(.headline)
                        .fontWeight(.bold)
                } else {
                    Text(line.text)
                        .font(.body)
                        .foregroundStyle(.secondary)
                }
            }
        }
    }

    private var environment: some View {
        VStack(alignment: .leading, spacing: 10) {
            sectionTitle("Environment")
            HStack(spacing: 10) {
                environmentValue("Client", value: project.clientSide, systemImage: "desktopcomputer")
                environmentValue("Server", value: project.serverSide, systemImage: "server.rack")
            }
        }
    }

    private func environmentValue(_ label: String, value: String, systemImage: String) -> some View {
        HStack(spacing: 9) {
            Image(systemName: systemImage)
                .font(.caption.weight(.semibold))
                .foregroundStyle(Color.rinthyGreen)
            VStack(alignment: .leading, spacing: 2) {
                Text(label).font(.caption2).foregroundStyle(.secondary)
                Text(value.capitalized).fontWeight(.semibold)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    @ViewBuilder
    private var categories: some View {
        if !project.categories.isEmpty {
            sectionTitle("Categories")
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    ForEach(project.categories, id: \.self) { category in
                        Text(category.capitalized)
                            .font(.caption.weight(.medium))
                            .padding(.horizontal, 10)
                            .padding(.vertical, 7)
                            .background(Color(uiColor: .secondarySystemBackground), in: RoundedRectangle(cornerRadius: 7))
                    }
                }
            }
        }
    }

    @ViewBuilder
    private var gallery: some View {
        if !project.gallery.isEmpty {
            sectionTitle("Gallery")
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
            sectionTitle("Details")
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
            sectionTitle("Resources")
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

    private func sectionTitle(_ title: String) -> some View {
        Text(title)
            .font(.title3.bold())
            .padding(.top, 28)
            .padding(.bottom, 10)
    }

    private func detailValue(_ label: String, value: String, systemImage: String) -> some View {
        HStack(spacing: 10) {
            Image(systemName: systemImage)
                .foregroundStyle(Color.rinthyGreen)
            Text(label).foregroundStyle(.secondary)
            Spacer()
            Text(value).fontWeight(.semibold).lineLimit(1)
        }
        .padding(.vertical, 9)
    }

    private func pendingTab(title: String, message: String) -> some View {
        VStack(alignment: .leading, spacing: 7) {
            Text(title)
                .font(.headline)
            Text(message)
                .font(.body)
                .foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(18)
        .background(Color(uiColor: .secondarySystemBackground), in: RoundedRectangle(cornerRadius: 12))
        .overlay {
            RoundedRectangle(cornerRadius: 12)
                .stroke(Color(uiColor: .separator), lineWidth: 0.5)
        }
    }

    private func compact(_ value: Int64) -> String {
        if value >= 1_000_000 { return String(format: "%.1fM", Double(value) / 1_000_000) }
        if value >= 1_000 { return String(format: "%.1fK", Double(value) / 1_000) }
        return "\(value)"
    }
}

private enum ProjectDetailTab: CaseIterable {
    case overview
    case versions
    case edit
    case members

    var label: String {
        switch self {
        case .overview: return "Overview"
        case .versions: return "Versions"
        case .edit: return "Edit"
        case .members: return "Members"
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

private struct VersionCard: View {
    let version: ProjectVersion

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(alignment: .top, spacing: 12) {
                VStack(alignment: .leading, spacing: 5) {
                    HStack(spacing: 7) {
                        Circle()
                            .fill(versionTypeColor)
                            .frame(width: 8, height: 8)
                        Text(version.versionNumber)
                            .font(.headline.weight(.bold))
                            .lineLimit(1)
                    }
                    Text(version.name)
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                        .lineLimit(2)
                }
                Spacer(minLength: 8)
                VStack(alignment: .trailing, spacing: 5) {
                    HStack(spacing: 5) {
                        Image(systemName: "arrow.down")
                        Text(compact(version.downloads))
                    }
                    .font(.caption.weight(.bold))
                    .foregroundStyle(Color.rinthyGreen)
                    if let published = version.datePublished?.prefix(10) {
                        Text(String(published))
                            .font(.caption2)
                            .foregroundStyle(.secondary)
                    }
                }
            }

            if !version.gameVersions.isEmpty || !version.loaders.isEmpty {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 6) {
                        ForEach(Array(version.gameVersions.prefix(6)), id: \.self) { gameVersion in
                            Text(gameVersion)
                                .font(.caption2.weight(.semibold))
                                .padding(.horizontal, 8)
                                .padding(.vertical, 5)
                                .background(Color(uiColor: .tertiarySystemBackground), in: RoundedRectangle(cornerRadius: 7))
                        }
                        ForEach(Array(version.loaders.prefix(4)), id: \.self) { loader in
                            Text(loader.uppercased())
                                .font(.caption2.weight(.bold))
                                .foregroundStyle(Color.rinthyGreen)
                                .padding(.horizontal, 8)
                                .padding(.vertical, 5)
                                .background(Color.rinthyGreen.opacity(0.12), in: RoundedRectangle(cornerRadius: 7))
                        }
                    }
                }
            }
        }
        .padding(14)
        .background(Color(uiColor: .secondarySystemBackground), in: RoundedRectangle(cornerRadius: 12))
        .overlay {
            RoundedRectangle(cornerRadius: 12)
                .stroke(Color(uiColor: .separator), lineWidth: 0.5)
        }
    }

    private var versionTypeColor: Color {
        switch version.versionType {
        case "release": return .rinthyGreen
        case "beta": return .orange
        case "alpha": return .red
        default: return .secondary
        }
    }

    private func compact(_ value: Int64) -> String {
        if value >= 1_000_000 { return String(format: "%.1fM", Double(value) / 1_000_000) }
        if value >= 1_000 { return String(format: "%.1fK", Double(value) / 1_000) }
        return "\(value)"
    }
}

private struct MarkdownLine: Hashable {
    let text: String
    let isHeading: Bool
}

private extension String {
    func cleanMarkdown() -> String {
        replacingOccurrences(
            of: #"\[!\[[^\]]*\]\([^)]+\)\]\([^)]+\)"#,
            with: "",
            options: .regularExpression
        )
        .replacingOccurrences(
            of: #"!\[[^\]]*\]\([^)]+\)"#,
            with: "",
            options: .regularExpression
        )
        .replacingOccurrences(
            of: #"\[([^\]]*)\]\([^)]+\)"#,
            with: "$1",
            options: .regularExpression
        )
        .replacingOccurrences(of: "**", with: "")
        .replacingOccurrences(of: "__", with: "")
        .replacingOccurrences(of: "`", with: "")
        .trimmingCharacters(in: .whitespacesAndNewlines)
    }

    var markdownLines: [MarkdownLine] {
        split(separator: "\n")
            .map { String($0).trimmingCharacters(in: .whitespacesAndNewlines) }
            .filter { !$0.isEmpty }
            .map { line in
                let headingLevel = line.prefix { $0 == "#" }.count
                if headingLevel > 0 {
                    return MarkdownLine(
                        text: String(line.dropFirst(headingLevel)).trimmingCharacters(in: .whitespaces),
                        isHeading: true
                    )
                }
                if line.hasPrefix("- ") || line.hasPrefix("* ") {
                    return MarkdownLine(text: "• \(String(line.dropFirst(2)))", isHeading: false)
                }
                if line.hasPrefix("](") || line.hasPrefix(")(") || line.hasPrefix("(") {
                    return MarkdownLine(text: "", isHeading: false)
                }
                return MarkdownLine(text: line, isHeading: false)
            }
            .filter { !$0.text.isEmpty }
    }
}
