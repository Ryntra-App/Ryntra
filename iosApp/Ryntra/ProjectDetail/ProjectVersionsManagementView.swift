import RyntraShared
import SwiftUI
import UniformTypeIdentifiers

struct ProjectVersionsManagementView: View {
    @EnvironmentObject private var model: AppModel

    let project: Project
    let versions: [ProjectVersion]
    let isReadOnly: Bool
    let isLoading: Bool
    let errorMessage: String?
    let onReload: () async -> Void

    @State private var editingVersion: ProjectVersion?
    @State private var isCreating = false

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack {
                Text("Releases").font(.title3.bold())
                Spacer()
                if !isReadOnly {
                    Button { isCreating = true } label: {
                        Image(systemName: "plus")
                    }
                    .buttonStyle(.borderedProminent)
                    .tint(.ryntraGreen)
                    .ryntraMinimumTouchTarget()
                    .accessibilityLabel(NSLocalizedString("Create version", comment: "Version action"))
                }
            }

            if isLoading {
                HStack(spacing: 10) {
                    ProgressView()
                    Text("Loading releases").foregroundStyle(.secondary)
                }
                .padding(.vertical, 28)
            } else if let errorMessage, versions.isEmpty {
                managementEmpty(title: "Versions unavailable", message: errorMessage, retry: onReload)
            } else if versions.isEmpty {
                managementEmpty(title: "No versions yet", message: "Published releases for this project will appear here.")
            } else {
                LazyVStack(spacing: 0) {
                    ForEach(versions, id: \.id) { version in
                        ManagedVersionCard(version: version) {
                            if !isReadOnly { editingVersion = version }
                        }
                    }
                }
            }

            if let error = model.projectActionError {
                Text(error).font(.caption).foregroundStyle(.red)
            }
        }
        .sheet(isPresented: $isCreating) {
            VersionEditorSheet(project: project, version: nil) {
                await onReload()
                isCreating = false
            }
        }
        .sheet(
            isPresented: Binding(
                get: { editingVersion != nil },
                set: { if !$0 { editingVersion = nil } }
            )
        ) {
            if let editingVersion {
                VersionEditorSheet(project: project, version: editingVersion) {
                    await onReload()
                    self.editingVersion = nil
                }
            }
        }
    }

    private func managementEmpty(
        title: String,
        message: String,
        retry: (() async -> Void)? = nil
    ) -> some View {
        VStack(alignment: .leading, spacing: 7) {
            Text(LocalizedStringKey(title)).font(.headline)
            Text(LocalizedStringKey(message)).foregroundStyle(.secondary)
            if let retry {
                Button {
                    Task { await retry() }
                } label: {
                    Label(NSLocalizedString("Retry", comment: "Common action"), systemImage: "arrow.clockwise")
                }
                .buttonStyle(.bordered)
                .padding(.top, 4)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(16)
        .background(Color.ryntraSurface, in: RoundedRectangle(cornerRadius: 8))
    }
}

private struct ManagedVersionCard: View {
    let version: ProjectVersion
    let onOpen: () -> Void

    var body: some View {
        VStack(spacing: 0) {
            Button(action: onOpen) {
                VStack(alignment: .leading, spacing: 10) {
                HStack(alignment: .top) {
                    VStack(alignment: .leading, spacing: 4) {
                        Text(version.versionNumber).font(.headline.bold())
                        Text(version.name).font(.subheadline).foregroundStyle(.secondary).lineLimit(2)
                    }
                    Spacer()
                    Label(ryntraExactCount(version.downloads), systemImage: "arrow.down")
                        .font(.caption.bold())
                        .foregroundStyle(Color.ryntraGreen)
                }
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 6) {
                        ForEach(version.gameVersions, id: \.self) { chip($0, accent: false) }
                        ForEach(version.loaders, id: \.self) { chip($0.uppercased(), accent: true) }
                    }
                }
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(14)
            }
            .buttonStyle(.plain)
            if !version.changelog.isEmpty {
                VStack(alignment: .leading, spacing: 6) {
                    ForEach(Array(MarkdownParser.shared.parse(markdown: version.changelog).prefix(2).enumerated()), id: \.offset) { _, block in
                        MarkdownBlockView(block: block)
                    }
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.horizontal, 14)
                .padding(.bottom, 12)
            }
            Divider()
        }
    }

    private func chip(_ text: String, accent: Bool) -> some View {
        Text(text)
            .font(.caption2.bold())
            .foregroundStyle(accent ? Color.ryntraGreen : Color.primary)
            .padding(.horizontal, 8)
            .padding(.vertical, 5)
            .background(accent ? Color.ryntraGreen.opacity(0.12) : Color.ryntraSurfaceRaised, in: RoundedRectangle(cornerRadius: 7))
    }

}

