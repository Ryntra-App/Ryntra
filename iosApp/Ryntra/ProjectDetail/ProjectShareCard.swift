import Foundation
import RyntraShared
import SwiftUI

#if canImport(UIKit)
import UIKit
#elseif canImport(AppKit)
import AppKit
#endif

enum ProjectShareCardFormat: String, CaseIterable, Identifiable {
    case square
    case post
    case story

    var id: String { rawValue }

    var aspectRatio: CGFloat {
        switch self {
        case .square: return 1
        case .post: return 1.91
        case .story: return 9.0 / 16.0
        }
    }

    var localizedName: String {
        switch self {
        case .square: return String(localized: "Square")
        case .post: return String(localized: "Post")
        case .story: return String(localized: "Story")
        }
    }
}

enum ProjectShareCardMessage: String, CaseIterable, Identifiable {
    case release
    case milestone
    case testers

    var id: String { rawValue }

    var localizedName: String {
        switch self {
        case .release: return String(localized: "Release")
        case .milestone: return String(localized: "Milestone")
        case .testers: return String(localized: "Call for testers")
        }
    }

    var eyebrow: String {
        switch self {
        case .release: return String(localized: "NEW RELEASE")
        case .milestone: return String(localized: "MILESTONE")
        case .testers: return String(localized: "TEST WITH US")
        }
    }
}

struct ProjectShareCardPalette: Identifiable {
    let id: String
    let name: String
    let backgroundStart: Color
    let backgroundEnd: Color
    let foreground: Color
    let secondary: Color
    let accent: Color
    let chip: Color

    static let all: [Self] = [
        .init(
            id: "midnight",
            name: String(localized: "Midnight"),
            backgroundStart: Color(red: 0.09, green: 0.11, blue: 0.20),
            backgroundEnd: Color(red: 0.035, green: 0.043, blue: 0.09),
            foreground: Color(red: 0.96, green: 0.95, blue: 1),
            secondary: Color(red: 0.73, green: 0.72, blue: 0.81),
            accent: Color(red: 0.66, green: 0.72, blue: 1),
            chip: Color(red: 0.15, green: 0.18, blue: 0.29)
        ),
        .init(
            id: "moss",
            name: String(localized: "Moss"),
            backgroundStart: Color(red: 0.16, green: 0.21, blue: 0.16),
            backgroundEnd: Color(red: 0.09, green: 0.12, blue: 0.09),
            foreground: Color(red: 0.95, green: 0.95, blue: 0.91),
            secondary: Color(red: 0.76, green: 0.78, blue: 0.71),
            accent: Color(red: 0.84, green: 0.70, blue: 0.43),
            chip: Color(red: 0.23, green: 0.29, blue: 0.22)
        ),
        .init(
            id: "paper",
            name: String(localized: "Paper"),
            backgroundStart: Color(red: 0.96, green: 0.95, blue: 0.91),
            backgroundEnd: Color(red: 1, green: 0.99, blue: 0.95),
            foreground: Color(red: 0.11, green: 0.16, blue: 0.13),
            secondary: Color(red: 0.35, green: 0.39, blue: 0.36),
            accent: Color(red: 0.16, green: 0.39, blue: 0.28),
            chip: Color(red: 0.88, green: 0.91, blue: 0.88)
        ),
    ]
}

struct ProjectShareCardCanvas: View {
    let project: Project
    let version: ProjectVersion?
    let message: ProjectShareCardMessage
    let format: ProjectShareCardFormat
    let palette: ProjectShareCardPalette
    let headline: String
    let description: String
    let iconData: Data?

