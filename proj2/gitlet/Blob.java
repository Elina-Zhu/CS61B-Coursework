package gitlet;

import java.io.File;
import java.io.Serializable;

import static gitlet.MyUtils.getFileID;
import static gitlet.Utils.*;

public class Blob implements Serializable {
    private String blobID;
    private String copiedFileID;
    private String copiedFileName;
    private String copiedFileContent;

    public Blob(File stagingFile) {
        this.blobID = sha1(serialize(this));
        this.copiedFileID = getFileID(stagingFile);
        this.copiedFileName = stagingFile.getName();
        this.copiedFileContent = readContentsAsString(stagingFile);
    }

    public String getBlobID() {
        return this.blobID;
    }

    public String getCopiedFileID() {
        return this.copiedFileID;
    }

    public String getCopiedFileName() {
        return this.copiedFileName;
    }

    public String getCopiedFileContent() {
        return this.copiedFileContent;
    }
}