private struct VersionEditorSheet: View {
    @EnvironmentObject private var model: AppModel
    @Environment(\.dismiss) private var dismiss

    let project: Project
    let version: ProjectVersion?
    let onSaved: () async -> Void

    @State private var name: String
    @State private var versionNumber: String
    @State private var changelog: String
    @State private var gameVersions: [String]
    @State private var loaders: [String]
    @State private var gameVersionInput = ""
    @State private var loaderInput = ""
    @State private var dependencies: [ProjectDependency]
    @State private var dependencyInput = ""
    @State private var versionType: String
    @State private var isFeatured: Bool
    @State private var changelogMode = 0
    @State private var changelogPreview: [MarkdownBlock] = []
    @State private var selectedFiles: [ProjectFileUpload] = []
    @State private var selectedFileNames: [String] = []
    @State private var selectedFileSizes: [Int] = []
    @State private var primaryFileIndex = 0
    @State private var isReadingFiles = false
    @State private var isFileImporterPresented = false
    @State private var localError: String?
    @State private var isConfirmingDeletion = false

    init(project: Project, version: ProjectVersion?, onSaved: @escaping () async -> Void) {
        self.project = project
        self.version = version
        self.onSaved = onSaved
        _name = State(initialValue: version?.name ?? "")
        _versionNumber = State(initialValue: version?.versionNumber ?? "")
        _changelog = State(initialValue: version?.changelog ?? "")
        _gameVersions = State(initialValue: version?.gameVersions ?? [])
        _loaders = State(initialValue: version?.loaders ?? [])
        _dependencies = State(initialValue: version?.dependencies ?? [])
        _versionType = State(initialValue: version?.versionType ?? "release")
        _isFeatured = State(initialValue: version?.featured ?? false)
    }

