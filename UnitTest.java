package gitlet;

import ucb.junit.textui;
import org.junit.Test;
import static org.junit.Assert.*;
import java.io.File;

/** The suite of all JUnit tests for the gitlet package.
 *  @author
 */
public class UnitTest {

    /** Testing file. */
    private static final File CWD = new File(".");

    /** The .gitlet foler. */
    private static File gitlet = Utils.join(new File(CWD, ".gitlet"));

    /** The commits folder which holds a list of all
     * the commits that have been made. */
    private static File commits = Utils.join(gitlet, "commits");

    /** The staging folder. */
    private static File stage = Utils.join(gitlet, "stage");

    /** The current branch. */
    private static File headRef = Utils.join(gitlet, "HEAD_REF");

    /** The current commit. */
    private static File head = Utils.join(gitlet, "HEAD");

    /** Instantiates the references folder which contins a list of all
     * the branches and their head commits. */
    private static File refs = Utils.join(gitlet, "refs");

    /** The folder containing all of the blobs that have ever been staged. */
    private static File blobs = Utils.join(gitlet, "blobs");

    /** Run the JUnit tests in the loa package. Add xxxTest.class entries to
     *  the arguments of runClasses to run other JUnit tests. */
    public static void main(String[] ignored) {
        textui.runClasses(UnitTest.class);
    }

    @Test
    public void hardReset() {
        if (gitlet.exists()) {
            gitletReset();
        }
    }

    @Test
    public void initTest() {
        Main.main("init");
        assertTrue(Utils.join(CWD, ".gitlet").exists());
        gitletReset();
    }

    @Test
    public void addTest() {
        Main.main("init");
        Utils.writeContents(Utils.join(CWD, "wug.txt"),
                Utils.readContents(getFile("wug.txt")));
        assertEquals(blobs.list().length, 0);
        Main.main("add", "wug.txt");
        assertEquals(blobs.list().length, 1);
        gitletReset();
    }

    @Test
    public void simpleCommitTest() {
        Main.main("init");
        Utils.writeContents(Utils.join(CWD, "wug.txt"),
                Utils.readContents(getFile("wug.txt")));
        Main.main("add", "wug.txt");
        assertEquals(commits.list().length, 1);
        Main.main("commit", "added wug1");
        assertEquals(commits.list().length, 2);
        gitletReset();
    }

    @Test
    public void simpleCheckoutTest() {
        Main.main("init");
        Utils.writeContents(Utils.join(CWD, "wug.txt"),
                Utils.readContents(getFile("wug.txt")));
        Main.main("add", "wug.txt");
        String sha1old = Utils.sha1(Utils.readContentsAsString(
                Utils.join(CWD, "wug.txt")));
        Main.main("commit", "added wug1");
        Utils.writeContents(Utils.join(CWD, "wug.txt"),
                Utils.readContents(getFile("wug2.txt")));
        String sha1new = Utils.sha1(Utils.readContentsAsString(Utils.join(
                CWD, "wug.txt")));
        assertNotEquals(sha1new, sha1old);
        Main.main("checkout", "--", "wug.txt");
        String sha1new2 = Utils.sha1(Utils.readContentsAsString(Utils.join(
                CWD, "wug.txt")));
        assertEquals(sha1new2, sha1old);
        gitletReset();
    }

    @Test
    public void simpleBranchTest() {
        Main.main("init");
        Utils.writeContents(Utils.join(CWD, "wug.txt"),
                Utils.readContents(getFile("wug.txt")));
        Main.main("add", "wug.txt");
        Main.main("commit", "added wug1");
        Main.main("branch", "cool-beans");
        assertEquals(refs.list().length, 2);
        Main.main("checkout", "cool-beans");
        assertEquals(Utils.readObject(headRef, String.class), "cool-beans");
        gitletReset();
    }