    var body: some View {
        GeometryReader { geometry in
            let size = geometry.size
            let isWide = format == .post
            let isStory = format == .story
            let padding = isStory ? size.width * 0.075 : (isWide ? size.height * 0.075 : size.width * 0.065)
            let iconSize = isStory ? size.width * 0.20 : (isWide ? size.height * 0.26 : size.width * 0.14)

            ZStack {
                LinearGradient(
                    colors: [palette.backgroundStart, palette.backgroundEnd],
                    startPoint: .topLeading,
                    endPoint: .bottomTrailing
                )

                Circle()
                    .fill(palette.accent.opacity(0.12))
                    .frame(width: min(size.width, size.height) * 0.84)
                    .position(x: size.width * 0.92, y: size.height * 0.08)
                Circle()
                    .fill(palette.foreground.opacity(0.045))
                    .frame(width: min(size.width, size.height) * 0.52)
                    .position(x: size.width * 0.06, y: size.height * 0.96)

                VStack(alignment: .leading, spacing: 0) {
                    HStack(spacing: max(8, padding * 0.45)) {
                        projectMark(size: iconSize)
                        VStack(alignment: .leading, spacing: 3) {
                            Text(message.eyebrow)
                                .font(.system(size: max(9, iconSize * 0.20), weight: .bold))
                                .tracking(0.6)
                                .foregroundStyle(palette.accent)
                            Text(project.title)
                                .font(.system(size: max(13, iconSize * 0.31), weight: .bold))
                                .foregroundStyle(palette.foreground)
                                .lineLimit(isStory ? 2 : 1)
                        }
                    }

                    Spacer(minLength: 8)

                    VStack(alignment: .leading, spacing: isWide ? 5 : 12) {
                        Text(headline)
                            .font(.system(size: headlineSize(in: size), weight: .black, design: .rounded))
                            .foregroundStyle(palette.foreground)
                            .lineLimit(isWide ? 2 : 3)
                            .minimumScaleFactor(0.72)
                        if !description.isEmpty {
                            Text(description)
                                .font(.system(size: descriptionSize(in: size), weight: .regular))
                                .foregroundStyle(palette.secondary)
                                .lineLimit(isStory ? 7 : (isWide ? 2 : 4))
                                .minimumScaleFactor(0.78)
                        }
                    }

                    Spacer(minLength: 8)

                    VStack(alignment: .leading, spacing: 9) {
                        let tags = cardTags(isWide: isWide)
                        if !tags.isEmpty {
                            HStack(spacing: 6) {
                                ForEach(tags, id: \.self) { tag in
                                    Text(tag)
                                        .font(.system(size: max(8, min(size.width, size.height) * 0.026), weight: .medium))
                                        .foregroundStyle(palette.foreground)
                                        .lineLimit(1)
                                        .padding(.horizontal, 9)
                                        .padding(.vertical, 5)
                                        .background(palette.chip, in: Capsule())
                                }
                            }
                        }
                        HStack(alignment: .lastTextBaseline, spacing: 8) {
                            Text("modrinth.com\(project.modrinthDisplayPath)")
                                .lineLimit(1)
                            Spacer(minLength: 4)
                            Text("RYNTRA").fontWeight(.bold)
                        }
                        .font(.system(size: max(8, min(size.width, size.height) * 0.024)))
                        .foregroundStyle(palette.secondary)
                    }
                }
                .padding(padding)
            }
            .clipShape(RoundedRectangle(cornerRadius: max(14, min(size.width, size.height) * 0.05), style: .continuous))
        }
        .aspectRatio(format.aspectRatio, contentMode: .fit)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(
            String.localizedStringWithFormat(
                NSLocalizedString("Share card preview for %@: %@", comment: "Share card preview"),
                project.title,
                headline
            )
        )
    }

    @ViewBuilder
    private func projectMark(size: CGFloat) -> some View {
        ZStack {
            RoundedRectangle(cornerRadius: size * 0.24, style: .continuous)
                .fill(palette.chip)
            Text(String(project.title.prefix(1)).uppercased())
                .font(.system(size: size * 0.46, weight: .black, design: .rounded))
                .foregroundStyle(palette.foreground)
            if let iconData, let image = ryntraImage(data: iconData) {
                image
                    .resizable()
                    .scaledToFill()
            }
        }
        .frame(width: size, height: size)
        .clipShape(RoundedRectangle(cornerRadius: size * 0.24, style: .continuous))
    }

    private func headlineSize(in size: CGSize) -> CGFloat {
        if format == .story { return size.width * 0.092 }
        if format == .post { return size.height * 0.115 }
        return size.width * 0.072
    }

    private func descriptionSize(in size: CGSize) -> CGFloat {
        format == .post ? size.height * 0.052 : size.width * 0.032
    }

    private func cardTags(isWide: Bool) -> [String] {
        var tags: [String] = []
        if let number = version?.versionNumber, !number.isEmpty { tags.append(number) }
        if let loader = version?.loaders.first { tags.append(loader.capitalized) }
        if let gameVersion = version?.gameVersions.first { tags.append(gameVersion) }
        if message == .milestone { tags.append(project.downloads.formatted()) }
        return Array(tags.prefix(isWide ? 2 : 3))
    }
}

func ryntraImage(data: Data) -> Image? {
#if canImport(UIKit)
    guard let image = UIImage(data: data) else { return nil }
    return Image(uiImage: image)
#elseif canImport(AppKit)
    guard let image = NSImage(data: data) else { return nil }
    return Image(nsImage: image)
#else
    return nil
#endif
}
