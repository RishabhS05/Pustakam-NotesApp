import SwiftUI

struct SearchBar: View {

    @Binding var text: String

    let placeholder: String
    let showMicrophone: Bool

    var onSubmit: (() -> Void)? = nil
    var onMicrophoneClick: (() -> Void)? = nil

    var body: some View {

        HStack(spacing: Theme.Spacing.md) {

            Image(systemName: "magnifyingglass")
                .font(.system(size: 16, weight: .medium))
                .foregroundStyle(Theme.Colors.text3)

            TextField(
                placeholder,
                text: $text,
                prompt: Text(placeholder)
                    .foregroundStyle(Theme.Colors.text3)
            )
            .font(Theme.Fonts.bodyText)
            .foregroundStyle(Theme.Colors.text3)
            .textInputAutocapitalization(.never)
            .autocorrectionDisabled()
            .submitLabel(.search)
            .onSubmit {
                onSubmit?()
            }

            if showMicrophone {
                Button {
                    onMicrophoneClick?()
                } label: {
                    Image(systemName: "mic.fill")
                        .font(.system(size: 16))
                        .foregroundStyle(Theme.Colors.accent)
                        .frame(width: 24, height: 24)
                }
                .buttonStyle(.plain)
            }
        }
        .padding(.horizontal, Theme.Spacing.lg)
        .frame(height: 52)
        .background(Theme.Colors.surface)
        .clipShape(
            RoundedRectangle(
                cornerRadius: Theme.Radius.lg
            )
        )
        .shadow(
            color: Theme.Elevation.card,
            radius: Theme.Elevation.cardRadius,
            x: 0,
            y: Theme.Elevation.cardY
        )
    }
}

#Preview {
    PreviewContainer()
}

private struct PreviewContainer: View {

    @State private var search = ""
    @State private var searchFilled = "Nalanda University"

    var body: some View {

        VStack(spacing: 20) {

            SearchBar(
                text: $search,
                placeholder: "Search notes, tags, audio…",
                showMicrophone: true,
                onSubmit: {
                    print("Search:", search)
                },
                onMicrophoneClick: {
                    print("Mic tapped")
                }
            )

            SearchBar(
                text: $searchFilled,
                placeholder: "Search",
                showMicrophone: true
            )

            SearchBar(
                text: $search,
                placeholder: "Search",
                showMicrophone: false
            )
        }
        .padding()
        .background(Theme.Colors.background)
    }
}

