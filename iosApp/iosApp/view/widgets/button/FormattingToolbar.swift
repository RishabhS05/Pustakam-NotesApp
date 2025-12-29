import SwiftUI
enum TextStyleType {
    case bold
    case italic
    case underline
    case strikethrough
    case numberedList
    case lineSpacing
    case alignCenter
    case justify
    case bulletList
    case spacingRight
    case spacingLeft
    case colorPalette
}
struct ButtonConfig: Identifiable {
    let id = UUID()
    let icon: String
    let type: TextStyleType

    init(_ icon: String, _ type: TextStyleType) {
        self.icon = icon
        self.type = type
    }
}

struct FormattingToolbar: View {
    let action: (TextStyleType) -> Void

     private let padding: CGFloat = 6
    
    var body: some View {
        VStack(spacing: 6) {

                   row(
                       ButtonConfig("bold", .bold),
                       ButtonConfig("italic", .italic),
                       ButtonConfig("underline", .underline),
                       ButtonConfig("strikethrough", .strikethrough)
                   )

                   row(
                       ButtonConfig("list.number", .numberedList),
                       ButtonConfig("line.horizontal.3.decrease", .lineSpacing),
                       ButtonConfig("text.aligncenter", .alignCenter),
                       ButtonConfig("text.justify", .justify)
                   )

                   row(
                       ButtonConfig("list.bullet", .bulletList),
                       ButtonConfig("increase.indent", .spacingRight),
                       ButtonConfig("decrease.indent", .spacingLeft),
                       ButtonConfig("paintpalette", .colorPalette)
                   )
               }
        .padding(8)
        .background(.ultraThinMaterial)
        .cornerRadius(8)
        .shadow(radius: 4)
    }
    
    private func row(_ buttons: ButtonConfig...) -> some View {
           HStack {
               ForEach(buttons) { config in
                   Button {
                       action(config.type)
                   } label: {
                       Image(systemName: config.icon)
                           .font(.system(size: 18))
                           .frame(width: 36, height: 36)
                   }
               }
           }
       }
}
