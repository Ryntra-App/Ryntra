import CoreTransferable
import Foundation
import RyntraShared
import SwiftUI
import UniformTypeIdentifiers

private let modrinthRevenueURL = URL(string: "https://modrinth.com/dashboard/revenue")!
private let modrinthRevenueInfoURL = URL(string: "https://modrinth.com/legal/cmp-info#pending")!
private let modrinthSupportURL = URL(string: "https://support.modrinth.com")!

/// The wallet screen pushed from Analytics
struct WalletScreen: View {
    @EnvironmentObject private var model: AppModel
    @AppStorage("themeStyle") private var storedThemeStyle = RyntraThemeStyle.platform.rawValue

    private var isPlatformNative: Bool {
        storedThemeStyle == RyntraThemeStyle.platform.rawValue
    }

    var body: some View {
        ScrollView {
            AnalyticsWalletView(
                report: model.walletReport,
                isLoading: model.isWalletLoading && model.walletReport == nil,
                errorMessage: model.walletError,
                isPlatformNative: isPlatformNative
            )
            .padding(.horizontal, 16)
            .padding(.top, 8)
            .padding(.bottom, isPlatformNative ? 20 : 96)
        }
        .ryntraScreenBackdrop()
        .refreshable { await model.refreshWallet() }
    }
}

