//
//  NotesViewModel.swift
//  iosApp
//
//  Created by Rishabh Shrivastava on 13/04/25.
//  Copyright © 2025 orgName. All rights reserved.
//
import SwiftUI
import Combine // Import Combine if you're not already using it


import shared
import Combine

class NotesViewModel : BaseViewModel, ObservableObject {
    
    @Published var page: Int = 1
    @Published var notes = [Note]()
    @Published var isLoading : Bool = false
    
    
    override init(){
        super.init()
//        getNotesCall()
            NoteRepositoryHelper().noteListStateHelper{ state in
                DispatchQueue.main.async {
                    self.clear()
                    print("NotesViewModel new List \(state.notes)\n count =  \(state.notes.count)")
                    self.notes = state.notes as! [Note]
                }
            }
    }
    func clear(){
        self.notes.removeAll()
    }
    
    func getNotesCall() {
        Task {
            DispatchQueue.main.async { self.isLoading = true }
            let response = await apiHandler( apiCall: {
                    try await noteRepositary.getAllNotes(
                        page: Int32(page)
                    )
                })
            
            DispatchQueue.main.async {
                self.isLoading = false
                 if response.error != nil { print("Error \(response.error!)") }
            }
        }
    }
}
