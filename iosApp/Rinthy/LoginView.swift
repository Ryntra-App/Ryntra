import SwiftUI

struct LoginView: View {
    @EnvironmentObject private var model: AppModel
    @State private var token = ""

    var isLoading = false
    var errorMessage: String?

    var body: some View {
        VStack(spacing: 0) {
            Spacer()
            Image("RinthyLogo")
                .resizable()
                .scaledToFit()
                .frame(width: 88, height: 88)
                .clipShape(RoundedRectangle(cornerRadius: 20))

            Text("Rinthy")
                .font(.system(size: 36, weight: .black))
                .padding(.top, 18)
            Text("Your Modrinth workspace, native on mobile")
                .font(.subheadline)
                .foregroundStyle(.secondary)
                .padding(.top, 6)
                .padding(.bottom, 28)

            SecureField("Personal access token", text: $token)
                .textContentType(.password)
                .padding(14)
                .background(.quaternary, in: RoundedRectangle(cornerRadius: 8))

            if let errorMessage {
                Text(errorMessage)
                    .font(.footnote)
                    .foregroundStyle(.red)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(.top, 10)
            }

            Button {
                model.signIn(token: token)
            } label: {
                Group {
                    if isLoading {
                        ProgressView().tint(.black)
                    } else {
                        Label("Connect to Modrinth", systemImage: "link")
                            .fontWeight(.bold)
                    }
                }
                .frame(maxWidth: .infinity, minHeight: 48)
            }
            .buttonStyle(.borderedProminent)
            .buttonBorderShape(.roundedRectangle(radius: 8))
            .disabled(token.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || isLoading)
            .padding(.top, 18)

            Label("Stored securely in Keychain", systemImage: "lock.shield")
                .font(.caption)
                .foregroundStyle(.secondary)
                .padding(.top, 14)
            Spacer()
        }
        .padding(.horizontal, 24)
    }
}