/// The creator wallet, laid out like modrinth.com/dashboard/revenue: the balance split by
/// payout date, tax status, withdrawal and the latest transactions. Withdrawing stays on the
/// site because it runs through Modrinth's tax form and payout-method checks.
///
/// Given `onOpenWallet`, it shrinks to the balance card Analytics shows above its charts
struct AnalyticsWalletView: View {
    @EnvironmentObject private var model: AppModel
    let report: WalletReport?
    let isLoading: Bool
    let errorMessage: String?
    let isPlatformNative: Bool
    var onOpenWallet: (() -> Void)?
    @State private var isHistoryPresented = false

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            if let report, report.isBalanceAvailable, let onOpenWallet {
                WalletComplianceNotices(report: report)
                WalletCard(isPlatformNative: isPlatformNative) {
                    WalletBalanceBreakdown(report: report, showsDetails: false)
                    Button(action: onOpenWallet) {
                        Label(NSLocalizedString("Open wallet", comment: "Wallet action"), systemImage: "chevron.right")
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.borderedProminent)
                    .padding(.top, 8)
                }
            } else if let report, report.isBalanceAvailable {
                WalletComplianceNotices(report: report)
                WalletCard(isPlatformNative: isPlatformNative) {
                    WalletBalanceBreakdown(report: report)
                    Link(destination: modrinthRevenueInfoURL) {
                        Text(NSLocalizedString("How Modrinth handles revenue", comment: "Wallet link"))
                            .font(.subheadline.weight(.semibold))
                    }
                    .padding(.top, 8)
                }
                withdrawCard(report)
                totals(report)
                if model.affiliateReport != nil || model.isAffiliateLoading || model.affiliateError != nil {
                    WalletAffiliateSection(isPlatformNative: isPlatformNative)
                }
                transactions(report)
            } else {
                WalletCard(isPlatformNative: isPlatformNative) {
                    if isLoading {
                        HStack(spacing: 10) {
                            ProgressView()
                            Text(NSLocalizedString("Loading balance and payouts", comment: "Wallet status"))
                                .foregroundStyle(.secondary)
                        }
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 24)
                    } else {
                        Text(walletUnavailableMessage(report: report, errorMessage: errorMessage))
                            .font(.subheadline)
                            .foregroundStyle(.secondary)
                        Button(NSLocalizedString("Retry", comment: "Wallet action")) {
                            Task { await model.refreshWallet() }
                        }
                        .padding(.top, 8)
                    }
                }
            }
        }
        .task {
            guard onOpenWallet == nil else { return }
            await model.loadAffiliateReport()
        }
        .sheet(isPresented: $isHistoryPresented) {
            if let report {
                WalletHistoryView(report: report)
                    .environmentObject(model)
            }
        }
    }

    private func withdrawCard(_ report: WalletReport) -> some View {
        WalletCard(isPlatformNative: isPlatformNative) {
            Text(NSLocalizedString("Withdraw", comment: "Wallet section")).font(.headline)
            Text(NSLocalizedString(
                report.isWithdrawalLocked
                    ? "Withdrawals are locked until your tax form is resolved with Modrinth support."
                    : "Withdraw from your available balance to any payout method. Withdrawals open on modrinth.com, where Modrinth handles payout methods and tax forms.",
                comment: "Wallet withdraw description"
            ))
            .font(.subheadline)
            .foregroundStyle(.secondary)
            Link(destination: modrinthRevenueURL) {
                Label(
                    String.localizedStringWithFormat(
                        NSLocalizedString("Withdraw %@ on Modrinth", comment: "Wallet withdraw button"),
                        walletMoney(report.available)
                    ),
                    systemImage: "arrow.up.right"
                )
                .frame(maxWidth: .infinity)
            }
            .buttonStyle(.borderedProminent)
            .disabled(report.isWithdrawalLocked || report.available <= 0)
            .padding(.top, 8)
        }
    }

    private func totals(_ report: WalletReport) -> some View {
        HStack(alignment: .top, spacing: 12) {
            WalletCard(isPlatformNative: isPlatformNative) {
                totalValue(NSLocalizedString("Withdrawn this year", comment: "Wallet total"), report.withdrawnThisYear)
                if let threshold = report.taxFormThreshold {
                    Text(String.localizedStringWithFormat(
                        NSLocalizedString("A tax form is required after %@ a year.", comment: "Wallet tax threshold"),
                        walletMoney(threshold.doubleValue)
                    ))
                    .font(.caption)
                    .foregroundStyle(.secondary)
                }
            }
            WalletCard(isPlatformNative: isPlatformNative) {
                totalValue(NSLocalizedString("Withdrawn all time", comment: "Wallet total"), report.withdrawnLifetime)
            }
        }
    }

    private func totalValue(_ label: String, _ amount: Double) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(label).font(.caption).foregroundStyle(.secondary)
            Text(walletMoney(amount))
                .font(.title3.bold())
                .monospacedDigit()
                .lineLimit(1)
                .minimumScaleFactor(0.7)
        }
    }

    @ViewBuilder
    private func transactions(_ report: WalletReport) -> some View {
        HStack {
            Text(NSLocalizedString("Transactions", comment: "Wallet section")).font(.headline)
            Spacer()
            if report.hasTransactions {
                Button(NSLocalizedString("See all", comment: "Wallet action")) { isHistoryPresented = true }
            }
        }
        .padding(.top, 8)
        if let actionError = model.walletActionError {
            Text(actionError).font(.caption).foregroundStyle(.red)
        }
        if !report.isHistoryAvailable {
            Text(NSLocalizedString("Modrinth did not return the transaction history.", comment: "Wallet history error"))
                .font(.subheadline)
                .foregroundStyle(.secondary)
        } else if !report.hasTransactions {
            VStack(spacing: 4) {
                Text(NSLocalizedString("No transactions", comment: "Wallet empty")).font(.headline)
                Text(NSLocalizedString("Your payouts and withdrawals will appear here.", comment: "Wallet empty"))
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 20)
        } else {
            ForEach(Array(report.recentTransactions(limit: 3).enumerated()), id: \.offset) { _, transaction in
                WalletCard(isPlatformNative: isPlatformNative) {
                    WalletTransactionRow(transaction: transaction)
                }
            }
        }
    }
}

// MARK: - Balance

private struct BalanceSlice: Identifiable {
    let id: String
    let label: String
    let detail: String?
    let amount: Double
    let color: Color
    let isHeld: Bool
}

