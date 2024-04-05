package gitlet;

import java.io.Serializable;

import static gitlet.MyUtils.saveObj;
import static gitlet.Repository.*;

public class Pointer implements Serializable {
    private String commitID;
    private String branchName;
    private String initCommitID;
    private String activeBranchName;

    public Pointer(boolean isHead, String name, String id) {
        if (isHead) {
            this.initCommitID = id;
            this.activeBranchName = name;
        } else {
            this.commitID = id;
            this.branchName = name;
        }
    }

    /** Save branch and head. */
    // Save branch by branchName
    public void saveBranchFile() {
        saveObj(BRANCH_FOLDER, this.branchName, this);
    }

    // Save HEAD by headName
    public void saveHeadFile() {
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
