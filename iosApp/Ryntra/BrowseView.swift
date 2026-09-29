import RyntraShared
import SwiftUI

/// The public Modrinth catalogue.
///
/// Reached from the search button in the chrome rather than from a tab: the four tabs are the
/// creator's own workspace, and browsing other people's projects is a different errand.
struct BrowseView: View {
    @EnvironmentObject private var model: AppModel
    @AppStorage("themeStyle") private var storedThemeStyle = RyntraThemeStyle.platform.rawValue

    let onOpenHit: (ProjectSearchHit) -> Void

    @State private var query = ProjectSearchQuery.companion.initial()
    @State private var hits: [ProjectSearchHit] = []
    @State private var totalHits = 0
    @State private var hasMore = false
    @State private var isLoading = false
    @State private var isLoadingMore = false
    @State private var errorMessage: String?
    @State private var highlights = BrowseHighlights(popular: [], recentlyUpdated: [])
    @State private var isLoadingHighlights = false
    @State private var metadata = BrowseMetadata(gameVersions: [], loaders: [])
    @State private var isFilterSheetPresented = false
    @State private var searchTask: Task<Void, Never>?

    /// With nothing typed and nothing filtered there is no result set, so the strips take over.
    private var isShowingHighlights: Bool { !query.hasText && !query.hasFilters }

    private var usesSystemSearch: Bool {
        RyntraSearchPlacement.usesSystemSearchBar(themeStyle: storedThemeStyle)
    }

    var body: some View {
        ScrollView {
            LazyVStack(alignment: .leading, spacing: 12) {
                if !usesSystemSearch {
                    RyntraSearchField(
                        text: Binding(get: { query.text }, set: { setText($0) }),
                        prompt: NSLocalizedString("Search Modrinth", comment: "Browse placeholder"),
                        onSubmit: { submit() }
                    )
                }
                categoryRow
                toolRow

                if let errorMessage {
                    Text(errorMessage)
                        .font(.caption)
                        .foregroundStyle(.red)
                }

                if isShowingHighlights {
                    highlightsContent
                } else {
                    resultsContent
                }
            }
            .frame(maxWidth: 760, alignment: .leading)
            .frame(maxWidth: .infinity)
            .padding(.horizontal, 16)
            .padding(.top, 12)
            .padding(.bottom, 36)
        }
        .ryntraInteractiveKeyboardDismissal()
        .ryntraScreenBackdrop()
        .ryntraSystemSearch(
            text: Binding(get: { query.text }, set: { setText($0) }),
            prompt: NSLocalizedString("Search Modrinth", comment: "Browse placeholder"),
            isEnabled: usesSystemSearch,
            onSubmit: { submit() }
        )
        .sheet(isPresented: $isFilterSheetPresented) {
            BrowseFilterSheet(
                query: query,
                metadata: metadata,
                onToggleGameVersion: { apply(query.togglingGameVersion(version: $0)) },
                onToggleLoader: { apply(query.togglingLoader(loader: $0)) },
                onReset: { apply(query.withoutVersionAndLoaderFilters()) }
            )
        }
        .task { await loadSupportingData() }
        .onDisappear { searchTask?.cancel() }
    }