private func balanceSlices(_ report: WalletReport) -> [BalanceSlice] {
    var slices = [
        BalanceSlice(
            id: "available",
            label: NSLocalizedString("Available now", comment: "Wallet balance row"),
            detail: nil,
            amount: report.available,
            color: .ryntraGreen,
            isHeld: false
        ),
    ]
    for (index, payout) in report.estimated.enumerated() {
        slices.append(BalanceSlice(
            id: "estimated-\(payout.date)",
            label: String.localizedStringWithFormat(
                NSLocalizedString("Estimated %@", comment: "Wallet balance row"),
                walletDate(payout.date)
            ),
            detail: NSLocalizedString("May still change until it becomes available.", comment: "Wallet balance row detail"),
            amount: payout.amount,
            color: Color.ryntraPayoutEstimates[index % Color.ryntraPayoutEstimates.count],
            isHeld: true
        ))
    }
    slices.append(BalanceSlice(
        id: "processing",
        label: NSLocalizedString("Processing", comment: "Wallet balance row"),
        detail: NSLocalizedString(
            "This month's revenue. It stays in processing until the month ends, then becomes available 60 days later.",
            comment: "Wallet balance row detail"
        ),
        amount: report.processingAmount,
        color: .secondary,
        isHeld: true
    ))
    return slices
}

private struct WalletBalanceBreakdown: View {
    let report: WalletReport
    var showsDetails = true

    var body: some View {
        let slices = balanceSlices(report)
        VStack(alignment: .leading, spacing: 0) {
            Text(NSLocalizedString("Balance", comment: "Wallet title"))
                .font(.subheadline.weight(.semibold))
                .foregroundStyle(.secondary)
            Text(walletMoney(report.balance))
                .font(.largeTitle.bold())
                .monospacedDigit()
                .lineLimit(1)
                .minimumScaleFactor(0.6)
            BalanceBar(slices: slices, total: report.balance)
                .frame(height: 12)
                .padding(.top, 12)
                .accessibilityHidden(true)
            ForEach(Array(slices.enumerated()), id: \.element.id) { index, slice in
                if index > 0 { Divider() }
                HStack(alignment: .top, spacing: 10) {
                    SliceSwatch(slice: slice)
                        .frame(width: 12, height: 12)
                        .padding(.top, 4)
                    VStack(alignment: .leading, spacing: 2) {
                        Text(slice.label).font(.subheadline)
                        if showsDetails, let detail = slice.detail {
                            Text(detail).font(.caption).foregroundStyle(.secondary)
                        }
                    }
                    Spacer(minLength: 8)
                    Text(walletMoney(slice.amount)).font(.subheadline.bold()).monospacedDigit()
                }
                .padding(.vertical, 9)
            }
        }
    }
}

/// Parts under 1 % are dropped, as on the site, so a cent of available money does not draw
/// a sliver that reads as a glitch; the rows below carry every amount as text
private struct BalanceBar: View {
    let slices: [BalanceSlice]
    let total: Double

    var body: some View {
        Canvas { context, size in
            let visible = total > 0 ? slices.filter { $0.amount / total >= 0.01 } : []
            let radius = size.height / 2
            guard !visible.isEmpty else {
                context.fill(Path(roundedRect: CGRect(origin: .zero, size: size), cornerRadius: radius), with: .color(.ryntraSeparator))
                return
            }
            let gap: CGFloat = 4
            let drawable = size.width - gap * CGFloat(visible.count - 1)
            let visibleTotal = visible.reduce(0) { $0 + $1.amount }
            var start: CGFloat = 0
            for slice in visible {
                let width = drawable * CGFloat(slice.amount / visibleTotal)
                let rect = CGRect(x: start, y: 0, width: width, height: size.height)
                drawSlice(slice, in: rect, context: context)
                start += width + gap
            }
        }
    }
}

private struct SliceSwatch: View {
    let slice: BalanceSlice

    var body: some View {
        Canvas { context, size in
            drawSlice(slice, in: CGRect(origin: .zero, size: size), context: context)
        }
        .accessibilityHidden(true)
    }
}

