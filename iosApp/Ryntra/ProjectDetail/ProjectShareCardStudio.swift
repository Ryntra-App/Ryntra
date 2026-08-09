import CoreTransferable
import Foundation
import RyntraShared
import SwiftUI
import UniformTypeIdentifiers

#if canImport(UIKit)
import UIKit
#elseif canImport(AppKit)
import AppKit
#endif

struct ProjectShareCardStudio: View {
    @EnvironmentObject private var model: AppModel
    @Environment(\.dismiss) private var dismiss
    @Environment(\.displayScale) private var displayScale
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize

    let project: Project
    let versions: [ProjectVersion]

    @State private var format = ProjectShareCardFormat.square
    @State private var message = ProjectShareCardMessage.release
    @State private var paletteID = ProjectShareCardPalette.all[0].id
    @State private var selectedVersionID: String?
    @State private var headline = ""
    @State private var description = ""
    @State private var iconData: Data?
    @State private var loadedVersions: [ProjectVersion] = []
    @State private var renderedCard: ProjectShareCardPayload?
    @State private var isRendering = false
    @State private var renderError: String?

    private var selectedVersion: ProjectVersion? {
        cardVersions.first { $0.id == selectedVersionID } ?? cardVersions.first
    }

    private var cardVersions: [ProjectVersion] {
        loadedVersions.isEmpty ? versions : loadedVersions
    }

    private var palette: ProjectShareCardPalette {
        ProjectShareCardPalette.all.first { $0.id == paletteID } ?? ProjectShareCardPalette.all[0]
    }

