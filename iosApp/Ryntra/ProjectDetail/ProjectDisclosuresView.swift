import RyntraShared
import SwiftUI

/// Content disclosures tab: Modrinth's own settings page, one card per disclosure that applies to
/// this project type. Section 6 of the Content Rules is what makes each of them mandatory.
struct ProjectDisclosuresView: View {
    @EnvironmentObject private var model: AppModel

    let project: Project
    let canEditDetails: Bool
    let versionCount: Int
    let saveRequest: Int
    let onEditingStateChanged: (Bool, Bool) -> Void
    let onSavingChanged: (Bool) -> Void
    let onSaved: () -> Void

    @State private var baseline = ProjectDisclosureDraft(entries: [])
    @State private var draft = ProjectDisclosureDraft(entries: [])
    @State private var hasLoaded = false
    @State private var isLoading = false
    @State private var isSaving = false
    @State private var loadError: String?
    @State private var saveError: String?

    // Modrinth refuses disclosures on a project that has never published a file.
    private var hasVersions: Bool { versionCount > 0 }
    private var isEditable: Bool { canEditDetails && hasVersions }

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            header

            if !hasVersions {
                notice(
                    title: NSLocalizedString("Upload a version first", comment: "Disclosures gate"),
                    message: NSLocalizedString(
                        "Modrinth accepts disclosures only on projects that already have at least one version.",
                        comment: "Disclosures gate detail"
                    )
                )
            } else if !canEditDetails {
                notice(
                    title: NSLocalizedString("Read-only", comment: "Disclosures permission"),
                    message: NSLocalizedString(
                        "Editing disclosures needs the “Edit details” permission on this project.",
                        comment: "Disclosures permission detail"
                    )
                )
            }

            if let loadError {
                errorCard(loadError)
            }