/// Held money is striped like on the site; only money that can be withdrawn now is solid
private func drawSlice(_ slice: BalanceSlice, in rect: CGRect, context: GraphicsContext) {
    let path = Path(roundedRect: rect, cornerRadius: min(rect.width, rect.height) / 2)
    guard slice.isHeld else {
        context.fill(path, with: .color(slice.color))
        return
    }
    context.fill(path, with: .color(slice.color.opacity(0.28)))
    var striped = context
    striped.clip(to: path)
    var x = rect.minX - rect.height
    while x < rect.maxX + rect.height {
        var line = Path()
        line.move(to: CGPoint(x: x, y: rect.maxY))
        line.addLine(to: CGPoint(x: x + rect.height, y: rect.minY))
        striped.stroke(line, with: .color(slice.color), lineWidth: 2.5)
        x += 7
    }
}

// MARK: - Compliance

private struct WalletComplianceNotices: View {
    let report: WalletReport

    var body: some View {
        if report.isWithdrawalLocked {
            notice(
                title: NSLocalizedString("Tax form failed", comment: "Wallet notice"),
                message: NSLocalizedString(
                    "Your withdrawals are temporarily locked because your TIN or SSN didn't match IRS records. Contact support to reset and resubmit your tax form.",
                    comment: "Wallet notice"
                ),
                action: NSLocalizedString("Contact support", comment: "Wallet notice action"),
                url: modrinthSupportURL,
                tint: .red,
                symbol: "exclamationmark.shield.fill"
            )
        } else if report.isTaxFormRequired {
            notice(
                title: NSLocalizedString("Tax form required", comment: "Wallet notice"),
                message: String.localizedStringWithFormat(
                    NSLocalizedString(
                        "You've withdrawn over %@ from Modrinth this year. To comply with tax regulations you need to complete a tax form; withdrawals are paused until it is submitted.",
                        comment: "Wallet notice"
                    ),
                    walletMoney(report.taxFormThreshold?.doubleValue ?? 0)
                ),
                action: NSLocalizedString("Complete tax form", comment: "Wallet notice action"),
                url: modrinthRevenueURL,
                tint: .orange,
                symbol: "doc.text.fill"
            )
        }
    }

    private func notice(title: String, message: String, action: String, url: URL, tint: Color, symbol: String) -> some View {
        HStack(alignment: .top, spacing: 12) {
            Image(systemName: symbol).foregroundStyle(tint)
            VStack(alignment: .leading, spacing: 4) {
                Text(title).font(.subheadline.weight(.semibold))
                Text(message).font(.caption).foregroundStyle(.secondary)
                Link(action, destination: url).font(.subheadline.weight(.semibold)).tint(tint)
            }
        }
        .padding(14)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(tint.opacity(0.12), in: RoundedRectangle(cornerRadius: 12, style: .continuous))
    }
}

// MARK: - Transactions

