import Foundation
import RinthyShared
import SwiftUI
import UIKit

struct AccountView: View {
    @EnvironmentObject private var model: AppModel
    @AppStorage("showFavoriteProjects") private var showFavoriteProjects = true
    @AppStorage("reduceMotion") private var reduceMotion = false
    @AppStorage("themeStyle") private var storedThemeStyle = RinthyThemeStyle.platform.rawValue
    @AppStorage("appearanceMode") private var storedAppearanceMode = RinthyAppearanceMode.system.rawValue
    @AppStorage("appLanguage") private var storedAppLanguage = RinthyAppLanguage.system.rawValue

    let account: Account
    let projectCount: Int
    let organizationCount: Int
    @State private var username = ""
    @State private var bio = ""
    @State private var syncedUsername = ""
    @State private var syncedBio = ""
    @State private var isSaving = false
    @State private var isAvatarSaving = false
    @State private var errorMessage: String?
    @State private var avatarError: String?

    private var normalizedUsername: String {
        username.trimmingCharacters(in: .whitespacesAndNewlines)
    }

    private var normalizedBio: String {
        bio.trimmingCharacters(in: .whitespacesAndNewlines)
    }

    private var hasProfileChanges: Bool {
        normalizedUsername != account.username || normalizedBio != (account.bio ?? "")
    }

    private var isPlatformNative: Bool {
        storedThemeStyle == RinthyThemeStyle.platform.rawValue
    }

    var body: some View {
        List {
            Section {
                HStack(spacing: 14) {
                    AccountAvatarEditor(
                        account: account,
                        isBusy: isAvatarSaving,
                        onChange: updateAvatar,
                        onDelete: deleteAvatar,
                        onError: { avatarError = $0 }
                    )

                    VStack(alignment: .leading, spacing: 4) {
                        Text(account.username).font(.title2.bold())
                        Text(account.role.capitalized)
                            .font(.subheadline.weight(.semibold))
                            .foregroundStyle(Color.rinthyGreen)
                    }
                }
                if let bio = account.bio, !bio.isEmpty {
                    Text(bio).foregroundStyle(.secondary)
                }
                if let avatarError {
                    Text(avatarError)
                        .font(.caption)
                        .foregroundStyle(.red)
                }
            }
            .themedListRowBackground(isPlatformNative: isPlatformNative)

            Section {
                TextField("Username", text: $username)
                    .textInputAutocapitalization(.never)
                    .disabled(isSaving)
                TextField("Bio", text: $bio, axis: .vertical)
                    .lineLimit(3...6)
                    .disabled(isSaving)
                if let errorMessage {
                    Text(errorMessage)
                        .font(.caption)
                        .foregroundStyle(.red)
                }
                Button {
                    Task { await saveProfile() }
                } label: {
                    if isSaving {
                        ProgressView()
                    } else {
                        Label("Save profile", systemImage: "checkmark.circle")
                    }
                }
                .disabled(normalizedUsername.isEmpty || !hasProfileChanges || isSaving)
                .tint(Color.rinthyGreen)
            } header: {
                RinthySectionLabel(text: "Profile")
            }
            .themedListRowBackground(isPlatformNative: isPlatformNative)

            Section {
                LabeledContent("Projects", value: "\(projectCount)")
                LabeledContent("Organizations", value: "\(organizationCount)")
            } header: {
                RinthySectionLabel(text: "Workspace")
            }
            .themedListRowBackground(isPlatformNative: isPlatformNative)

            Section {
                Link(destination: URL(string: "https://modrinth.com/user/\(account.username)")!) {
                    Label("Open Modrinth profile", systemImage: "arrow.up.right.square")
                }
                Link(destination: URL(string: "https://modrinth.com/settings/pats")!) {
                    Label("API tokens", systemImage: "key")
                }
                Link(destination: URL(string: "https://modrinth.com/dashboard/revenue")!) {
                    Label("Creator payouts", systemImage: "wallet.pass")
                }
                Button {
                    UIPasteboard.general.string = account.id
                } label: {
                    Label("Copy account ID", systemImage: "doc.on.doc")
                }
            } header: {
                RinthySectionLabel(text: "Account")
            }
            .tint(Color.rinthyGreen)
            .themedListRowBackground(isPlatformNative: isPlatformNative)

            Section {
                Picker("Interface style", selection: $storedThemeStyle) {
                    ForEach(RinthyThemeStyle.allCases) { style in
                        Text(style.label).tag(style.rawValue)
                    }
                }
                .pickerStyle(.segmented)

                if isPlatformNative {
                    Picker(NSLocalizedString("Appearance", comment: "Settings"), selection: $storedAppearanceMode) {
                        ForEach(RinthyAppearanceMode.allCases) { appearance in
                            Text(appearance.label).tag(appearance.rawValue)
                        }
                    }
                    .pickerStyle(.segmented)
                }

                Picker(NSLocalizedString("Language", comment: "Settings"), selection: $storedAppLanguage) {
                    ForEach(RinthyAppLanguage.allCases) { language in
                        Text(language.label).tag(language.rawValue)
                    }
                }
                .pickerStyle(.segmented)
                .onChange(of: storedAppLanguage) { _ in
                    RinthyAppLanguage.apply(storedAppLanguage)
                }

                Toggle(isOn: $showFavoriteProjects) {
                    Label(NSLocalizedString("Favorite projects", comment: "Settings"), systemImage: "star")
                }
                .tint(Color.rinthyGreen)
                Toggle(isOn: $reduceMotion) {
                    Label(
                        NSLocalizedString("Reduce motion", comment: "Settings"),
                        systemImage: "bolt.slash"
                    )
                }
                .tint(Color.rinthyGreen)
                Button {
                    storedThemeStyle = RinthyThemeStyle.platform.rawValue
                    storedAppearanceMode = RinthyAppearanceMode.system.rawValue
                    showFavoriteProjects = true
                    reduceMotion = false
                    UserDefaults.standard.removeObject(forKey: "projectSortMode")
                } label: {
                    Label("Reset appearance", systemImage: "arrow.counterclockwise")
                }
            } header: {
                RinthySectionLabel(text: "Appearance")
            } footer: {
                Text("Platform follows the native controls and appearance of this device. Rinthy preserves the dark glass interface.")
            }
            .tint(Color.rinthyGreen)
            .themedListRowBackground(isPlatformNative: isPlatformNative)

            Section {
                Button {
                    URLCache.shared.removeAllCachedResponses()
                } label: {
                    Label("Clear image cache", systemImage: "photo.badge.arrow.down")
                }
            } header: {
                RinthySectionLabel(text: "Local data")
            }
            .tint(Color.rinthyGreen)
            .themedListRowBackground(isPlatformNative: isPlatformNative)

            Section {
                LabeledContent(
                    NSLocalizedString("Ryntra", comment: "App name"),
                    value: appVersion
                )
                Text(
                    String(
                        format: NSLocalizedString("Unofficial Modrinth client · v%@", comment: "About"),
                        appVersion
                    )
                )
                .font(.caption)
                .foregroundStyle(.secondary)
                Link(destination: URL(string: "https://modrinth.com/user/sawiq_")!) {
                    Label(
                        NSLocalizedString("Author", comment: "About") + " · sawiq",
                        systemImage: "person.crop.circle"
                    )
                }
                Link(destination: URL(string: "https://github.com/imsawiq/Rinthy/releases")!) {
                    Label("Releases", systemImage: "arrow.up.right.square")
                }
                Button(role: .destructive, action: model.signOut) {
                    Label("Sign out", systemImage: "rectangle.portrait.and.arrow.right")
                }
            } header: {
                RinthySectionLabel(text: NSLocalizedString("About", comment: "Settings"))
            }
            .themedListRowBackground(isPlatformNative: isPlatformNative)
        }
        .listStyle(.insetGrouped)
        .scrollContentBackground(isPlatformNative ? .visible : .hidden)
        .background(isPlatformNative ? Color(uiColor: .systemGroupedBackground) : Color.rinthyBackground)
        .onAppear(perform: resetDraft)
        .onChange(of: account.username) { _ in synchronizeDraft() }
        .onChange(of: account.bio) { _ in synchronizeDraft() }
    }