    @Test
    public void branchCommitTest() {
        gitletReset();
        Main.main("init");
        Utils.writeContents(Utils.join(CWD, "wug.txt"),
                Utils.readContents(getFile("wug.txt")));
        Main.main("add", "wug.txt");
        Main.main("commit", "added wug1");
        String sha1one = Utils.sha1(Utils.readContentsAsString(
                Utils.join(CWD, "wug.txt")));
        String sha1commit1 = Utils.sha1(Utils.serialize(
                Utils.readObject(head, Commit.class)));
        Main.main("branch", "cool-beans");
        Main.main("checkout", "cool-beans");
        Utils.writeContents(Utils.join(CWD, "wug.txt"),
                Utils.readContents(getFile("wug2.txt")));
        Main.main("add", "wug.txt");
        Main.main("commit", "added wug2");
        String sha1three = Utils.sha1(Utils.readContentsAsString(
                Utils.join(CWD, "wug.txt")));
        String sha1commit3 = Utils.sha1(Utils.serialize(
                Utils.readObject(head, Commit.class)));
        Main.main("checkout", "master");
        String sha1two = Utils.sha1(Utils.readContentsAsString(Utils.join(
                CWD, "wug.txt")));
        String sha1commit2 = Utils.sha1(Utils.serialize(
                Utils.readObject(head, Commit.class)));

        assertEquals(sha1one, sha1two);
        assertEquals(sha1commit1, sha1commit2);
        assertNotEquals(sha1commit1, sha1commit3);

        Utils.writeContents(Utils.join(CWD, "wug.txt"),
                Utils.readContents(getFile("wug3.txt")));
        Main.main("add", "wug.txt");
        Main.main("commit", "added wug3");
        Main.main("checkout", "cool-beans");
        String sha1four = Utils.sha1(Utils.readContentsAsString(
                Utils.join(CWD, "wug.txt")));
        String sha1commit4 = Utils.sha1(Utils.serialize(
                Utils.readObject(head, Commit.class)));

        assertEquals(sha1three, sha1four);
        assertEquals(sha1commit4, sha1commit3);
        gitletReset();
    }

    @Test
    public void simpleRmTest() {
        gitletReset();
        Main.main("init");
        Utils.writeContents(Utils.join(CWD, "wug.txt"),
                Utils.readContents(getFile("wug.txt")));
        Main.main("add", "wug.txt");
        Main.main("commit", "added wug1");
        Utils.writeContents(Utils.join(CWD, "wug2.txt"),
                Utils.readContents(getFile("wug2.txt")));
        Utils.writeContents(Utils.join(CWD, "wug3.txt"),
                Utils.readContents(getFile("wug3.txt")));
        Main.main("add", "wug2.txt");
        Main.main("add", "wug3.txt");
        Main.main("commit", "added wug2 and wug3");
        Utils.writeContents(Utils.join(CWD, "wug2.txt"),
                Utils.readContents(getFile("wug4.txt")));
        Utils.writeContents(Utils.join(CWD, "wug3.txt"),
                Utils.readContents(getFile("notwug.txt")));
        Main.main("add", "wug2.txt");
        Main.main("add", "wug3.txt");
        assertEquals(5, blobs.list().length);
        Main.main("rm", "wug.txt");
        Main.main("rm", "wug2.txt");
        assertEquals(1, CWD.list(new Commands.FileCheck()).length);
        assertEquals(1, stage.list().length);
        assertEquals(4, blobs.list().length);
        assertEquals(2, Utils.readObject(head, Commit.class).getRms().size());
        Main.main("commit", "set back to just wug.txt");
        assertEquals(0, Utils.readObject(head, Commit.class).getRms().size());
        assertEquals(1, Utils.readObject(head,
                Commit.class).getBlobs().size());
        assertEquals(4, blobs.list().length);

        gitletReset();
    }

