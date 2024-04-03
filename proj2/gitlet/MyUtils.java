package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.List;

import static gitlet.Repository.GITLET_DIR;
import static gitlet.Repository.getCurrentCommit;
import static gitlet.Utils.*;

public class MyUtils {
    public static void saveDirAndObjInBlobs(Serializable SerObj, File FOLDER, String ID) {
        Commit parentCommit = getCurrentCommit();
        List<String> parentBlobIDs = parentCommit.getBlobIDs();
        if (parentBlobIDs.size() != 0) {
            for (String parentBlobID : parentBlobIDs) {
                if (ID.equals(parentBlobID)) {
                    return;
                }
            }
        }

        List<String> dirIDList = Utils.plainFilenamesIn(FOLDER);
        String dirID = getDirID(ID);
        if (!dirIDList.contains(dirID)) {
            saveDir(FOLDER, dirID);
        }

        List<String> IDList = Utils.plainFilenamesIn(join(FOLDER, dirID));
        if (IDList != null && !IDList.contains(ID)) {
            saveObj(FOLDER, dirID, ID, SerObj);
        }
    }

    public static String getDirID(String ID) {
        return ID.substring(0, 2);
    }

    public static void saveDir(File FOLDER, String dirID) {
        File dir = join(FOLDER, dirID);
        dir.mkdir();
    }

    // Save file by FOLDER, subdirectory ID and current ID
    public static void saveObj(File FOLDER, String dirID, String ID, Serializable SerObj) {
        File file = join(FOLDER, dirID, ID);
        writeObject(file, SerObj);
    }

    /** Usage:
     *  Obj: writeObject(file, SerObj);
     /*  File: writeObject(file, content); */
    public static void saveObj(File FOLDER, String name, Serializable SerObj) {
        File file = join(FOLDER, name);
        writeObject(file, SerObj);
    }

    public static void saveContent(File FOLDER, String name, String content) {
        File file = join(FOLDER, name);
        writeContents(file, content);
    }

    // Get file id through filename and file content
    public static String getFileID(File file) {
        return sha1(serialize(file.getName()), serialize(readContentsAsString(file)));
    }

    public static boolean validateInit(){
        return GITLET_DIR.exists();
    }
}
