import Foundation
import RyntraShared
import SwiftUI
import UIKit

struct AccountView: View {
    @EnvironmentObject private var model: AppModel
    @AppStorage("showFavoriteProjects") private var showFavoriteProjects = true
    @AppStorage("reduceMotion") private var reduceMotion = false
    @AppStorage("themeStyle") private var storedThemeStyle = RyntraThemeStyle.platform.rawValue
    @AppStorage("appearanceMode") private var storedAppearanceMode = RyntraAppearanceMode.system.rawValue
    @AppStorage("appLanguage") private var storedAppLanguage = RyntraAppLanguage.system.rawValue
    @AppStorage(LocalNotificationManager.enabledKey) private var localNotificationsEnabled = false

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
    @State private var isChangingNotifications = false
    @State private var copiedSupportAddress: String?

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
        storedThemeStyle == RyntraThemeStyle.platform.rawValue
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
                            .foregroundStyle(Color.ryntraGreen)
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
                Link(destination: SupportDetails.donationAlertsURL) {
                    Label(NSLocalizedString("DonationAlerts", comment: "Support option"), systemImage: "heart")
                }
                Button {
                    copySupportAddress(SupportDetails.usdtTRC20, label: "USDT · TRC20")
                } label: {
                    supportAddressLabel("USDT · TRC20", address: SupportDetails.usdtTRC20)
                }
                Button {
                    copySupportAddress(SupportDetails.tonUSDT, label: "USDT · TON")
                } label: {
                    supportAddressLabel("USDT · TON", address: SupportDetails.tonUSDT)
                }
            } header: {
                RyntraSectionLabel(text: NSLocalizedString("Support the author", comment: "Settings section"))
            } footer: {
                if let copiedSupportAddress {
                    Text(String(format: NSLocalizedString("%@ address copied.", comment: "Support copy result"), copiedSupportAddress))
                } else {
                    Text(NSLocalizedString("Thank you for supporting Ryntra development.", comment: "Support hint"))
                }
            }
            .tint(Color.ryntraGreen)
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
                .tint(Color.ryntraGreen)
            } header: {
                RyntraSectionLabel(text: "Profile")
            }
            .themedListRowBackground(isPlatformNative: isPlatformNative)

            Section {
                LabeledContent("Projects", value: "\(projectCount)")
                LabeledContent("Organizations", value: "\(organizationCount)")
            } header: {
                RyntraSectionLabel(text: "Workspace")
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
                RyntraSectionLabel(text: "Account")
            }
            .tint(Color.ryntraGreen)
            .themedListRowBackground(isPlatformNative: isPlatformNative)

            Section {
                Picker("Interface style", selection: $storedThemeStyle) {
                    ForEach(RyntraThemeStyle.allCases) { style in
                        Text(style.label).tag(style.rawValue)
                    }
                }
                .pickerStyle(.segmented)

                if isPlatformNative {
                    Picker(NSLocalizedString("Appearance", comment: "Settings"), selection: $storedAppearanceMode) {
                        ForEach(RyntraAppearanceMode.allCases) { appearance in
                            Text(appearance.label).tag(appearance.rawValue)
                        }
                    }
                    .pickerStyle(.segmented)
                }

                Picker(NSLocalizedString("Language", comment: "Settings"), selection: $storedAppLanguage) {
                    ForEach(RyntraAppLanguage.allCases) { language in
                        Text(language.label).tag(language.rawValue)
                    }
                }
                .pickerStyle(.menu)
                .onChange(of: storedAppLanguage) { _ in
                    RyntraAppLanguage.apply(storedAppLanguage)
                }

                Toggle(isOn: $showFavoriteProjects) {
                    Label(NSLocalizedString("Pin favorites first", comment: "Settings"), systemImage: "star")
                }
                .tint(Color.ryntraGreen)
                Toggle(isOn: $reduceMotion) {
                    Label(
                        NSLocalizedString("Reduce motion", comment: "Settings"),
                        systemImage: "bolt.slash"
                    )
                }
                .tint(Color.ryntraGreen)
                Button {
                    storedThemeStyle = RyntraThemeStyle.platform.rawValue
                    storedAppearanceMode = RyntraAppearanceMode.system.rawValue
                    showFavoriteProjects = true
                    reduceMotion = false
                    UserDefaults.standard.removeObject(forKey: "projectSortMode")
                } label: {
                    Label("Reset appearance", systemImage: "arrow.counterclockwise")
                }
            } header: {
                RyntraSectionLabel(text: "Appearance")
            } footer: {
                Text("Platform follows the native controls and appearance of this device. Ryntra preserves the dark glass interface.")
            }
            .tint(Color.ryntraGreen)
            .themedListRowBackground(isPlatformNative: isPlatformNative)

            Section {
                Toggle(isOn: Binding(
                    get: { localNotificationsEnabled },
                    set: { requestedValue in
                        isChangingNotifications = true
                        Task {
                            localNotificationsEnabled = await LocalNotificationManager.shared.setEnabled(requestedValue)
                            isChangingNotifications = false
                        }
                    }
                )) {
                    Label(NSLocalizedString("Local background checks", comment: "Notification setting"), systemImage: "bell")
                }
                .disabled(isChangingNotifications)
                .tint(Color.ryntraGreen)
            } header: {
                RyntraSectionLabel(text: NSLocalizedString("Notifications", comment: "Settings"))
            } footer: {
                Text(NSLocalizedString(
                    "Private on-device checks scheduled by iOS. Delivery time is not guaranteed.",
                    comment: "Notification setting hint"
                ))
            }
            .themedListRowBackground(isPlatformNative: isPlatformNative)

            Section {
                Button {
                    URLCache.shared.removeAllCachedResponses()
                } label: {
                    Label("Clear image cache", systemImage: "photo.badge.arrow.down")
                }
            } header: {
                RyntraSectionLabel(text: "Local data")
            }
            .tint(Color.ryntraGreen)
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
                Link(destination: URL(string: "https://github.com/imsawiq/Ryntra/releases")!) {
                    Label("Releases", systemImage: "arrow.up.right.square")
                }
                Button(role: .destructive, action: model.signOut) {
                    Label("Sign out", systemImage: "rectangle.portrait.and.arrow.right")
                }
            } header: {
                RyntraSectionLabel(text: NSLocalizedString("About", comment: "Settings"))
            }
            .themedListRowBackground(isPlatformNative: isPlatformNative)
        }
        .listStyle(.insetGrouped)
        .scrollContentBackground(isPlatformNative ? .visible : .hidden)
        .background(isPlatformNative ? Color(uiColor: .systemGroupedBackground) : Color.ryntraBackground)
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

    private func copySupportAddress(_ value: String, label: String) {
        UIPasteboard.general.string = value
        copiedSupportAddress = label
    }

    private func supportAddressLabel(_ title: String, address: String) -> some View {
        VStack(alignment: .leading, spacing: 3) {
            Text(title).foregroundStyle(.primary)
            Text(address)
                .font(.caption.monospaced())
                .foregroundStyle(.secondary)
                .fixedSize(horizontal: false, vertical: true)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}

private extension View {
    @ViewBuilder
    func themedListRowBackground(isPlatformNative: Bool) -> some View {
        if isPlatformNative {
            self
        } else {
            listRowBackground(Color.ryntraSurface)
        }
    }
}