    @Test
    public void findTest() {
        gitletReset();
        Main.main("init");
        Utils.writeContents(Utils.join(CWD, "wug.txt"),
                Utils.readContents(getFile("wug.txt")));
        Main.main("add", "wug.txt");
        Main.main("commit", "added wug");
        Main.main("branch", "cool-beans");
        Main.main("checkout", "cool-beans");
        Utils.writeContents(Utils.join(CWD, "wug.txt"),
                Utils.readContents(getFile("wug2.txt")));
        Main.main("add", "wug.txt");
        Main.main("commit", "added wug");
        Main.main("checkout", "master");
        Utils.writeContents(Utils.join(CWD, "wug.txt"),
                Utils.readContents(getFile("wug3.txt")));
        Main.main("add", "wug.txt");
        Main.main("commit", "added wug3");
        Main.main("checkout", "cool-beans");
        Main.main("find", "added wug");
        System.out.println();
        Main.main("find", "added wug3");

        gitletReset();
    }

    @Test
    public void resetTest() {
        gitletReset();
        Main.main("init");
        Utils.writeContents(Utils.join(CWD, "wug.txt"),
                Utils.readContents(getFile("wug.txt")));
        Main.main("add", "wug.txt");
        Main.main("commit", "added wug1");
        Utils.writeContents(Utils.join(CWD, "wug2.txt"),
                Utils.readContents(getFile("wug2.txt")));
        Utils.writeContents(Utils.join(CWD, "wug3.txt"),
                Utils.readContents(getFile("wug3.txt")));
        Main.main("add", "wug2.txt");
        Main.main("add", "wug3.txt");
        Main.main("commit", "added wug2 and wug3");
        String sha1wug2one = Utils.sha1(Utils.readContentsAsString(
                Utils.join(CWD, "wug.txt")));
        String sha1wug3one = Utils.sha1(Utils.readContentsAsString(
                Utils.join(CWD, "wug.txt")));
        String sha1commit1 = Utils.readObject(Utils.join(refs,
                Utils.readObject(headRef, String.class)), String.class);
        Utils.writeContents(Utils.join(CWD, "wug2.txt"),
                Utils.readContents(getFile("wug4.txt")));
        Utils.writeContents(Utils.join(CWD, "wug3.txt"),
                Utils.readContents(getFile("notwug.txt")));
        Main.main("add", "wug2.txt");
        Main.main("add", "wug3.txt");
        Main.main("commit", "changed wug2 and wug3");
        Main.main("reset", sha1commit1);
        String sha1wug2two = Utils.sha1(Utils.readContentsAsString(
                Utils.join(CWD, "wug.txt")));
        String sha1wug3two = Utils.sha1(Utils.readContentsAsString(
                Utils.join(CWD, "wug.txt")));
        assertEquals(sha1wug2one, sha1wug2two);
        assertEquals(sha1wug3one, sha1wug3two);
        assertEquals(sha1commit1, Utils.readObject(Utils.join(refs,
                Utils.readObject(headRef, String.class)), String.class));
        gitletReset();
    }

    @Test
    public void rmBranchTest() {
        gitletReset();
        Main.main("init");
        Utils.writeContents(Utils.join(CWD, "wug.txt"),
                Utils.readContents(getFile("wug.txt")));
        Main.main("add", "wug.txt");
        Main.main("commit", "added wug");
        Main.main("branch", "cool-beans");
        Main.main("checkout", "cool-beans");
        Utils.writeContents(Utils.join(CWD, "wug.txt"),
                Utils.readContents(getFile("wug2.txt")));
        Main.main("add", "wug.txt");
        Main.main("commit", "added wug");
        Main.main("checkout", "master");
        Utils.writeContents(Utils.join(CWD, "wug.txt"),
                Utils.readContents(getFile("wug3.txt")));
        Main.main("add", "wug.txt");
        Main.main("commit", "added wug3");
        Main.main("rm-branch", "cool-beans");

        assertEquals(1, refs.list().length);

        gitletReset();
    }

