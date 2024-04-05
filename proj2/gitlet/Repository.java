package gitlet;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;

import static gitlet.MyUtils.*;
import static gitlet.Utils.*;

/** Represents a gitlet repository.
 *  @author Yun Zhu
 */
public class Repository {
    /** List all instance variables of the Repository class here with a useful
     *  comment above them describing what that variable represents and how that
     *  variable is used. We've provided two examples for you.
     */

    /** The current working directory. */
    public static final File CWD = new File(System.getProperty("user.dir"));
    /** The .gitlet directory. */
    public static final File GITLET_DIR = join(CWD, ".gitlet");
    public static final File ADDITION_FOLDER = join(GITLET_DIR, "addition");
    public static final File REMOVED_FOLDER = join(GITLET_DIR, "removed");
    public static final File COMMITS_FOLDER = join(GITLET_DIR, "commits");
    public static final File BLOB_FOLDER = join(GITLET_DIR, "blobs");
    public static final File BRANCH_FOLDER = join(GITLET_DIR, "branch");
    public static final File REMOTE_FOLDER = join(GITLET_DIR, "remote");
    public static final String HEADNAME = "HEAD";
    public static final String MASTERNAME = "master";

    public static void init(String msg) {
        if (validateInit()) {
            System.out.println("A Gitlet version-control system already exists in the current directory.");
        } else {
            setupPersistence(msg);
        }
    }

    public static void add(String fileName) {
        if (!hasFileNameInCWD(fileName)) {
            printAndExit("File does not exist.");
        }

        if (plainFilenamesIn(REMOVED_FOLDER).contains(fileName)) {
            unrestrictedDelete(join(REMOVED_FOLDER, fileName));
        }

        String fileID = getFileID(join(CWD, fileName));
        for (Blob blob : getCurrentCommit().getBlobs()) {
            if (fileID.equals(blob.getCopiedFileID())) {
                unrestrictedDelete(join(REMOTE_FOLDER, fileName));
                unrestrictedDelete(join(ADDITION_FOLDER, fileName));
                return;
            }
        }

        File workingFile = join(CWD, fileName);
        String workingFileID = getFileID(workingFile);
        if (!comparedCommitsAndWorking(workingFileID)) {
            saveAdditionFile(fileName, readContentsAsString(workingFile));
        }
    }

    public static void commit(String message) {
        commitHelper(message, false, null);
    }

    public static void rm(String fileName) {
        // Unstage the file if it is currently staged for addition
        if (plainFilenamesIn(ADDITION_FOLDER).contains(fileName)) {
            List<String> stagingFileIDs = new LinkedList<>();
            for (String stagingFileName : plainFilenamesIn(ADDITION_FOLDER)) {
                String stagingFileID = getFileID(join(ADDITION_FOLDER, stagingFileName));
                stagingFileIDs.add(stagingFileID);
            }

            if (stagingFileIDs.size() != 0) {
                String fileID = getFileID(join(ADDITION_FOLDER, fileName));
                for (String stagingFileID : stagingFileIDs) {
                    if (fileID.equals(stagingFileID)) {
                        unrestrictedDelete(join(ADDITION_FOLDER, fileName));
                        return;
                    }
                }
            }
        }

        Commit currentCommit = getCurrentCommit();
        for (Blob blob : currentCommit.getBlobs()) {
            if (fileName.equals(blob.getCopiedFileName())) {
                // Stage the file for removal
                saveRemovedFile(fileName, blob.getCopiedFileContent());
                unrestrictedDelete(join(ADDITION_FOLDER, fileName));

                // Remove the file from the working directory
                if (hasFileNameInCWD(fileName)) {
                    restrictedDelete(join(CWD, fileName));
                }
                return;
            }
        }

        System.out.println("No reason to remove the file.");
    }

    public static void log() {
        printCommitLogInActiveBranch(getCurrentCommit());
    }

    public static void globalLog() {
        for (String id : plainFilenamesIn(COMMITS_FOLDER)) {
            Commit commit = readObject(join(COMMITS_FOLDER, id), Commit.class);
            printCommitLog(commit);
        }
    }

    public static void find(String message) {
        boolean hasCommit = false;
        for (String id : plainFilenamesIn(COMMITS_FOLDER)) {
            Commit commit = readObject(join(COMMITS_FOLDER, id), Commit.class);
            if (commit.getMessage().equals(message)) {
                System.out.println(commit.getCommitID());
                hasCommit = true;
            }
        }

        if (!hasCommit) {
            System.out.println("Found no commit with that message.");
        }
    }

    public static void status() {
        printStatus();
    }

    public static void checkout(String[] args) {
        if (args.length == 2) {
            checkoutWithBranchName(args[1]);
        }
        if (args.length == 3) {
            checkoutWithFileName(args[2]);
        }
        if (args.length == 4) {
            checkoutWithCommitIDAndFileName(args[1], args[3]);
        }
    }

