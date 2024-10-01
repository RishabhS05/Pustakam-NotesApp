//
//  AvatarImageView.swift
//  iosApp
//
//  Created by Rishabh Shrivastava on 02/10/24.
//  Copyright © 2024 orgName. All rights reserved.
//

import SwiftUI

struct AvatarImageView: View {
    var imageName : String = "avatar"
    var body: some View {
        Image(imageName).resizable().scaledToFit().frame(width: 200,height: 200).clipShape(Circle())
            .overlay {
                    Circle().stroke(.brown, lineWidth: 4)
                       }
            .shadow(radius:7)
    }
}
#Preview {
    AvatarImageView()
}
