package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import static gitlet.MyUtils.*;
import static gitlet.Repository.*;
import static gitlet.Utils.*;

/** Represents a gitlet commit object.
 *  @author Yun Zhu
 */
public class Commit implements Serializable {
    /** List all instance variables of the Commit class here with a useful
     *  comment above them describing what that variable represents and how that
     *  variable is used. We've provided one example for `message`.
     */

    private Date date;
    private String message;
    private String CommitID;
    private List<String> blobIDs;
    private List<String> parentIDs;
    private List<String> copiedFileIDs;
    private List<String> copiedFileNames;
    private int markedCount;
    private int distance;

    public Commit(String message, Date date, List<String> parentIDs) {
        this.date = date;
        this.message = message;
        this.CommitID = sha1(serialize(this));
        this.blobIDs = new LinkedList<>();
        this.parentIDs = parentIDs;
        this.copiedFileIDs = new LinkedList<>();
        this.copiedFileNames = new LinkedList<>();
        getInfoFromParent();
        getInfoFromStaging();
        this.markedCount = 0;
        this.distance = 0;
    }

    public Blob makeBlob(File stagingFile) {
        Blob blob = new Blob(stagingFile);
        String blobID = blob.getBlobID();
        saveDirAndObjInBlobs(blob, BLOB_FOLDER, blobID);
        return blob;
    }

    private void getInfoFromParent() {
        if (this.parentIDs.size() == 1) {
            Commit parentCommit = readObject(join(COMMITS_FOLDER, this.parentIDs.get(0)), Commit.class);
            this.blobIDs.addAll(parentCommit.getBlobIDs());
            this.copiedFileIDs.addAll(parentCommit.getCopiedFileIDs());
            this.copiedFileNames.addAll(parentCommit.getCopiedFileNames());
        }
    }

    private void getInfoFromStaging() {
        // First consider the ADDITION_FOLDER
        // Get fileNames and fileIDs in the ADDITION_FOLDER
        for (String fileName : plainFilenamesIn(ADDITION_FOLDER)) {
            File file = join(ADDITION_FOLDER, fileName);
            String fileID = getFileID(file);
            // For the file with different filename and different fileID, add it from staging
            if (!this.copiedFileNames.contains(fileName) && !this.copiedFileIDs.contains(fileID)) {
                this.copiedFileNames.add(fileName);
                this.copiedFileIDs.add(fileID);
                this.blobIDs.add(makeBlob(file).getBlobID());
                // For the file with the same filename but different fileID, use the file from staging to replace the file from parent
            } else if (this.copiedFileNames.contains(fileName) && !this.copiedFileIDs.contains(fileID)) {
                // Delete the file (copiedFileID and blobID) from parent
                for (String blobID : this.blobIDs) {
                    Blob blob = readObject(join(BLOB_FOLDER, getDirID(blobID), blobID), Blob.class);
                    if (blob.getCopiedFileName().equals(fileName)) {
                        this.copiedFileIDs.remove(blob.getCopiedFileID());
                        this.blobIDs.remove(blob.getBlobID());
                        break;
                    }
                }
                // Add the file from staging
                this.copiedFileIDs.add(fileID);
                this.blobIDs.add(makeBlob(file).getBlobID());
            }
            // For the file with the same filename and same fileID, do nothing
        }

        // Then consider the REMOVED_FOLDER
        // Get fileNames and fileIDs in the REMOVED_FOLDER
        List<String> removedBlobIDs = new LinkedList<>();
        for (String fileName : plainFilenamesIn(REMOVED_FOLDER)) {
            File file = join(REMOVED_FOLDER, fileName);
            String fileID = getFileID(file);
            // For the file with the same filename and same fileID, remove it from the new commit
            if (this.copiedFileNames.contains(fileName) && this.copiedFileIDs.contains(fileID)) {
                this.copiedFileNames.remove(fileName);
                this.copiedFileIDs.remove(fileID);
                // Add the BlobID to be deleted to removedBlobIDs
                for (String blobID : this.blobIDs) {
                    Blob blob = readObject(join(BLOB_FOLDER, getDirID(blobID), blobID), Blob.class);
                    if (fileID.equals(blob.getCopiedFileID())) {
                        removedBlobIDs.add(blobID);
                        break;
                    }
                }
            }
        }
        this.blobIDs.removeAll(removedBlobIDs);
    }

    public Date getDate() {
        return this.date;
    }

    public String getMessage() {
        return this.message;
    }

    public String getCommitID() {
        return this.CommitID;
    }

    public List<String> getBlobIDs() {
        return this.blobIDs;
    }

    public HashSet<Blob> getBlobs() {
        HashSet<Blob> blobs = new HashSet<>();
        for (String ID : blobIDs) {
            Blob blob = readObject(join(BLOB_FOLDER, getDirID(ID), ID), Blob.class);
            blobs.add(blob);
        }
        return blobs;
    }

    public List<String> getParentIDs() {
        return this.parentIDs;
    }

    public List<String> getCopiedFileIDs() {
        return this.copiedFileIDs;
    }

    public List<String> getCopiedFileNames() {
        return this.copiedFileNames;
    }

    public void resetDistance() {
        this.distance = 0;
    }

    public void updatedDistance(int distance) {
        this.distance += distance;
    }

    public int getDistance() {
        return this.distance;
    }

    public void resetMarkedCount() {
        this.markedCount = 0;
    }

    public void updatedMarkedCount() {
        this.markedCount += 1;
    }

    public int getMarkedCount() {
        return this.markedCount;
    }
}
