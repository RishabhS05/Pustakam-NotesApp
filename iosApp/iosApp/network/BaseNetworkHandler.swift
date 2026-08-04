//
//  BaseNetworkHandler.swift
//  iosApp
//  Created by Rishabh Shrivastava on 09/10/24.
//  Copyright © 2024 orgName. All rights reserved.
//

import Foundation
import shared

// 🔧 P0/4: DEPRECATED — the whole repo-direct + casting path is superseded by the
//   bridges (NotesBridge/NoteContentBridge/AuthBridge) + UiState. Zero conformers
//   remain (BaseViewModel removed). Kept until you approve deletion of this file.
@available(*, deprecated, message: "Use NotesBridgeAdapter/AuthBridgeAdapter + UiState instead")
struct BaseResult<T> {
    let isSuccessful: Bool
    let data: T?
    let error: Error?
}

@available(*, deprecated, message: "Use the bridge adapters — repositories are not exposed to iOS anymore")
protocol IBaseHandler {}
extension IBaseHandler {
    
    func apiHandler<T: KotlinBase>(apiCall: () async throws -> Result) async
        -> BaseResult<T?>
    {
        var baseResult: BaseResult<T?>
        let response = try? await apiCall()
    
    print("swift api response : \(String(describing: response))")
        if response is ResultSuccess<T> {
            let res = response as? ResultSuccess<T>
            let data = res?.data as? BaseResponse<T>
        
            print("swift api response data : \(String(describing: data?.data))")
            baseResult = BaseResult<T?>(
                isSuccessful: true, data: data?.data, error: nil)
        }  else {
            let err = response as? ResultError<NetworkError>
            print("swift api response error : \(String(describing: err))")
            baseResult = BaseResult<T?>(
                isSuccessful: false, data: nil, error: err?.error)
        }
        return baseResult
    }
}
