import PhotosUI
import RyntraShared
import SwiftUI

struct AccountAvatarEditor: View {
    let account: Account
    let isBusy: Bool
    let onChange: (ProjectFileUpload) async throws -> Void
    let onDelete: () async throws -> Void
    let onError: (String) -> Void

    @State private var selectedItem: PhotosPickerItem?
    @State private var isPickerPresented = false

    var body: some View {
        ZStack(alignment: .bottomTrailing) {
            AsyncImage(url: URL(string: account.avatarUrl ?? "")) { image in
                image.resizable().scaledToFill()
            } placeholder: {
                Circle().fill(.quaternary)
            }
            .frame(width: 68, height: 68)
            .clipShape(Circle())
            .overlay(Circle().stroke(Color.ryntraGreen.opacity(0.45), lineWidth: 1.5))

            if isBusy {
                ProgressView()
                    .controlSize(.small)
                    .tint(.white)
                    .frame(width: 28, height: 28)
                    .background(Color.ryntraGreen, in: Circle())
                    .overlay(Circle().stroke(Color.ryntraBackground, lineWidth: 2))
            } else {
                Menu {
                    Button {
                        isPickerPresented = true
                    } label: {
                        Label(
                            NSLocalizedString("Choose photo", comment: "Profile action"),
                            systemImage: "photo"
                        )
                    }
                    if account.avatarUrl != nil {
                        Button(role: .destructive) {
                            Task { await removeAvatar() }
                        } label: {
                            Label(
                                NSLocalizedString("Remove avatar", comment: "Profile action"),
                                systemImage: "trash"
                            )
                        }
                    }
                } label: {
                    Image(systemName: "camera.fill")
                        .font(.caption.weight(.semibold))
                        .foregroundStyle(.white)
                        .frame(width: 28, height: 28)
                        .background(Color.ryntraGreen, in: Circle())
                        .overlay(Circle().stroke(Color.ryntraBackground, lineWidth: 2))
                }
                .accessibilityLabel(NSLocalizedString("Avatar actions", comment: "Profile action"))
            }
        }
        .photosPicker(isPresented: $isPickerPresented, selection: $selectedItem, matching: .images)
        .onChange(of: selectedItem) { item in
            guard let item else { return }
            Task { await upload(item) }
        }
    }

    @MainActor
    private func upload(_ item: PhotosPickerItem) async {
        defer { selectedItem = nil }
        do {
            let upload = try await ProjectImageUploadReader.load(
                item,
                baseName: "avatar",
                maxBytes: 2 * 1024 * 1024
            )
            try await onChange(upload)
        } catch {
            onError(error.localizedDescription)
        }
    }

    @MainActor
    private func removeAvatar() async {
        do {
            try await onDelete()
        } catch {
            onError(error.localizedDescription)
        }
    }
}