    private var appVersion: String {
        Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String ?? "3.0.0"
    }

    private func resetDraft() {
        username = account.username
        bio = account.bio ?? ""
        syncedUsername = username
        syncedBio = bio
    }

    private func synchronizeDraft() {
        if username == syncedUsername { username = account.username }
        if bio == syncedBio { bio = account.bio ?? "" }
        syncedUsername = account.username
        syncedBio = account.bio ?? ""
    }

    private func saveProfile() async {
        guard !normalizedUsername.isEmpty else {
            errorMessage = "Username cannot be empty."
            return
        }

        isSaving = true
        errorMessage = nil
        do {
            try await model.updateProfile(
                userId: account.id,
                username: normalizedUsername,
                bio: normalizedBio
            )
        } catch {
            errorMessage = error.localizedDescription
        }
        isSaving = false
    }

    @MainActor
    private func updateAvatar(_ upload: ProjectFileUpload) async throws {
        isAvatarSaving = true
        avatarError = nil
        defer { isAvatarSaving = false }
        try await model.changeAvatar(userID: account.id, file: upload)
    }

    @MainActor
    private func deleteAvatar() async throws {
        isAvatarSaving = true
        avatarError = nil
        defer { isAvatarSaving = false }
        try await model.deleteAvatar(userID: account.id)
    }
}

private extension View {
    @ViewBuilder
    func themedListRowBackground(isPlatformNative: Bool) -> some View {
        if isPlatformNative {
            self
        } else {
            listRowBackground(Color.rinthySurface)
        }
    }
}
