//
//  String.ext.swift
//  iosApp
//
//  Created by Rishabh Shrivastava on 11/04/25.
//  Copyright © 2025 orgName. All rights reserved.
//
import shared

extension String {
    func toLocalFormat(showTime: Bool = true) -> String {
        let timeZone = Kotlinx_datetimeTimeZone.companion.currentSystemDefault()
        return String_extKt.toLocalFormat(self, timeZone: timeZone, showTime: showTime)
    }
}
