//
//  NoteEditorViewModel.swift
//  iosApp
//
//  Created by Rishabh Shrivastava on 13/04/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import shared
    // note viewmodels are basically ui logic handlers only
class NoteEditorViewModel: BaseViewModel, ObservableObject {
    var note: Note? = nil
    
    @Published var noteContents = [NoteContentModel]()
    
    override init() {
        super.init()
    }
    
    func setNote(note : Note?){
        if note == nil {
                // if note doesnt exist create a empty notebook
            Task{
                let value =
                await apiHandler(apiCall: {
                    try await noteRepositary.getANote(id: nil)
                })
                print("value \(String(describing: value.data as? Note))")
                self.note = value.data as? Note
            }
        }else {

                // if note already exist
            self.note = note
            noteContentRepository.addAllNoteContent(note: note!)
            self.noteContents = note!.contents!
        }
    }
    
    // Add Content into list
    func addContent(content: NoteContentModel){
        noteContents += [content]
        if content.isPlayingMedia() {
            noteContentRepository.updateNoteContent(note: content as! NoteContentModel.MediaContent)
        }
    }
    /***
     Create new or update note async method
     */
    private func  createUpdateNoteCall() async -> BaseResult<BaseResponse<Note>?> {
        
        return await apiHandler(apiCall: {
            print("NoteEditorViewModel note \(String(describing: self.note))")
            return try await noteRepositary.insertOrUpdateNote(note : self.note!)
        })
    }
    
    /***
     Create new or update note exposed method
     */
    func createorUpdateNoteCall() {
        Task {
         await self.createUpdateNoteCall()
        }
    }
        // delete a note
    func deleteNoteCall(noteId: String) async -> BaseResult<BaseResponse<DeleteDataModel>?> {
        return await apiHandler(apiCall: { try await noteRepositary.deleteNote(id: noteId) })
    }
    
    func addNewText(){
        let text = NoteContentObjectHelper()
            .createText(noteId: note!.id,
                        positionedAt: Int64(noteContents.count),
                        text: "")
        addContent(content: text)
    }
 
    /***
     update note content by index
     
     1- if index is -1 then add a new Content in the list
     2- updation is performed first on the Ui list.
     3- it will call api from repository.
     */
    
    func updateContent(index : Int = -1 , content : NoteContentModel){
        if let index = noteContents.firstIndex(where: { $0.id == content.id }) {noteContents[index] = content
         }else  {
             noteContents.append(content)
        }
        if let noteIndex = note?.contents?.firstIndex(where: {
            $0.id == content.id
        }){
            note?.contents?[noteIndex] = content
        }else {
            note?.contents! += [content]
        }
    }
    
    
    
    func getCapturedData(media : CapturedMedia?){
        let timeStamp =  DateTimeUtilsKt.getCurrentTimestamp()
            switch media {
                case .image(let image):
                    let type = ContentType.image
                    let folderName = "\(type.name)/\(note!.id)"
                    let fileName = "\(timeStamp)\(type.getExt())"
                    if let data = image.pngData() {
                        let localpath = saveImageFile(data: data, in : folderName, to:  fileName)
                    let image = NoteContentObjectHelper()
                            .createMedia(
                                contentType: type,
                                noteId: note!.id ,
                                positionedAt: 0 ,
                                localPath: localpath,
                                url: "",
                                duration: 0,
                                timestamp: "\(timeStamp)"
                            )
                        addContent(content: image)
                        //update note
                        note?.contents! += [image]
                        print("Image saved to: \(localpath)")
                        
                    }
                   
                case .video(let url):
                    let type = ContentType.video
                    let folderName = "\(type.name)/\(note!.id)"
                    let fileName = "\(timeStamp)\(type.getExt())"
                    guard let savedURL =  copyFile(to: folderName, fileName: fileName, from: url)else { return }
                    let video = NoteContentObjectHelper().createMedia(
                                contentType: type,
                                noteId: note!.id ,
                                positionedAt: 0 ,
                                localPath: savedURL.absoluteString,
                                url: "",
                                duration: 0,
                                timestamp: "\(timeStamp)"
                            )
                    addContent(content: video)
                    //update note
                    note?.contents! += [video]
                    print("Video saved to: \(savedURL.absoluteString)")
               
                case .audio(let audio):
                    let type = ContentType.video
                    let folderName = "\(type.name)/\(note!.id)"
                    let fileName = "\(timeStamp)\(type.getExt())"
                    break
                    
                case .none: break
            
            }
        }
    
        // remove note content from list, db , server and stroage
    func removeContent(){}
    
        //
    func addContentData() {}
    
    
    func shareNote(){}
    
}
