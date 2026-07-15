import RyntraShared
import SwiftUI

struct AnalyticsBreakdownRow: View {
    let insight: ProjectInsight
    let previous: AnalyticsMetrics
    let metric: AnalyticsMetric
    let total: Double

    private var value: Double { metric.value(insight.metrics) }
    private var projectColor: Color { analyticsProjectColor(insight.project.id) }
    private var change: Double {
        let previousValue = metric.value(previous)
        if previousValue == 0 { return value == 0 ? 0 : 100 }
        return ((value - previousValue) / previousValue) * 100
    }

    var body: some View {
        HStack(spacing: 11) {
            AsyncImage(url: URL(string: insight.project.iconUrl ?? "")) { image in
                image.resizable().scaledToFill()
            } placeholder: {
                RoundedRectangle(cornerRadius: 8).fill(Color.ryntraSurfaceRaised)
            }
            .frame(width: 42, height: 42)
            .clipShape(RoundedRectangle(cornerRadius: 8))

            VStack(alignment: .leading, spacing: 3) {
                Text(insight.project.title)
                    .font(.subheadline.weight(.semibold))
                    .lineLimit(1)
                Text("\(AnalyticsMetric.views.formatted(insight.metrics.views)) views · \(AnalyticsMetric.downloads.formatted(insight.metrics.downloads)) downloads")
                    .font(.caption2)
                    .foregroundStyle(.secondary)
                    .lineLimit(1)
                Text("\(AnalyticsMetric.revenue.formatted(insight.metrics.revenue)) · \(AnalyticsMetric.playtime.formatted(insight.metrics.playtimeSeconds))")
                    .font(.caption2)
                    .foregroundStyle(.secondary)
            }
            Spacer(minLength: 8)
            VStack(alignment: .trailing, spacing: 3) {
                Text(metric.formatted(value))
                    .font(.subheadline.bold())
                    .foregroundStyle(projectColor)
                    .monospacedDigit()
                Text("\(change.formatted(.number.sign(strategy: .always()).precision(.fractionLength(1))))% · \((value / max(total, 1) * 100).formatted(.number.precision(.fractionLength(0))))%")
                    .font(.caption2)
                    .foregroundStyle(change >= 0 ? Color.ryntraGreen : Color.red)
                    .monospacedDigit()
                    .lineLimit(1)
            }
        }
        .padding(.vertical, 11)
    }
}
