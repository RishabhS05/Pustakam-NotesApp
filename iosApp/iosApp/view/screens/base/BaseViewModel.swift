//
//  BaseHandler.swift
//  iosApp
//
//  Created by Rishabh Shrivastava on 18/11/24.
//  Copyright © 2024 orgName. All rights reserved.
//
import shared

class BaseViewModel  : IBaseHandler {
   
    var baseRepositary: BaseRepository
    
    var noteRepositary: NoteRepository
 
    var noteContentRepository: NoteContentRepository
    
    init(){
        self.baseRepositary = KoinHelper().getBaseRepository() 
        self.noteRepositary = KoinHelper().getNoteRepository()
        self.noteContentRepository = KoinHelper().getNoteContentRepository()
    }
    
}
