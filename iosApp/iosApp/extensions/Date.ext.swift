//
//  Date.ext.swift
//  iosApp
//
//  Created by Rishabh Shrivastava on 24/11/24.
//  Copyright © 2024 orgName. All rights reserved.
//
import Foundation

extension Date {
    func toString(dateFormat: String) -> String {
        let formatter = DateFormatter()
        formatter.dateFormat = dateFormat
        return formatter.string(from: self)
    }
}