    public static void branch(String branchName) {
        //  If a branch with the given name already exists
        checkExistSameFileInFolder(branchName, BRANCH_FOLDER, "A branch with that name already exists.");

        saveBranch(branchName, getCurrentCommit().getCommitID());
    }

    public static void rmBranch(String branchName) {
        // If a branch with the given name does not exist
        checkNotExistSameFileInFolder(branchName, BRANCH_FOLDER, "A branch with that name does not exist.");
        // If the branch to be removed is the current branch
        if (extractHEADThenGetActiveBranchName().equals(branchName)) {
            printAndExit("Cannot remove the current branch.");
        }

        unrestrictedDelete(join(BRANCH_FOLDER, branchName));
    }

    public static void reset(String commitID) {
        // If no commit with the given id exists
        checkNotExistSameFileInFolder(commitID, COMMITS_FOLDER, "No commit with that id exists.");
        Commit commit = readObject(join(COMMITS_FOLDER, commitID), Commit.class);
        checkUntrackedFileError();

        // 1. Remove tracked files that are not present in the given commit
        for (String workingFileName : plainFilenamesIn(CWD)) {
            if (!commit.getCopiedFileIDs().contains(getFileID(join(CWD, workingFileName)))) {
                restrictedDelete(join(CWD, workingFileName));
            }
        }

        // 2. Check out all the files tracked by the given commit
        for (String copiedFileName : commit.getCopiedFileNames()) {
            for (Blob blob : commit.getBlobs()) {
                if (copiedFileName.equals(blob.getCopiedFileName())) {
                    // Put the file in the CWD or overwrite the old version
                    String content = blob.getCopiedFileContent();
                    saveContent(CWD, copiedFileName, content);
                    return;
                }
            }
        }

        // 3. Move the current branch's head to that commit node
        saveActiveBranch(extractHEADThenGetActiveBranchName(), commitID);

        // 4. Clear the staging area
        cleanStaging();
    }

    public static void merge(String branchName) {
        branchName = convertRemoteBranchName(branchName);
        // If there are staged additions or removals present
        if (plainFilenamesIn(ADDITION_FOLDER).size() > 0 || plainFilenamesIn(REMOVED_FOLDER).size() > 0) {
            printAndExit("You have uncommitted changes.");
        }
        // If a branch with the given name does not exist
        if (!plainFilenamesIn(BRANCH_FOLDER).contains(branchName)) {
            printAndExit("A branch with that name does not exist.");
        }
        // If attempting to merge a branch with itself
        if (extractHEADThenGetActiveBranchName().equals(branchName)) {
            printAndExit("Cannot merge a branch with itself.");
        }
        // If an untracked file in the current commit would be overwritten or deleted by the merge
        checkUntrackedFileError();
        // Get split commit
        Commit split = getSplitCommit(branchName);
        String splitCommitID = split.getCommitID();
        // If the split point is the same commit as the given branch
        if (splitCommitID.equals(extractBranchThenGetCommitID(branchName))) {
            printAndExit("Given branch is an ancestor of the current branch.");
        }
        // If the split point is the current branch, then the effect is to check out the given branch
        if (splitCommitID.equals(getCurrentCommit().getCommitID())) {
            checkoutWithBranchName(branchName);
            printAndExit("Current branch fast-forwarded.");
        }

        // Get the target branch commit as "other"
        String otherCommitID = extractBranchThenGetCommitID(branchName);
        Commit other = readObject(join(COMMITS_FOLDER, otherCommitID), Commit.class);
        mergeHelper(split, other);
        commitHelper(getMergeMessage(branchName), true, branchName);
    }

    public static void addRemote(String remoteName, String dirPathString) {
        // If a remote with the given name already exists
        if (plainFilenamesIn(REMOTE_FOLDER).contains(remoteName)) {
            printAndExit("A remote with that name already exists.");
        }

        Remote remote = new Remote(remoteName, dirPathString);
        saveObj(REMOTE_FOLDER, remoteName, remote);
    }

    public static void rmRemote(String remoteName) {
        // If a remote with the given name already exists, print the error message and exit
        if (!plainFilenamesIn(REMOTE_FOLDER).contains(remoteName)) {
            printAndExit("A remote with that name does not exist.");
        }

        for (String fileName : plainFilenamesIn(REMOTE_FOLDER)) {
            if (fileName.equals(remoteName)) {
                unrestrictedDelete(join(REMOTE_FOLDER, fileName));
            }
        }
    }