    private var renderIdentity: String {
        [format.rawValue, message.rawValue, paletteID, selectedVersionID ?? "", headline, description, String(iconData?.count ?? 0)]
            .joined(separator: "|")
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 24) {
                    ProjectShareCardCanvas(
                        project: project,
                        version: selectedVersion,
                        message: message,
                        format: format,
                        palette: palette,
                        headline: headline,
                        description: description,
                        iconData: iconData
                    )
                    .frame(maxWidth: 620)
                    .frame(maxWidth: .infinity)
                    .shadow(color: .black.opacity(0.12), radius: 16, y: 6)

                    studioSection("Message") {
                        Picker("Message", selection: $message) {
                            ForEach(ProjectShareCardMessage.allCases) { option in
                                Text(option.localizedName).tag(option)
                            }
                        }
                        .pickerStyle(.menu)
                        .onChange(of: message) { _ in resetCopy() }
                    }

                    studioSection("Format") {
                        Picker("Format", selection: $format) {
                            ForEach(ProjectShareCardFormat.allCases) { option in
                                Text(option.localizedName).tag(option)
                            }
                        }
                        .pickerStyle(.segmented)
                    }

                    studioSection("Style") {
                        let layout: AnyLayout = dynamicTypeSize >= .accessibility1
                            ? AnyLayout(VStackLayout(spacing: 10))
                            : AnyLayout(HStackLayout(spacing: 10))
                        layout {
                            ForEach(ProjectShareCardPalette.all) { option in
                                paletteButton(option)
                            }
                        }
                    }

                    if !cardVersions.isEmpty {
                        studioSection("Version") {
                            Picker("Version", selection: $selectedVersionID) {
                                ForEach(cardVersions, id: \.id) { version in
                                    Text(version.versionNumber).tag(Optional(version.id))
                                }
                            }
                            .pickerStyle(.menu)
                            .onChange(of: selectedVersionID) { _ in resetCopy() }
                        }
                    }

                    studioSection("Headline") {
                        TextField("Headline", text: $headline, axis: .vertical)
                            .lineLimit(2...3)
                            .onChange(of: headline) { value in
                                if value.count > 90 { headline = String(value.prefix(90)) }
                            }
                        characterCount(headline.count, limit: 90)
                    }

                    studioSection("Description") {
                        TextField("Description", text: $description, axis: .vertical)
                            .lineLimit(3...6)
                            .onChange(of: description) { value in
                                if value.count > 240 { description = String(value.prefix(240)) }
                            }
                        characterCount(description.count, limit: 240)
                    }

                    if let renderError {
                        Label(renderError, systemImage: "exclamationmark.triangle.fill")
                            .font(.footnote)
                            .foregroundStyle(.red)
                    }
                }
                .frame(maxWidth: 760)
                .frame(maxWidth: .infinity)
                .padding(16)
                .padding(.bottom, 86)
            }
            .ryntraInteractiveKeyboardDismissal()
            .navigationTitle("Share card")
            .ryntraInlineNavigationTitle()
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Close") { dismiss() }
                }
            }
            .safeAreaInset(edge: .bottom, spacing: 0) {
                shareBar
            }
        }
        .interactiveDismissDisabled(isRendering)
        .task { await prepareStudio() }
        .task(id: renderIdentity) { await renderCardAfterChanges() }
    }

    private var shareBar: some View {
        VStack(spacing: 0) {
            Divider()
            Group {
                if let renderedCard {
                    ShareLink(
                        item: renderedCard,
                        preview: SharePreview(project.title, image: renderedCard.previewImage)
                    ) {
                        Label("Share PNG", systemImage: "square.and.arrow.up")
                            .font(.headline)
                            .frame(maxWidth: .infinity, minHeight: 44)
                    }
                    .buttonStyle(.borderedProminent)
                    .tint(.ryntraGreen)
                } else {
                    Button {} label: {
                        HStack(spacing: 8) {
                            ProgressView().controlSize(.small)
                            Text(isRendering
                                ? NSLocalizedString("Preparing image…", comment: "Share card export")
                                : NSLocalizedString("Share PNG", comment: "Share card export"))
                        }
                        .font(.headline)
                        .frame(maxWidth: .infinity, minHeight: 44)
                    }
                    .buttonStyle(.borderedProminent)
                    .disabled(true)
                }
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 10)
        }
        .background(.regularMaterial)
    }

    private func studioSection<Content: View>(
        _ title: LocalizedStringKey,
        @ViewBuilder content: () -> Content
    ) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            Text(title).font(.headline)
            content()
        }
    }

    private func paletteButton(_ option: ProjectShareCardPalette) -> some View {
        Button {
            paletteID = option.id
        } label: {
            VStack(spacing: 7) {
                RoundedRectangle(cornerRadius: 8, style: .continuous)
                    .fill(LinearGradient(
                        colors: [option.backgroundStart, option.backgroundEnd],
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    ))
                    .frame(height: 38)
                    .overlay(alignment: .topTrailing) {
                        Circle().fill(option.accent).frame(width: 8, height: 8).padding(7)
                    }
                    .overlay {
                        RoundedRectangle(cornerRadius: 8, style: .continuous)
                            .stroke(paletteID == option.id ? Color.accentColor : Color.ryntraSeparator, lineWidth: paletteID == option.id ? 2 : 0.5)
                    }
                Text(option.name)
                    .font(.caption)
                    .foregroundStyle(.primary)
                    .lineLimit(1)
            }
            .frame(maxWidth: .infinity)
        }
        .buttonStyle(.plain)
        .accessibilityAddTraits(paletteID == option.id ? .isSelected : [])
    }

    private func characterCount(_ count: Int, limit: Int) -> some View {
        Text("\(count) / \(limit)")
            .font(.caption.monospacedDigit())
            .foregroundStyle(.secondary)
            .frame(maxWidth: .infinity, alignment: .trailing)
            .accessibilityLabel(String.localizedStringWithFormat(
                NSLocalizedString("%d of %d characters", comment: "Character count"),
                count,
                limit
            ))
    }

    @MainActor
    private func prepareStudio() async {
        if versions.isEmpty {
            loadedVersions = (try? await model.loadProjectVersions(project: project)) ?? []
        }
        selectedVersionID = cardVersions.first?.id
        resetCopy()
        guard let value = project.iconUrl, let url = URL(string: value) else { return }
        do {
            let (data, response) = try await URLSession.shared.data(from: url)
            if let response = response as? HTTPURLResponse,
               !(200..<300).contains(response.statusCode) {
                return
            }
            guard data.count <= 4 * 1024 * 1024 else { return }
            iconData = data
        } catch {
            // The project initial remains as a truthful fallback if the icon is unavailable.
        }
    }

    @MainActor
    private func resetCopy() {
        headline = defaultHeadline
        description = defaultDescription
    }

    private var defaultHeadline: String {
        switch message {
        case .release:
            if let version = selectedVersion?.versionNumber, !version.isEmpty {
                return String.localizedStringWithFormat(
                    NSLocalizedString("Version %@ is available", comment: "Share card headline"),
                    version
                )
            }
            return String(localized: "A new project update is here")
        case .milestone:
            return String(localized: "Thank you for being part of this milestone")
        case .testers:
            return String(localized: "Help test the next update")
        }
    }

    private var defaultDescription: String {
        if message == .release {
            let highlights = projectShareCardHighlights(selectedVersion?.changelog ?? "")
            if !highlights.isEmpty { return highlights.map { "• \($0)" }.joined(separator: "\n") }
        }
        return String(project.description_.trimmingCharacters(in: .whitespacesAndNewlines).prefix(240))
    }

    @MainActor
    private func renderCardAfterChanges() async {
        guard !headline.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else {
            renderedCard = nil
            return
        }
        try? await Task.sleep(nanoseconds: 180_000_000)
        guard !Task.isCancelled else { return }
        isRendering = true
        renderError = nil
        defer { isRendering = false }

        let width: CGFloat = 600
        let canvas = ProjectShareCardCanvas(
            project: project,
            version: selectedVersion,
            message: message,
            format: format,
            palette: palette,
            headline: headline,
            description: description,
            iconData: iconData
        )
        .frame(width: width, height: width / format.aspectRatio)

        let renderer = ImageRenderer(content: canvas)
        renderer.scale = max(2, displayScale)
#if canImport(UIKit)
        guard let image = renderer.uiImage, let data = image.pngData() else {
            renderFailed()
            return
        }
#elseif canImport(AppKit)
        guard let image = renderer.nsImage,
              let tiff = image.tiffRepresentation,
              let bitmap = NSBitmapImageRep(data: tiff),
              let data = bitmap.representation(using: .png, properties: [:]) else {
            renderFailed()
            return
        }
#else
        renderFailed()
        return
#endif
        renderedCard = ProjectShareCardPayload(
            data: data,
            filename: "\((project.slug ?? project.id).shareCardFilename)-share-card.png"
        )
    }

    @MainActor
    private func renderFailed() {
        renderedCard = nil
        renderError = String(localized: "Couldn’t create the image. Try again.")
    }
}

