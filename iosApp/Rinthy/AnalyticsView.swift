import RinthyShared
import SwiftUI

struct AnalyticsView: View {
    let dashboard: Dashboard

    private var topProjects: [Project] {
        Array(dashboard.projects.sorted { $0.downloads > $1.downloads }.prefix(5))
    }

    private var largestDownloadCount: Int64 {
        max(topProjects.first?.downloads ?? 1, 1)
    }

    var body: some View {
        ScrollView {
            LazyVStack(alignment: .leading, spacing: 0) {
                Text("Workspace performance")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                    .padding(.bottom, 18)

                HStack(spacing: 8) {
                    metric(
                        "Downloads",
                        value: compact(dashboard.projects.reduce(0) { $0 + $1.downloads }),
                        symbol: "arrow.down"
                    )
                    metric(
                        "Followers",
                        value: compact(dashboard.projects.reduce(0) { $0 + $1.followers }),
                        symbol: "heart"
                    )
                    metric("Projects", value: "\(dashboard.projects.count)", symbol: "shippingbox")
                }

                sectionTitle("Top projects")
                if topProjects.isEmpty {
                    EmptyStateView(
                        title: "No analytics yet",
                        systemImage: "chart.bar",
                        message: "Project performance will appear after your projects load."
                    )
                } else {
                    ForEach(topProjects, id: \.id) { project in
                        projectPerformance(project)
                    }
                }

                sectionTitle("Project mix")
                ForEach(projectMix, id: \.name) { item in
                    HStack {
                        Text(item.name).foregroundStyle(.secondary)
                        Spacer()
                        Text("\(item.count)").fontWeight(.semibold)
                    }
                    .padding(.vertical, 8)
                }
            }
            .padding(.horizontal, 16)
            .padding(.top, 16)
            .padding(.bottom, 120)
        }
        .background(Color(uiColor: .systemBackground))
    }

    private var projectMix: [(name: String, count: Int)] {
        let grouped = Dictionary(grouping: dashboard.projects) { $0.projectType.capitalized }
        return grouped.map { (name: $0.key, count: $0.value.count) }
            .sorted { $0.count > $1.count }
    }

    private func metric(_ label: String, value: String, symbol: String) -> some View {
        VStack(alignment: .leading, spacing: 5) {
            Image(systemName: symbol)
                .font(.caption.weight(.semibold))
                .foregroundStyle(Color.rinthyGreen)
            Text(value).font(.headline).monospacedDigit()
            Text(label).font(.caption2).foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, 10)
        .padding(.vertical, 14)
        .background(Color(uiColor: .secondarySystemBackground), in: RoundedRectangle(cornerRadius: 10))
    }

    private func projectPerformance(_ project: Project) -> some View {
        VStack(spacing: 9) {
            HStack {
                Text(project.title).fontWeight(.semibold).lineLimit(1)
                Spacer(minLength: 12)
                Text(compact(project.downloads))
                    .font(.caption.weight(.medium))
                    .foregroundStyle(.secondary)
            }
            GeometryReader { proxy in
                ZStack(alignment: .leading) {
                    Capsule().fill(Color.secondary.opacity(0.18))
                    Capsule()
                        .fill(Color.rinthyGreen)
                        .frame(
                            width: proxy.size.width * CGFloat(Double(project.downloads) / Double(largestDownloadCount))
                        )
                }
            }
            .frame(height: 6)
        }
        .padding(.vertical, 11)
    }

    private func sectionTitle(_ title: String) -> some View {
        Text(title)
            .font(.title3.bold())
            .padding(.top, 30)
            .padding(.bottom, 10)
    }

    private func compact(_ value: Int64) -> String {
        if value >= 1_000_000 { return String(format: "%.1fM", Double(value) / 1_000_000) }
        if value >= 1_000 { return String(format: "%.1fK", Double(value) / 1_000) }
        return "\(value)"
    }
}
