import Charts
import Foundation
import RyntraShared
import SwiftUI

private enum AnalyticsChartStyle: String, CaseIterable, Identifiable {
    case line = "Line"
    case area = "Area"
    case bar = "Bar"

    var id: String { rawValue }
}

private struct AnalyticsSeriesPoint: Identifiable {
    let projectID: String
    let title: String
    let index: Int
    let value: Double

    var id: String { "\(projectID)-\(index)" }
}

private struct AnalyticsEventMarker: Identifiable {
    let index: Int
    let event: AnalyticsProjectEvent

    var id: String { "\(event.projectId)-\(event.timestamp)-\(event.kind)" }
}

struct AnalyticsTrendView: View {
    let report: AnalyticsReport?
    let projects: [Project]
    let selectedProjectID: String?
    let metric: AnalyticsMetric
    let rangeDays: Int

    @State private var style = AnalyticsChartStyle.line
    @State private var selectedIndex: Int?

    private var points: [AnalyticsPoint] {
        metric == .revenue ? report?.revenuePoints ?? [] : report?.points ?? []
    }

    private var visibleProjects: [Project] {
        if let selectedProjectID {
            return projects.filter { $0.id == selectedProjectID }
        }
        return projects
            .sorted { metric.value(report?.projectMetrics(projectId: $0.id) ?? zeroMetrics) > metric.value(report?.projectMetrics(projectId: $1.id) ?? zeroMetrics) }
            .filter { metric.value(report?.projectMetrics(projectId: $0.id) ?? zeroMetrics) > 0 }
            .prefix(5)
            .map { $0 }
    }

    private var series: [AnalyticsSeriesPoint] {
        visibleProjects.flatMap { project in
            points.enumerated().map { index, point in
                AnalyticsSeriesPoint(
                    projectID: project.id,
                    title: project.title,
                    index: index,
                    value: metric.value(point.projects[project.id] ?? zeroMetrics)
                )
            }
        }
    }

    private var aggregateValues: [Double] {
        points.map { point in
            guard let selectedProjectID else { return metric.value(point.metrics) }
            return metric.value(point.projects[selectedProjectID] ?? zeroMetrics)
        }
    }