    public static void push(String remoteName, String remoteBranchName) {
        Remote remote = readObject(join(REMOTE_FOLDER, remoteName), Remote.class);
        // If the remote .gitlet directory does not exist
        validateRemoteDir(remote);
        File remoteDir = remote.getRemoteDir();
        // If the remote branch's head is not in the history of the current local head
        Pointer remoteActiveBranch = readObject(join(remoteDir, "branch", remoteBranchName), Pointer.class);
        // Note: HEAD point to branch, branch point to commit
        String remoteHeadID = remoteActiveBranch.getCommitID();
        if (!isRemoteHeadIDInHistoryOfLocal(remoteHeadID, getCurrentCommit())) {
            printAndExit("Please pull down remote changes before pushing.");
        }
        Commit localCurrentCommit = getCurrentCommit();
        pushHelper(remoteDir, localCurrentCommit, remoteHeadID);
        saveRemoteBranch(remoteDir, remoteBranchName, localCurrentCommit.getCommitID());
        saveRemoteHEAD(remoteDir, remoteBranchName, getInitCommitID());
    }

    public static void fetch(String remoteName, String remoteBranchName) {
        Remote remote = readObject(join(REMOTE_FOLDER, remoteName), Remote.class);
        // If the remote .gitlet directory does not exist
        validateRemoteDir(remote);
        // If the remote Gitlet repository does not have the given branch name
        if (!remote.getBranchNames().contains(remoteBranchName)) {
            printAndExit("That remote does not have that branch.");
        }
        File remoteDir = remote.getRemoteDir();
        Pointer branch = readObject(join(remoteDir, "branch", remoteBranchName), Pointer.class);
        Commit commit = readObject(join(remoteDir, "commits", branch.getCommitID()), Commit.class);
        fetchHelper(remoteDir, commit);
        saveBranch(remoteName + "\\" + remoteBranchName, branch.getCommitID());
    }

    public static void pull(String remoteName, String remoteBranchName) {
        fetch(remoteName, remoteBranchName);
        merge(remoteName + "/" + remoteBranchName);
    }

    /** The helper methods */
    public static void setupPersistence(String msg) {
        // Create the file system (directories and folders)
        GITLET_DIR.mkdir();
        COMMITS_FOLDER.mkdir();
        BLOB_FOLDER.mkdir();
        ADDITION_FOLDER.mkdir();
        REMOVED_FOLDER.mkdir();
        BRANCH_FOLDER.mkdir();
        REMOTE_FOLDER.mkdir();

        // Create the initial commit
        Commit initCommit = makeCommitWithInit(msg);
        String initCommitID = initCommit.getCommitID();

        // Create HEAD and master
        saveActiveBranch(MASTERNAME, initCommitID);
        saveHEAD(MASTERNAME, initCommitID);
    }

    private static boolean hasFileNameInCWD(String fileName) {
        for (String workingFileName : plainFilenamesIn(CWD)) {
            if (workingFileName.equals(fileName)) {
                return true;
            }
        }
        return false;
    }

    public static boolean comparedCommitsAndWorking(String workingFileID) {
        for (Blob blob : getCurrentCommit().getBlobs()) {
            if (workingFileID.equals(blob.getCopiedFileID())) {
                return true;
            }
        }
        return false;
    }

    private static void commitHelper(String message, boolean afterMerge, String branchName) {
        if (plainFilenamesIn(ADDITION_FOLDER).size() == 0 && plainFilenamesIn(REMOVED_FOLDER).size() == 0) {
            printAndExit("No changes added to the commit.");
        }

        if (afterMerge) {
            makeCommitAfterMerge(message, branchName);
        } else {
            makeCommitWithoutInit(message);
        }

        cleanStaging();
    }

    public static Commit makeCommitWithoutInit(String msg) {
        return makeCommit(msg, false, null);
    }

    public static Commit makeCommitWithInit(String msg) {
        return makeCommit(msg, true, null);
    }

    public static Commit makeCommitAfterMerge(String msg, String branchName) {
        return makeCommit(msg, false, branchName);
    }

    private static Commit makeCommit(String msg, boolean isInit, String otherBranchName) {
        Commit commit;
        Date date = getDate(isInit);
        if (isInit) {
            commit = new Commit(msg, date, new LinkedList<>());
        } else {
            List<String> parentID;
            if (otherBranchName == null) {
                parentID = getFirstParentID();
            } else {
                parentID = getTwoParentIDs(otherBranchName);
            }
            commit = new Commit(msg, date, parentID);
        }

        String commitID = commit.getCommitID();
        saveObj(COMMITS_FOLDER, commitID, commit);

        if (!isInit) {
            String activeBranchName = extractHEADThenGetActiveBranchName();
            saveActiveBranch(activeBranchName, commitID);
        }
        return commit;
    }

    private static void printCommitLogInActiveBranch(Commit commit) {
        if (commit == null) {
            return;
        }
        printCommitLog(commit);
        List<String> parentIDs = commit.getParentIDs();
        if (parentIDs.size() > 0) {
            Commit parentCommit = readObject(join(COMMITS_FOLDER, parentIDs.get(0)), Commit.class);
            printCommitLogInActiveBranch(parentCommit);
        }
    }

