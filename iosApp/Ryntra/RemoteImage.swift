import SwiftUI

#if canImport(UIKit)
import UIKit
#elseif canImport(AppKit)
import AppKit
#endif

/// A drop-in replacement for `AsyncImage` that remembers what it downloaded.
///
/// `AsyncImage` keeps no cache: every time SwiftUI rebuilds the view it starts
/// the request again and shows the placeholder while it runs. Avatars and icons
/// therefore flash on every scroll tick and on every toolbar update. This view
/// checks an in-memory cache synchronously during `body`, so an image that has
/// already been fetched is drawn immediately and never blinks back.
struct RemoteImage<Content: View, Placeholder: View>: View {
    private let url: URL?
    private let content: (Image) -> Content
    private let placeholder: () -> Placeholder

    @State private var downloaded: Image?

    init(
        url: URL?,
        @ViewBuilder content: @escaping (Image) -> Content,
        @ViewBuilder placeholder: @escaping () -> Placeholder
    ) {
        self.url = url
        self.content = content
        self.placeholder = placeholder
    }

    var body: some View {
        Group {
            if let image = downloaded ?? url.flatMap(RemoteImageCache.shared.image(for:)) {
                content(image)
            } else {
                placeholder()
            }
        }
        .task(id: url) {
            guard let url, RemoteImageCache.shared.image(for: url) == nil else { return }
            downloaded = await RemoteImageCache.shared.load(url)
        }
    }
}

/// Decoded images kept in memory, keyed by URL.
@MainActor
final class RemoteImageCache {
    static let shared = RemoteImageCache()

    private let cache = NSCache<NSURL, CacheEntry>()
    private var inFlight: [URL: Task<Image?, Never>] = [:]

    private init() {
        cache.countLimit = 240
    }

    func image(for url: URL) -> Image? {
        cache.object(forKey: url as NSURL)?.image
    }

    func load(_ url: URL) async -> Image? {
        if let cached = image(for: url) { return cached }
        // Several rows can ask for the same avatar at once; share one request.
        if let existing = inFlight[url] { return await existing.value }

        let task = Task<Image?, Never> { [weak self] in
            defer { self?.inFlight[url] = nil }
            do {
                let (data, _) = try await URLSession.shared.data(from: url)
                guard let image = Self.decode(data) else { return nil }
                self?.cache.setObject(CacheEntry(image: image), forKey: url as NSURL)
                return image
            } catch {
                return nil
            }
        }
        inFlight[url] = task
        return await task.value
    }

    private static func decode(_ data: Data) -> Image? {
#if canImport(UIKit)
        UIImage(data: data).map(Image.init(uiImage:))
#elseif canImport(AppKit)
        NSImage(data: data).map(Image.init(nsImage:))
#endif
    }

    private final class CacheEntry {
        let image: Image
        init(image: Image) { self.image = image }
    }
}
