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
