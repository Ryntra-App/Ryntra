import RyntraShared
import SwiftUI

struct MarkdownProjectEditor: View {
    @Environment(\.dismiss) private var dismiss
    @Binding var text: String
    @State private var isPreviewing = false
    @State private var previewBlocks: [MarkdownBlock] = []

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                Picker("Editor mode", selection: $isPreviewing) {
                    Text("Write").tag(false)
                    Text("Preview").tag(true)
                }
                .pickerStyle(.segmented)
                .padding(.horizontal, 16)
                .padding(.vertical, 10)

                Divider()

                if isPreviewing {
                    ScrollView {
                        VStack(alignment: .leading, spacing: 9) {
                            if previewBlocks.isEmpty {
                                Text(NSLocalizedString("Nothing to preview", comment: "Markdown editor"))
                                    .foregroundStyle(.secondary)
                                    .frame(maxWidth: .infinity, minHeight: 180, alignment: .topLeading)
                            } else {
                                ForEach(Array(previewBlocks.enumerated()), id: \.offset) { _, block in
                                    MarkdownBlockView(block: block)
                                }
                            }
                        }
                        .frame(maxWidth: 760, alignment: .leading)
                        .frame(maxWidth: .infinity)
                        .padding(16)
                    }
                } else {
                    TextEditor(text: $text)
                        .font(.body)
                        .padding(.horizontal, 12)
                        .scrollContentBackground(.hidden)
                        .background(Color.ryntraBackground)
                }
            }
            .background(Color.ryntraBackground)
            .navigationTitle(NSLocalizedString("Full description", comment: "Project editor title"))
            .ryntraInlineNavigationTitle()
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button(NSLocalizedString("Done", comment: "Finish editing")) { dismiss() }
                }
            }
            .onChange(of: isPreviewing) { previewing in
                guard previewing else { return }
                let markdown = text
                Task {
                    previewBlocks = await Task.detached(priority: .userInitiated) {
                        MarkdownParser.shared.parse(markdown: markdown)
                    }.value
                }
            }
        }
    }
}
