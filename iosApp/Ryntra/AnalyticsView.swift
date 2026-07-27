import Foundation
import RyntraShared
import SwiftUI

enum AnalyticsMetric: String, CaseIterable, Identifiable {
    case downloads = "Downloads"
    case views = "Views"
    case playtime = "Playtime"
    case revenue = "Revenue"

    var id: String { rawValue }
    var localizedLabel: String { NSLocalizedString(rawValue, comment: "Analytics metric") }

    var symbol: String {
        switch self {
        case .downloads: return "arrow.down"
        case .views: return "eye"
        case .playtime: return "clock"
        case .revenue: return "dollarsign"
        }
    }

    var color: Color {
        switch self {
        case .downloads: return .analyticsBlue
        case .views: return .analyticsViolet
        case .playtime: return .analyticsOrange
        case .revenue: return .analyticsGreen
        }
    }

    func value(_ metrics: AnalyticsMetrics) -> Double {
        switch self {
        case .downloads: return metrics.downloads
        case .views: return metrics.views
        case .playtime: return metrics.playtimeSeconds
        case .revenue: return metrics.revenue
        }
    }

    func formatted(_ value: Double) -> String {
        switch self {
        case .downloads, .views:
            return Int64(value.rounded()).formatted(.number.grouping(.automatic))
        case .playtime:
            let minutes = max(Int64((value / 60).rounded()), 0)
            if minutes < 60 { return "\(minutes.formatted(.number.grouping(.automatic)))m" }
            let hours = minutes / 60
            let remainder = minutes % 60
            return remainder == 0 ? "\(hours.formatted(.number.grouping(.automatic)))h" : "\(hours.formatted(.number.grouping(.automatic)))h \(remainder)m"
        case .revenue:
            return value.formatted(.currency(code: "USD").precision(.fractionLength(2)))
        }
    }
}

struct ProjectInsight: Identifiable {
    let project: Project
    let metrics: AnalyticsMetrics

    var id: String { project.id }
}

private struct AnalyticsMetricDisplay: Identifiable {
    let metric: AnalyticsMetric?
    let label: String
    let value: String
    let symbol: String
    let change: Double?

    var id: String { label }
}

struct AnalyticsView: View {
    /// The surface of a card inside a grouped list, per platform.
    private static var groupedSurface: Color {
#if canImport(UIKit)
        Color(uiColor: .secondarySystemGroupedBackground)
#elseif canImport(AppKit)
        Color(nsColor: .controlBackgroundColor)
#endif
    }

    @EnvironmentObject private var model: AppModel
    @Environment(\.accessibilityReduceMotion) private var systemReduceMotion
    @AppStorage("reduceMotion") private var appReduceMotion = false
    @AppStorage("themeStyle") private var storedThemeStyle = RyntraThemeStyle.platform.rawValue

    let dashboard: Dashboard
    let isActive: Bool
    @State private var rangeDays = 30
    @State private var selectedMetric = AnalyticsMetric.downloads
    @State private var selectedProjectID: String?

    private var report: AnalyticsReport? { model.analyticsReport }
    private var lifetimeDownloads: Int64 { dashboard.projects.reduce(0) { $0 + $1.downloads } }
    private var lifetimeFollowers: Int64 { dashboard.projects.reduce(0) { $0 + $1.followers } }
    private var currentMetrics: AnalyticsMetrics {
        if let selectedProjectID { return report?.projectMetrics(projectId: selectedProjectID) ?? emptyAnalyticsMetrics }
        return report?.periodTotals ?? emptyAnalyticsMetrics
    }
    private var previousMetrics: AnalyticsMetrics {
        if let selectedProjectID { return report?.previousProjectMetrics(projectId: selectedProjectID) ?? emptyAnalyticsMetrics }
        return report?.previousPeriodTotals ?? emptyAnalyticsMetrics
    }
    private var insights: [ProjectInsight] {
        dashboard.projects
            .filter { selectedProjectID == nil || $0.id == selectedProjectID }
            .map { ProjectInsight(project: $0, metrics: report?.projectMetrics(projectId: $0.id) ?? emptyAnalyticsMetrics) }
            .sorted { selectedMetric.value($0.metrics) > selectedMetric.value($1.metrics) }
    }

