import PhotosUI
import RyntraShared
import SwiftUI

struct ProjectGalleryEditor: View {
    @EnvironmentObject private var model: AppModel

    let project: Project
    let onSaved: () async -> Void

    @State private var isAdding = false
    @State private var addTitle = ""
    @State private var addDescription = ""
    @State private var addAsBanner = false
    @State private var selectedItem: PhotosPickerItem?
    @State private var viewedImage: GallerySelection?
    @State private var editedImage: GallerySelection?
    @State private var deleteTarget: GallerySelection?
    @State private var localError: String?

    private var images: [GalleryImage] {
        project.gallery.sorted {
            if $0.featured != $1.featured { return $0.featured }
            return $0.ordering < $1.ordering
        }
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            if images.isEmpty {
                VStack(spacing: 8) {
                    Image(systemName: "photo.on.rectangle.angled")
                        .font(.title2)
                        .foregroundStyle(.secondary)
                    Text(NSLocalizedString("No gallery images", comment: "Project gallery empty"))
                        .font(.headline)
                    Text(NSLocalizedString(
                        "Add screenshots to present the project. The featured image becomes its banner.",
                        comment: "Project gallery empty"
                    ))
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                    .multilineTextAlignment(.center)
                }
                .frame(maxWidth: .infinity)
                .padding(.vertical, 20)
            } else {
                LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 10) {
                    ForEach(images, id: \.url) { image in
                        galleryTile(image)
                    }
                }
            }

            DisclosureGroup(isExpanded: $isAdding) {
                VStack(spacing: 12) {
                    TextField(NSLocalizedString("Title", comment: "Gallery field"), text: $addTitle)
                        .textFieldStyle(.roundedBorder)
                    TextField(
                        NSLocalizedString("Description", comment: "Gallery field"),
                        text: $addDescription,
                        axis: .vertical
                    )
                    .lineLimit(2...4)
                    .textFieldStyle(.roundedBorder)
                    Toggle(
                        NSLocalizedString("Use as project banner", comment: "Gallery field"),
                        isOn: $addAsBanner
                    )
                    .tint(Color.ryntraGreen)
                    PhotosPicker(selection: $selectedItem, matching: .images) {
                        Label(
                            NSLocalizedString("Choose image", comment: "Gallery action"),
                            systemImage: "photo.badge.plus"
                        )
                        .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.borderedProminent)
                    .tint(Color.ryntraGreen)
                    .disabled(model.isProjectActionRunning)
                }
                .padding(.top, 12)
            } label: {
                Label(
                    NSLocalizedString("Add gallery image", comment: "Gallery action"),
                    systemImage: "plus.circle"
                )
                .font(.headline)
            }
            .tint(Color.ryntraGreen)

            if let error = localError ?? model.projectActionError {
                Text(error)
                    .font(.caption)
                    .foregroundStyle(.red)
            }
        }
        .onChange(of: selectedItem) { item in
            guard let item else { return }
            Task { await add(item) }
        }
        .sheet(item: $viewedImage) { selection in
            GalleryViewer(
                image: selection.image,
                isBusy: model.isProjectActionRunning,
                onSetBanner: { await setBanner(selection) },
                onEdit: {
                    viewedImage = nil
                    editedImage = selection
                },
                onDelete: {
                    viewedImage = nil
                    deleteTarget = selection
                }
            )
        }
        .sheet(item: $editedImage) { selection in
            GalleryMetadataEditor(image: selection.image) { featured, title, description, ordering in
                try await model.modifyGalleryImage(
                    project: project,
                    imageURL: selection.image.url,
                    featured: featured,
                    title: title,
                    description: description,
                    ordering: ordering
                )
                await onSaved()
            }
        }
        .confirmationDialog(
            NSLocalizedString("Delete gallery image?", comment: "Gallery confirmation"),
            isPresented: Binding(
                get: { deleteTarget != nil },
                set: { if !$0 { deleteTarget = nil } }
            ),
            titleVisibility: .visible
        ) {
            Button(NSLocalizedString("Delete", comment: "Gallery action"), role: .destructive) {
                guard let target = deleteTarget else { return }
                Task { await delete(target) }
            }
            Button(NSLocalizedString("Cancel", comment: "Gallery action"), role: .cancel) {}
        }
    }

    private func galleryTile(_ image: GalleryImage) -> some View {
        Button {
            viewedImage = GallerySelection(image: image)
        } label: {
            ZStack(alignment: .topLeading) {
                AsyncImage(url: URL(string: image.url)) { loaded in
                    loaded.resizable().scaledToFill()
                } placeholder: {
                    Color.secondary.opacity(0.12)
                }
                .frame(maxWidth: .infinity)
                .aspectRatio(1.45, contentMode: .fit)
                .clipShape(RoundedRectangle(cornerRadius: 8))

                if image.featured {
                    Label(
                        NSLocalizedString("Banner", comment: "Gallery badge"),
                        systemImage: "star.fill"
                    )
                    .font(.caption2.weight(.semibold))
                    .padding(.horizontal, 7)
                    .padding(.vertical, 5)
                    .background(.regularMaterial, in: Capsule())
                    .padding(7)
                }
            }
        }
        .buttonStyle(.plain)
        .contextMenu {
            if !image.featured {
                Button {
                    Task { await setBanner(GallerySelection(image: image)) }
                } label: {
                    Label(NSLocalizedString("Set as banner", comment: "Gallery action"), systemImage: "star")
                }
            }
            Button {
                editedImage = GallerySelection(image: image)
            } label: {
                Label(NSLocalizedString("Edit", comment: "Gallery action"), systemImage: "pencil")
            }
            Button(role: .destructive) {
                deleteTarget = GallerySelection(image: image)
            } label: {
                Label(NSLocalizedString("Delete", comment: "Gallery action"), systemImage: "trash")
            }
        }
    }

    @MainActor
    private func add(_ item: PhotosPickerItem) async {
        defer { selectedItem = nil }
        localError = nil
        do {
            let upload = try await ProjectImageUploadReader.load(
                item,
                baseName: "gallery",
                maxBytes: 5 * 1024 * 1024
            )
            try await model.addGalleryImage(
                project: project,
                file: upload,
                featured: addAsBanner,
                title: addTitle.trimmingCharacters(in: .whitespacesAndNewlines),
                description: addDescription.trimmingCharacters(in: .whitespacesAndNewlines)
            )
            addTitle = ""
            addDescription = ""
            addAsBanner = false
            isAdding = false
            await onSaved()
        } catch {
            localError = error.localizedDescription
        }
    }

    @MainActor
    private func setBanner(_ selection: GallerySelection) async {
        localError = nil
        do {
            try await model.setGalleryImageAsBanner(project: project, imageURL: selection.image.url)
            viewedImage = nil
            await onSaved()
        } catch {
            localError = error.localizedDescription
        }
    }

    @MainActor
    private func delete(_ selection: GallerySelection) async {
        localError = nil
        deleteTarget = nil
        do {
            try await model.deleteGalleryImage(project: project, imageURL: selection.image.url)
            await onSaved()
        } catch {
            localError = error.localizedDescription
        }
    }
}