            if isLoading && !hasLoaded {
                ProgressView()
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 44)
            } else if draft.entries.isEmpty {
                notice(
                    title: NSLocalizedString("No disclosures apply", comment: "Disclosures empty"),
                    message: NSLocalizedString(
                        "Modrinth does not ask for any disclosure on this kind of project.",
                        comment: "Disclosures empty detail"
                    )
                )
            } else {
                ForEach(draft.entries, id: \.type.apiValue) { entry in
                    DisclosureCardView(
                        entry: entry,
                        isEditable: isEditable,
                        onChange: { updated in draft = draft.replacing(entry: updated) }
                    )
                }
                issuesCard
            }

            if let saveError {
                Text(saveError)
                    .font(.caption)
                    .foregroundStyle(.red)
            }
        }
        .task(id: project.id) { await load(force: false) }
        .onAppear { reportEditingState() }
        .onChange(of: draft) { _ in reportEditingState() }
        .onChange(of: saveRequest) { _ in
            guard saveRequest > 0 else { return }
            Task { await save() }
        }
    }

    private var header: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack(spacing: 8) {
                Text(NSLocalizedString("Content disclosures", comment: "Disclosures section"))
                    .font(.headline)
                Spacer(minLength: 8)
                Button {
                    Task { await load(force: true) }
                } label: {
                    if isLoading {
                        ProgressView().controlSize(.small)
                    } else {
                        Image(systemName: "arrow.clockwise")
                    }
                }
                .buttonStyle(.plain)
                .disabled(isLoading)
                .accessibilityLabel(NSLocalizedString("Refresh disclosures", comment: "Disclosures action"))
            }
            Text(NSLocalizedString(
                "Modrinth requires every applicable disclosure to be declared so players know what a project contains before they download it.",
                comment: "Disclosures section detail"
            ))
            .font(.caption)
            .foregroundStyle(.secondary)
            Button {
                if let url = URL(string: DisclosureCopy.contentRulesURL) {
                    ryntraOpenExternalURL(url)
                }
            } label: {
                Label(
                    NSLocalizedString("Read the Content Rules", comment: "Disclosures action"),
                    systemImage: "book"
                )
                .font(.subheadline)
            }
            .buttonStyle(.bordered)
        }
    }

    @ViewBuilder
    private var issuesCard: some View {
        let issues = draft.issues()
        if !issues.isEmpty {
            VStack(alignment: .leading, spacing: 6) {
                Text(NSLocalizedString("Before saving", comment: "Disclosures validation"))
                    .font(.subheadline.weight(.semibold))
                ForEach(issues, id: \.key) { issue in
                    Text(DisclosureCopy.issueMessage(issue))
                        .font(.caption)
                        .foregroundStyle(.red)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(14)
            .background(Color.ryntraSurface, in: RoundedRectangle(cornerRadius: 12))
            .overlay {
                RoundedRectangle(cornerRadius: 12)
                    .stroke(Color.red.opacity(0.42), lineWidth: 0.5)
            }
        }
    }

    private func notice(title: String, message: String) -> some View {
        HStack(alignment: .top, spacing: 10) {
            Image(systemName: "info.circle.fill")
                .foregroundStyle(Color.ryntraGreen)
            VStack(alignment: .leading, spacing: 2) {
                Text(title).font(.subheadline.weight(.semibold))
                Text(message).font(.caption).foregroundStyle(.secondary)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(14)
        .background(Color.ryntraSurface, in: RoundedRectangle(cornerRadius: 12))
    }

    private func errorCard(_ message: String) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            Text(message).font(.subheadline)
            Button {
                Task { await load(force: true) }
            } label: {
                Label(NSLocalizedString("Retry", comment: "Retry action"), systemImage: "arrow.clockwise")
            }
            .buttonStyle(.bordered)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(14)
        .background(Color.ryntraSurface, in: RoundedRectangle(cornerRadius: 12))
        .overlay {
            RoundedRectangle(cornerRadius: 12)
                .stroke(Color.red.opacity(0.42), lineWidth: 0.5)
        }
    }

    @MainActor
    private func load(force: Bool) async {
        guard force || !hasLoaded, !isLoading else { return }
        isLoading = true
        loadError = nil
        do {
            let loaded = try await model.loadProjectDisclosures(project: project)
            rebaseline(with: loaded)
            hasLoaded = true
        } catch {
            loadError = error.localizedDescription
        }
        isLoading = false
    }

    @MainActor
    private func save() async {
        guard !isSaving, draft.canSave else { return }
        isSaving = true
        onSavingChanged(true)
        saveError = nil
        do {
            let saved = try await model.saveProjectDisclosures(
                project: project,
                draft: draft,
                baseline: baseline
            )
            rebaseline(with: saved)
            onSaved()
        } catch {
            saveError = error.localizedDescription
        }
        isSaving = false
        onSavingChanged(false)
    }

    /// Modrinth owns the disclosure record, so both a load and a save reset the editor to what the
    /// server now reports rather than to whatever was typed locally.
    private func rebaseline(with disclosures: [ProjectDisclosure]) {
        let rebuilt = ProjectDisclosureDraft.companion.from(project: project, disclosures: disclosures)
        baseline = rebuilt
        draft = rebuilt
        // A rebaseline can leave `draft` unchanged, so onChange would not fire on its own, and
        // draft now equals baseline by construction — there is nothing unsaved left to report.
        onEditingStateChanged(false, rebuilt.canSave)
    }

    private func reportEditingState() {
        onEditingStateChanged(draft.hasChangesFrom(baseline: baseline), draft.canSave)
    }
}

private struct DisclosureCardView: View {
    let entry: ProjectDisclosure
    let isEditable: Bool
    let onChange: (ProjectDisclosure) -> Void

    private var canToggle: Bool { isEditable && entry.canEdit && (entry.canDisable || !entry.enabled) }
    private var canEditDetails: Bool { isEditable && entry.canEdit }

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(alignment: .top, spacing: 12) {
                Image(systemName: DisclosureCopy.symbol(entry.type))
                    .font(.system(size: 17, weight: .semibold))
                    .foregroundStyle(Color.ryntraGreen)
                    .frame(width: 38, height: 38)
                    .background(Color.ryntraGreen.opacity(0.10), in: RoundedRectangle(cornerRadius: 10))
                VStack(alignment: .leading, spacing: 2) {
                    Text(DisclosureCopy.title(entry.type))
                        .font(.subheadline.weight(.semibold))
                    Text(DisclosureCopy.summary(entry.type))
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
                Spacer(minLength: 8)
                Toggle("", isOn: Binding(
                    get: { entry.enabled },
                    set: { onChange(entry.withEnabled(enabled: $0)) }
                ))
                .labelsHidden()
                .disabled(!canToggle)
                .accessibilityLabel(DisclosureCopy.title(entry.type))
            }

            if entry.setByModerator || !entry.canDisable {
                Label(
                    entry.canEdit
                        ? NSLocalizedString(
                            "A moderator applied this disclosure. You can update the details, but not remove it.",
                            comment: "Disclosure lock"
                        )
                        : NSLocalizedString("A moderator locked this disclosure.", comment: "Disclosure lock"),
                    systemImage: "lock"
                )
                .font(.caption)
                .foregroundStyle(.secondary)
            }

            if entry.enabled {
                DisclosureEditorView(entry: entry, isEnabled: canEditDetails, onChange: onChange)
            }
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.ryntraSurface, in: RoundedRectangle(cornerRadius: 16))
        .overlay {
            RoundedRectangle(cornerRadius: 16)
                .stroke(Color.ryntraSeparator, lineWidth: 0.5)
        }
    }
}

private struct DisclosureEditorView: View {
    let entry: ProjectDisclosure
    let isEnabled: Bool
    let onChange: (ProjectDisclosure) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            switch entry.type.apiValue {
            case "ai_content":
                fieldLabel(NSLocalizedString("What is generative AI used for?", comment: "AI disclosure"))
                aiUsagePicker
                noteField(
                    label: NSLocalizedString("Explanation (optional)", comment: "Disclosure field"),
                    placeholder: NSLocalizedString(
                        "e.g. The Chinese and Arabic translations are AI-generated.",
                        comment: "AI disclosure placeholder"
                    )
                )

            case "advertisements":
                noteField(
                    label: NSLocalizedString("Explanation", comment: "Disclosure field"),
                    placeholder: NSLocalizedString(
                        "e.g. Adds the Modrinth SMP server to your server list automatically.",
                        comment: "Advertising disclosure placeholder"
                    )
                )

            case "epilepsy_triggers":
                noteField(
                    label: NSLocalizedString("Explanation", comment: "Disclosure field"),
                    placeholder: NSLocalizedString(
                        "e.g. Adds a flashlight with a strobe mode. It can be turned off in Accessibility settings.",
                        comment: "Photosensitivity disclosure placeholder"
                    )
                )

            case "system_interactions":
                noteField(
                    label: NSLocalizedString("Describe the external interactions", comment: "Disclosure field"),
                    placeholder: NSLocalizedString(
                        "e.g. It writes a file called wake_up.txt to the desktop.",
                        comment: "System interactions disclosure placeholder"
                    )
                )

            case "archived":
                noteField(
                    label: NSLocalizedString("Explanation (optional)", comment: "Disclosure field"),
                    placeholder: NSLocalizedString(
                        "e.g. I no longer have time for this project — feel free to fork it!",
                        comment: "Archived disclosure placeholder"
                    )
                )

            case "telemetry":
                fieldLabel(NSLocalizedString("Consent model", comment: "Telemetry disclosure"))
                Picker("", selection: Binding(
                    get: { entry.consent.apiValue },
                    set: { selected in
                        guard let consent = DisclosureRules.shared.telemetryConsents
                            .first(where: { $0.apiValue == selected }) else { return }
                        onChange(entry.withConsent(consent: consent))
                    }
                )) {
                    ForEach(DisclosureRules.shared.telemetryConsents, id: \.apiValue) { consent in
                        Text(DisclosureCopy.consentLabel(consent)).tag(consent.apiValue)
                    }
                }
                .pickerStyle(.segmented)
                .labelsHidden()

                fieldLabel(NSLocalizedString("What data is collected?", comment: "Telemetry disclosure"))
                Text(NSLocalizedString(
                    "Add a privacy policy or list the kinds of data collected. Say whether it is anonymous or contains personally identifiable information.",
                    comment: "Telemetry disclosure detail"
                ))
                .font(.caption)
                .foregroundStyle(.secondary)
                rowsEditor(
                    rows: entry.editableRows(),
                    placeholder: NSLocalizedString(
                        "e.g. Anonymous launch analytics: Minecraft version and loader.",
                        comment: "Telemetry disclosure placeholder"
                    ),
                    addLabel: NSLocalizedString("Add data type", comment: "Telemetry disclosure action"),
                    onRowsChange: { onChange(entry.withDataCollected(dataCollected: $0)) }
                )

            case "paid_features":
                fieldLabel(NSLocalizedString("List the kinds of paid feature", comment: "Paid features disclosure"))
                rowsEditor(
                    rows: entry.editableRows(),
                    placeholder: NSLocalizedString(
                        "e.g. Cosmetics available as a Patreon reward",
                        comment: "Paid features disclosure placeholder"
                    ),
                    addLabel: NSLocalizedString("Add paid feature", comment: "Paid features disclosure action"),
                    onRowsChange: { onChange(entry.withFeatures(features: $0)) }
                )

            case "derivative_work":
                derivativeSourcesEditor

            default:
                EmptyView()
            }
        }
        .disabled(!isEnabled)
    }

    private var aiUsagePicker: some View {
        VStack(alignment: .leading, spacing: 8) {
            ForEach(DisclosureRules.shared.aiUsages, id: \.apiValue) { use in
                Toggle(isOn: Binding(
                    get: { entry.hasUse(use: use) },
                    set: { onChange(entry.withUse(use: use, selected: $0)) }
                )) {
                    Text(DisclosureCopy.usageLabel(use))
                        .font(.subheadline)
                }
            }
        }
    }

    private var derivativeSourcesEditor: some View {
        let sources = entry.editableSources()
        return VStack(alignment: .leading, spacing: 12) {
            ForEach(Array(sources.enumerated()), id: \.offset) { index, source in
                VStack(alignment: .leading, spacing: 10) {
                    HStack {
                        Text(String.localizedStringWithFormat(
                            NSLocalizedString("Original work %d", comment: "Derivative disclosure"),
                            index + 1
                        ))
                        .font(.caption.weight(.medium))
                        .foregroundStyle(.secondary)
                        Spacer()
                        if sources.count > 1 {
                            Button(role: .destructive) {
                                onChange(entry.withSources(sources: removing(sources, at: index)))
                            } label: {
                                Image(systemName: "trash")
                            }
                            .buttonStyle(.plain)
                            .accessibilityLabel(NSLocalizedString(
                                "Remove original work",
                                comment: "Derivative disclosure action"
                            ))
                        }
                    }
                    labelledField(
                        NSLocalizedString("Name of the original work", comment: "Derivative disclosure"),
                        placeholder: NSLocalizedString("Example project", comment: "Derivative disclosure placeholder"),
                        text: Binding(
                            get: { source.label },
                            set: { value in
                                onChange(entry.withSources(sources: replacing(sources, at: index, with: source.withLabel(label: value))))
                            }
                        )
                    )
                    labelledField(
                        NSLocalizedString("Link to the original work", comment: "Derivative disclosure"),
                        placeholder: "https://example.com",
                        text: Binding(
                            get: { source.link },
                            set: { value in
                                onChange(entry.withSources(sources: replacing(sources, at: index, with: source.withLink(link: value))))
                            }
                        ),
                        isURL: true
                    )
                    labelledField(
                        NSLocalizedString("How this project builds on it (optional)", comment: "Derivative disclosure"),
                        placeholder: NSLocalizedString(
                            "e.g. A fork that keeps 1.20 support alive.",
                            comment: "Derivative disclosure placeholder"
                        ),
                        text: Binding(
                            get: { source.note },
                            set: { value in
                                onChange(entry.withSources(sources: replacing(sources, at: index, with: source.withNote(note: value))))
                            }
                        )
                    )
                }
                .padding(12)
                .background(Color.ryntraSurfaceRaised, in: RoundedRectangle(cornerRadius: 12))
            }
            Button {
                onChange(entry.withSources(sources: sources + [DerivativeSource(label: "", link: "", note: "")]))
            } label: {
                Label(
                    NSLocalizedString("Add original work", comment: "Derivative disclosure action"),
                    systemImage: "plus"
                )
            }
            .buttonStyle(.bordered)
        }
    }

    private func rowsEditor(
        rows: [String],
        placeholder: String,
        addLabel: String,
        onRowsChange: @escaping ([String]) -> Void
    ) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            ForEach(Array(rows.enumerated()), id: \.offset) { index, row in
                HStack(spacing: 8) {
                    TextField(placeholder, text: Binding(
                        get: { row },
                        set: { onRowsChange(replacing(rows, at: index, with: $0)) }
                    ))
                    .textFieldStyle(.roundedBorder)
                    .frame(minHeight: 44)
                    if rows.count > 1 {
                        Button(role: .destructive) {
                            onRowsChange(removing(rows, at: index))
                        } label: {
                            Image(systemName: "trash")
                        }
                        .buttonStyle(.plain)
                    }
                }
            }
            Button {
                onRowsChange(rows + [""])
            } label: {
                Label(addLabel, systemImage: "plus")
            }
            .buttonStyle(.bordered)
        }
    }

    private func noteField(label: String, placeholder: String) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            fieldLabel(label)
            TextField(placeholder, text: Binding(
                get: { entry.note },
                set: { onChange(entry.withNote(note: $0)) }
            ), axis: .vertical)
            .lineLimit(3...6)
            .textFieldStyle(.roundedBorder)
        }
    }

    private func labelledField(
        _ label: String,
        placeholder: String,
        text: Binding<String>,
        isURL: Bool = false
    ) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            fieldLabel(label)
            if isURL {
                TextField(placeholder, text: text)
                    .textFieldStyle(.roundedBorder)
                    .frame(minHeight: 44)
                    .ryntraURLKeyboard()
                    .ryntraNoAutocapitalization()
                    .autocorrectionDisabled()
            } else {
                TextField(placeholder, text: text)
                    .textFieldStyle(.roundedBorder)
                    .frame(minHeight: 44)
            }
        }
    }

    private func fieldLabel(_ text: String) -> some View {
        Text(text)
            .font(.caption.weight(.medium))
            .foregroundStyle(.secondary)
            .frame(maxWidth: .infinity, alignment: .leading)
    }

    private func replacing<T>(_ values: [T], at index: Int, with value: T) -> [T] {
        var updated = values
        guard updated.indices.contains(index) else { return updated }
        updated[index] = value
        return updated
    }

    private func removing<T>(_ values: [T], at index: Int) -> [T] {
        var updated = values
        guard updated.indices.contains(index) else { return updated }
        updated.remove(at: index)
        return updated
    }
}