    @Test
    public void statusTest() {
        gitletReset();
        Main.main("init");
        Utils.writeContents(Utils.join(CWD, "wug.txt"),
                Utils.readContents(getFile("wug.txt")));
        Main.main("add", "wug.txt");
        Main.main("commit", "added wug1");
        Utils.writeContents(Utils.join(CWD, "wug2.txt"),
                Utils.readContents(getFile("wug2.txt")));
        Utils.writeContents(Utils.join(CWD, "wug3.txt"),
                Utils.readContents(getFile("wug3.txt")));
        Main.main("add", "wug2.txt");
        Main.main("add", "wug3.txt");
        Main.main("commit", "added wug2 and wug3");
        Utils.writeContents(Utils.join(CWD, "wug2.txt"),
                Utils.readContents(getFile("wug4.txt")));
        Utils.writeContents(Utils.join(CWD, "wug3.txt"),
                Utils.readContents(getFile("notwug.txt")));
        Utils.writeContents(Utils.join(CWD, "wug4.txt"),
                Utils.readContents(getFile("wug4.txt")));
        Main.main("add", "wug2.txt");
        Main.main("add", "wug3.txt");
        Main.main("add", "wug4.txt");
        Main.main("commit", "changed wug2 and wug3 and added wug4");
        Utils.writeContents(Utils.join(CWD, "wug5.txt"),
                Utils.readContents(getFile("wug.txt")));
        Utils.writeContents(Utils.join(CWD, "wug4.txt"),
                Utils.readContents(getFile("wug.txt")));
        Main.main("add", "wug4.txt");
        Utils.writeContents(Utils.join(CWD, "wug4.txt"),
                Utils.readContents(getFile("wug4.txt")));
        Main.main("rm", "wug2.txt");
        Main.main("status");
        gitletReset();
    }

    @Test
    public void mergeTest() {
        gitletReset();
        Main.main("init");
        Utils.writeContents(Utils.join(CWD, "wug.txt"),
                Utils.readContents(getFile("wug.txt")));
        Utils.writeContents(Utils.join(CWD, "wug2.txt"),
                Utils.readContents(getFile("wug2.txt")));
        Main.main("add", "wug.txt");
        Main.main("add", "wug2.txt");
        Main.main("commit", "added wug1 and wug2");
        Main.main("branch", "cool-beans");
        Utils.writeContents(Utils.join(CWD, "wug3.txt"),
                Utils.readContents(getFile("wug3.txt")));
        Utils.writeContents(Utils.join(CWD, "wug4.txt"),
                Utils.readContents(getFile("wug4.txt")));
        Main.main("add", "wug3.txt");
        Main.main("add", "wug4.txt");
        Main.main("commit", "added wug3 and wug4");
        Main.main("checkout", "cool-beans");
        Utils.writeContents(Utils.join(CWD, "wug5.txt"),
                Utils.readContents(getFile("wug.txt")));
        Utils.writeContents(Utils.join(CWD, "wug2.txt"),
                Utils.readContents(getFile("notwug.txt")));
        Utils.writeContents(Utils.join(CWD, "wug3.txt"),
                Utils.readContents(getFile("wug3.txt")));
        Utils.writeContents(Utils.join(CWD, "wug4.txt"),
                Utils.readContents(getFile("wug4.txt")));
        Main.main("add", "wug2.txt");
        Main.main("add", "wug3.txt");
        Main.main("add", "wug4.txt");
        Main.main("add", "wug5.txt");
        Main.main("commit", "added wug3, wug4, wug5 and changed wug2");
        Main.main("checkout", "master");
        Main.main("merge", "cool-beans");

        gitletReset();
    }




    private File getFile(String name) {
        return Utils.join(Utils.join(Utils.join(new File(
                "."), "testing"), "src"), name);
    }

    private void gitletReset() {
        assertEquals(new File("."), CWD);
        Utils.join(CWD, "wug.txt").delete();
        Utils.join(CWD, "wug2.txt").delete();
        Utils.join(CWD, "wug3.txt").delete();
        Utils.join(CWD, "wug4.txt").delete();
        Utils.join(CWD, "wug5.txt").delete();
        Utils.join(CWD, "notwug.txt").delete();
        if (gitlet.exists()) {
            assertTrue(deleteDirectory(gitlet));
        }
    }

    private boolean deleteDirectory(File dir) {
        for (File file : dir.listFiles()) {
            if (file.isFile()) {
                file.delete();
            } else {
                deleteDirectory(file);
            }
        }
        return dir.delete();
    }

}


