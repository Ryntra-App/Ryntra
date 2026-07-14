import RinthyShared
import SwiftUI

struct MarkdownBlockView: View {
    let block: MarkdownBlock

    var body: some View {
        switch block.type {
        case .divider:
            Divider().padding(.vertical, 4)
        case .image:
            markdownImages
        case .codeblock:
            Text(block.content)
                .font(.system(.footnote, design: .monospaced))
                .foregroundStyle(.primary)
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(10)
                .background(Color.secondary.opacity(0.12), in: RoundedRectangle(cornerRadius: 8))
        case .table:
            ScrollView(.horizontal, showsIndicators: false) {
                Text(block.content)
                    .font(.system(.footnote, design: .monospaced))
                    .foregroundStyle(.secondary)
                    .fixedSize(horizontal: true, vertical: false)
                    .padding(.vertical, 8)
            }
        case .quote:
            HStack(alignment: .top, spacing: 10) {
                RoundedRectangle(cornerRadius: 2)
                    .fill(Color.rinthyGreen)
                    .frame(width: 3)
                markdownText
            }
        case .listitem:
            HStack(alignment: .top, spacing: 8) {
                Text(listMarker)
                    .font(font)
                    .foregroundStyle(foregroundStyle)
                markdownText
            }
        default:
            markdownText
        }
    }

    @ViewBuilder
    private var markdownImages: some View {
        if !block.images.isEmpty, block.images.allSatisfy(\.isBadge) {
            MarkdownBadgeRowView(images: block.images)
                .frame(maxWidth: .infinity, minHeight: 42, maxHeight: 42)
        } else if block.images.count == 1,
           !block.images[0].isBadge,
           let url = URL(string: block.images[0].url) {
            imageLink(block.images[0]) {
                AsyncImage(url: url) { image in
                    image.resizable().scaledToFit()
                } placeholder: {
                    Color.secondary.opacity(0.1)
                }
                .frame(maxWidth: 640, maxHeight: 360, alignment: .leading)
                .clipShape(RoundedRectangle(cornerRadius: 8))
            }
        } else {
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 6) {
                    ForEach(Array(block.images.enumerated()), id: \.offset) { _, badge in
                        if let url = URL(string: badge.url) {
                            imageLink(badge) {
                                AsyncImage(url: url) { image in
                                    image.resizable().scaledToFit()
                                } placeholder: {
                                    Color.secondary.opacity(0.1)
                                }
                                .frame(height: 36)
                                .clipShape(RoundedRectangle(cornerRadius: 5))
                            }
                        }
                    }
                }
            }
        }
    }

    private var markdownText: some View {
        Text(attributedString)
            .font(font)
            .foregroundStyle(foregroundStyle)
    }

    private var attributedString: AttributedString {
        var attributed = AttributedString(block.content)
        for span in block.spans {
            let start = attributed.index(attributed.startIndex, offsetBy: Int(span.start))
            let end = attributed.index(attributed.startIndex, offsetBy: Int(span.end))
            let range = start..<end

            switch span.type {
            case .bold:
                attributed[range].inlinePresentationIntent = .stronglyEmphasized
            case .italic:
                attributed[range].inlinePresentationIntent = .emphasized
            case .strikethrough:
                attributed[range].strikethroughStyle = .single
            case .code:
                attributed[range].font = .system(.body, design: .monospaced)
                attributed[range].backgroundColor = .secondary.opacity(0.15)
            case .link:
                if let urlString = span.linkUrl, let url = URL(string: urlString) {
                    attributed[range].link = url
                }
            default:
                break
            }
        }
        return attributed
    }

    private var font: Font {
        switch block.type {
        case .heading:
            if block.level == 1 { return .title2.bold() }
            if block.level == 2 { return .title3.bold() }
            return .headline.bold()
        default:
            return .body
        }
    }

    private var foregroundStyle: Color {
        block.type == .heading ? .primary : .secondary
    }

    private var listMarker: String {
        if let checked = block.checked { return checked.boolValue ? "[x]" : "[ ]" }
        return block.ordered ? "\(block.ordinal)." : "•"
    }

    @ViewBuilder
    private func imageLink<Content: View>(
        _ image: MarkdownImage,
        @ViewBuilder content: () -> Content
    ) -> some View {
        if let linkString = image.linkUrl, let link = URL(string: linkString) {
            Link(destination: link) { content() }
        } else {
            content()
        }
    }
}
