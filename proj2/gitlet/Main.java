package gitlet;

import java.util.regex.*;
import static gitlet.Utils.*;

/** Driver class for Gitlet, a subset of the Git version-control system.
 *  @author Yun Zhu
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND1> <OPERAND2> ... 
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            printAndExit("Please enter a command.");
        }

        String firstArg = args[0];
        switch(firstArg) {
            case "init":
                validateOperands("init", args, 1);
                Repository.init("initial commit");
                break;
            case "add":
                validateInitAndOperands("add", args, 2);
                Repository.add(args[1]);
                break;
            case "commit":
                validateInitAndOperands("commit", args, 2);
                Repository.commit(args[1]);
                break;
            case "rm":
                validateInitAndOperands("rm", args, 2);
                Repository.rm(args[1]);
                break;
            case "log":
                validateInitAndOperands("log", args, 1);
                Repository.log();
                break;
            case "global-log":
                validateInitAndOperands("global-log", args, 1);
                Repository.globalLog();
                break;
            case "find":
                validateInitAndOperands("global-log", args, 2);
                Repository.find(args[1]);
                break;
            case "status":
                validateInitAndOperands("status", args, 1);
                Repository.status();
                break;
            case "checkout":
                if (args.length == 2 || args.length == 3 || args.length == 4) {
                    validateInitAndOperands("checkout", args, args.length);
                } else {
                    printAndExit("Incorrect operands.");
                }
                Repository.checkout(args);
                break;
            case "branch":
                validateInitAndOperands("branch", args, 2);
                Repository.branch(args[1]);
                break;
            case "rm-branch":
                validateInitAndOperands("rm-branch", args, 2);
                Repository.rmBranch(args[1]);
                break;
            case "reset":
                validateInitAndOperands("reset", args, 2);
                Repository.reset(args[1]);
                break;
            case "merge":
                validateInitAndOperands("merge", args, 2);
                Repository.merge(args[1]);
                break;
            case "add-remote":
                validateInitAndOperands("add-remote", args, 3);
                Repository.addRemote(args[1], args[2]);
                break;
            case "rm-remote":
                validateInitAndOperands("rm-remote", args, 2);
                Repository.rmRemote(args[1]);
                break;
            case "push":
                validateInitAndOperands("push", args, 3);
                Repository.push(args[1], args[2]);
                break;
            case "fetch":
                validateInitAndOperands("fetch", args, 3);
                Repository.fetch(args[1], args[2]);
                break;
            case "pull":
                validateInitAndOperands("pull", args, 3);
                Repository.pull(args[1], args[2]);
                break;
            default:
                printAndExit("No command with that name exists.");
        }
    }

    public static void validateInitAndOperands(String cmd, String[] args, int n) {
        if (!MyUtils.validateInit()) {
            printAndExit("Not in an initialized Gitlet directory.");
        }

        validateOperands(cmd, args, n);
    }

    public static void validateOperands(String cmd, String[] args, int n) {
        if (args.length != n) {
            printAndExit("Incorrect operands.");
        }

        String firstArg = args[0];
        switch (firstArg) {
            case "add":
                matchFileName(args[1]);
                break;
            case "commit":
                if (args[1].equals("")) {
                    printAndExit("Please enter a commit message.");
                }
                matchMessage(args[1]);
                break;
            case "rm":
                matchFileName(args[1]);
                break;
            case "find":
                matchMessage(args[1]);
                break;
            case "checkout":
                // checkout [branch name]
                if (args.length == 2) {
                    matchBranchName(args[1]);
                }
                // checkout -- [file name]
                if (args.length == 3) {
                    matchTwoLines(args[1]);
                    matchFileName(args[2]);
                }
                // checkout [commit id] -- [file name]
                if (args.length == 4) {
                    matchCommitID(args[1]);
                    matchTwoLines(args[2]);
                    matchFileName(args[3]);
                }
                break;
            case "reset":
                matchCommitID(args[1]);
                break;
            case "branch":
                matchBranchName(args[1]);
                break;
            case "rm-branch":
                matchBranchName(args[1]);
                break;
            case "add-remote":
                matchRemoteName(args[1]);
                matchRemoteDirectory(args[2]);
                break;
            case "rm-remote":
                matchRemoteName(args[1]);
                break;
            case "fetch":
            case "push":
            case "pull":
                matchRemoteName(args[1]);
                matchBranchName(args[2]);
                break;
        }
    }

    private static void matchFileName(String fileName) {
        String fileNamePattern = "[^\\/\\\\\\:\\*\\\"\\>\\|\\?]+\\.[^\\/\\\\\\:\\*\\\"\\>\\|\\?]+";
        if (!Pattern.matches(fileNamePattern, fileName)) {
            printAndExit("Incorrect operands.");
        }
    }

    private static void matchMessage(String message) {
        String messagePattern = ".+";
        if (!Pattern.matches(messagePattern, message)) {
            printAndExit("Incorrect operands.");
        }
    }

    private static void matchTwoLines(String TwoLines) {
        String twoLinesPattern = "--";
        if (!Pattern.matches(twoLinesPattern, TwoLines)) {
            printAndExit("Incorrect operands.");
        }
    }

    private static void matchBranchName(String branchName) {
        String branchNamePattern = "[\\w\\d\\s\\/]+";
        if (!Pattern.matches(branchNamePattern, branchName)) {
            printAndExit("Incorrect operands.");
        }
    }

    private static void matchCommitID(String message) {
        String commitIDPattern = "[a-f\\d]+";
        if (!Pattern.matches(commitIDPattern, message)) {
            printAndExit("Incorrect operands.");
        }
    }

    private static void matchRemoteName(String remoteName) {
        matchBranchName(remoteName);
    }

    private static void matchRemoteDirectory(String dirName) {
        String dirNamePattern = "([\\w\\d\\.]+\\/?)+\\/.gitlet";
        if (!Pattern.matches(dirNamePattern, dirName)) {
            printAndExit("Incorrect operands.");
        }
    }
}
