//
//  BaseHandler.swift
//  iosApp
//
//  Created by Rishabh Shrivastava on 18/11/24.
//  Copyright © 2024 orgName. All rights reserved.
//
import shared

class BaseHandler  : IBaseHandler {
    var base: BaseRepository = KoinHelper().getBaseRepository()
}
