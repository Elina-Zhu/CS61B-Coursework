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
        for (String ID : plainFilenamesIn(COMMITS_FOLDER)) {
            Commit commit = readObject(join(COMMITS_FOLDER, ID), Commit.class);
            printCommitLog(commit);
        }
    }

    public static void find(String message) {
        boolean hasCommit = false;
        for (String ID : plainFilenamesIn(COMMITS_FOLDER)) {
            Commit commit = readObject(join(COMMITS_FOLDER, ID), Commit.class);
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
            checkoutHelper(commit, copiedFileName);
        }

        // 3. Move the current branch's head to that commit node
        saveActiveBranch(extractHEADThenGetActiveBranchName(), commitID);

        // 4. Clear the staging area
        cleanStaging();
    }

    public static void merge(String branchName) {}

    public static void addRemote(String remoteName, String dirPathString) {}

    public static void rmRemote(String remoteName) {}

    public static void push(String remoteName, String remoteBranchName) {}

    public static void fetch(String remoteName, String remoteBranchName) {}

    public static void pull(String remoteName, String remoteBranchName) {}

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
        saveHEAD(branchName, getInitCommitID());
    }

    private static void checkoutWithCommitIDAndFileName(String commitID, String fileName) {
        if (commitID.length() == 8) {
            for (String ID : plainFilenamesIn(COMMITS_FOLDER)) {
                if (ID.substring(0, 8).equals(commitID)) {
                    commitID = ID;
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

        System.out.println("File does not exist in that commit.");
    }

    private static String convertRemoteBranchName(String branchName) {
        if (branchName.contains("/")) {
            int indexOfSlash = branchName.indexOf("/");
            branchName = branchName.substring(0, indexOfSlash) + "\\" + branchName.substring(indexOfSlash + 1);
        }
        return branchName;
    }

    /** The helper methods for checking the errors. */
    private static void checkNotExistSameFileInFolder(String fileName, File FOLDER, String message) {
        boolean existSameFile = false;
        for (String name : plainFilenamesIn(FOLDER)) {
            if (name.equals(fileName)) {
                existSameFile = true;
                break;
            }
        }
        if (!existSameFile) {
            printAndExit(message);
        }
    }

    private static void checkExistSameFileInFolder(String fileName, File FOLDER, String message) {
        for (String name : plainFilenamesIn(FOLDER)) {
            if (name.equals(fileName)) {
                printAndExit(message);
            }
        }
    }

    private static void checkNotExistSameFileInCommit(String fileName, Commit commit, String message) {
        boolean fileExist = false;
        for (String name : commit.getCopiedFileNames()) {
            if (fileName.equals(name)) {
                fileExist = true;
                break;
            }
        }
        if (!fileExist) {
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
        Pointer HEAD = readObject(join(GITLET_DIR, HEADNAME), Pointer.class);
        return HEAD.getInitCommitID();
    }

    public static String extractHEADThenGetActiveBranchName() {
        Pointer HEAD = readObject(join(GITLET_DIR, HEADNAME), Pointer.class);
        return HEAD.getActiveBranchName();
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
        Pointer HEAD = new Pointer(true, activeBranchName, initCommitID);
        HEAD.saveHEADFile();
    }
}
