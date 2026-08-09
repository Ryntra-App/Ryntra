import SwiftUI

struct LoginView: View {
    @EnvironmentObject private var model: AppModel
    @Environment(\.openURL) private var openURL
    @State private var token = ""
    @State private var isPatVisible = false
    @FocusState private var isTokenFocused: Bool

    var isLoading = false
    var errorMessage: String?

    var body: some View {
        GeometryReader { geometry in
            ScrollView {
                VStack(spacing: 0) {
                    Spacer(minLength: 24)
            Image("RyntraLogo")
                .resizable()
                .scaledToFit()
                .frame(width: 88, height: 88)
                .clipShape(RoundedRectangle(cornerRadius: 20))

            Text("Ryntra")
                .font(.largeTitle.bold())
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
                        ProgressView().tint(Color.ryntraOnAccent)
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
                    .ryntraNoAutocapitalization()
                    .focused($isTokenFocused)
                    .submitLabel(.go)
                    .onSubmit { connectWithToken() }
                    .padding(14)
                    .background(.quaternary, in: RoundedRectangle(cornerRadius: 8))

                Button {
                    connectWithToken()
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
                    Spacer(minLength: 24)
                }
                .frame(maxWidth: 560)
                .frame(maxWidth: .infinity, minHeight: geometry.size.height)
                .padding(.horizontal, 24)
            }
            .ryntraInteractiveKeyboardDismissal()
        }
    }

    private func connectWithToken() {
        let value = token.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !value.isEmpty, !isLoading else { return }
        isTokenFocused = false
        model.signIn(token: value)
    }
}
