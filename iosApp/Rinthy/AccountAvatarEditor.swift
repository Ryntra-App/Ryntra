import PhotosUI
import RinthyShared
import SwiftUI

struct AccountAvatarEditor: View {
    let account: Account
    let isBusy: Bool
    let onChange: (ProjectFileUpload) async throws -> Void
    let onDelete: () async throws -> Void
    let onError: (String) -> Void

    @State private var selectedItem: PhotosPickerItem?

    var body: some View {
        VStack(spacing: 8) {
            PhotosPicker(selection: $selectedItem, matching: .images) {
                ZStack(alignment: .bottomTrailing) {
                    AsyncImage(url: URL(string: account.avatarUrl ?? "")) { image in
                        image.resizable().scaledToFill()
                    } placeholder: {
                        Circle().fill(.quaternary)
                    }
                    .frame(width: 68, height: 68)
                    .clipShape(Circle())
                    .overlay(Circle().stroke(Color.rinthyGreen.opacity(0.45), lineWidth: 1.5))

                    Image(systemName: "camera.fill")
                        .font(.caption.weight(.semibold))
                        .foregroundStyle(.white)
                        .frame(width: 26, height: 26)
                        .background(Color.rinthyGreen, in: Circle())
                        .overlay(Circle().stroke(Color.rinthyBackground, lineWidth: 2))
                }
            }
            .buttonStyle(.plain)
            .disabled(isBusy)
            .accessibilityLabel(NSLocalizedString("Change avatar", comment: "Profile action"))

            if isBusy {
                ProgressView().controlSize(.small)
            } else if account.avatarUrl != nil {
                Button(role: .destructive) {
                    Task { await removeAvatar() }
                } label: {
                    Image(systemName: "trash")
                }
                .buttonStyle(.borderless)
                .accessibilityLabel(NSLocalizedString("Remove avatar", comment: "Profile action"))
            }
        }
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
