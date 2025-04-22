
import Foundation


//Creating A file in specific path
func createFilepath(in folderName: String, fileName: String, contents: Data) -> URL? {
    guard let folderURL = createFolder(named: folderName) else { return nil }
    let fileURL = folderURL.appendingPathComponent(fileName)

    FileManager.default.createFile(atPath: fileURL.path, contents: contents, attributes: nil)
    return fileURL
}

//Create a New Folder in Documents Directory

func createFolder(named folderName: String) -> URL? {
    let documentsDirectory = getDocumentsDirectory()
    let folderURL = documentsDirectory.appendingPathComponent(folderName)

    if !FileManager.default.fileExists(atPath: folderURL.path) {
        do {
            try FileManager.default.createDirectory(at: folderURL, withIntermediateDirectories: true, attributes: nil)
            print("Folder created at: \(folderURL.path)")
        } catch {
            print("Error creating folder: \(error)")
            return nil
        }
    }

    return folderURL
}


//Get a Specific Directory Path (e.g., Documents)

func getDocumentsDirectory() -> URL {
    FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first!
}


func getATempFilePath(extension ext : String) -> URL{
    FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString + ext)
}

//save data into location
func saveData(url : URL, data : Data){
    do {
        try data.write(to: url)
    }catch {
        print("❌ Failed to save: \(error)")
    }
}

//move data into location
func moveData(from url  : URL , to destinationUrl  : URL){
    do{
        try FileManager.default.copyItem(at: url, to: destinationUrl)
    }catch {
        print("❌ Failed to move: \(error)")
    }
}
// save media file
func saveImageFile(data content : Data, in folder : String,to filePath : String ,) -> String {
    
    guard let  fileSavingPath = createFilepath(in: folder, fileName: filePath, contents: content) else {return ""}
    saveData(url: fileSavingPath , data: content)
    return fileSavingPath.absoluteString
}

func saveVideoFile(in folder : String,to filePath : String, url : URL) -> String{
     var localpath  = ""
    if let videoData = try? Data(contentsOf: url) {
       let   savedURL = createFilepath(in: folder, fileName: filePath, contents: videoData)
        print("Saved at: \(String(describing: savedURL))")
    } else {
        print("Failed to read video data.")
    }
    
    return localpath
}
// for large files
func copyFile(to folderName: String, fileName: String, from sourceURL: URL) -> URL? {
    guard let folderURL = createFolder(named: folderName) else { return nil }
    let destinationURL = folderURL.appendingPathComponent(fileName)

    do {
        try FileManager.default.copyItem(at: sourceURL, to: destinationURL)
        return destinationURL
    } catch {
        print("Failed to copy file: \(error)")
        return nil
    }
}