    private static void printCommitLog(Commit commit) {
        System.out.println("===");
        System.out.println("commit " + commit.getCommitID());
        List<String> parentIDs = commit.getParentIDs();
        if (parentIDs.size() == 2) {
            System.out.println("Merge: " + parentIDs.get(0).substring(0, 7) + " "
                    + parentIDs.get(1).substring(0, 7));
        }
        SimpleDateFormat dateFormat = new SimpleDateFormat("E MMM dd HH:mm:ss yyyy Z");
        System.out.println("Date: " + dateFormat.format(commit.getDate()));
        System.out.println(commit.getMessage());
        System.out.println();
    }

    private static Date getDate(boolean isInit) {
        if (isInit) {
            return new Date(0); // get the epoch time
        } else {
            Date date = new Date();
            return new Date(date.getTime());
        }
    }

    private static List<String> getFirstParentID() {
        List<String> parentIDs = new LinkedList<>();
        // Set current CommitID as parentID
        String currentCommitID = getCurrentCommit().getCommitID();
        parentIDs.add(currentCommitID);
        return parentIDs;
    }

    private static List<String> getTwoParentIDs(String otherBranchName) {
        List<String> parentIDs = new LinkedList<>();
        // Set current CommitID as parentID
        String currentCommitID = getCurrentCommit().getCommitID();
        parentIDs.add(currentCommitID);
        // Set CommitID in other branch as parentID
        String otherBranchCommitID = extractBranchThenGetCommitID(otherBranchName);
        parentIDs.add(otherBranchCommitID);
        return parentIDs;
    }

    public static void cleanStaging() {
        for (String fileName : plainFilenamesIn(ADDITION_FOLDER)) {
            unrestrictedDelete(join(ADDITION_FOLDER, fileName));
        }
        for (String fileName : plainFilenamesIn(REMOVED_FOLDER)) {
            unrestrictedDelete(join(REMOVED_FOLDER, fileName));
        }
    }

    /** The helper methods for the status command. */
    private static void printStatus() {
        System.out.println("=== Branches ===");
        String activeBranchName = extractHEADThenGetActiveBranchName();
        for (String branchFileName : plainFilenamesIn(BRANCH_FOLDER)) {
            if (activeBranchName.equals(branchFileName)) {
                System.out.println("*" + branchFileName);
            } else {
                System.out.println(branchFileName);
            }
        }
        System.out.println();
        System.out.println("=== Staged Files ===");
        for (String stagingFileName : plainFilenamesIn(ADDITION_FOLDER)) {
            System.out.println(stagingFileName);
        }
        System.out.println();
        System.out.println("=== Removed Files ===");
        for (String removedFileName : plainFilenamesIn(REMOVED_FOLDER)) {
            System.out.println(removedFileName);
        }
        System.out.println();
        System.out.println("=== Modifications Not Staged For Commit ===");
        printModificationsNotStagedForCommit();
        System.out.println();
        System.out.println("=== Untracked Files ===");
        printUntrackedFiles();
        System.out.println();
    }

    private static void printModificationsNotStagedForCommit() {
        Commit currentCommit = getCurrentCommit();
        boolean trackedCurrentCommit = false;
        boolean stagedForAddition = false;
        boolean changedFromCommit = true;
        boolean changedFromAddition = true;

        for (String workingFileName : plainFilenamesIn(CWD)) {
            String workingFileID = getFileID(join(CWD, workingFileName));

            // Tracked in the current commit, changed in the working directory, but not staged
            for (Blob blob : currentCommit.getBlobs()) {
                if (blob.getCopiedFileName().equals(workingFileName)) {
                    trackedCurrentCommit = true;
                    if (blob.getCopiedFileID().equals(workingFileID)) {
                        changedFromCommit = false;
                    }
                    break;
                }
            }

            // Staged for addition, but with different contents than in the working directory
            for (String fileName : plainFilenamesIn(ADDITION_FOLDER)) {
                if (fileName.equals(workingFileName)) {
                    stagedForAddition = true;
                    String fileID = getFileID(join(ADDITION_FOLDER, fileName));
                    if (fileID.equals(workingFileID)) {
                        changedFromAddition = false;
                    }
                    break;
                }
            }

            if (trackedCurrentCommit && changedFromCommit && !stagedForAddition) {
                System.out.println(workingFileName + " (modified)");
            }
            if (stagedForAddition && changedFromAddition) {
                System.out.println(workingFileName + " (modified)");
            }
        }

        // Staged for addition, but deleted in the working directory
        for (String fileName : plainFilenamesIn(ADDITION_FOLDER)) {
            if (!plainFilenamesIn(CWD).contains(fileName)) {
                System.out.println(fileName + " (deleted)");
            }
        }

        // Not staged for removal, but tracked in the current commit and deleted from the working directory
        for (Blob blob : currentCommit.getBlobs()) {
            String blobCopiedFileName = blob.getCopiedFileName();
            if (!plainFilenamesIn(REMOVED_FOLDER).contains(blobCopiedFileName)
                    && !plainFilenamesIn(CWD).contains(blobCopiedFileName)) {
                System.out.println(blobCopiedFileName + " (deleted)");
            }
        }
    }