    private var categoryRow: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                ForEach(BrowseCatalogue.categories, id: \.apiValue) { category in
                    BrowseCategoryButton(
                        title: BrowseCatalogue.label(for: category),
                        systemImage: BrowseCatalogue.symbol(for: category),
                        isSelected: category == query.category
                    ) {
                        apply(query.withCategory(category: category))
                    }
                }
            }
            .padding(.vertical, 2)
        }
    }

    /// Sort as a menu of choices, so the current one carries the system checkmark, and the
    /// filters in a sheet, the way Apple's own apps keep a result list uncluttered
    private var toolRow: some View {
        HStack(spacing: 8) {
            Menu {
                Picker(
                    NSLocalizedString("Sort", comment: "Browse sort menu"),
                    selection: Binding(
                        get: { query.effectiveSort.apiValue },
                        set: { value in
                            guard let sort = BrowseCatalogue.sorts.first(where: { $0.apiValue == value }) else { return }
                            apply(query.withSort(sort: sort))
                        }
                    )
                ) {
                    ForEach(BrowseCatalogue.sorts, id: \.apiValue) { sort in
                        Label(BrowseCatalogue.label(for: sort), systemImage: BrowseCatalogue.symbol(for: sort))
                            .tag(sort.apiValue)
                    }
                }
            } label: {
                Label(BrowseCatalogue.label(for: query.effectiveSort), systemImage: "arrow.up.arrow.down")
                    .font(.subheadline)
            }
            .menuStyle(.borderlessButton)
            .fixedSize()

            Spacer(minLength: 8)

            let activeFilterCount = query.gameVersions.count + query.loaders.count
            Button {
                isFilterSheetPresented = true
            } label: {
                Label(
                    activeFilterCount > 0
                        ? String.localizedStringWithFormat(
                            NSLocalizedString("Filters · %d", comment: "Browse filters with count"),
                            activeFilterCount
                        )
                        : NSLocalizedString("Filters", comment: "Browse filters"),
                    systemImage: activeFilterCount > 0
                        ? "line.3.horizontal.decrease.circle.fill"
                        : "line.3.horizontal.decrease.circle"
                )
                .font(.subheadline)
            }
            .buttonStyle(.bordered)
            .buttonBorderShape(.capsule)
            .tint(activeFilterCount > 0 ? Color.accentColor : Color.secondary)
        }
    }

    @ViewBuilder
    private var highlightsContent: some View {
        if !model.recentSearches.isEmpty {
            HStack {
                Text(NSLocalizedString("Recent searches", comment: "Browse section"))
                    .font(.subheadline.weight(.semibold))
                Spacer()
                Button(NSLocalizedString("Clear", comment: "Browse action")) {
                    model.clearRecentSearches()
                }
                .font(.caption)
            }
            .padding(.top, 6)

            ForEach(model.recentSearches, id: \.self) { recent in
                // Two sibling buttons rather than a tap gesture on the row, so VoiceOver and
                // the keyboard reach both actions
                HStack(spacing: 12) {
                    Button {
                        setText(recent)
                        submit()
                    } label: {
                        HStack(spacing: 12) {
                            Image(systemName: "clock").foregroundStyle(.secondary).font(.caption)
                            Text(recent).font(.subheadline).lineLimit(1)
                            Spacer(minLength: 8)
                        }
                        .frame(minHeight: 44)
                        .contentShape(Rectangle())
                    }
                    .buttonStyle(.plain)
                    Button {
                        model.forgetSearch(recent)
                    } label: {
                        Image(systemName: "xmark")
                            .font(.caption)
                            .foregroundStyle(.secondary)
                            .ryntraMinimumTouchTarget()
                    }
                    .buttonStyle(.plain)
                    .accessibilityLabel(NSLocalizedString("Remove from history", comment: "Browse action"))
                }
            }
        }

        if isLoadingHighlights && highlights.isEmpty {
            ProgressView().frame(maxWidth: .infinity).padding(.vertical, 44)
        } else {
            if !highlights.popular.isEmpty {
                BrowseStrip(
                    title: NSLocalizedString("Popular right now", comment: "Browse section"),
                    hits: highlights.popular,
                    onOpen: onOpenHit
                )
            }
            if !highlights.recentlyUpdated.isEmpty {
                BrowseStrip(
                    title: NSLocalizedString("Recently updated", comment: "Browse section"),
                    hits: highlights.recentlyUpdated,
                    onOpen: onOpenHit
                )
            }
        }
    }

    @ViewBuilder
    private var resultsContent: some View {
        if isLoading {
            ProgressView().frame(maxWidth: .infinity).padding(.vertical, 44)
        } else if hits.isEmpty {
            VStack(alignment: .leading, spacing: 6) {
                Text(NSLocalizedString("Nothing found", comment: "Browse empty"))
                    .font(.subheadline.weight(.semibold))
                Text(NSLocalizedString(
                    "Try a different query, or relax the filters.",
                    comment: "Browse empty detail"
                ))
                .font(.caption)
                .foregroundStyle(.secondary)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.vertical, 36)
        } else {
            Text(String.localizedStringWithFormat(
                NSLocalizedString("%d projects found", comment: "Browse result count"),
                totalHits
            ))
            .font(.caption)
            .foregroundStyle(.secondary)
            .padding(.top, 6)

            ForEach(hits, id: \.projectId) { hit in
                BrowseResultRow(hit: hit) { onOpenHit(hit) }
                    .onAppear { loadMoreIfNeeded(after: hit) }
            }

            if isLoadingMore {
                ProgressView().frame(maxWidth: .infinity).padding(.vertical, 16)
            }
        }
    }

    // MARK: - Loading

    @MainActor
    private func loadSupportingData() async {
        if metadata.gameVersions.isEmpty {
            metadata = (try? await model.loadBrowseMetadata()) ?? metadata
        }
        guard highlights.isEmpty else { return }
        isLoadingHighlights = true
        highlights = (try? await model.loadBrowseHighlights()) ?? highlights
        isLoadingHighlights = false
    }

    /// Typing searches on its own after a short pause, so every keystroke is not a request.
    private func setText(_ text: String) {
        apply(query.withText(text: text), debounced: true)
    }

    private func submit() {
        searchTask?.cancel()
        if query.hasText {
            model.rememberSearch(query.text)
        }
        runSearch(query)
    }

    private func apply(_ next: ProjectSearchQuery, debounced: Bool = false) {
        query = next
        searchTask?.cancel()
        guard next.hasText || next.hasFilters else {
            hits = []
            totalHits = 0
            hasMore = false
            isLoading = false
            errorMessage = nil
            return
        }
        guard debounced else {
            runSearch(next)
            return
        }
        searchTask = Task {
            try? await Task.sleep(nanoseconds: searchDebounceNanoseconds)
            guard !Task.isCancelled else { return }
            await performSearch(next, replacing: true)
        }
    }

    private func runSearch(_ next: ProjectSearchQuery) {
        searchTask = Task { await performSearch(next, replacing: true) }
    }

    private func loadMoreIfNeeded(after hit: ProjectSearchHit) {
        guard hasMore, !isLoading, !isLoadingMore else { return }
        guard hits.suffix(prefetchDistance).contains(where: { $0.projectId == hit.projectId }) else { return }
        let next = query.nextPage()
        query = next
        Task { await performSearch(next, replacing: false) }
    }

    @MainActor
    private func performSearch(_ searched: ProjectSearchQuery, replacing: Bool) async {
        if replacing { isLoading = true } else { isLoadingMore = true }
        errorMessage = nil
        do {
            let page = try await model.searchProjects(query: searched)
            guard !Task.isCancelled else { return }
            if replacing {
                hits = page.hits
            } else {
                // Modrinth can repeat a project across pages when the index shifts mid-scroll.
                var seen = Set(hits.map(\.projectId))
                hits += page.hits.filter { seen.insert($0.projectId).inserted }
            }
            totalHits = Int(page.totalHits)
            hasMore = page.hasMore
        } catch {
            guard !Task.isCancelled else { return }
            if replacing {
                hits = []
                totalHits = 0
                hasMore = false
            }
            errorMessage = error.localizedDescription
        }
        isLoading = false
        isLoadingMore = false
    }

    private var searchDebounceNanoseconds: UInt64 { 350_000_000 }
    private var prefetchDistance: Int { 4 }
}

