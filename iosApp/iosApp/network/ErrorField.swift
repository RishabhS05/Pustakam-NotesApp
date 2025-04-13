//
//  ErrorField.swift
//  iosApp
//
//  Created by Rishabh Shrivastava on 09/10/24.
//  Copyright © 2024 orgName. All rights reserved.
//

import Foundation

struct ErrorField {
     var showErrorAlert : Bool = false
     var alertType : AlertUCPermission = AlertUCPermission.WARNING
     var errorMessage  : String  = ""
     var errorMessageTitle  : String  = ""
}