private struct WalletTransactionRow: View {
    @EnvironmentObject private var model: AppModel
    let transaction: WalletTransaction
    @State private var isConfirmingCancel = false

    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: transaction.isIncome ? "arrow.down.left" : "arrow.up.right")
                .font(.subheadline.weight(.semibold))
                .foregroundStyle(transaction.isIncome ? Color.ryntraGreen : Color.primary)
                .frame(width: 36, height: 36)
                .background(
                    (transaction.isIncome ? Color.ryntraGreen : Color.primary).opacity(0.12),
                    in: Circle()
                )
            VStack(alignment: .leading, spacing: 2) {
                Text(transactionTitle(transaction)).font(.subheadline.weight(.semibold)).lineLimit(1)
                Text(transactionDetail(transaction))
                    .font(.caption)
                    .foregroundStyle(isFailed ? Color.red : Color.secondary)
                    .lineLimit(2)
                if let address = transaction.methodAddress {
                    Text(address).font(.caption).foregroundStyle(.secondary).lineLimit(1)
                }
            }
            Spacer(minLength: 8)
            VStack(alignment: .trailing, spacing: 4) {
                Text((transaction.isIncome ? "+" : "−") + walletMoney(transaction.amount))
                    .font(.subheadline.bold())
                    .monospacedDigit()
                    .foregroundStyle(transaction.isIncome ? Color.ryntraGreen : Color.primary)
                if transaction.canCancel {
                    if model.cancellingPayoutID == transaction.id {
                        ProgressView().controlSize(.small)
                    } else {
                        Button(NSLocalizedString("Cancel", comment: "Wallet cancel withdrawal"), role: .destructive) {
                            isConfirmingCancel = true
                        }
                        .font(.caption.weight(.semibold))
                        .disabled(model.cancellingPayoutID != nil)
                    }
                }
            }
        }
        .confirmationDialog(
            NSLocalizedString("Cancel this withdrawal?", comment: "Wallet cancel withdrawal"),
            isPresented: $isConfirmingCancel,
            titleVisibility: .visible
        ) {
            Button(NSLocalizedString("Cancel withdrawal", comment: "Wallet cancel withdrawal"), role: .destructive) {
                guard let id = transaction.id else { return }
                Task { await model.cancelPayout(id: id) }
            }
            Button(NSLocalizedString("Keep", comment: "Wallet keep withdrawal"), role: .cancel) {}
        } message: {
            Text(String.localizedStringWithFormat(
                NSLocalizedString(
                    "%1$@ to %2$@ will be returned to your balance. Modrinth can only cancel a withdrawal while it is still in transit.",
                    comment: "Wallet cancel withdrawal"
                ),
                walletMoney(transaction.amount),
                transactionTitle(transaction)
            ))
        }
    }

    private var isFailed: Bool {
        ["failed", "cancelled", "cancelling"].contains(transaction.status?.apiValue ?? "")
    }
}

private struct TransactionsCSV: Transferable {
    let text: String

    static var transferRepresentation: some TransferRepresentation {
        DataRepresentation(exportedContentType: .commaSeparatedText) { Data($0.text.utf8) }
    }
}

/// The site's transfers page: a year filter, totals for the selection and the rows by month
private struct WalletHistoryView: View {
    @EnvironmentObject private var model: AppModel
    @Environment(\.dismiss) private var dismiss
    let report: WalletReport
    /// 0 is "all years"; a Kotlin `Int?` does not make a clean Picker tag
    @State private var selectedYear = 0

    private var history: WalletHistory {
        WalletHistory(
            transactions: model.walletReport?.transactions ?? report.transactions,
            year: selectedYear == 0 ? nil : KotlinInt(int: Int32(selectedYear))
        )
    }

    var body: some View {
        let history = self.history
        NavigationStack {
            List {
                Section {
                    if history.years.count > 1 {
                        Picker(NSLocalizedString("Year", comment: "Wallet history filter"), selection: $selectedYear) {
                            Text(NSLocalizedString("All years", comment: "Wallet history filter")).tag(0)
                            ForEach(history.years.map(\.intValue), id: \.self) { year in
                                Text(String(year)).tag(year)
                            }
                        }
                    }
                    LabeledContent(NSLocalizedString("Received", comment: "Wallet history total"), value: walletMoney(history.received))
                    LabeledContent(NSLocalizedString("Withdrawn", comment: "Wallet history total"), value: walletMoney(history.withdrawn))
                    LabeledContent(NSLocalizedString("Transactions", comment: "Wallet history total"), value: "\(history.filtered.count)")
                }
                if let actionError = model.walletActionError {
                    Section { Text(actionError).foregroundStyle(.red) }
                }
                ForEach(history.months, id: \.yearMonth) { month in
                    Section(walletMonthLabel(year: Int(month.year), month: Int(month.month))) {
                        ForEach(Array(month.transactions.enumerated()), id: \.offset) { _, transaction in
                            WalletTransactionRow(transaction: transaction)
                        }
                    }
                }
            }
            .navigationTitle(NSLocalizedString("Transactions", comment: "Wallet history title"))
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button(NSLocalizedString("Done", comment: "Sheet action")) { dismiss() }
                }
                ToolbarItem(placement: .primaryAction) {
                    ShareLink(
                        item: TransactionsCSV(text: history.toCsv()),
                        preview: SharePreview(NSLocalizedString("Modrinth transactions", comment: "Wallet CSV export"))
                    ) {
                        Label(NSLocalizedString("Export CSV", comment: "Wallet CSV export"), systemImage: "square.and.arrow.up")
                    }
                    .disabled(history.filtered.isEmpty)
                }
            }
        }
