
import SwiftUI
import shared
struct TagView  : View {
    let tag : Tag
    var body : some View {
        let color = tag.getColor()
        let cornerRadius : CGFloat = 5
        HStack{
            Image(systemName: "bookmark.fill")
                .foregroundColor(color)
                .font(.system(size: 16))
                .frame(height: 28,alignment: .leading)
            Text(tag.label ?? "Unknown",)
                .font(.system(size: 14, weight: .medium))
                .foregroundColor(color)
        }
        .padding(.horizontal, 8.0)
        .background(color.opacity(0.1))
        .cornerRadius(cornerRadius)
        .overlay{
            RoundedRectangle(cornerRadius: cornerRadius)
                .stroke(color.opacity(0.7), lineWidth: 1)
                
        }
        .shadow(radius: 10)
    }
}
extension Tag {
    func getColor() -> Color {
        return Color(hex: self.color ?? "#FFFFFF")
    }
}
#Preview {
    TagView(tag: Tag(id: "1", label: "Maths", color: "#FFFFFF"))
}
