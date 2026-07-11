import SwiftUI

enum RinthyDestination: Int, CaseIterable {
    case overview
    case projects
    case teams
    case account

    var label: String {
        switch self {
        case .overview: return "Overview"
        case .projects: return "Projects"
        case .teams: return "Teams"
        case .account: return "Account"
        }
    }

    var symbol: String {
        switch self {
        case .overview: return "square.grid.2x2.fill"
        case .projects: return "shippingbox.fill"
        case .teams: return "person.3.fill"
        case .account: return "person.crop.circle.fill"
        }
    }
}

struct RinthyTopBar: View {
    let title: String
    let avatarURL: String?
    let username: String
    let isRefreshing: Bool
    let canRefresh: Bool
    let onRefresh: () -> Void
    let onAvatarTap: () -> Void

    var body: some View {
        HStack(spacing: 8) {
            Text(title)
                .font(.title3.bold())
                .lineLimit(1)
            Spacer(minLength: 8)
            if canRefresh {
                Button(action: onRefresh) {
                    Group {
                        if isRefreshing {
                            ProgressView()
                        } else {
                            Image(systemName: "arrow.clockwise")
                        }
                    }
                    .frame(width: 38, height: 38)
                }
                .buttonStyle(.plain)
                .accessibilityLabel("Refresh")
                .disabled(isRefreshing)
            }
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
        .padding(.leading, 16)
        .padding(.trailing, 8)
        .frame(height: 54)
        .rinthyGlass(cornerRadius: 22)
        .padding(.horizontal, 16)
        .padding(.top, 8)
        .padding(.bottom, 6)
    }
}

struct RinthyTabBar: View {
    @Binding var selection: RinthyDestination

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
                            .background(
                                selection == destination
                                    ? Color.rinthyGreen.opacity(0.13)
                                    : Color.clear,
                                in: RoundedRectangle(cornerRadius: 11)
                            )
                        Text(destination.label)
                            .font(.caption2.weight(selection == destination ? .semibold : .regular))
                            .lineLimit(1)
                    }
                    .foregroundStyle(selection == destination ? Color.rinthyGreen : Color.secondary)
                    .frame(maxWidth: .infinity, minHeight: 60)
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
}

private extension View {
    @ViewBuilder
    func rinthyGlass(cornerRadius: CGFloat) -> some View {
        if #available(iOS 26.0, *) {
            glassEffect(in: .rect(cornerRadius: cornerRadius))
        } else {
            background(.ultraThinMaterial, in: RoundedRectangle(cornerRadius: cornerRadius))
                .overlay {
                    RoundedRectangle(cornerRadius: cornerRadius)
                        .stroke(Color.primary.opacity(0.16), lineWidth: 0.5)
                }
                .shadow(color: .black.opacity(0.12), radius: 12, y: 5)
        }
    }

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