    private static void printUntrackedFiles() {
        // Files present in the working directory but neither staged for addition nor tracked
        boolean isSameName = false;
        for (String workingFileName : plainFilenamesIn(CWD)) {
            // 1. Compared with the files in current commit
            for (String fileName : getCurrentCommit().getCopiedFileNames()) {
                if (fileName.equals(workingFileName)) {
                    isSameName = true;
                }
            }
            // 2. Compared with the files in the ADDITION_FOLDER
            if (plainFilenamesIn(ADDITION_FOLDER).contains(workingFileName)) {
                isSameName = true;
            }

            if (!isSameName) {
                System.out.println(workingFileName);
            }
        }
    }

    /** The helper methods for the checkout command. */
    private static void checkoutWithFileName(String fileName) {
        checkoutHelper(getCurrentCommit(), fileName);
    }

    private static void checkoutWithBranchName(String branchName) {
        branchName = convertRemoteBranchName(branchName);
        // If no branch with that name exists
        List<String> branchNames = plainFilenamesIn(BRANCH_FOLDER);
        if (!branchNames.contains(branchName)) {
            printAndExit("No such branch exists.");
        }
        // If that branch is the current branch
        if (extractHEADThenGetActiveBranchName().equals(branchName)) {
            printAndExit("No need to checkout the current branch.");
        }
        String commitID = readObject(join(BRANCH_FOLDER, branchName), Pointer.class).getCommitID();
        Commit branchCommit = readObject(join(COMMITS_FOLDER, commitID), Commit.class);
        // If a working file is untracked in the current branch and would be overwritten by the checkout
        checkUntrackedFileError();

        // Takes all files in the commit at the head of the given branch,
        // and puts them in the working directory,
        // overwriting the versions of the files that are already there if they exist.
        // 1. Delete all files in the CWD
        for (String name : plainFilenamesIn(CWD)) {
            restrictedDelete(name);
        }
        // 2. Checkout all copiedFiles in the commit
        for (String copiedFileName : branchCommit.getCopiedFileNames()) {
            checkoutHelper(branchCommit, copiedFileName);
        }
        // Also, at the end of this command, the given branch will now be considered the current branch (HEAD).
        saveHEAD(branchName, commitID);
    }

    private static void checkoutWithCommitIDAndFileName(String commitID, String fileName) {
        if (commitID.length() == 8) {
            for (String id : plainFilenamesIn(COMMITS_FOLDER)) {
                if (id.substring(0, 8).equals(commitID)) {
                    commitID = id;
                    break;
                }
            }
        }

        // If no commit with the given id exists
        checkNotExistSameFileInFolder(commitID, COMMITS_FOLDER, "No commit with that id exists.");
        Commit commit = readObject(join(COMMITS_FOLDER, commitID), Commit.class);
        // If the file does not exist in the given commit
        checkNotExistSameFileInCommit(fileName, commit, "File does not exist in that commit.");
        checkoutHelper(commit, fileName);
    }

    private static void checkoutHelper(Commit commit, String fileName) {
        // Find the file in the commit
        for (Blob blob : commit.getBlobs()) {
            if (fileName.equals(blob.getCopiedFileName())) {
                // Put the file in the CWD or overwrite the old version
                String content = blob.getCopiedFileContent();
                saveContent(CWD, fileName, content);
                return;
            }
        }

        // If the file does not in the commit
        System.out.println("File does not exist in that commit.");
    }

    private static String convertRemoteBranchName(String branchName) {
        if (branchName.contains("/")) {
            int indexOfSlash = branchName.indexOf("/");
            branchName = branchName.substring(0, indexOfSlash) + "\\" + branchName.substring(indexOfSlash + 1);
        }
        return branchName;
    }

    /** The helper methods for the merge command. */
    private static Commit getSplitCommit(String branchName) {
        // Mark the current (head) branch
        Commit headCommit = getCurrentCommit();
        markBranch(headCommit, 0);
        // Get and mark the other branch commit
        String otherCommitID = extractBranchThenGetCommitID(branchName);
        Commit otherCommit = readObject(join(COMMITS_FOLDER, otherCommitID), Commit.class);
        markBranch(otherCommit, 0);
        // Get the smallest split commit
        Commit splitCommit = readObject(join(COMMITS_FOLDER, getInitCommitID()), Commit.class);
        int splitDistance = splitCommit.getDistance();
        for (String commitID : plainFilenamesIn(COMMITS_FOLDER)) {
            Commit commit = readObject(join(COMMITS_FOLDER, commitID), Commit.class);
            if (commit.getMarkedCount() == 2 && commit.getDistance() < splitDistance) {
                splitCommit = commit;
                splitDistance = commit.getDistance();
            }
        }
        // Reset marked count and distance in all commits
        for (String commitID : plainFilenamesIn(COMMITS_FOLDER)) {
            Commit commit = readObject(join(COMMITS_FOLDER, commitID), Commit.class);
            commit.resetMarkedCount();
            commit.resetDistance();
            saveObj(COMMITS_FOLDER, commit.getCommitID(), commit);
        }
        return splitCommit;
    }