    private var activeIndex: Int {
        min(max(selectedIndex ?? aggregateValues.indices.last ?? 0, 0), max(aggregateValues.count - 1, 0))
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(alignment: .center) {
                VStack(alignment: .leading, spacing: 2) {
                    Text(metric.formatted(aggregateValues[safe: activeIndex] ?? 0))
                        .font(.title2.bold())
                        .monospacedDigit()
                    Text(dateLabel(activeIndex))
                        .font(.caption2)
                        .foregroundStyle(.secondary)
                }
                Spacer()
                Picker("Chart style", selection: $style) {
                    ForEach(AnalyticsChartStyle.allCases) { value in
                        Text(value.rawValue).tag(value)
                    }
                }
                .pickerStyle(.segmented)
                .frame(maxWidth: 190)
            }

            if aggregateValues.contains(where: { $0 > 0 }) {
                chart
                    .frame(height: 220)
                HStack {
                    Text(dateLabel(0))
                    Spacer()
                    Text("Peak \(metric.formatted(aggregateValues.max() ?? 0))")
                    Spacer()
                    Text(dateLabel(max(aggregateValues.count - 1, 0)))
                }
                .font(.caption2)
                .foregroundStyle(.secondary)
            } else {
                Text("No \(metric.rawValue.lowercased()) recorded for this range.")
                    .font(.caption)
                    .foregroundStyle(.secondary)
                    .padding(.vertical, 36)
            }
        }
        .padding(14)
        .background(Color.ryntraSurface, in: RoundedRectangle(cornerRadius: 10))
        .overlay {
            RoundedRectangle(cornerRadius: 10).stroke(Color.ryntraSeparator, lineWidth: 0.5)
        }
        .onChange(of: metric) { _ in selectedIndex = nil }
    }

    private var chart: some View {
        Chart {
            ForEach(series) { point in
                switch style {
                case .line:
                    LineMark(
                        x: .value("Day", point.index),
                        y: .value(metric.rawValue, point.value),
                        series: .value("Project", point.title)
                    )
                    .interpolationMethod(.linear)
                    .lineStyle(StrokeStyle(lineWidth: 2.2, lineCap: .round, lineJoin: .round))
                    .foregroundStyle(by: .value("Project", point.title))
                case .area:
                    AreaMark(
                        x: .value("Day", point.index),
                        y: .value(metric.rawValue, point.value),
                        series: .value("Project", point.title)
                    )
                    .interpolationMethod(.linear)
                    .foregroundStyle(by: .value("Project", point.title))
                    .opacity(0.28)
                    LineMark(
                        x: .value("Day", point.index),
                        y: .value(metric.rawValue, point.value),
                        series: .value("Project", point.title)
                    )
                    .foregroundStyle(by: .value("Project", point.title))
                case .bar:
                    BarMark(
                        x: .value("Day", point.index),
                        y: .value(metric.rawValue, point.value)
                    )
                    .position(by: .value("Project", point.title))
                    .foregroundStyle(by: .value("Project", point.title))
                }
            }
            ForEach(eventMarkers) { marker in
                RuleMark(x: .value("Project event", marker.index))
                    .foregroundStyle(Color.secondary.opacity(0.5))
                    .lineStyle(StrokeStyle(lineWidth: 1, dash: [4, 4]))
            }
            if !aggregateValues.isEmpty {
                RuleMark(x: .value("Selected day", activeIndex))
                    .foregroundStyle(Color.secondary.opacity(0.7))
            }
        }
        .chartForegroundStyleScale(
            domain: visibleProjects.map(\.title),
            range: visibleProjects.map { analyticsProjectColor($0.id) }
        )
        .chartLegend(visibleProjects.count > 1 ? .visible : .hidden)
        .chartXAxis {
            AxisMarks(values: .automatic(desiredCount: 3)) { value in
                AxisGridLine().foregroundStyle(Color.ryntraSeparator.opacity(0.55))
                AxisValueLabel {
                    if let index = value.as(Int.self) { Text(dateLabel(index)) }
                }
            }
        }
        .chartYAxis {
            AxisMarks(position: .leading) { _ in
                AxisGridLine().foregroundStyle(Color.ryntraSeparator.opacity(0.55))
                AxisValueLabel().foregroundStyle(.secondary)
            }
        }
        .chartOverlay { proxy in
            GeometryReader { geometry in
                Rectangle()
                    .fill(.clear)
                    .contentShape(Rectangle())
                    .gesture(
                        DragGesture(minimumDistance: 0)
                            .onChanged { value in
                                let frame = geometry[proxy.plotAreaFrame]
                                let x = value.location.x - frame.origin.x
                                if let index: Int = proxy.value(atX: x) {
                                    selectedIndex = min(max(index, 0), max(aggregateValues.count - 1, 0))
                                }
                            }
                    )
            }
        }
    }

    private var eventMarkers: [AnalyticsEventMarker] {
        guard
            let report,
            let start = ISO8601DateFormatter().date(from: report.periodStartTime),
            let end = ISO8601DateFormatter().date(from: report.periodEndTime),
            end > start,
            points.count > 1
        else { return [] }
        let duration = end.timeIntervalSince(start)
        return report.events.compactMap { event in
            guard
                selectedProjectID == nil || event.projectId == selectedProjectID,
                let date = ISO8601DateFormatter().date(from: event.timestamp),
                date >= start,
                date <= end
            else { return nil }
            let ratio = date.timeIntervalSince(start) / duration
            return AnalyticsEventMarker(index: Int((ratio * Double(points.count - 1)).rounded()), event: event)
        }
    }

    private func dateLabel(_ index: Int) -> String {
        guard !aggregateValues.isEmpty else { return "" }
        let daysAgo = max(aggregateValues.count - 1 - index, 0)
        let date = Calendar.current.date(byAdding: .day, value: -daysAgo, to: Date()) ?? Date()
        return date.formatted(.dateTime.day().month(.abbreviated))
    }
}

private let zeroMetrics = AnalyticsMetrics(downloads: 0, views: 0, playtimeSeconds: 0, revenue: 0)

func analyticsProjectColor(_ projectID: String) -> Color {
    let scalar = projectID.unicodeScalars.reduce(0) { ($0 &* 31) &+ Int($1.value) }
    let index = Int(UInt(bitPattern: scalar) % UInt(Color.analyticsSeries.count))
    return Color.analyticsSeries[index]
}

private extension Collection {
    subscript(safe index: Index) -> Element? {
        indices.contains(index) ? self[index] : nil
    }
}