private struct BrowseResultRow: View {
    let hit: ProjectSearchHit
    let onOpen: () -> Void

    var body: some View {
        Button(action: onOpen) {
            HStack(alignment: .top, spacing: 12) {
                RemoteImage(url: URL(string: hit.iconUrl ?? "")) { image in
                    image.resizable().scaledToFill()
                } placeholder: {
                    RoundedRectangle(cornerRadius: 10).fill(.quaternary)
                }
                .frame(width: 52, height: 52)
                .clipShape(RoundedRectangle(cornerRadius: 10))

                VStack(alignment: .leading, spacing: 3) {
                    Text(hit.title)
                        .font(.subheadline.weight(.semibold))
                        .lineLimit(1)
                    Text(BrowseCatalogue.subtitle(for: hit))
                        .font(.caption)
                        .foregroundStyle(.secondary)
                        .lineLimit(1)
                    if !hit.description_.isEmpty {
                        Text(hit.description_)
                            .font(.caption)
                            .foregroundStyle(.secondary)
                            .lineLimit(2)
                    }
                    HStack(spacing: 12) {
                        Label(BrowseCatalogue.count(hit.downloads), systemImage: "arrow.down.circle")
                        Label(BrowseCatalogue.count(hit.follows), systemImage: "heart")
                    }
                    .font(.caption2)
                    .foregroundStyle(.secondary)
                    .padding(.top, 2)
                }
                Spacer(minLength: 8)
                Image(systemName: "chevron.right").font(.caption).foregroundStyle(.tertiary)
            }
            .padding(.vertical, 10)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }
}

private struct BrowseStrip: View {
    let title: String
    let hits: [ProjectSearchHit]
    let onOpen: (ProjectSearchHit) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text(title).font(.subheadline.weight(.semibold)).padding(.top, 8)
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 10) {
                    ForEach(hits, id: \.projectId) { hit in
                        Button {
                            onOpen(hit)
                        } label: {
                            VStack(alignment: .leading, spacing: 6) {
                                RemoteImage(url: URL(string: hit.iconUrl ?? "")) { image in
                                    image.resizable().scaledToFill()
                                } placeholder: {
                                    RoundedRectangle(cornerRadius: 10).fill(.quaternary)
                                }
                                .frame(width: 44, height: 44)
                                .clipShape(RoundedRectangle(cornerRadius: 10))

                                Text(hit.title)
                                    .font(.subheadline.weight(.semibold))
                                    .lineLimit(1)
                                if let byline = hit.byline {
                                    Text(byline).font(.caption2).foregroundStyle(.secondary).lineLimit(1)
                                }
                                Label(BrowseCatalogue.count(hit.downloads), systemImage: "arrow.down.circle")
                                    .font(.caption2)
                                    .foregroundStyle(.secondary)
                            }
                            .frame(width: 160, alignment: .leading)
                            .padding(12)
                            .background(Color.ryntraSurface, in: RoundedRectangle(cornerRadius: 14))
                        }
                        .buttonStyle(.plain)
                    }
                }
                .padding(.vertical, 2)
            }
        }
    }
}

