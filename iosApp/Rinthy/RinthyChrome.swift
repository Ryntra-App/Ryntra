import Foundation
import SwiftUI

enum RinthyDestination: Int, CaseIterable {
    case dashboard
    case projects
    case teams
    case analytics

    var label: String {
        switch self {
        case .dashboard: return NSLocalizedString("Dashboard", comment: "Navigation destination")
        case .projects: return NSLocalizedString("Projects", comment: "Navigation destination")
        case .teams: return NSLocalizedString("Teams", comment: "Navigation destination")
        case .analytics: return NSLocalizedString("Analytics", comment: "Navigation destination")
        }
    }

    var symbol: String {
        switch self {
        case .dashboard: return "square.grid.2x2.fill"
        case .projects: return "shippingbox.fill"
        case .teams: return "person.3.fill"
        case .analytics: return "chart.bar.fill"
        }
    }

    var platformSymbol: String {
        switch self {
        case .dashboard: return "square.grid.2x2"
        case .projects: return "shippingbox"
        case .teams: return "person.3"
        case .analytics: return "chart.bar"
        }
    }
}

struct RinthyTopBar: View {
    let title: String
    let avatarURL: String?
    let username: String
    let isRefreshing: Bool
    let onAvatarTap: () -> Void
    var showsBackButton = false
    var onBack: () -> Void = {}
    var showsAvatar = true

    var body: some View {
        HStack(spacing: 8) {
            if showsBackButton {
                Button(action: onBack) {
                    Image(systemName: "chevron.left")
                        .frame(width: 38, height: 38)
                }
                .buttonStyle(.plain)
                .accessibilityLabel("Back")
            }
            Text(title)
                .font(.largeTitle.bold())
                .lineLimit(1)
            Spacer(minLength: 8)
            if isRefreshing {
                ProgressView().padding(.trailing, 8)
            }
            if showsAvatar {
                Button(action: onAvatarTap) {
                    AsyncImage(url: URL(string: avatarURL ?? "")) { image in
                        image.resizable().scaledToFill()
                    } placeholder: {
                        Circle().fill(.quaternary)
                    }
                    .frame(width: 38, height: 38)
                    .clipShape(Circle())
                }
                .buttonStyle(.plain)
                .accessibilityLabel("Open \(username)'s account")
            }
        }
        .padding(.horizontal, 20)
        .frame(height: 76)
    }
}

struct RinthyTabBar: View {
    @Binding var selection: RinthyDestination
    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.accessibilityReduceMotion) private var systemReduceMotion
    @AppStorage("reduceMotion") private var appReduceMotion = false

    var body: some View {
        HStack(spacing: 0) {
            ForEach(RinthyDestination.allCases, id: \.rawValue) { destination in
                Button {
                    if systemReduceMotion || appReduceMotion {
                        selection = destination
                    } else {
                        withAnimation(RinthyMotion.control) {
                            selection = destination
                        }
                    }
                } label: {
                    VStack(spacing: 2) {
                        Image(systemName: destination.symbol)
                            .font(.system(size: 18, weight: .semibold))
                            .frame(width: 38, height: 28)
                        Text(destination.label)
                            .font(.caption2.weight(selection == destination ? .semibold : .regular))
                            .lineLimit(1)
                    }
                    .foregroundStyle(selection == destination ? Color.rinthyGreen : Color.secondary)
                    .frame(maxWidth: .infinity, minHeight: 54)
                    .background(
                        selection == destination ? selectedBackground : Color.clear,
                        in: RoundedRectangle(cornerRadius: 24)
                    )
                    .scaleEffect(selection == destination ? 1 : 0.97)
                    .overlay {
                        if selection == destination {
                            RoundedRectangle(cornerRadius: 24)
                                .stroke(Color.primary.opacity(0.16), lineWidth: 0.5)
                        }
                    }
                    .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
                .accessibilityAddTraits(selection == destination ? .isSelected : [])
            }
        }
        .padding(.horizontal, 4)
        .frame(height: 60)
        .rinthyCapsuleGlass()
        .padding(.horizontal, 16)
        .padding(.top, 6)
        .padding(.bottom, 8)
        .animation(
            RinthyMotion.resolved(.control, reduceMotion: systemReduceMotion || appReduceMotion),
            value: selection
        )
    }

    private var selectedBackground: Color {
        colorScheme == .dark ? Color.white.opacity(0.075) : Color.black.opacity(0.055)
    }
}

private extension View {
    @ViewBuilder
    func rinthyCapsuleGlass() -> some View {
        if #available(iOS 26.0, *) {
            glassEffect(.regular.interactive(), in: .capsule)
        } else {
            background(.ultraThinMaterial, in: Capsule())
                .overlay {
                    Capsule().stroke(Color.primary.opacity(0.16), lineWidth: 0.5)
                }
                .shadow(color: .black.opacity(0.14), radius: 14, y: 6)
        }
    }
}