/// Labels and symbols keyed by the API value rather than by the bridged enum case: Kotlin/Native
/// rewrites enum entry names on export, and the wire value is the one thing guaranteed to be stable.
enum DisclosureCopy {
    static let contentRulesURL = "https://modrinth.com/legal/rules#generative-ai"

    static func symbol(_ type: DisclosureType) -> String {
        switch type.apiValue {
        case "ai_content": return "sparkles"
        case "advertisements": return "megaphone.fill"
        case "epilepsy_triggers": return "eye.fill"
        case "system_interactions": return "cpu.fill"
        case "telemetry": return "antenna.radiowaves.left.and.right"
        case "derivative_work": return "arrow.triangle.branch"
        case "paid_features": return "dollarsign.circle.fill"
        case "archived": return "archivebox.fill"
        default: return "checkmark.shield.fill"
        }
    }

    static func title(_ type: DisclosureType) -> String {
        switch type.apiValue {
        case "ai_content":
            return NSLocalizedString("Contains AI-generated content", comment: "Disclosure title")
        case "advertisements":
            return NSLocalizedString("Contains advertisements", comment: "Disclosure title")
        case "epilepsy_triggers":
            return NSLocalizedString("Photosensitivity warning", comment: "Disclosure title")
        case "system_interactions":
            return NSLocalizedString("External system interactions", comment: "Disclosure title")
        case "telemetry":
            return NSLocalizedString("Contains telemetry", comment: "Disclosure title")
        case "derivative_work":
            return NSLocalizedString("Contains derivative content", comment: "Disclosure title")
        case "paid_features":
            return NSLocalizedString("Contains paid features", comment: "Disclosure title")
        case "archived":
            return NSLocalizedString("Archive project", comment: "Disclosure title")
        default:
            return type.apiValue
        }
    }