    private static void markBranch(Commit branchCommit, int distance) {
        branchCommit.updatedMarkedCount();
        branchCommit.updatedDistance(distance);
        saveObj(COMMITS_FOLDER, branchCommit.getCommitID(), branchCommit);
        distance += 1;
        for (String parentID : branchCommit.getParentIDs()) {
            branchCommit = readObject(join(COMMITS_FOLDER, parentID), Commit.class);
            markBranch(branchCommit, distance);
        }
    }

    public static void mergeHelper(Commit split, Commit other) {
        Commit head = getCurrentCommit();
        Set<String> allFileNames = getAllFileNames(split, head, other);
        rulesDealFiles(split, head, other, allFileNames);
    }

    private static void rulesDealFiles(Commit split, Commit head, Commit other, Set<String> allFileNames) {
        List<String> fileNamesInSplit = split.getCopiedFileNames();
        List<String> fileNamesInHead = head.getCopiedFileNames();
        List<String> fileNamesInOther = other.getCopiedFileNames();

        for (String fileName : allFileNames) {
            String fileIDInHead = null;
            String fileIDInOther = null;
            String fileIDInSplit = null;
            Blob headBlob = null;
            Blob otherBlob = null;
            // Get head blob with fileID and other blob with file ID
            for (Blob blob : head.getBlobs()) {
                if (blob.getCopiedFileName().equals(fileName)) {
                    headBlob = blob;
                    fileIDInHead = blob.getCopiedFileID();
                    break;
                }
            }
            for (Blob blob : other.getBlobs()) {
                if (blob.getCopiedFileName().equals(fileName)) {
                    otherBlob = blob;
                    fileIDInOther = blob.getCopiedFileID();
                    break;
                }
            }
            for (Blob blob : split.getBlobs()) {
                if (blob.getCopiedFileName().equals(fileName)) {
                    fileIDInSplit = blob.getCopiedFileID();
                    break;
                }
            }

            boolean isHeadModified = (fileIDInHead != null) && (!fileIDInHead.equals(fileIDInSplit));
            boolean isOtherModified = (fileIDInOther != null) && (!fileIDInOther.equals(fileIDInSplit));

            if (fileNamesInSplit.contains(fileName) && fileNamesInHead.contains(fileName) && fileNamesInOther.contains(fileName)) {
                // 1. Modified in other but not head -> stage the file for addition and put it to the cwd
                if (isOtherModified && !isHeadModified) {
                    saveWorkingFile(fileName, otherBlob.getCopiedFileContent());
                    saveAdditionFile(fileName, otherBlob.getCopiedFileContent());
                    continue;
                }
                // 2. Modified in head but not other -> stage the file for addition and put it to the cwd
                if (isHeadModified && !isOtherModified) {
                    saveWorkingFile(fileName, headBlob.getCopiedFileContent());
                    saveAdditionFile(fileName, headBlob.getCopiedFileContent());
                    continue;
                }
            }

            // 5. Neither in split nor other but in head -> stage file from head for addition
            if (!fileNamesInSplit.contains(fileName) && !fileNamesInOther.contains(fileName) && fileNamesInHead.contains(fileName)) {
                saveWorkingFile(fileName, headBlob.getCopiedFileContent());
                saveAdditionFile(fileName, headBlob.getCopiedFileContent());
                continue;
            }

            // 6. Neither in split nor head but in other -> stage file from other for addition
            if (!fileNamesInSplit.contains(fileName) && !fileNamesInHead.contains(fileName) && fileNamesInOther.contains(fileName)) {
                saveWorkingFile(fileName, otherBlob.getCopiedFileContent());
                saveAdditionFile(fileName, otherBlob.getCopiedFileContent());
                continue;
            }

            // 7a. Unmodified in head but not present in other -> stage file from head for removed
            // 7b. Modified in head but not present in other -> conflict
            if (fileNamesInSplit.contains(fileName) && fileNamesInHead.contains(fileName) && !fileNamesInOther.contains(fileName)) {
                if (!isHeadModified) {
                    saveRemovedFile(fileName, headBlob.getCopiedFileContent());
                    // delete it in working
                    if (hasFileNameInCWD(fileName)) {
                        restrictedDelete(join(CWD, fileName));
                    }
                } else {
                    makeConflictFile(fileName, headBlob.getCopiedFileContent(), "");
                }
                continue;
            }

            // 8a. Unmodified in other but not present in head -> stage file from other for removed
            // 8b. Modified in other but not present in head -> conflict
            if (fileNamesInSplit.contains(fileName) && fileNamesInOther.contains(fileName) && !fileNamesInHead.contains(fileName)) {
                if (!isOtherModified) {
                    saveRemovedFile(fileName, otherBlob.getCopiedFileContent());
                    // delete it in working
                    if (hasFileNameInCWD(fileName)) {
                        restrictedDelete(join(CWD, fileName));
                    }
                } else {
                    makeConflictFile(fileName, "", otherBlob.getCopiedFileContent());
                }
                continue;
            }

            if (fileNamesInOther.contains(fileName) && fileNamesInHead.contains(fileName)) {
                if (isOtherModified && isHeadModified) {
                    // 3. Modified in head and other in the same way -> stage file from head/other for addition and put it to the cwd
                    if (fileIDInHead.equals(fileIDInOther)) {
                        saveWorkingFile(fileName, headBlob.getCopiedFileContent());
                        saveAdditionFile(fileName, headBlob.getCopiedFileContent());
                        // 4. Modified in head and other in different ways -> Store conflict file to cwd and addition folder
                    } else {
                        makeConflictFile(fileName, headBlob.getCopiedFileContent(), otherBlob.getCopiedFileContent());
                    }
                }
            }
        }
    }

