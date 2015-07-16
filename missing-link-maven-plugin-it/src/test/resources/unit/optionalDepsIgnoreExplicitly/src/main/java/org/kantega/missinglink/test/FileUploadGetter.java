package unit.optionalDepsIgnoreExplicitly.src.main.java.org.kantega.missinglink.test;

import org.apache.commons.fileupload.FileUpload;

public class FileUploadGetter {
    public FileUpload getFileUpload(){
        return new FileUpload();
    }
}
