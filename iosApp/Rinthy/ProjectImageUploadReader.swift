import Foundation
import PhotosUI
import RinthyShared
import UniformTypeIdentifiers

enum ProjectImageUploadError: LocalizedError {
    case unreadable
    case unsupportedFormat
    case tooLarge(mebibytes: Int)

    var errorDescription: String? {
        switch self {
        case .unreadable:
            return NSLocalizedString("Unable to read that image.", comment: "Image upload error")
        case .unsupportedFormat:
            return NSLocalizedString("Select a supported image file.", comment: "Image upload error")
        case .tooLarge(let mebibytes):
            return String(
                format: NSLocalizedString("Image must be %d MiB or smaller.", comment: "Image upload error"),
                mebibytes
            )
        }
    }
}

enum ProjectImageUploadReader {
    static func load(
        _ item: PhotosPickerItem,
        baseName: String,
        maxBytes: Int
    ) async throws -> ProjectFileUpload {
        guard let contentType = item.supportedContentTypes.first(where: { $0.conforms(to: .image) }) else {
            throw ProjectImageUploadError.unsupportedFormat
        }
        guard let data = try await item.loadTransferable(type: Data.self), !data.isEmpty else {
            throw ProjectImageUploadError.unreadable
        }
        guard data.count <= maxBytes else {
            throw ProjectImageUploadError.tooLarge(mebibytes: maxBytes / 1024 / 1024)
        }

        let fileName = "\(baseName).\(contentType.preferredFilenameExtension ?? "png")"
        let mimeType = contentType.preferredMIMEType ?? "image/png"
        let base64 = await Task.detached(priority: .userInitiated) {
            data.base64EncodedString()
        }.value
        return ProjectUploadFactory.shared.fromBase64(
            fileName: fileName,
            contentType: mimeType,
            base64: base64
        )
    }
}