    var body: some View {
        ScrollView {
            LazyVStack(alignment: .leading, spacing: 0) {
                performanceHeader
                sectionTitle("Wallet")
                AnalyticsWalletView(
                    report: model.walletReport,
                    isLoading: model.isAnalyticsLoading && model.walletReport == nil,
                    errorMessage: model.walletError
                )
                if let wallet = model.walletReport, !wallet.transactions.isEmpty {
                    sectionTitle("Recent payouts")
                    AnalyticsPayoutHistoryView(report: wallet)
                }

                projectPicker.padding(.top, 12)

                sectionTitle("Last \(rangeDays) days")
                if let error = model.analyticsError {
                    notice(error)
                } else if let report, !report.isCoreAvailable {
                    notice(analyticsAvailabilityMessage(report))
                }
                periodMetrics

                sectionTitle("Trend")
                metricPicker
                AnalyticsTrendView(
                    report: report,
                    projects: dashboard.projects,
                    selectedProjectID: selectedProjectID,
                    metric: selectedMetric,
                    rangeDays: rangeDays
                )
                .padding(.top, 12)

                sectionTitle("Breakdown")
                Text("Exact project totals · ranked by \(selectedMetric.rawValue.lowercased())")
                    .font(.caption)
                    .foregroundStyle(.secondary)
                    .padding(.bottom, 6)
                if insights.isEmpty {
                    notice(report?.isCoreAvailable == true ? "No activity in this range." : "Project range data is unavailable.")
                } else {
                    ForEach(insights) { insight in
                        AnalyticsBreakdownRow(
                            insight: insight,
                            previous: report?.previousProjectMetrics(projectId: insight.project.id) ?? emptyAnalyticsMetrics,
                            metric: selectedMetric,
                            total: selectedMetric.value(currentMetrics)
                        )
                    }
                }

                sectionTitle("Lifetime")
                Text("Exact totals from your Modrinth projects")
                    .font(.caption)
                    .foregroundStyle(.secondary)
                    .padding(.bottom, 12)
                lifetimeMetrics
            }
            .padding(.horizontal, 16)
            .padding(.top, 8)
            .padding(.bottom, isPlatformNative ? 20 : 96)
        }
        .background(Color.ryntraBackground)
        .task(id: analyticsTaskKey) {
            guard isActive else { return }
            await model.loadAnalytics(projects: dashboard.projects, rangeDays: rangeDays)
        }
        .onChange(of: dashboard.projects.map(\.id)) { projectIDs in
            if let selectedProjectID, !projectIDs.contains(selectedProjectID) { self.selectedProjectID = nil }
        }
    }

    private var isPlatformNative: Bool {
        storedThemeStyle == RyntraThemeStyle.platform.rawValue
    }

    private var reduceMotion: Bool {
        systemReduceMotion || appReduceMotion
    }

    private var analyticsTaskKey: String {
        "\(isActive):\(rangeDays):\(dashboard.projects.map(\.id).joined(separator: ","))"
    }

