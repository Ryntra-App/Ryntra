import Foundation
import RyntraShared
import SwiftUI

struct ProjectBannerCard: View {
    let project: Project
    let isFavorite: Bool
    let onFavoriteTap: (() -> Void)?

    var body: some View {
        VStack(spacing: 0) {
            banner
                .frame(height: 112)
                .clipped()

            HStack(spacing: 11) {
                ProjectArtwork(project: project)
                    .frame(width: 48, height: 48)
                VStack(alignment: .leading, spacing: 3) {
                    HStack(spacing: 8) {
                        Text(project.title).fontWeight(.semibold).lineLimit(1)
                        Spacer(minLength: 0)
                        if project.status != "approved" { statusLabel }
                    }
                    Text(project.slug.map { "\($0)  ·  \(project.displayTypeLabel)" } ?? project.displayTypeLabel)
                        .font(.caption.weight(.medium))
                        .foregroundStyle(.secondary)
                        .lineLimit(1)
                    HStack(spacing: 14) {
                        metric(ryntraExactCount(project.downloads), symbol: "arrow.down", tint: .analyticsBlue)
                        metric(ryntraExactCount(project.followers), symbol: "heart", tint: .analyticsPink)
                        if let updated = ryntraProjectDate(project.updated) {
                            Text(updated).font(.caption2).foregroundStyle(.secondary)
                        }
                    }
                    .padding(.top, 2)
                }
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 11)
        }
        .background(Color.ryntraSurface, in: RoundedRectangle(cornerRadius: 12))
        .clipShape(RoundedRectangle(cornerRadius: 12))
        .overlay { RoundedRectangle(cornerRadius: 12).stroke(Color.ryntraSeparator, lineWidth: 0.5) }
    }

    @ViewBuilder
    private var banner: some View {
        ZStack(alignment: .topTrailing) {
            if let bannerURL = URL(string: project.bannerUrl ?? "") {
                AsyncImage(url: bannerURL) { image in
                    image.resizable().scaledToFill()
                } placeholder: {
                    Color.ryntraSurfaceRaised
                }
            } else {
                Color.ryntraSurfaceRaised
                    .overlay {
                        Text(String(project.title.prefix(1)).uppercased())
                            .font(.largeTitle.bold())
                            .foregroundStyle(.secondary.opacity(0.45))
                    }
            }

            if let onFavoriteTap {
                Button(action: onFavoriteTap) {
                    Image(systemName: "star")
                        .symbolVariant(isFavorite ? .fill : .none)
                        .font(.caption.weight(.semibold))
                        .foregroundStyle(isFavorite ? Color.accentColor : Color.primary)
                        .frame(width: 40, height: 40)
                        .background(
                            isFavorite ? Color.accentColor.opacity(0.18) : Color.clear,
                            in: Circle()
                        )
                        .background(.regularMaterial, in: Circle())
                        .scaleEffect(isFavorite ? 1.06 : 1)
                }
                .buttonStyle(.plain)
                .padding(8)
                .accessibilityLabel(
                    NSLocalizedString(isFavorite ? "Remove favorite" : "Add favorite", comment: "Project favorite action")
                )
            }
        }
    }

    private var statusLabel: some View {
        Text(project.localizedStatusLabel)
            .font(.caption2.weight(.semibold))
            .foregroundStyle(statusColor)
            .padding(.horizontal, 6)
            .padding(.vertical, 3)
            .background(statusColor.opacity(0.12), in: RoundedRectangle(cornerRadius: 6))
    }

    private var statusColor: Color {
        switch project.status {
        case "rejected", "withheld": return .red
        case "processing", "scheduled", "draft": return .orange
        default: return .secondary
        }
    }

    private func metric(_ value: String, symbol: String, tint: Color) -> some View {
        HStack(spacing: 4) {
            Image(systemName: symbol).foregroundStyle(tint)
            Text(value).foregroundStyle(.secondary)
        }
        .font(.caption2)
    }
}
