import SwiftUI

enum RinthyDestination: Int, CaseIterable {
    case dashboard
    case projects
    case teams
    case analytics

    var label: String {
        switch self {
        case .dashboard: return "Dashboard"
        case .projects: return "Projects"
        case .teams: return "Teams"
        case .analytics: return "Analytics"
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

    var body: some View {
        HStack(spacing: 0) {
            ForEach(RinthyDestination.allCases, id: \.rawValue) { destination in
                Button {
                    selection = destination
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
                    .frame(maxWidth: .infinity, minHeight: 60)
                    .background(
                        selection == destination ? selectedBackground : Color.clear,
                        in: RoundedRectangle(cornerRadius: 24)
                    )
                    .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
                .accessibilityAddTraits(selection == destination ? .isSelected : [])
            }
        }
        .padding(.horizontal, 4)
        .frame(height: 66)
        .rinthyCapsuleGlass()
        .padding(.horizontal, 16)
        .padding(.top, 6)
        .padding(.bottom, 8)
    }

    private var selectedBackground: Color {
        colorScheme == .dark ? Color.black.opacity(0.72) : Color.white.opacity(0.78)
    }
}

private extension View {
    @ViewBuilder
    func rinthyCapsuleGlass() -> some View {
        if #available(iOS 26.0, *) {
            glassEffect(in: .capsule)
        } else {
            background(.ultraThinMaterial, in: Capsule())
                .overlay {
                    Capsule().stroke(Color.primary.opacity(0.16), lineWidth: 0.5)
                }
                .shadow(color: .black.opacity(0.14), radius: 14, y: 6)
        }
    }
}
