import RinthyShared
import SwiftUI

struct OrganizationsView: View {
    @EnvironmentObject private var model: AppModel

    let organizations: [Organization]
    @State private var selectedOrganization: Organization?

    var body: some View {
        Group {
            if let selectedOrganization {
                OrganizationDetailView(organization: selectedOrganization)
            } else {
                organizationList
            }
        }
        .toolbar {
            if selectedOrganization != nil {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Teams") {
                        selectedOrganization = nil
                    }
                }
            }
        }
    }

    private var organizationList: some View {
        List {
            if organizations.isEmpty {
                EmptyStateView(
                    title: "No organizations",
                    systemImage: "person.3",
                    message: "Your personal projects are still available in Projects."
                )
            } else {
                ForEach(organizations, id: \.id) { organization in
                    Button {
                        selectedOrganization = organization
                    } label: {
                        OrganizationRow(organization: organization)
                    }
                    .buttonStyle(.plain)
                    .padding(.vertical, 4)
                }
            }
            Color.clear
                .frame(height: 90)
                .listRowSeparator(.hidden)
        }
        .listStyle(.plain)
        .refreshable { model.refresh() }
    }
}

private struct OrganizationRow: View {
    let organization: Organization

    var body: some View {
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
    }
}

private struct OrganizationDetailView: View {
    @EnvironmentObject private var model: AppModel

    let organization: Organization
    @State private var projects: [Project] = []
    @State private var isLoading = false
    @State private var errorMessage: String?

    var body: some View {
        List {
            organizationHeader

            if isLoading {
                HStack(spacing: 10) {
                    ProgressView()
                    Text("Loading projects")
                        .foregroundStyle(.secondary)
                }
                .padding(.vertical, 20)
            } else if let errorMessage, projects.isEmpty {
                EmptyStateView(title: "Projects unavailable", systemImage: "exclamationmark.triangle", message: errorMessage)
            } else if projects.isEmpty {
                EmptyStateView(
                    title: "No organization projects",
                    systemImage: "shippingbox",
                    message: "Projects transferred into this organization will appear here."
                )
            } else {
                Section("\(projects.count) projects") {
                    ForEach(projects, id: \.id) { project in
                        ProjectRow(project: project, showDescription: true, showStatus: true)
                    }
                }
            }
        }
        .listStyle(.plain)
        .task(id: organization.id) {
            await loadProjects()
        }
    }

    private var organizationHeader: some View {
        HStack(alignment: .center, spacing: 14) {
            AsyncImage(url: URL(string: organization.iconUrl ?? "")) { image in
                image.resizable().scaledToFill()
            } placeholder: {
                RoundedRectangle(cornerRadius: 14).fill(.quaternary)
            }
            .frame(width: 76, height: 76)
            .clipShape(RoundedRectangle(cornerRadius: 14))

            VStack(alignment: .leading, spacing: 4) {
                Text(organization.name)
                    .font(.title2.bold())
                Text("@\(organization.slug)")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                if !organization.description_.isEmpty {
                    Text(organization.description_)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                        .lineLimit(2)
                }
            }
        }
        .padding(.vertical, 8)
    }

    private func loadProjects() async {
        isLoading = true
        errorMessage = nil
        do {
            projects = try await model.loadOrganizationProjects(organization: organization)
        } catch {
            errorMessage = error.localizedDescription
        }
        isLoading = false
    }
}
