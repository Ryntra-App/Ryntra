import RinthyShared
import SwiftUI

struct OrganizationsView: View {
    @EnvironmentObject private var model: AppModel
    let organizations: [Organization]

    var body: some View {
        List {
            if organizations.isEmpty {
                ContentUnavailableView(
                    "No organizations",
                    systemImage: "person.3",
                    description: Text("Your personal projects are still available in Projects.")
                )
            } else {
                ForEach(organizations, id: \.id) { organization in
                    HStack(spacing: 12) {
                        AsyncImage(url: URL(string: organization.iconUrl ?? "")) { image in
                            image.resizable().scaledToFill()
                        } placeholder: {
                            RoundedRectangle(cornerRadius: 8).fill(.quaternary)
                        }
                        .frame(width: 52, height: 52)
                        .clipShape(RoundedRectangle(cornerRadius: 8))

                        VStack(alignment: .leading, spacing: 4) {
                            Text(organization.name).fontWeight(.bold)
                            Text("@\(organization.slug)")
                                .font(.caption.weight(.semibold))
                                .foregroundStyle(Color.rinthyGreen)
                            if !organization.description_.isEmpty {
                                Text(organization.description_)
                                    .font(.caption)
                                    .foregroundStyle(.secondary)
                                    .lineLimit(2)
                            }
                        }
                    }
                    .padding(.vertical, 4)
                }
            }
        }
        .listStyle(.insetGrouped)
        .refreshable { model.refresh() }
    }
}