/// Loaders and Minecraft versions as a grouped form of checkmark rows, the system pattern for
/// picking several values. Changes apply as they are made; Done only closes the sheet
private struct BrowseFilterSheet: View {
    @Environment(\.dismiss) private var dismiss
    let query: ProjectSearchQuery
    let metadata: BrowseMetadata
    let onToggleGameVersion: (String) -> Void
    let onToggleLoader: (String) -> Void
    let onReset: () -> Void

    /// Twelve releases cover the last few years; older ones are one tap further
    private static let foldedVersionCount = 12
    @State private var showsAllVersions = false

    var body: some View {
        let versions = metadata.defaultReleaseVersions
        let loaders = metadata.loadersFor(category: query.category)
        NavigationStack {
            Form {
                if versions.isEmpty && loaders.isEmpty {
                    HStack(spacing: 10) {
                        ProgressView()
                        Text(NSLocalizedString("Loading filter options…", comment: "Browse filters"))
                            .foregroundStyle(.secondary)
                    }
                }
                if !loaders.isEmpty {
                    Section {
                        ForEach(loaders, id: \.self) { loader in
                            checkRow(
                                title: loader.capitalized,
                                isSelected: query.loaders.contains(loader)
                            ) { onToggleLoader(loader) }
                        }
                    } header: {
                        Label(NSLocalizedString("Loader", comment: "Browse filter"), systemImage: "wrench.and.screwdriver")
                    }
                }
                if !versions.isEmpty {
                    Section {
                        ForEach(visibleVersions(versions), id: \.self) { version in
                            checkRow(
                                title: version,
                                isSelected: query.gameVersions.contains(version)
                            ) { onToggleGameVersion(version) }
                        }
                        if versions.count > Self.foldedVersionCount {
                            Button(
                                showsAllVersions
                                    ? NSLocalizedString("Show fewer", comment: "Browse filters")
                                    : String.localizedStringWithFormat(
                                        NSLocalizedString("Show all (%d)", comment: "Browse filters"),
                                        versions.count
                                    )
                            ) {
                                withAnimation { showsAllVersions.toggle() }
                            }
                        }
                    } header: {
                        Label(NSLocalizedString("Minecraft version", comment: "Browse filter"), systemImage: "gamecontroller")
                    }
                }
            }
            .formStyle(.grouped)
            .navigationTitle(NSLocalizedString("Filters", comment: "Browse filters"))
            .ryntraInlineNavigationTitle()
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(NSLocalizedString("Reset", comment: "Browse filters"), action: onReset)
                        .disabled(query.gameVersions.isEmpty && query.loaders.isEmpty)
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button(NSLocalizedString("Done", comment: "Sheet action")) { dismiss() }
                }
            }
        }
#if os(macOS)
        // A Mac sheet sizes itself to its content, and a Form has no intrinsic height
        .frame(minWidth: 420, minHeight: 520)
#else
        .presentationDetents([.medium, .large])