    private var performanceHeader: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                VStack(alignment: .leading, spacing: 4) {
                    Text("Performance").font(.title2.bold())
                    HStack(spacing: 6) {
                        if model.isAnalyticsLoading {
                            ProgressView().controlSize(.small).tint(.ryntraGreen)
                        } else {
                            Circle()
                                .fill(report?.isCoreAvailable == true ? Color.ryntraGreen : Color.orange)
                                .frame(width: 7, height: 7)
                        }
                        Text(
                            model.isAnalyticsLoading
                                ? NSLocalizedString("Refreshing data", comment: "Analytics")
                                : report?.isCoreAvailable == true
                                    ? NSLocalizedString("Live analytics", comment: "Analytics")
                                    : NSLocalizedString("Limited analytics", comment: "Analytics")
                        )
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    }
                }
                Spacer()
            }
            Picker("Range", selection: $rangeDays) {
                Text("7D").tag(7)
                Text("30D").tag(30)
                Text("90D").tag(90)
                Text("180D").tag(180)
            }
            .pickerStyle(.segmented)
        }
    }

    private var projectPicker: some View {
        Menu {
            Button("All projects") { selectedProjectID = nil }
            ForEach(dashboard.projects, id: \.id) { project in
                Button(project.title) { selectedProjectID = project.id }
            }
        } label: {
            HStack(spacing: 10) {
                Image(systemName: "square.3.layers.3d")
                    .foregroundStyle(Color.ryntraGreen)
                VStack(alignment: .leading, spacing: 2) {
                    Text("Project").font(.caption2).foregroundStyle(.secondary)
                    Text(dashboard.projects.first { $0.id == selectedProjectID }?.title ?? "All projects")
                        .font(.subheadline.weight(.semibold))
                        .lineLimit(1)
                }
                Spacer()
                Image(systemName: "chevron.up.chevron.down").font(.caption).foregroundStyle(.secondary)
            }
            .padding(12)
            .background(Color.ryntraSurface, in: RoundedRectangle(cornerRadius: 9))
            .overlay { RoundedRectangle(cornerRadius: 9).stroke(Color.ryntraSeparator, lineWidth: 0.5) }
        }
        .buttonStyle(.plain)
    }

    private var periodMetrics: some View {
        let isCoreAvailable = report?.isCoreAvailable != false
        return metricGrid([
            display(.downloads, available: isCoreAvailable),
            display(.views, available: isCoreAvailable),
            display(.playtime, available: isCoreAvailable),
            display(.revenue, available: report?.isRevenueAvailable == true),
        ])
    }

    private var lifetimeMetrics: some View {
        VStack(spacing: 0) {
            HStack(spacing: 0) {
                lifetimeMetric(
                    "Downloads",
                    value: lifetimeDownloads.formatted(.number.grouping(.automatic)),
                    symbol: "arrow.down",
                    tint: .analyticsBlue
                )
                Divider().frame(height: 56)
                lifetimeMetric(
                    "Followers",
                    value: lifetimeFollowers.formatted(.number.grouping(.automatic)),
                    symbol: "heart",
                    tint: .analyticsPink
                )
            }
            Divider()
            HStack(spacing: 0) {
                lifetimeMetric(
                    "Projects",
                    value: dashboard.projects.count.formatted(),
                    symbol: "shippingbox",
                    tint: .ryntraGreen
                )
                Divider().frame(height: 56)
                lifetimeMetric(
                    "Active",
                    value: dashboard.projects.filter { $0.status == "approved" }.count.formatted(),
                    symbol: "checkmark.circle",
                    tint: .ryntraGreen
                )
            }
        }
        .background(
            isPlatformNative ? Self.groupedSurface : Color.ryntraSurface,
            in: RoundedRectangle(cornerRadius: 12, style: .continuous)
        )
        .overlay {
            RoundedRectangle(cornerRadius: 12, style: .continuous)
                .stroke(Color.ryntraSeparator, lineWidth: 0.5)
        }
        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
    }

    private func lifetimeMetric(_ label: String, value: String, symbol: String, tint: Color) -> some View {
        HStack(spacing: 10) {
            Image(systemName: symbol)
                .font(.body.weight(.semibold))
                .foregroundStyle(tint)
                .frame(width: 24)
            VStack(alignment: .leading, spacing: 3) {
                Text(label)
                    .font(.caption)
                    .foregroundStyle(.secondary)
                Text(value)
                    .font(.title3.weight(.semibold))
                    .foregroundStyle(.primary)
                    .monospacedDigit()
                    .lineLimit(1)
                    .minimumScaleFactor(0.75)
            }
        }
        .frame(maxWidth: .infinity, minHeight: 74, alignment: .leading)
        .padding(.horizontal, 14)
        .accessibilityElement(children: .combine)
    }

    private func display(_ metric: AnalyticsMetric, available: Bool = true) -> AnalyticsMetricDisplay {
        let current = metric.value(currentMetrics)
        let previous = metric.value(previousMetrics)
        return AnalyticsMetricDisplay(
            metric: metric,
            label: metric.localizedLabel,
            value: available ? metric.formatted(current) : "Unavailable",
            symbol: metric.symbol,
            change: available ? report?.percentageChange(current: current, previous: previous) : nil
        )
    }

    private func analyticsAvailabilityMessage(_ report: AnalyticsReport) -> String {
        switch report.coreStatus {
        case 401:
            return "Analytics needs a fresh Modrinth sign-in. Sign out and connect again."
        case 403:
            return "This Modrinth token cannot read analytics. Sign in with analytics permission enabled."
        case 429:
            return "Modrinth is rate limiting analytics right now. Try again shortly."
        case 0:
            return "Analytics could not be decoded from Modrinth. Lifetime totals remain exact."
        default:
            return "Modrinth analytics request failed (\(report.coreStatus)). Lifetime totals remain exact."
        }
    }

    private func metricGrid(_ values: [AnalyticsMetricDisplay]) -> some View {
        Grid(horizontalSpacing: 8, verticalSpacing: 8) {
            ForEach(0..<2, id: \.self) { row in
                GridRow {
                    metricCard(values[row * 2])
                    metricCard(values[row * 2 + 1])
                }
            }
        }
    }

    private func metricCard(_ item: AnalyticsMetricDisplay) -> some View {
        Button {
            guard let metric = item.metric else { return }
            withAnimation(RyntraMotion.resolved(RyntraMotion.control, reduceMotion: reduceMotion)) { selectedMetric = metric }
        } label: {
            VStack(alignment: .leading, spacing: 6) {
                HStack(spacing: 6) {
                    Image(systemName: item.symbol)
                        .font(.caption.weight(.semibold))
                        .foregroundStyle(item.metric?.color ?? Color.secondary)
                    Text(item.label).font(.caption2).foregroundStyle(.secondary)
                }
                Text(item.value).font(.headline).monospacedDigit().lineLimit(1).minimumScaleFactor(0.72)
                if let change = item.change {
                    Text("\(change.formatted(.number.sign(strategy: .always()).precision(.fractionLength(1))))% vs previous")
                        .font(.caption2)
                        .foregroundStyle(change >= 0 ? Color.ryntraGreen : Color.red)
                        .lineLimit(1)
                }
            }
            .frame(maxWidth: .infinity, minHeight: item.change == nil ? 54 : 76, alignment: .leading)
            .padding(12)
            .background(selectedMetric == item.metric ? Color.ryntraSurfaceRaised : Color.ryntraSurface, in: RoundedRectangle(cornerRadius: 8))
            .overlay {
                RoundedRectangle(cornerRadius: 8)
                    .stroke(selectedMetric == item.metric ? item.metric?.color ?? Color.ryntraGreen : Color.ryntraSeparator, lineWidth: selectedMetric == item.metric ? 1 : 0.5)
            }
        }
        .buttonStyle(.plain)
        .accessibilityAddTraits(item.metric == nil ? .isStaticText : .isButton)
    }

    private var metricPicker: some View {
        HStack(spacing: 4) {
            ForEach(AnalyticsMetric.allCases) { metric in
                Button {
                    withAnimation(RyntraMotion.resolved(RyntraMotion.control, reduceMotion: reduceMotion)) { selectedMetric = metric }
                } label: {
                    Image(systemName: metric.symbol)
                        .frame(maxWidth: .infinity)
                        .frame(height: 40)
                        .foregroundStyle(selectedMetric == metric ? metric.color : Color.secondary)
                        .background(
                            selectedMetric == metric ? Color.ryntraSurfaceRaised : Color.clear,
                            in: RoundedRectangle(cornerRadius: 9, style: .continuous)
                        )
                }
                .buttonStyle(.plain)
                .accessibilityLabel(metric.localizedLabel)
            }
        }
        .padding(4)
        .background(Color.ryntraSurface, in: RoundedRectangle(cornerRadius: 12, style: .continuous))
        .overlay {
            RoundedRectangle(cornerRadius: 12, style: .continuous)
                .stroke(Color.ryntraSeparator, lineWidth: 0.5)
        }
    }

    private func sectionTitle(_ title: String) -> some View {
        RyntraSectionLabel(text: title).padding(.top, 20).padding(.bottom, 8)
    }

    private func notice(_ message: String) -> some View {
        Text(message)
            .font(.caption)
            .foregroundStyle(.secondary)
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(12)
            .background(Color.ryntraSurface, in: RoundedRectangle(cornerRadius: 8))
    }
}

private let emptyAnalyticsMetrics = AnalyticsMetrics(downloads: 0, views: 0, playtimeSeconds: 0, revenue: 0)
