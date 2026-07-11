import RinthyShared
import SwiftUI

struct AccountView: View {
    @EnvironmentObject private var model: AppModel

    let account: Account
    let projectCount: Int
    let organizationCount: Int

    var body: some View {
        List {
            Section {
                HStack(spacing: 14) {
                    AsyncImage(url: URL(string: account.avatarUrl ?? "")) { image in
                        image.resizable().scaledToFill()
                    } placeholder: {
                        Circle().fill(.quaternary)
                    }
                    .frame(width: 68, height: 68)
                    .clipShape(Circle())

                    VStack(alignment: .leading, spacing: 4) {
                        Text(account.username).font(.title2.bold())
                        Text(account.role.capitalized)
                            .font(.subheadline.weight(.semibold))
                            .foregroundStyle(.secondary)
                    }
                }
                if let bio = account.bio, !bio.isEmpty {
                    Text(bio).foregroundStyle(.secondary)
                }
            }

            Section("Workspace") {
                LabeledContent("Projects", value: "\(projectCount)")
                LabeledContent("Organizations", value: "\(organizationCount)")
            }

            Section {
                Button(role: .destructive, action: model.signOut) {
                    Label("Sign out", systemImage: "rectangle.portrait.and.arrow.right")
                }
            } footer: {
                Text("Rinthy 3.0 · Unofficial Modrinth client")
            }
        }
        .listStyle(.insetGrouped)
    }
}