#endif
    }

    /// A selection past the fold is never hidden
    private func visibleVersions(_ versions: [String]) -> [String] {
        guard !showsAllVersions, versions.count > Self.foldedVersionCount else { return versions }
        let folded = Array(versions.prefix(Self.foldedVersionCount))
        let selectedBeyond = versions.dropFirst(Self.foldedVersionCount).filter { query.gameVersions.contains($0) }
        return folded + selectedBeyond
    }

    private func checkRow(title: String, isSelected: Bool, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            HStack {
                Text(title).foregroundStyle(.primary)
                Spacer()
                if isSelected {
                    Image(systemName: "checkmark")
                        .font(.body.weight(.semibold))
                        .foregroundStyle(.tint)
                }
            }
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityAddTraits(isSelected ? .isSelected : [])
    }
}

/// A content type as a capsule button with the symbol the site uses for it
private struct BrowseCategoryButton: View {
    let title: String
    let systemImage: String
    let isSelected: Bool
    let action: () -> Void

    var body: some View {
        if isSelected {
            button.buttonStyle(.borderedProminent)
        } else {
            button.buttonStyle(.bordered).tint(.secondary)
        }
    }

    private var button: some View {
        Button(action: action) {
            Label(title, systemImage: systemImage).font(.subheadline)
        }
        .buttonBorderShape(.capsule)
        .accessibilityAddTraits(isSelected ? .isSelected : [])
    }
}

/// Labels and formatting for the catalogue, keyed by the API value rather than by the bridged
/// enum case: Kotlin/Native rewrites enum entry names on export, and the wire value is stable.
enum BrowseCatalogue {
    static var categories: [BrowseCategory] { BrowseCategory.companion.browsable }

    static var sorts: [ProjectSearchSort] { ProjectSearchSort.companion.all }

    static func label(for category: BrowseCategory) -> String {
        switch category.apiValue {
        case nil: return NSLocalizedString("All", comment: "Browse category")
        case "mod": return NSLocalizedString("Mods", comment: "Browse category")
        case "plugin": return NSLocalizedString("Plugins", comment: "Browse category")
        case "datapack": return NSLocalizedString("Data packs", comment: "Browse category")
        case "shader": return NSLocalizedString("Shaders", comment: "Browse category")
        case "resourcepack": return NSLocalizedString("Resource packs", comment: "Browse category")
        case "modpack": return NSLocalizedString("Modpacks", comment: "Browse category")
        default: return category.apiValue ?? ""
        }
    }

    /// SF Symbols closest to the icons modrinth.com gives each content type
    static func symbol(for category: BrowseCategory) -> String {
        switch category.apiValue {
        case nil: return "square.grid.2x2"
        case "mod": return "cube"
        case "plugin": return "powerplug"
        case "datapack": return "curlybraces"
        case "shader": return "eyeglasses"
        case "resourcepack": return "paintbrush"
        case "modpack": return "shippingbox"
        default: return "square.grid.2x2"
        }
    }

    static func symbol(for sort: ProjectSearchSort) -> String {
        switch sort.apiValue {
        case "relevance": return "sparkles"
        case "downloads": return "arrow.down.circle"
        case "follows": return "heart"
        case "updated": return "arrow.clockwise"
        case "newest": return "calendar.badge.plus"
        default: return "arrow.up.arrow.down"
        }
    }

    static func label(for sort: ProjectSearchSort) -> String {
        switch sort.apiValue {
        case "relevance": return NSLocalizedString("Relevance", comment: "Browse sort")
        case "downloads": return NSLocalizedString("Downloads", comment: "Browse sort")
        case "follows": return NSLocalizedString("Followers", comment: "Browse sort")
        case "updated": return NSLocalizedString("Recently updated", comment: "Browse sort")
        case "newest": return NSLocalizedString("Newest", comment: "Browse sort")
        default: return sort.apiValue
        }
    }

    /// Browsing someone else's catalogue, the author matters more than the slug.
    static func subtitle(for hit: ProjectSearchHit) -> String {
        let kind = ProjectStatusSupport.typeLabel(forType: hit.kindKey)
        guard let byline = hit.byline else { return kind }
        return "\(byline)  ·  \(kind)"
    }

    static func count(_ value: Int64) -> String {
        let formatter = NumberFormatter()
        formatter.numberStyle = .decimal
        return formatter.string(from: NSNumber(value: value)) ?? "\(value)"
    }
}
