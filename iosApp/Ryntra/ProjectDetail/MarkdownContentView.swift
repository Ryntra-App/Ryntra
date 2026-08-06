import RyntraShared
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
            ScrollView(.horizontal, showsIndicators: true) {
                Text(block.content)
                    .font(.system(.footnote, design: .monospaced))
                    .foregroundStyle(.primary)
                    .fixedSize(horizontal: true, vertical: false)
                    .textSelection(.enabled)
                    .padding(10)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(Color.secondary.opacity(0.12), in: RoundedRectangle(cornerRadius: 8))
        case .table:
            markdownTable
        case .quote:
            HStack(alignment: .top, spacing: 10) {
                RoundedRectangle(cornerRadius: 2)
                    .fill(Color.ryntraGreen)
                    .frame(width: 3)
                markdownText
            }
        case .listitem:
            HStack(alignment: .top, spacing: 8) {
                if let checked = block.checked {
                    Image(systemName: checked.boolValue ? "checkmark.square.fill" : "square")
                        .foregroundStyle(checked.boolValue ? Color.ryntraGreen : .secondary)
                        .accessibilityLabel(checked.boolValue ? "Completed" : "Not completed")
                } else {
                    Text(listMarker).font(font).foregroundStyle(foregroundStyle)
                }
                markdownText
            }
            .padding(.leading, CGFloat(block.level) * 18)
        default:
            markdownText
        }
    }

    @ViewBuilder
    private var markdownTable: some View {
        if let table = block.table {
            ScrollView(.horizontal, showsIndicators: true) {
                Grid(alignment: .leading, horizontalSpacing: 0, verticalSpacing: 0) {
                    tableRow(table.headers, alignments: table.alignments, header: true)
                    Divider().gridCellUnsizedAxes(.horizontal)
                    ForEach(Array(table.rows.enumerated()), id: \.offset) { index, row in
                        tableRow(row.cells, alignments: table.alignments, header: false)
                        if index != table.rows.count - 1 {
                            Divider().gridCellUnsizedAxes(.horizontal)
                        }
                    }
                }
                .padding(.vertical, 4)
            }
            .background(Color.secondary.opacity(0.08), in: RoundedRectangle(cornerRadius: 8))
        } else {
            Text(block.content).font(.system(.footnote, design: .monospaced))
        }
    }

    private func tableRow(_ cells: [String], alignments: [MarkdownTableAlignment], header: Bool) -> some View {
        GridRow {
            ForEach(Array(cells.enumerated()), id: \.offset) { index, cell in
                Text(cell)
                    .font(.footnote.weight(header ? .semibold : .regular))
                    .foregroundStyle(header ? .primary : .secondary)
                    .frame(
                        minWidth: 120,
                        maxWidth: 240,
                        alignment: tableAlignment(alignments.indices.contains(index) ? alignments[index] : .start)
                    )
                    .padding(.horizontal, 12)
                    .padding(.vertical, 9)
            }
        }
    }

    private func tableAlignment(_ alignment: MarkdownTableAlignment) -> Alignment {
        switch alignment {
        case .center: return .center
        case .end: return .trailing
        default: return .leading
        }
    }

    @ViewBuilder
    private var markdownImages: some View {
        if !block.images.isEmpty, block.images.allSatisfy(\.isBadge) {
            MarkdownBadgeRowView(images: block.images)
                .frame(maxWidth: .infinity, minHeight: 42, maxHeight: 42)
        } else if block.images.count == 1,
           !block.images[0].isBadge,
           let url = URL(string: block.images[0].url), url.isSafeMarkdownURL {
            imageLink(block.images[0]) {
                RemoteImage(url: url) { image in
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
                        if let url = URL(string: badge.url), url.isSafeMarkdownURL {
                            imageLink(badge) {
                                RemoteImage(url: url) { image in
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
            let start = attributed.index(attributed.startIndex, offsetByCharacters: Int(span.start))
            let end = attributed.index(attributed.startIndex, offsetByCharacters: Int(span.end))
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
                if let urlString = span.linkUrl, let url = URL(string: urlString), url.isSafeMarkdownURL {
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
        return block.ordered ? "\(block.ordinal)." : "•"
    }

    @ViewBuilder
    private func imageLink<Content: View>(
        _ image: MarkdownImage,
        @ViewBuilder content: () -> Content
    ) -> some View {
        if let linkString = image.linkUrl, let link = URL(string: linkString), link.isSafeMarkdownURL {
            Link(destination: link) { content() }
        } else {
            content()
        }
    }
}

private extension URL {
    var isSafeMarkdownURL: Bool {
        guard let scheme = scheme?.lowercased() else { return false }
        return ["http", "https", "mailto"].contains(scheme)
    }
}