    static func summary(_ type: DisclosureType) -> String {
        switch type.apiValue {
        case "ai_content":
            return NSLocalizedString(
                "Required when a substantial part of the code is AI-generated, any asset is substantially AI-generated, the functionality relies on generative AI, or the project page was written with it.",
                comment: "Disclosure detail"
            )
        case "advertisements":
            return NSLocalizedString(
                "Required when the project contains advertising, sponsorships or promotion of other works.",
                comment: "Disclosure detail"
            )
        case "epilepsy_triggers":
            return NSLocalizedString(
                "Enable this if the project contains anything that may be dangerous to people sensitive to flashing lights or patterns.",
                comment: "Disclosure detail"
            )
        case "system_interactions":
            return NSLocalizedString(
                "Required when the project reads or changes anything on the player’s system outside the game.",
                comment: "Disclosure detail"
            )
        case "telemetry":
            return NSLocalizedString(
                "Required when the project sends usage data back to you or to a third party.",
                comment: "Disclosure detail"
            )
        case "derivative_work":
            return NSLocalizedString(
                "Required when the project is a fork of another project or contains a substantial amount of someone else’s work.",
                comment: "Disclosure detail"
            )
        case "paid_features":
            return NSLocalizedString(
                "Required when the project has features that can be obtained by spending real-world money.",
                comment: "Disclosure detail"
            )
        case "archived":
            return NSLocalizedString(
                "Marks the project as no longer maintained. Archived projects stay discoverable while their visibility is public.",
                comment: "Disclosure detail"
            )
        default:
            return ""
        }
    }

