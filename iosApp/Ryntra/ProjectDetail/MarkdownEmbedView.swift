import RyntraShared
import SwiftUI

/// A video or server embedded in a project description.
///
/// The card hands playback to YouTube rather than hosting it: an inline `WKWebView` inside a
/// lazily scrolled description means running a JavaScript runtime for content anyone can author,
/// and a web view nested in a scroll view fights the gesture on both iOS and macOS.
struct MarkdownEmbedView: View {
    let embed: MarkdownEmbed

    private var label: String {
        // Matched on the wire value: Kotlin/Native rewrites enum case names on export.
        switch embed.provider.key {
        case "youtube": return NSLocalizedString("YouTube video", comment: "Description embed")
        case "discord": return NSLocalizedString("Discord server", comment: "Description embed")
        default: return embed.url
        }
    }

    private var symbol: String {
        switch embed.provider.key {
        case "discord": return "bubble.left.and.bubble.right.fill"
        default: return "play.fill"
        }
    }

    var body: some View {
        Button {
            if let url = URL(string: embed.url) {
                ryntraOpenExternalURL(url)
            }
        } label: {
            VStack(spacing: 0) {
                if let thumbnail = embed.thumbnailUrl, let url = URL(string: thumbnail) {
                    ZStack {
                        Color.black
                        RemoteImage(url: url) { image in
                            image.resizable().scaledToFill()
                        } placeholder: {
                            Color.black
                        }
                        Image(systemName: "play.fill")
                            .font(.title2)
                            .foregroundStyle(.white)
                            .frame(width: 52, height: 52)
                            .background(.black.opacity(0.62), in: Circle())
                    }
                    .aspectRatio(16.0 / 9.0, contentMode: .fit)
                    .clipped()
                }
                HStack(spacing: 10) {
                    Image(systemName: symbol)
                        .font(.footnote)
                        .foregroundStyle(Color.ryntraGreen)
                    Text(label)
                        .font(.subheadline.weight(.semibold))
                        .lineLimit(1)
                    Spacer(minLength: 8)
                    Image(systemName: "arrow.up.forward.square")
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(12)
            }
            .frame(maxWidth: .infinity)
            .background(Color.ryntraSurface, in: RoundedRectangle(cornerRadius: 14))
            .clipShape(RoundedRectangle(cornerRadius: 14))
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityLabel(String.localizedStringWithFormat(
            NSLocalizedString("Open %@", comment: "Description embed action"),
            label
        ))
    }
}