private struct GallerySelection: Identifiable {
    let image: GalleryImage
    var id: String { image.url }
}

private struct GalleryViewer: View {
    @Environment(\.dismiss) private var dismiss

    let image: GalleryImage
    let isBusy: Bool
    let onSetBanner: () async -> Void
    let onEdit: () -> Void
    let onDelete: () -> Void

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 14) {
                    AsyncImage(url: URL(string: image.url)) { loaded in
                        loaded.resizable().scaledToFit()
                    } placeholder: {
                        ProgressView().frame(maxWidth: .infinity, minHeight: 220)
                    }
                    .frame(maxWidth: .infinity)

                    if let title = image.displayTitle {
                        Text(title).font(.headline)
                    }
                    if let description = image.description_, !description.isEmpty {
                        Text(description).foregroundStyle(.secondary)
                    }

                    HStack {
                        if !image.featured {
                            Button {
                                Task { await onSetBanner() }
                            } label: {
                                Label(NSLocalizedString("Set as banner", comment: "Gallery action"), systemImage: "star")
                            }
                        }
                        Button(action: onEdit) {
                            Label(NSLocalizedString("Edit", comment: "Gallery action"), systemImage: "pencil")
                        }
                        Button(role: .destructive, action: onDelete) {
                            Label(NSLocalizedString("Delete", comment: "Gallery action"), systemImage: "trash")
                        }
                    }
                    .buttonStyle(.bordered)
                    .disabled(isBusy)
                }
                .padding()
            }
            .navigationTitle(image.displayTitle ?? NSLocalizedString("Gallery image", comment: "Gallery title"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button(NSLocalizedString("Done", comment: "Gallery action")) { dismiss() }
                }
            }
        }
    }
}

private struct GalleryMetadataEditor: View {
    @Environment(\.dismiss) private var dismiss

    let image: GalleryImage
    let onSave: (Bool, String, String, Int32?) async throws -> Void

    @State private var title: String
    @State private var description: String
    @State private var ordering: String
    @State private var featured: Bool
    @State private var isSaving = false
    @State private var errorMessage: String?

    init(
        image: GalleryImage,
        onSave: @escaping (Bool, String, String, Int32?) async throws -> Void
    ) {
        self.image = image
        self.onSave = onSave
        _title = State(initialValue: image.displayTitle ?? "")
        _description = State(initialValue: image.description_ ?? "")
        _ordering = State(initialValue: String(image.ordering))
        _featured = State(initialValue: image.featured)
    }

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    TextField(NSLocalizedString("Title", comment: "Gallery field"), text: $title)
                    TextField(
                        NSLocalizedString("Description", comment: "Gallery field"),
                        text: $description,
                        axis: .vertical
                    )
                    .lineLimit(2...5)
                    TextField(NSLocalizedString("Ordering", comment: "Gallery field"), text: $ordering)
                        .keyboardType(.numberPad)
                    Toggle(
                        NSLocalizedString("Use as project banner", comment: "Gallery field"),
                        isOn: $featured
                    )
                    .tint(Color.ryntraGreen)
                }
                if let errorMessage {
                    Section { Text(errorMessage).foregroundStyle(.red) }
                }
            }
            .navigationTitle(NSLocalizedString("Edit gallery image", comment: "Gallery title"))
            .navigationBarTitleDisplayMode(.inline)
            .disabled(isSaving)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(NSLocalizedString("Cancel", comment: "Gallery action")) { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button(NSLocalizedString("Save", comment: "Gallery action")) {
                        Task { await save() }
                    }
                }
            }
        }
    }

    @MainActor
    private func save() async {
        isSaving = true
        errorMessage = nil
        defer { isSaving = false }
        do {
            try await onSave(featured, title, description, Int32(ordering))
            dismiss()
        } catch {
            errorMessage = error.localizedDescription
        }
    }
}