    static func usageLabel(_ usage: AiUsage) -> String {
        switch usage.apiValue {
        case "code": return NSLocalizedString("Code", comment: "AI usage")
        case "assets": return NSLocalizedString("Assets", comment: "AI usage")
        case "text": return NSLocalizedString("Text", comment: "AI usage")
        case "functionality": return NSLocalizedString("Functionality", comment: "AI usage")
        default: return usage.apiValue
        }
    }

    static func consentLabel(_ consent: TelemetryConsent) -> String {
        switch consent.apiValue {
        case "opt_in": return NSLocalizedString("Opt-in", comment: "Telemetry consent")
        case "opt_out": return NSLocalizedString("Opt-out", comment: "Telemetry consent")
        case "always_active": return NSLocalizedString("Always active", comment: "Telemetry consent")
        default: return consent.apiValue
        }
    }

    static func issueMessage(_ issue: DisclosureIssue) -> String {
        switch issue.key {
        case "advertising_note":
            return NSLocalizedString("The advertising disclosure needs an explanation.", comment: "Disclosure validation")
        case "photosensitivity_note":
            return NSLocalizedString("The photosensitivity warning needs a description.", comment: "Disclosure validation")
        case "system_interactions_note":
            return NSLocalizedString("The external system interactions disclosure needs a description.", comment: "Disclosure validation")
        case "telemetry_empty":
            return NSLocalizedString("The telemetry disclosure needs at least one type of collected data.", comment: "Disclosure validation")
        case "derivative_empty":
            return NSLocalizedString("The derivative content disclosure needs at least one original work.", comment: "Disclosure validation")
        case "derivative_source_label":
            return NSLocalizedString("Every original work needs a name.", comment: "Disclosure validation")
        case "paid_features_empty":
            return NSLocalizedString("The paid features disclosure needs at least one feature.", comment: "Disclosure validation")
        default:
            return issue.key
        }
    }
}