#if os(macOS)
        // A Mac sheet sizes itself to its content, and a List has no intrinsic height
        .frame(minWidth: 520, minHeight: 600)
#endif
    }
}

// MARK: - Affiliate links

/// Affiliate link performance. Links are listed by code because naming, creating and
/// revoking them is limited to Modrinth's website
private struct WalletAffiliateSection: View {
    @EnvironmentObject private var model: AppModel
    let isPlatformNative: Bool

    var body: some View {
        Text(NSLocalizedString("Affiliate links", comment: "Wallet section")).font(.headline).padding(.top, 8)
        WalletCard(isPlatformNative: isPlatformNative) {
            if let report = model.affiliateReport {
                Text(String.localizedStringWithFormat(
                    NSLocalizedString("Last %d days", comment: "Affiliate period"),
                    Int(report.rangeDays)
                ))
                .font(.caption)
                .foregroundStyle(.secondary)
                HStack(alignment: .top) {
                    figure(NSLocalizedString("Clicks", comment: "Affiliate metric"), report.totalClicks.formatted())
                    figure(NSLocalizedString("Purchases", comment: "Affiliate metric"), report.totalConversions.formatted())
                    figure(NSLocalizedString("Revenue", comment: "Affiliate metric"), walletMoney(report.totalRevenue))
                }
                if report.codes.isEmpty {
                    Text(NSLocalizedString(
                        "No affiliate links yet. Create one on Modrinth to start tracking it here.",
                        comment: "Affiliate empty"
                    ))
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                } else {
                    ForEach(report.codes, id: \.id) { code in
                        Divider()
                        HStack {
                            VStack(alignment: .leading, spacing: 2) {
                                Text(code.link).font(.subheadline).lineLimit(1).truncationMode(.middle)
                                Text("\(code.clicks.formatted()) · \(code.conversions.formatted()) · \(walletMoney(code.revenue))")
                                    .font(.caption)
                                    .foregroundStyle(.secondary)
                            }
                            Spacer()
                            Button {
                                ryntraCopyToPasteboard(code.link)
                            } label: {
                                Image(systemName: "doc.on.doc")
                            }
                            .buttonStyle(.borderless)
                            .accessibilityLabel(NSLocalizedString("Copy link", comment: "Affiliate action"))
                        }
                    }
                }
            } else if model.isAffiliateLoading {
                ProgressView().frame(maxWidth: .infinity).padding(.vertical, 16)
            } else {
                Text(model.affiliateError ?? NSLocalizedString("Affiliate statistics could not be loaded.", comment: "Affiliate error"))
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            }
            Text(NSLocalizedString(
                "Modrinth lets only its website create, rename or revoke affiliate links, so links are shown by their code here.",
                comment: "Affiliate hint"
            ))
            .font(.caption)
            .foregroundStyle(.secondary)
            .padding(.top, 8)
            Link(
                NSLocalizedString("Manage links on Modrinth", comment: "Affiliate action"),
                destination: URL(string: AffiliateKt.MODRINTH_AFFILIATE_LINKS_URL)!
            )
            .font(.subheadline.weight(.semibold))
        }
    }

    private func figure(_ label: String, _ value: String) -> some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(label).font(.caption).foregroundStyle(.secondary)
            Text(value).font(.headline).monospacedDigit().lineLimit(1).minimumScaleFactor(0.7)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}

// MARK: - Shared wallet helpers

