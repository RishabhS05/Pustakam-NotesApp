//
//  BaseNetworkHandler.swift
//  iosApp
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
    var baseRepositary: BaseRepository { get set }
    var noteRepositary: NoteRepository { get set }
    var noteContentRepository: NoteContentRepository { get set }
}
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
