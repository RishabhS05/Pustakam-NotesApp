//
//  NotesViewModel.swift
//  iosApp
//
//  Created by Rishabh Shrivastava on 13/04/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import shared

class NotesViewModel : BaseViewModel, ObservableObject {
    
    @Published var page: Int = 1
    @Published var notes = [Note]()
    @Published var isLoading : Bool = false
    
    
    override init(){
        super.init()
            NoteRepositoryHelper().noteListStateHelper{ [weak self] state in
                DispatchQueue.main.async {
                    self?.notes = state.notes as! [Note]
                }
            }
        getNotesCall()
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
                if response.isSuccessful {
                    let data =  response.data as? Notes
                    data?.notes
                        .forEach{note in
                            print(note)
                            self.notes.append(note as! Note)
                        }
                }
                else if response.error != nil { print("Error \(response.error!)") }
            }
        }
    }
}