private struct WalletCard<Content: View>: View {
    let isPlatformNative: Bool
    @ViewBuilder let content: () -> Content

    var body: some View {
        VStack(alignment: .leading, spacing: 4) { content() }
            .padding(16)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(cardBackground, in: RoundedRectangle(cornerRadius: 12, style: .continuous))
            .overlay {
                if !isPlatformNative {
                    RoundedRectangle(cornerRadius: 12, style: .continuous).stroke(Color.ryntraSeparator, lineWidth: 0.5)
                }
            }
    }

    private var cardBackground: Color {
        guard isPlatformNative else { return .ryntraSurface }
#if canImport(UIKit)
        return Color(uiColor: .secondarySystemGroupedBackground)
#elseif canImport(AppKit)
        return Color(nsColor: .controlBackgroundColor)
#endif
    }
}

private func walletMoney(_ value: Double) -> String {
    value.formatted(.currency(code: WalletKt.WALLET_CURRENCY).precision(.fractionLength(2)))
}

/// Availability dates are UTC midnights; the device zone would show the day before for
/// everyone west of Greenwich, which is not the date Modrinth promises
private func walletDate(_ iso: String) -> String {
    let parser = ISO8601DateFormatter()
    parser.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
    let fallback = ISO8601DateFormatter()
    guard let date = parser.date(from: iso) ?? fallback.date(from: iso) else { return String(iso.prefix(10)) }
    var style = Date.FormatStyle(date: .abbreviated, time: .omitted)
    style.timeZone = TimeZone(identifier: "UTC") ?? .current
    return date.formatted(style)
}

private func walletMonthLabel(year: Int, month: Int) -> String {
    var calendar = Calendar(identifier: .gregorian)
    calendar.timeZone = TimeZone(identifier: "UTC") ?? .current
    let now = calendar.dateComponents([.year, .month], from: Date())
    let monthsAgo = ((now.year ?? year) - year) * 12 + ((now.month ?? month) - month)
    switch monthsAgo {
    case 0: return NSLocalizedString("This month", comment: "Wallet history month")
    case 1: return NSLocalizedString("Last month", comment: "Wallet history month")
    default:
        guard let date = calendar.date(from: DateComponents(year: year, month: month)) else { return String(year) }
        var style = Date.FormatStyle().year().month(.wide)
        style.timeZone = calendar.timeZone
        return date.formatted(style)
    }
}

private func transactionTitle(_ transaction: WalletTransaction) -> String {
    if transaction.isIncome {
        return transaction.payoutSource == "affiliates" || transaction.payoutSource == "affilites"
            ? NSLocalizedString("Affiliate rewards", comment: "Wallet income source")
            : NSLocalizedString("Creator rewards", comment: "Wallet income source")
    }
    return WalletHistoryKt.payoutMethodName(methodType: transaction.methodType)
}

private func transactionDetail(_ transaction: WalletTransaction) -> String {
    var parts: [String] = []
    if let status = transaction.status { parts.append(payoutStatusLabel(status)) }
    parts.append(walletDate(transaction.created))
    if let fee = transaction.fee?.doubleValue, fee > 0 {
        parts.append(String.localizedStringWithFormat(NSLocalizedString("Fee %@", comment: "Wallet transaction fee"), walletMoney(fee)))
    }
    return parts.joined(separator: " • ")
}

private func payoutStatusLabel(_ status: PayoutStatus) -> String {
    switch status.apiValue {
    case "success": return NSLocalizedString("Success", comment: "Payout status")
    case "in-transit": return NSLocalizedString("In transit", comment: "Payout status")
    case "cancelling": return NSLocalizedString("Cancelling", comment: "Payout status")
    case "cancelled": return NSLocalizedString("Cancelled", comment: "Payout status")
    case "failed": return NSLocalizedString("Failed", comment: "Payout status")
    default: return NSLocalizedString("Unknown", comment: "Payout status")
    }
}

private func walletUnavailableMessage(report: WalletReport?, errorMessage: String?) -> String {
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
