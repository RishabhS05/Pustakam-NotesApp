//
//  BaseNetworkHandler.swift
//  iosApp
//
//  Created by Rishabh Shrivastava on 09/10/24.
//  Copyright © 2024 orgName. All rights reserved.
//

import Foundation
import shared

struct BaseResult<T> {
    let isSuccessful: Bool
    let data: T?
    let error: Error?
}

protocol IBaseHandler {
    var base: BaseRepository { get set }
}
extension IBaseHandler {
    func apiHandler<T: KotlinBase>(apiCall: () async throws -> Result) async
        -> BaseResult<T?>
    {
        var baseResult: BaseResult<T?>
        let response = try? await apiCall()
        if response is ResultSuccess<T> {
            let res = response as? ResultSuccess<T>
            baseResult = BaseResult<T?>(
                isSuccessful: true, data: res?.data, error: nil)
        } else {
            let err = response as? ResultError<NetworkError>
            baseResult = BaseResult<T?>(
                isSuccessful: false, data: nil, error: err?.error)
        }
        return baseResult
    }
}
