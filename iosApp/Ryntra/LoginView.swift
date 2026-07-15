import SwiftUI

struct LoginView: View {
    @EnvironmentObject private var model: AppModel
    @Environment(\.openURL) private var openURL
    @State private var token = ""
    @State private var isPatVisible = false

    var isLoading = false
    var errorMessage: String?

    var body: some View {
        VStack(spacing: 0) {
            Spacer()
            Image("RyntraLogo")
                .resizable()
                .scaledToFit()
                .frame(width: 88, height: 88)
                .clipShape(RoundedRectangle(cornerRadius: 20))

            Text("Ryntra")
                .font(.system(size: 36, weight: .black))
                .padding(.top, 18)
            Text("Your Modrinth workspace, native on mobile")
                .font(.subheadline)
                .foregroundStyle(.secondary)
                .padding(.top, 6)
                .padding(.bottom, 28)

            Button {
                if let url = model.startOAuth() {
                    openURL(url)
                }
            } label: {
                Group {
                    if isLoading {
                        ProgressView().tint(.black)
                    } else {
                        Label("Continue with Modrinth", systemImage: "globe")
                            .fontWeight(.bold)
                    }
                }
                .frame(maxWidth: .infinity, minHeight: 48)
            }
            .buttonStyle(.borderedProminent)
            .buttonBorderShape(.roundedRectangle(radius: 8))
            .disabled(isLoading)

            if let message = model.oauthError ?? errorMessage {
                Text(message)
                    .font(.footnote)
                    .foregroundStyle(.red)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(.top, 10)
            }

            Button {
                isPatVisible.toggle()
            } label: {
                Label(
                    isPatVisible ? "Hide access token" : "Use personal access token",
                    systemImage: "key"
                )
            }
            .disabled(isLoading)
            .padding(.top, 10)

            if isPatVisible {
                SecureField("Personal access token", text: $token)
                    .textContentType(.password)
                    .padding(14)
                    .background(.quaternary, in: RoundedRectangle(cornerRadius: 8))

                Button {
                    model.signIn(token: token)
                } label: {
                    Label("Connect to Modrinth", systemImage: "link")
                        .fontWeight(.bold)
                        .frame(maxWidth: .infinity, minHeight: 48)
                }
                .buttonStyle(.bordered)
                .buttonBorderShape(.roundedRectangle(radius: 8))
                .disabled(token.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || isLoading)
                .padding(.top, 14)

                Label("Stored securely in Keychain", systemImage: "lock.shield")
                    .font(.caption)
                    .foregroundStyle(.secondary)
                    .padding(.top, 14)
            }
            Spacer()
        }
        .padding(.horizontal, 24)
    }
}
