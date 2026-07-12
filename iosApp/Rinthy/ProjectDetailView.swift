import RinthyShared
import SwiftUI

struct ProjectDetailView: View {
    let project: Project

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
            .padding(.horizontal, 16)
            .padding(.top, 12)
            .padding(.bottom, 36)
        }
        .background(Color(uiColor: .systemBackground))
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
