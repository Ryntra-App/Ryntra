import Foundation
import RyntraShared
import SwiftUI

struct AnalyticsWalletView: View {
    let report: WalletReport?
    let isLoading: Bool
    let errorMessage: String?

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack(spacing: 11) {
                Image(systemName: "creditcard.fill")
                    .font(.system(size: 17, weight: .semibold))
                    .foregroundStyle(Color.ryntraGreen)
                    .frame(width: 38, height: 38)
                    .background(Color.ryntraGreen.opacity(0.12), in: RoundedRectangle(cornerRadius: 9))
                VStack(alignment: .leading, spacing: 2) {
                    Text("Modrinth wallet").fontWeight(.semibold)
                    Text(subtitle).font(.caption2).foregroundStyle(.secondary)
                }
                Spacer()
                if isLoading { ProgressView().controlSize(.small).tint(.ryntraGreen) }
            }

            if let report, report.isAvailable {
                walletAmounts(report)
            } else if !isLoading {
                Text(unavailableMessage)
                    .font(.caption)
                    .foregroundStyle(.secondary)
                    .padding(.top, 15)
            }

            Link(destination: URL(string: "https://modrinth.com/dashboard/revenue")!) {
                HStack(spacing: 7) {
                    Text("Open Modrinth revenue").font(.subheadline.weight(.semibold))
                    Image(systemName: "arrow.up.right").font(.caption.weight(.semibold))
                }
                .foregroundStyle(Color.ryntraGreen)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 12)
                .background(Color.ryntraSurfaceRaised, in: RoundedRectangle(cornerRadius: 8))
            }
            .padding(.top, 15)
        }
        .padding(15)
        .background(Color.ryntraSurface, in: RoundedRectangle(cornerRadius: 10))
        .overlay {
            RoundedRectangle(cornerRadius: 10).stroke(Color.ryntraSeparator, lineWidth: 0.5)
        }
    }

    private var subtitle: String {
        if isLoading && report == nil {
            return NSLocalizedString("Loading balance and payouts", comment: "Wallet status")
        }
        return NSLocalizedString(
            report?.isAvailable == true ? "Balance and payout activity" : "Creator payout account",
            comment: "Wallet status"
        )
    }

    @ViewBuilder
    private func walletAmounts(_ report: WalletReport) -> some View {
        let primary = report.balance ?? report.available
        let stats = walletStats(report)

        if let primary {
            Text("CURRENT BALANCE")
                .font(.caption2.weight(.medium))
                .foregroundStyle(.secondary)
                .padding(.top, 20)
            Text(money(primary, currency: report.currency))
                .font(.title.bold())
                .monospacedDigit()
                .lineLimit(1)
                .minimumScaleFactor(0.72)
                .padding(.top, 3)
        }

        if !stats.isEmpty {
            Grid(horizontalSpacing: 18, verticalSpacing: 14) {
                ForEach(Array(stride(from: 0, to: stats.count, by: 2)), id: \.self) { index in
                    GridRow {
                        value(stats[index].label, money(stats[index].amount, currency: report.currency))
                        if stats.indices.contains(index + 1) {
                            value(stats[index + 1].label, money(stats[index + 1].amount, currency: report.currency))
                        } else {
                            Color.clear
                        }
                    }
                }
            }
            .padding(.top, 18)
        } else if primary == nil {
            Text("No balance details were returned for this account.")
                .font(.caption)
                .foregroundStyle(.secondary)
                .padding(.top, 15)
        }
    }

    private func value(_ label: String, _ value: String) -> some View {
        VStack(alignment: .leading, spacing: 3) {
            Text(label).font(.caption2).foregroundStyle(.secondary)
            Text(value).font(.subheadline.weight(.semibold)).lineLimit(1).minimumScaleFactor(0.72)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private struct WalletStat {
        let label: String
        let amount: KotlinDouble
    }

    private func walletStats(_ report: WalletReport) -> [WalletStat] {
        var stats: [WalletStat] = []
        if report.balance != nil, let available = report.available { stats.append(.init(label: NSLocalizedString("Available now", comment: "Wallet stat"), amount: available)) }
        if let pending = report.pending { stats.append(.init(label: NSLocalizedString("Pending", comment: "Wallet stat"), amount: pending)) }
        if let paid = report.withdrawnLifetime { stats.append(.init(label: NSLocalizedString("Paid out", comment: "Wallet stat"), amount: paid)) }
        if let lifetime = report.lifetimeEarnings { stats.append(.init(label: NSLocalizedString("Lifetime earnings", comment: "Wallet stat"), amount: lifetime)) }
        return stats
    }

    private func money(_ value: KotlinDouble, currency: String) -> String {
        value.doubleValue.formatted(.currency(code: currency).precision(.fractionLength(2)))
    }

    private var unavailableMessage: String {
        if let errorMessage { return errorMessage }
        guard let report else { return NSLocalizedString("Wallet data could not be loaded.", comment: "Wallet error") }
        if report.balanceStatus == 0, report.historyStatus == 0 {
            return NSLocalizedString("Modrinth wallet could not be reached. Try refreshing.", comment: "Wallet error")
        }
        if [401, 403].contains(Int(report.balanceStatus)) || [401, 403].contains(Int(report.historyStatus)) {
            return NSLocalizedString("Connect again to allow creator payout access.", comment: "Wallet error")
        }
        return NSLocalizedString("No wallet details were returned for this account.", comment: "Wallet error")
    }

}

struct AnalyticsPayoutHistoryView: View {
    let report: WalletReport

    var body: some View {
        ForEach(Array(report.transactions.prefix(10).enumerated()), id: \.offset) { _, payout in
            HStack(spacing: 11) {
                Image(systemName: "clock")
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(statusColor(payout.status))
                VStack(alignment: .leading, spacing: 2) {
                    Text(payout.status.isEmpty ? NSLocalizedString("Payout", comment: "Payout status") : payout.status.capitalized)
                        .font(.subheadline.weight(.semibold))
                    Text(payoutDate(payout.created)).font(.caption).foregroundStyle(.secondary)
                }
                Spacer(minLength: 12)
                Text(payout.amount.formatted(.currency(code: report.currency).precision(.fractionLength(2))))
                    .font(.subheadline.weight(.bold))
                    .monospacedDigit()
                    .foregroundStyle(statusColor(payout.status))
            }
            .padding(.vertical, 11)
        }
    }

    private func payoutDate(_ value: String) -> String {
        guard value.count >= 10 else { return value.isEmpty ? "Date unavailable" : value }
        return String(value.prefix(10))
    }

    private func statusColor(_ status: String) -> Color {
        switch status.lowercased() {
        case "paid", "sent", "completed", "success": return .ryntraGreen
        case "pending", "processing": return .orange
        case "failed", "cancelled", "canceled": return .red
        default: return .secondary
        }
    }
}