struct ProjectShareCardPayload: Transferable, Sendable {
    let data: Data
    let filename: String

    static var transferRepresentation: some TransferRepresentation {
        FileRepresentation(exportedContentType: .png) { payload in
            let directory = FileManager.default.temporaryDirectory
                .appendingPathComponent("RyntraShareCards", isDirectory: true)
                .appendingPathComponent(UUID().uuidString, isDirectory: true)
            try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
            let url = directory.appendingPathComponent(payload.filename)
            try payload.data.write(to: url, options: .atomic)
            return SentTransferredFile(url)
        }
    }

    var previewImage: Image {
        ryntraImage(data: data) ?? Image(systemName: "photo")
    }
}

private func projectShareCardHighlights(_ changelog: String) -> [String] {
    changelog
        .split(whereSeparator: \.isNewline)
        .map { String($0).trimmingCharacters(in: .whitespacesAndNewlines) }
        .map { line in
            var result = line
            while let first = result.first, "-*+".contains(first) {
                result.removeFirst()
                result = result.trimmingCharacters(in: .whitespaces)
            }
            return result
        }
        .filter { !$0.isEmpty && !$0.hasPrefix("#") }
        .map { line in
            line
                .replacingOccurrences(
                    of: "!\\[[^]]*\\]\\([^)]*\\)",
                    with: "",
                    options: .regularExpression
                )
                .replacingOccurrences(
                    of: "\\[([^]]+)\\]\\([^)]*\\)",
                    with: "$1",
                    options: .regularExpression
                )
                .replacingOccurrences(of: "**", with: "")
                .replacingOccurrences(of: "__", with: "")
                .replacingOccurrences(of: "`", with: "")
        }
        .map { $0.count > 86 ? String($0.prefix(83)).trimmingCharacters(in: .whitespaces) + "…" : $0 }
        .prefix(3)
        .map { $0 }
}

private extension String {
    var shareCardFilename: String {
        let allowed = CharacterSet.alphanumerics.union(CharacterSet(charactersIn: "-_"))
        let cleaned = unicodeScalars.map { allowed.contains($0) ? Character(String($0)) : "-" }
        let result = String(cleaned).trimmingCharacters(in: CharacterSet(charactersIn: "-"))
        return result.isEmpty ? "project" : result
    }
}
