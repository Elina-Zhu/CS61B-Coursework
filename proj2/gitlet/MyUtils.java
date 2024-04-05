package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.List;

import static gitlet.Repository.GITLET_DIR;
import static gitlet.Repository.getCurrentCommit;
import static gitlet.Utils.*;

public class MyUtils {
    public static void saveDirAndObjInBlobs(Serializable serObj, File folder, String id) {
        Commit parentCommit = getCurrentCommit();
        List<String> parentBlobIDs = parentCommit.getBlobIDs();
        if (parentBlobIDs.size() != 0) {
            for (String parentBlobID : parentBlobIDs) {
                if (id.equals(parentBlobID)) {
                    return;
                }
            }
        }

        List<String> dirIDList = Utils.plainFilenamesIn(folder);
        String dirID = getDirID(id);
        if (!dirIDList.contains(dirID)) {
            saveDir(folder, dirID);
        }

        List<String> idList = Utils.plainFilenamesIn(join(folder, dirID));
        if (idList != null && !idList.contains(id)) {
            saveObj(folder, dirID, id, serObj);
        }
    }

    public static String getDirID(String id) {
        return id.substring(0, 2);
    }

    public static void saveDir(File folder, String dirID) {
        File dir = join(folder, dirID);
        dir.mkdir();
    }

    // Save file by FOLDER, subdirectory ID and current ID
    public static void saveObj(File folder, String dirID, String id, Serializable serObj) {
        File file = join(folder, dirID, id);
        writeObject(file, serObj);
    }

    /** Usage:
     *  Obj: writeObject(file, SerObj);
     /*  File: writeObject(file, content); */
    public static void saveObj(File folder, String name, Serializable serObj) {
        File file = join(folder, name);
        writeObject(file, serObj);
    }

    public static void saveContent(File folder, String name, String content) {
        File file = join(folder, name);
        writeContents(file, content);
    }

    // Get file id through filename and file content
    public static String getFileID(File file) {
        return sha1(serialize(file.getName()), serialize(readContentsAsString(file)));
    }

    public static boolean validateInit() {
        return GITLET_DIR.exists();
    }
}
