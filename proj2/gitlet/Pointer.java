package gitlet;

import java.io.Serializable;

import static gitlet.MyUtils.saveObj;
import static gitlet.Repository.*;

public class Pointer implements Serializable {
    private String commitID;
    private String branchName;
    private String initCommitID;
    private String activeBranchName;

    public Pointer(boolean isHead, String Name, String ID) {
        if (isHead) {
            this.initCommitID = ID;
            this.activeBranchName = Name;
        } else {
            this.commitID = ID;
            this.branchName = Name;
        }
    }

    /** Save branch and head. */
    // Save branch by branchName
    public void saveBranchFile() {
        saveObj(BRANCH_FOLDER, this.branchName, this);
    }

    // Save HEAD by headName
    public void saveHEADFile() {
        saveObj(GITLET_DIR, HEADNAME, this);
    }

    /** Get variables from commit. */
    // Get ActiveBranchName in HEAD
    public String getActiveBranchName() {
        return this.activeBranchName;
    }

    // Get initCommitID in HEAD
    public String getInitCommitID() {
        return this.initCommitID;
    }

    // Get CommitID in branch
    public String getCommitID() {
        return this.commitID;
    }
}