    private static Set<String> getAllFileNames(Commit split, Commit head, Commit other) {
        Set<String> fileNames = new HashSet<>();
        for (String fileName : split.getCopiedFileNames()) {
            fileNames.add(fileName);
        }
        for (String fileName : head.getCopiedFileNames()) {
            fileNames.add(fileName);
        }
        for (String fileName : other.getCopiedFileNames()) {
            fileNames.add(fileName);
        }
        return fileNames;
    }

    private static void makeConflictFile(String fileName, String fileContentsFromHead, String fileContentsFromOther) {
        String contents = "<<<<<<< HEAD" + "\n"
                + fileContentsFromHead
                + "=======" + "\n"
                + fileContentsFromOther
                + ">>>>>>>" + "\n";
        saveWorkingFile(fileName, contents);
        saveAdditionFile(fileName, contents);
        System.out.println("Encountered a merge conflict.");
    }

    private static String deConvertRemoteBranchName(String branchName) {
        if (branchName.contains("\\")) {
            int indexOfBackslash = branchName.indexOf("\\");
            branchName = branchName.substring(0, indexOfBackslash) + "/" + branchName.substring(indexOfBackslash + 1);
        }
        return branchName;
    }

    private static String getMergeMessage(String branchName) {
        branchName = deConvertRemoteBranchName(branchName);
        String mergeMessage = "Merged " + branchName + " into " + extractHEADThenGetActiveBranchName() + ".";
        return mergeMessage;
    }

    /** The helper methods for the push command. */
    /** Push all commits and blobs from remote repo by recursion. */
    private static void pushHelper(File remoteDir, Commit commit, String remoteHeadID) {
        String commitID = commit.getCommitID();
        File remoteCommitsFolder = join(remoteDir, "commits");
        if (commitID.equals(remoteHeadID) || plainFilenamesIn(remoteCommitsFolder).contains(commitID)) {
            return;
        }
        // Save commit
        if (!plainFilenamesIn(remoteCommitsFolder).contains(commitID)) {
            saveObj(remoteCommitsFolder, commitID, commit);
        }
        // Save blobs with comparing
        for (String blobID : commit.getBlobIDs()) {
            Blob blob = readObject(join(BLOB_FOLDER, getDirID(blobID), blobID), Blob.class);
            saveDirAndObjInBlobs(blob, join(remoteDir, "blobs"), blob.getBlobID());
        }
        // Push from parent commits
        for (String parentID : commit.getParentIDs()) {
            Commit parentCommit = readObject(join(COMMITS_FOLDER, parentID), Commit.class);
            pushHelper(remoteDir, parentCommit, remoteHeadID);
        }
    }

    private static boolean isRemoteHeadIDInHistoryOfLocal(String remoteHeadID, Commit commit) {
        boolean isInHistoryOfLocal = commit.getCommitID().equals(remoteHeadID);
        // Check parent commits
        for (String parentID : commit.getParentIDs()) {
            Commit parentCommit = readObject(join(COMMITS_FOLDER, parentID), Commit.class);
            return isInHistoryOfLocal || isRemoteHeadIDInHistoryOfLocal(remoteHeadID, parentCommit);
        }
        return isInHistoryOfLocal;
    }

    // Save or change a branch in remote
    public static void saveRemoteBranch(File remoteDir, String branchName, String commitID) {
        Pointer branch = new Pointer(false, branchName, commitID);
        saveObj(join(remoteDir, "branch"), branchName, branch);
    }

