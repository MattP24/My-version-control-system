package gitlet;

import java.io.File;

/** Driver class for Gitlet, the tiny stupid version-control system.
 *  @author Super Stressed McSad
 */
public class Main {

    /** Initial commit when a .gitlet repository is verse instantiated. */
    public static final Commit INITIAL_COMMIT = new Commit();

    /** Sha1 identifier for the initial commit. */
    public static final String SHA1_INITIAL_COMMIT =
            Utils.sha1(Utils.serialize(INITIAL_COMMIT));

    /** Current Working Directory. */
    static final File CWD = new File(".");

    /** Denoted the length of a sha! indentifier */
    public static final int SHA1_LENGTH = 40;

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND> .... */
    public static void main(String... args) {
        if (args.length == 0) {
            Utils.message("Please enter a command.");
            System.exit(0);
        } else if (args[0].compareTo("init") == 0) {
            checkArgs(1, args);
            Commands.init();
        } else if (!Utils.join(CWD, ".gitlet").exists()) {
            Utils.message("Not in an initialized gitlet directory.");
            System.exit(0);
        } else {
            runCommand(args);
        }
    }

    /** Helper function to determine which command
     * should be run based on ARGS. */
    private static void runCommand(String... args) {

        switch (args[0]) {
        case "add":
            checkArgs(2, args);
            Commands.add(args[1]);
            break;
        case "commit":
            checkArgs(2, args);
            Commands.commit(args[1], null, null);
            break;
        case "checkout":
            checkoutdecider(args);
            break;
        case "log":
            checkArgs(1, args);
            Commands.log();
            break;
        case "rm":
            checkArgs(2, args);
            Commands.rm(args[1]);
            break;
        case "global-log":
            checkArgs(1, args);
            Commands.gloablLog();
            break;
        case "find":
            checkArgs(2, args);
            Commands.find(args[1]);
            break;
        case "branch":
            checkArgs(2, args);
            Commands.branch(args[1]);
            break;
        case "reset":
            checkArgs(2, args);
            Commands.reset(args[1]);
            break;
        case "rm-branch":
            checkArgs(2, args);
            Commands.rmBranch(args[1]);
            break;
        case "status":
            checkArgs(1, args);
            Commands.status();
            break;
        case "merge":
            checkArgs(2, args);
            Commands.merge(args[1]);
            break;
        default:
            Utils.message("No command with that name exists."); }
    }

    /** Helper function to decide with checkout to call based on ARGS. */
    private static void checkoutdecider(String... args) {
        if (args.length == 2) {
            Commands.checkoutBranch(args[1]);
        } else if (args.length == 3 && args[1].equals("--")) {
            Commands.checkoutFile(args[2], null);
        } else if (args.length == 4 && args[2].equals("--")
                && args[1].length() <= SHA1_LENGTH) {
            Commands.checkoutFile(args[3], args[1]);
        } else {
            Utils.message("Incorrect operands.");
        }
    }

    /** A check to ensure that N number of ARGS are used for a command. */
    private static void checkArgs(int n, String... args) {
        if (args.length != n) {
            throw Utils.error("Incorrect operands.");
        }
    }

}