    private var canSave: Bool {
        !name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty &&
        !versionNumber.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty &&
        !gameVersions.isEmpty && !loaders.isEmpty &&
        (version != nil || !selectedFiles.isEmpty) && !isReadingFiles
    }

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    TextField("Name", text: $name)
                    TextField("Version number", text: $versionNumber)
                    Picker("Release channel", selection: $versionType) {
                        Text("Release").tag("release")
                        Text("Beta").tag("beta")
                        Text("Alpha").tag("alpha")
                    }
                    Toggle("Featured", isOn: $isFeatured)
                } header: {
                    Text(NSLocalizedString("Version", comment: "Version section"))
                } footer: {
                    Text(NSLocalizedString(
                        "Name and version number are required.",
                        comment: "Version validation guidance"
                    ))
                }
                Section {
                    valueEditor(
                        title: NSLocalizedString("Minecraft versions", comment: "Version compatibility"),
                        placeholder: NSLocalizedString("For example 1.21.5", comment: "Version compatibility input"),
                        input: $gameVersionInput,
                        values: gameVersions,
                        suggestions: [],
                        normalize: { $0 },
                        onChange: { gameVersions = $0 }
                    )
                    valueEditor(
                        title: NSLocalizedString("Loaders", comment: "Version compatibility"),
                        placeholder: NSLocalizedString("For example fabric", comment: "Loader input"),
                        input: $loaderInput,
                        values: loaders,
                        suggestions: ["fabric", "forge", "neoforge", "quilt"],
                        normalize: { $0.lowercased() },
                        onChange: { loaders = $0 }
                    )
                } header: {
                    Text(NSLocalizedString("Compatibility", comment: "Version section"))
                } footer: {
                    Text(NSLocalizedString(
                        "Add at least one Minecraft version and one loader. Enter one value at a time without spaces or commas.",
                        comment: "Version validation guidance"
                    ))
                }
                Section {
                    HStack {
                        TextField(
                            NSLocalizedString("Project ID", comment: "Dependency input"),
                            text: $dependencyInput
                        )
                        .ryntraNoAutocapitalization()
                        .autocorrectionDisabled()
                        .submitLabel(.done)
                        .onSubmit(addDependency)
                        Button(NSLocalizedString("Add", comment: "Common action"), action: addDependency)
                            .disabled(!isValidDependencyInput)
                    }
                    ForEach(dependencies.indices, id: \.self) { index in
                        HStack(spacing: 10) {
                            VStack(alignment: .leading, spacing: 2) {
                                Text(dependencyIdentifier(dependencies[index]))
                                    .lineLimit(1)
                                Text(NSLocalizedString("Modrinth project dependency", comment: "Dependency hint"))
                                    .font(.caption)
                                    .foregroundStyle(.secondary)
                            }
                            Spacer()
                            Picker(
                                NSLocalizedString("Dependency type", comment: "Dependency type"),
                                selection: dependencyTypeBinding(at: index)
                            ) {
                                Text(NSLocalizedString("Required", comment: "Dependency type")).tag("required")
                                Text(NSLocalizedString("Optional", comment: "Dependency type")).tag("optional")
                                Text(NSLocalizedString("Incompatible", comment: "Dependency type")).tag("incompatible")
                                Text(NSLocalizedString("Embedded", comment: "Dependency type")).tag("embedded")
                            }
                            .labelsHidden()
                            .pickerStyle(.menu)
                            Button(role: .destructive) {
                                dependencies.remove(at: index)
                            } label: {
                                Image(systemName: "trash")
                                    .ryntraMinimumTouchTarget()
                            }
                            .buttonStyle(.plain)
                            .accessibilityLabel(NSLocalizedString("Remove dependency", comment: "Dependency action"))
                        }
                    }
                } header: {
                    Text(NSLocalizedString("Dependencies", comment: "Version section"))
                } footer: {
                    Text(NSLocalizedString(
                        "Add the Modrinth project ID, then choose whether it is required, optional, incompatible, or embedded.",
                        comment: "Dependency guidance"
                    ))
                }
                Section("Changelog") {
                    Picker("Changelog mode", selection: $changelogMode) {
                        Text("Write").tag(0)
                        Text("Preview").tag(1)
                    }
                    .pickerStyle(.segmented)
                    if changelogMode == 0 {
                        TextEditor(text: $changelog)
                            .frame(minHeight: 170)
                            .scrollContentBackground(.hidden)
                            .background(Color.ryntraSurface)
                    } else {
                        VStack(alignment: .leading, spacing: 8) {
                            if changelogPreview.isEmpty {
                                Text("Nothing to preview").font(.caption).foregroundStyle(.secondary)
                            }
                            ForEach(Array(changelogPreview.enumerated()), id: \.offset) { _, block in
                                MarkdownBlockView(block: block)
                            }
                        }
                        .frame(maxWidth: .infinity, minHeight: 170, alignment: .topLeading)
                    }
                }
                if version == nil {
                    Section("Files") {
                        Button {
                            isFileImporterPresented = true
                        } label: {
                            Label(isReadingFiles ? "Reading files" : "Add version files", systemImage: "doc.badge.plus")
                        }
                        .disabled(isReadingFiles)
                        ForEach(selectedFileNames.indices, id: \.self) { index in
                            HStack {
                                Button {
                                    primaryFileIndex = index
                                } label: {
                                    Image(systemName: primaryFileIndex == index ? "checkmark.circle.fill" : "circle")
                                        .foregroundStyle(primaryFileIndex == index ? Color.ryntraGreen : Color.secondary)
                                }
                                .buttonStyle(.plain)
                                Text(selectedFileNames[index]).lineLimit(1)
                                Spacer()
                                Button(role: .destructive) { removeFile(at: index) } label: {
                                    Image(systemName: "trash")
                                }
                                .buttonStyle(.plain)
                            }
                        }
                    }
                } else if let version, !version.files.isEmpty {
                    Section("Files") {
                        ForEach(version.files, id: \.url) { file in
                            if let fileURL = URL(string: file.url) {
                                Link(file.filename, destination: fileURL)
                            } else {
                                Text(file.filename)
                                    .foregroundStyle(.secondary)
                            }
                        }
                    }
                }
                if let error = localError ?? model.projectActionError {
                    Section { Text(error).foregroundStyle(.red) }
                }
                if let version {
                    Section {
                        Button(role: .destructive) {
                            isConfirmingDeletion = true
                        } label: {
                            Label("Delete version", systemImage: "trash")
                        }
                    }
                }
            }
            .ryntraInteractiveKeyboardDismissal()
            .navigationTitle(version == nil ? "Create version" : "Edit version")
            .ryntraInlineNavigationTitle()
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                        .disabled(model.isProjectActionRunning || isReadingFiles)
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Save") { Task { await save() } }
                        .disabled(!canSave || model.isProjectActionRunning)
                }
            }
            .fileImporter(
                isPresented: $isFileImporterPresented,
                allowedContentTypes: [.data, .archive, .zip],
                allowsMultipleSelection: true
            ) { result in
                Task { await importFiles(result) }
            }
            .onChange(of: changelogMode) { mode in
                if mode == 1 {
                    changelogPreview = MarkdownParser.shared.parse(markdown: changelog)
                }
            }
            .confirmationDialog(
                NSLocalizedString("Delete this version?", comment: "Version deletion confirmation"),
                isPresented: $isConfirmingDeletion,
                titleVisibility: .visible
            ) {
                Button(NSLocalizedString("Delete version", comment: "Version action"), role: .destructive) {
                    Task { await deleteVersion() }
                }
                Button(NSLocalizedString("Cancel", comment: "Common action"), role: .cancel) {}
            } message: {
                Text(NSLocalizedString(
                    "The release and its files will be permanently removed. This cannot be undone.",
                    comment: "Version deletion confirmation"
                ))
            }
        }
        .interactiveDismissDisabled(model.isProjectActionRunning || isReadingFiles)
    }

    @MainActor
    private func save() async {
        localError = nil
        do {
            if let version {
                let update = VersionUpdate(
                    name: name.trimmingCharacters(in: .whitespacesAndNewlines),
                    versionNumber: versionNumber.trimmingCharacters(in: .whitespacesAndNewlines),
                    changelog: changelog,
                    dependencies: dependencies,
                    gameVersions: gameVersions,
                    versionType: versionType,
                    loaders: loaders,
                    featured: KotlinBoolean(bool: isFeatured),
                    status: nil
                )
                try await model.updateVersion(versionID: version.id, update: update)
            } else if !selectedFiles.isEmpty {
                let request = CreateVersionRequest(
                    name: name.trimmingCharacters(in: .whitespacesAndNewlines),
                    versionNumber: versionNumber.trimmingCharacters(in: .whitespacesAndNewlines),
                    changelog: changelog,
                    dependencies: dependencies,
                    gameVersions: gameVersions,
                    versionType: versionType,
                    loaders: loaders,
                    featured: isFeatured,
                    files: selectedFiles,
                    primaryFileIndex: Int32(primaryFileIndex)
                )
                try await model.createVersion(project: project, request: request)
            }
            await onSaved()
            dismiss()
        } catch {
            localError = error.localizedDescription
        }
    }

    @ViewBuilder
    private func valueEditor(
        title: String,
        placeholder: String,
        input: Binding<String>,
        values: [String],
        suggestions: [String],
        normalize: @escaping (String) -> String,
        onChange: @escaping ([String]) -> Void
    ) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(title).font(.caption).foregroundStyle(.secondary)
            HStack {
                TextField(placeholder, text: input)
                    .ryntraNoAutocapitalization()
                    .autocorrectionDisabled()
                    .submitLabel(.done)
                    .onSubmit { addValue(input: input, values: values, normalize: normalize, onChange: onChange) }
                Button(NSLocalizedString("Add", comment: "Common action")) {
                    addValue(input: input, values: values, normalize: normalize, onChange: onChange)
                }
                .disabled(normalizedValue(input.wrappedValue, using: normalize) == nil)
            }
            if !suggestions.isEmpty {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 7) {
                        ForEach(suggestions, id: \.self) { suggestion in
                            valueChip(
                                suggestion,
                                isSelected: values.contains(suggestion),
                                action: { toggleValue(suggestion, values: values, onChange: onChange) }
                            )
                        }
                    }
                }
            }
            let customValues = values.filter { !suggestions.contains($0) }
            if !customValues.isEmpty {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 7) {
                        ForEach(customValues, id: \.self) { value in
                            valueChip(
                                value,
                                isSelected: true,
                                action: { toggleValue(value, values: values, onChange: onChange) }
                            )
                        }
                    }
                }
                .accessibilityLabel(title)
            }
        }
    }

    private func valueChip(_ value: String, isSelected: Bool, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Label(value, systemImage: isSelected ? "checkmark" : "plus")
                .font(.caption.weight(.semibold))
                .foregroundStyle(isSelected ? Color.ryntraGreen : Color.secondary)
                .padding(.horizontal, 9)
                .padding(.vertical, 6)
                .background(
                    isSelected ? Color.ryntraGreen.opacity(0.12) : Color.ryntraSurfaceRaised,
                    in: Capsule()
                )
        }
        .buttonStyle(.plain)
        .ryntraMinimumTouchTarget()
        .accessibilityAddTraits(isSelected ? .isSelected : [])
    }

    private func addValue(
        input: Binding<String>,
        values: [String],
        normalize: (String) -> String,
        onChange: ([String]) -> Void
    ) {
        guard let value = normalizedValue(input.wrappedValue, using: normalize) else { return }
        if !values.contains(value) { onChange(values + [value]) }
        input.wrappedValue = ""
    }

    private func normalizedValue(_ rawValue: String, using normalize: (String) -> String) -> String? {
        let value = normalize(rawValue.trimmingCharacters(in: .whitespacesAndNewlines))
        guard !value.isEmpty, !value.contains(","), !value.contains(where: \.isWhitespace) else { return nil }
        return value
    }

    private func toggleValue(_ value: String, values: [String], onChange: ([String]) -> Void) {
        onChange(values.contains(value) ? values.filter { $0 != value } : values + [value])
    }

    private var isValidDependencyInput: Bool {
        normalizedValue(dependencyInput, using: { $0 }) != nil
    }

    private func addDependency() {
        guard let projectID = normalizedValue(dependencyInput, using: { $0 }),
              !dependencies.contains(where: { $0.projectId == projectID }) else { return }
        dependencies.append(ProjectDependency(
            versionId: nil,
            projectId: projectID,
            fileName: nil,
            dependencyType: "required",
            title: nil,
            iconUrl: nil
        ))
        dependencyInput = ""
    }

    private func dependencyIdentifier(_ dependency: ProjectDependency) -> String {
        dependency.projectId ?? dependency.versionId ?? dependency.fileName ??
            NSLocalizedString("Unknown dependency", comment: "Dependency fallback")
    }

    private func dependencyTypeBinding(at index: Int) -> Binding<String> {
        Binding(
            get: { dependencies[index].dependencyType },
            set: { type in
                let dependency = dependencies[index]
                dependencies[index] = ProjectDependency(
                    versionId: dependency.versionId,
                    projectId: dependency.projectId,
                    fileName: dependency.fileName,
                    dependencyType: type,
                    title: dependency.title,
                    iconUrl: dependency.iconUrl
                )
            }
        )
    }

    @MainActor
    private func deleteVersion() async {
        guard let version else { return }
        localError = nil
        do {
            try await model.deleteVersion(versionID: version.id)
            await onSaved()
            dismiss()
        } catch {
            localError = error.localizedDescription
        }
    }

    @MainActor
    private func importFiles(_ result: Result<[URL], Error>) async {
        isReadingFiles = true
        defer { isReadingFiles = false }
        do {
            let urls = try result.get()
            let payloads = try await Task.detached(priority: .userInitiated) {
                try urls.map { url -> (String, String, String, Int) in
                    let hasAccess = url.startAccessingSecurityScopedResource()
                    defer { if hasAccess { url.stopAccessingSecurityScopedResource() } }
                    let data = try Data(contentsOf: url, options: .mappedIfSafe)
                    let mime = UTType(filenameExtension: url.pathExtension)?.preferredMIMEType ?? "application/octet-stream"
                    return (url.lastPathComponent, mime, data.base64EncodedString(), data.count)
                }
            }.value
            let totalSize = payloads.reduce(selectedFileSizes.reduce(0, +)) { $0 + $1.3 }
            guard totalSize <= 128 * 1024 * 1024 else {
                localError = "Version files must be 128 MiB or smaller in total."
                return
            }
            for payload in payloads where !selectedFileNames.contains(payload.0) {
                selectedFiles.append(
                    ProjectUploadFactory.shared.fromBase64(
                        fileName: payload.0,
                        contentType: payload.1,
                        base64: payload.2
                    )
                )
                selectedFileNames.append(payload.0)
                selectedFileSizes.append(payload.3)
            }
            localError = nil
        } catch {
            localError = error.localizedDescription
        }
    }

    private func removeFile(at index: Int) {
        selectedFiles.remove(at: index)
        selectedFileNames.remove(at: index)
        selectedFileSizes.remove(at: index)
        primaryFileIndex = min(primaryFileIndex, max(selectedFiles.count - 1, 0))
    }
}