    // Save or change HEAD in remote
    public static void saveRemoteHEAD(File remoteDir, String activeBranchName, String initCommitID) {
        Pointer head = new Pointer(true, activeBranchName, initCommitID);
        saveObj(remoteDir, activeBranchName, head);
    }

    /** The helper methods for the fetch command. */
    /** Fetch all commits and blobs from remote repo by recursion. */
    private static void fetchHelper(File remoteDir, Commit commit) {
        String commitID = commit.getCommitID();
        if (commit == null || plainFilenamesIn(COMMITS_FOLDER).contains(commitID)) {
            return;
        }
        // Save commit
        if (!plainFilenamesIn(COMMITS_FOLDER).contains(commitID)) {
            saveObj(COMMITS_FOLDER, commitID, commit);
        }
        // Save blobs with comparing
        for (String blobID : commit.getBlobIDs()) {
            Blob blob = readObject(join(remoteDir, "blobs", getDirID(blobID), blobID), Blob.class);
            saveDirAndObjInBlobs(blob, BLOB_FOLDER, blob.getBlobID());
        }
        // Fetch from parent commits
        for (String parentID : commit.getParentIDs()) {
            Commit parentCommit = readObject(join(remoteDir, "commits", parentID), Commit.class);
            fetchHelper(remoteDir, parentCommit);
        }
    }

    private static void validateRemoteDir(Remote remote) {
        if (!remote.getRemoteDir().exists()) {
            printAndExit("Remote directory not found.");
        }
    }

    /** The helper methods for checking the errors. */
    private static void checkNotExistSameFileInFolder(String fileName, File folder, String message) {
        if (!plainFilenamesIn(folder).contains(fileName)) {
            printAndExit(message);
        }
    }

    private static void checkExistSameFileInFolder(String fileName, File folder, String message) {
        for (String name : plainFilenamesIn(folder)) {
            if (name.equals(fileName)) {
                printAndExit(message);
            }
        }
    }

    private static void checkNotExistSameFileInCommit(String fileName, Commit commit, String message) {
        if (!commit.getCopiedFileNames().contains(fileName)) {
            printAndExit(message);
        }
    }

    private static void checkUntrackedFileError() {
        List<String> copiedFileIDs = getCurrentCommit().getCopiedFileIDs();
        List<String> additionFileIDs = new LinkedList<>();
        for (String fileName : plainFilenamesIn(ADDITION_FOLDER)) {
            additionFileIDs.add(getFileID(join(ADDITION_FOLDER, fileName)));
        }
        for (String workingFileName : plainFilenamesIn(CWD)) {
            String workingFileID = getFileID(join(CWD, workingFileName));
            if (!copiedFileIDs.contains(workingFileID) && !additionFileIDs.contains(workingFileID)) {
                printAndExit("There is an untracked file in the way; delete it, or add and commit it first.");
            }
        }
    }

    public static void saveAdditionFile(String fileName, String contents) {
        saveContent(ADDITION_FOLDER, fileName, contents);
    }

    public static void saveRemovedFile(String fileName, String contents) {
        saveContent(REMOVED_FOLDER, fileName, contents);
    }

    public static void saveWorkingFile(String fileName, String contents) {
        saveContent(CWD, fileName, contents);
    }

    public static Commit getCurrentCommit() {
        String activeBranchName = extractHEADThenGetActiveBranchName();
        String currentCommitID = extractActiveBranchThenGetCurrentCommitID(activeBranchName);
        Commit currentCommit = readObject(join(COMMITS_FOLDER, currentCommitID), Commit.class);
        return currentCommit;
    }

    public static String getInitCommitID() {
        Pointer head = readObject(join(GITLET_DIR, HEADNAME), Pointer.class);
        return head.getInitCommitID();
    }

    public static String extractHEADThenGetActiveBranchName() {
        Pointer head = readObject(join(GITLET_DIR, HEADNAME), Pointer.class);
        return head.getActiveBranchName();
    }

    public static String extractActiveBranchThenGetCurrentCommitID(String activeBranchName) {
        Pointer activeBranch = readObject(join(BRANCH_FOLDER, activeBranchName), Pointer.class);
        return activeBranch.getCommitID();
    }

    public static String extractBranchThenGetCommitID(String branchName) {
        Pointer branch = readObject(join(BRANCH_FOLDER, branchName), Pointer.class);
        return branch.getCommitID();
    }

    public static void saveActiveBranch(String branchName, String commitID) {
        saveBranch(branchName, commitID);
    }

    public static void saveBranch(String branchName, String commitID) {
        Pointer branch = new Pointer(false, branchName, commitID);
        branch.saveBranchFile();
    }

    public static void saveHEAD(String activeBranchName, String initCommitID) {
        Pointer head = new Pointer(true, activeBranchName, initCommitID);
        head.saveHeadFile();
    }
}
