//
//  Any.ext.swift
//  iosApp
//
//  Created by Rishabh Shrivastava on 19/11/24.
//  Copyright © 2024 orgName. All rights reserved.
//

extension Any? {
    func isNil() -> Bool {
        return self == nil
    }
}

extension AnyObject? {
func isNil () -> Bool {
        return self == nil
    }
}
extension String? {
    func isNotNilOrEmpty() -> Bool {
        return self != nil && !self!.isEmpty
    }
}
