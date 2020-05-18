package gitlet;

import java.io.File;
import java.io.FilenameFilter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Date;
import java.util.HashSet;

/** Contains all the commands that gitlet is capable of utilizing.
 *  @author Super Stressed McSad */
public class Commands {

    /** The .gitlet foler. */
    private static File gitlet = Utils.join(Main.CWD, ".gitlet");

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

    /** Creates a new .gitlet directory in the current
     *  folder if one doesn't already exist. */
    public static void init() {

        if (gitlet.exists()) {
            Utils.message("A Gitlet version-control system "
                    + "already exists in the current directory.");
            System.exit(0);
        } else {
            gitlet.mkdirs();

            stage.mkdirs();

            blobs.mkdirs();

            commits.mkdirs();
            File initialCommit = Utils.join(commits, Main.SHA1_INITIAL_COMMIT);
            Utils.writeObject(initialCommit, Main.INITIAL_COMMIT);

            refs.mkdirs();
            File master = Utils.join(refs, "master");
            Utils.writeObject(master, Main.SHA1_INITIAL_COMMIT);

            Utils.writeObject(headRef, "master");

            Utils.writeObject(head, Main.INITIAL_COMMIT);
        }

    }

    /** Adds a file NAME to the staging area. */
    public static void add(String name) {
        File toAdd = Utils.join(Main.CWD, name);
        if (!toAdd.exists()) {
            Utils.message("File does not exist.");
            System.exit(0);
        }

        String sha1ToAdd = Utils.sha1(Utils.readContentsAsString(toAdd));
        Commit current = Utils.readObject(head, Commit.class);
        String sha1Committed = current.getBlobs().get(name);
        File staged = Utils.join(stage, name);
        String sha1Staged = null;
        HashSet<String> rms = current.getRms();
        if (rms.contains(name)) {
            rms.remove(name);
            Utils.writeObject(head, current);
        }

        if (staged.exists()) {
            sha1Staged = Utils.readObject(staged, String.class);
        }

        if (sha1Committed != null
                && sha1ToAdd.compareTo(sha1Committed) == 0) {
            if (staged.exists()) {
                Utils.restrictedDelete(staged);
            }
        } else if (sha1Staged != null
                && sha1Staged.compareTo(sha1ToAdd) != 0) {
            Utils.restrictedDelete(Utils.join(blobs, sha1Staged));
            Utils.writeObject(staged, sha1ToAdd);
            Utils.writeContents(Utils.join(blobs, sha1ToAdd),
                    Utils.readContents(toAdd));
        } else if (!staged.exists()) {
            Utils.writeObject(staged, sha1ToAdd);
            Utils.writeContents(Utils.join(blobs, sha1ToAdd),
                    Utils.readContents(toAdd));
        }
    }

    /** Saves a snapshot of the working directory, dependent on
     * the staged files. Will contain the commit message MSG. For merges,
     * PARENT1 and PARENT2 commits are given.*/
    public static void commit(String msg, String parent1, String parent2) {

        Commit current = Utils.readObject(head, Commit.class);
        HashSet<String> rms = current.getRms();
        String[] updates = stage.list();
        if (updates.length == 0 && rms.size() == 0) {
            Utils.message("No changes added to the commit.");
            System.exit(0);
        } else if (msg.length() == 0) {
            Utils.message("Please enter a commit message.");
            System.exit(0);
        }

        String currentBranch = Utils.readObject(headRef, String.class);
        HashMap<String, String> old = current.getBlobs();
        HashMap<String, String> fresh = new HashMap<>(old);

        for (String name : rms) {
            fresh.remove(name);
        }

        for (String name : updates) {
            fresh.put(name, Utils.readObject(Utils.join(stage, name),
                    String.class));
            Utils.join(stage, name).delete();
        }


        if (parent2 != null) {
            Commit newHead = new Commit(msg, currentBranch, new Date(), "MP",
                    parent1, parent2, fresh, new HashSet<String>());
        } else {
            Commit newHead = new Commit(msg, currentBranch, new Date(), "MP",
                    Utils.readObject(Utils.join(refs, currentBranch),
                    String.class), null, fresh, new HashSet<String>());
        }

        Commit newHead = new Commit(msg, currentBranch, new Date(),
                "MP", Utils.readObject(Utils.join(refs, currentBranch),
                String.class), null, fresh, new HashSet<String>());

        String newSha1 = Utils.sha1(Utils.serialize(newHead));
        Utils.writeObject(head, newHead);
        Utils.writeObject(Utils.join(commits, newSha1), newHead);
        Utils.writeObject(Utils.join(refs, currentBranch), newSha1);

        Utils.readObject(head, Commit.class);

    }

    /** Reverts to a previous file version of NAME. Uses commit ID,
     * or head commit as default. */
    public static void checkoutFile(String name, String id) {
        Commit revert = null;
        if (id == null) {
            revert = Utils.readObject(head, Commit.class);
        } else {
            for (String commit : commits.list()) {
                if (id.equals(commit.substring(0, id.length()))) {
                    revert = Utils.readObject(Utils.join(commits, commit),
                            Commit.class);
                    break;
                }
            }
            if (revert == null) {
                Utils.message("No commit with that id exists.");
                System.exit(0);
            }
        }

        String sha1File = revert.getBlobs().get(name);

        if (sha1File == null) {
            Utils.message("File does not exist in that commit.");
            System.exit(0);
        }

        File realFile = Utils.join(Main.CWD, name);
        Utils.writeContents(realFile, Utils.readContents(
                Utils.join(blobs, sha1File)));
    }

    /** Reverts working directory to the head commit of BRANCH. */
    public static void checkoutBranch(String branch) {
        if (Utils.readObject(headRef, String.class).equals(branch)) {
            Utils.message(" No need to checkout the current branch.");
            System.exit(0);
        }

        File newBranch = Utils.join(refs, branch);
        if (!newBranch.exists()) {
            Utils.message("No such branch exists.");
            System.exit(0);
        }
        String sha1newBranch = Utils.readObject(newBranch, String.class);

        Commit commit = Utils.readObject(Utils.join(commits,
                sha1newBranch), Commit.class);
        HashMap<String, String> newFiles = commit.getBlobs();
        HashMap<String, String> trackedFiles = Utils.readObject(head,
                Commit.class).getBlobs();

        for (String name : Main.CWD.list(new FileCheck())) {
            if (!trackedFiles.containsKey(name)
                    && !Utils.sha1(Utils.readContentsAsString(Utils.join(
                            Main.CWD, name))).equals(newFiles.get(name))) {
                Utils.message("There is an untracked file in the way;"
                        + " delete it or add it first.");
                System.exit(0);
            }
        }

        for (String name : Main.CWD.list(new FileCheck())) {
            if (!newFiles.containsKey(name)) {
                Utils.join(Main.CWD, name).delete();
            }
        }

        for (String name : newFiles.keySet()) {
            Utils.writeContents(Utils.join(Main.CWD, name),
                    Utils.readContents(Utils.join(blobs, newFiles.get(name))));
        }



        Utils.writeObject(head, commit);
        Utils.writeObject(headRef, branch);
        Utils.writeObject(newBranch, sha1newBranch);
    }

    /** Prints out the past commits in the current branch. */
    public static void log() {
        Commit first = Utils.readObject(head, Commit.class);
        String id = Utils.readObject(Utils.join(refs,
                Utils.readObject(headRef, String.class)), String.class);

        while (id != null) {
            System.out.println("===");
            System.out.println("commit " + id);
            System.out.println("Date: " + new SimpleDateFormat(
                    "EEE MMM d HH:mm:ss yyyy Z").format(first.getDate()));
            System.out.println(first.getMsg());
            System.out.println();
            id = first.getParent();
            if (id != null) {
                first = Utils.readObject(Utils.join(commits, id), Commit.class);
            }
        }
    }

    /** Removes the file NAME. */
    public static void rm(String name) {
        Commit current = Utils.readObject(head, Commit.class);
        boolean tracked = current.getBlobs().containsKey(name);
        boolean staged = Utils.join(stage, name).exists();

        if (!staged && !tracked) {
            Utils.message("No reason to remove the file.");
            System.exit(0);
        }

        if (staged) {
            Utils.join(blobs, Utils.readObject(Utils.join(stage, name),
                    String.class)).delete();
            Utils.join(stage, name).delete();
        }

        if (tracked) {
            current.getRms().add(name);
            Utils.writeObject(head, current);
            Utils.restrictedDelete(Utils.join(Main.CWD, name));
        }
    }

    /** Prints out all past commits that have ever been made. */
    public static void gloablLog() {

        for (String name : commits.list()) {
            Commit commit = Utils.readObject(Utils.join(commits, name),
                    Commit.class);
            System.out.println("===");
            System.out.println("commit " + name);
            System.out.println("Date: " + new SimpleDateFormat(
                    "EEE MMM d HH:mm:ss yyyy Z").format(commit.getDate()));
            System.out.println(commit.getMsg());
            System.out.println();
        }
    }

    /** Prints out the sha1 ID of any commit with the commit message, MSG. */
    public static void find(String msg) {
        boolean found = false;

        for (String name : commits.list()) {
            Commit commit = Utils.readObject(Utils.join(commits, name),
                    Commit.class);
            if (commit.getMsg().equals(msg)) {
                System.out.println(name);
                found = true;
            }
        }

        if (!found) {
            Utils.message("Found no commit with that message.");
            System.exit(0);
        }
    }

    /** Creates a new branch named NAME. */
    public static void branch(String name) {
        File branch = Utils.join(refs, name);
        if (branch.exists()) {
            Utils.message("A branch with that name already exists.");
            System.exit(0);
        }

        String sha1Current = Utils.readObject(Utils.join(refs,
                Utils.readObject(headRef,
                String.class)), String.class);
        Utils.writeObject(branch, sha1Current);
    }

    /** Reverts to a past commit identified by ID.
     * Also moves to the branch of that commit. */
    public static void reset(String id) {

        File revert = Utils.join(commits, id);
        String sha1commit = id;
        if (!revert.exists()) {
            for (String name : commits.list()) {
                if (id.equals(name.substring(0, id.length()))) {
                    revert = Utils.join(commits, name);
                    sha1commit = name;
                    break;
                }
            }
            if (!revert.exists()) {
                Utils.message("No commit with that id exists.");
                System.exit(0);
            }
        }
        Commit commit = Utils.readObject(revert, Commit.class);
        HashMap<String, String> newFiles = commit.getBlobs();
        HashMap<String, String> trackedFiles =
                Utils.readObject(head, Commit.class).getBlobs();

        for (String name : Main.CWD.list(new FileCheck())) {
            if (!trackedFiles.containsKey(name)
                    && !Utils.join(stage, name).exists()
                    && !Utils.sha1(Utils.readContentsAsString(Utils.join(
                            Main.CWD, name))).equals(newFiles.get(name))) {
                Utils.message("There is an untracked file in the way;"
                        + " delete it or add it first.");
                System.exit(0);
            }
        }

        for (String name : newFiles.keySet()) {
            Utils.writeContents(Utils.join(Main.CWD, name),
                    Utils.readContents(Utils.join(blobs,
                            newFiles.get(name))));
        }

        for (String name : Main.CWD.list(new FileCheck())) {
            if (!newFiles.containsKey(name)) {
                Utils.restrictedDelete(Utils.join(Main.CWD, name));
            }
        }

        for (File file : stage.listFiles()) {
            file.delete();
        }

        Utils.writeObject(head, commit);
        File branch = Utils.join(refs, Utils.readObject(headRef, String.class));
        Utils.writeObject(branch, sha1commit);

    }

    /** Deletes the branch NAME. */
    public static void rmBranch(String name) {
        if (Utils.readObject(headRef, String.class).equals(name)) {
            Utils.message("Cannot remove the current branch.");
            System.exit(0);
        }

        for (String branch : refs.list()) {
            if (branch.equals(name)) {
                Utils.join(refs, name).delete();
                return;
            }
        }

        Utils.message("A branch with that name does not exist.");
        System.exit(0);
    }

    /** Reports the tracking status of files in the working directory. */
    public static void status() {
        String currentBranch = Utils.readObject(headRef, String.class);
        Commit current = Utils.readObject(head, Commit.class);
        String[] branches = refs.list();
        lexicoSort(branches, branches.length);
        String[] staged = stage.list();
        ArrayList<String> stagedList = new ArrayList<String>();
        lexicoSort(staged, staged.length);
        Object[] removed = current.getRms().toArray();
        ArrayList<String> removedList = new ArrayList<String>();
        lexicoSort(removed, removed.length);
        HashMap<String, String> tracked = current.getBlobs();
        ArrayList<String> modefiedList = new ArrayList<String>();
        ArrayList<String> untrackedList = new ArrayList<String>();
        ArrayList<String> workingList = new ArrayList<String>();
        listBranches(currentBranch);
        stagedList = listStaged(stagedList);
        removedList = listRemoved(removedList, current);
        System.out.println("=== Modifications Not Staged For Commit ===");
        for (String name : Main.CWD.list(new FileCheck())) {
            workingList.add(name);
            String sha1Wroking = Utils.sha1(
                    Utils.readContentsAsString(Utils.join(Main.CWD, name)));
            if (!tracked.containsKey(name) && !stagedList.contains(name)) {
                untrackedList.add(name);
            } else if (tracked.containsKey(name)
                    && !tracked.get(name).equals(sha1Wroking)
                    && !stagedList.contains(name)) {
                modefiedList.add(name);
            }
        }
        for (String name : staged) {
            if (!workingList.contains(name)) {
                modefiedList.add(name);
            } else if (!Utils.readObject(Utils.join(stage, name),
                    String.class).equals(Utils.sha1(Utils.readContentsAsString(
                            Utils.join(Main.CWD, name))))) {
                modefiedList.add(name);
            }
        }
        for (String name : tracked.keySet()) {
            if (!workingList.contains(name) && !removedList.contains(name)) {
                modefiedList.add(name);
            }
        }
        Object[] modefied = modefiedList.toArray();
        lexicoSort(modefied, modefied.length);

        for (Object name: modefied) {
            System.out.println(name);
        }
        System.out.println();
        System.out.println("=== Untracked Files ===");
        Object[] untracked = untrackedList.toArray();
        lexicoSort(untracked, untracked.length);
        for (Object name : untracked) {
            System.out.println(name);
        }
        System.out.println();
    }

    /** Helper for status that lists all the
     * branches. includes an * for the CURRENTBRANCH. */
    public static void listBranches(String currentBranch) {
        System.out.println("=== Branches ===");
        for (String name : refs.list()) {
            if (name.equals(currentBranch)) {
                System.out.print("*");
            }
            System.out.println(name);
        }
        System.out.println();
    }

    /** Helper for status that lists all the staged
     * files. Returns a list of Staged files, STAGEDLIST. */
    public static ArrayList<String> listStaged(ArrayList<String> stagedList) {
        System.out.println("=== Staged Files ===");
        for (String name : stage.list()) {
            System.out.println(name);
            stagedList.add(name);
        }
        System.out.println();
        return stagedList;
    }

    /** Helper function of status that lists all the
     * Removed files. Return list REMOVEDLIST. Uses CURRENT commit. */
    public static ArrayList<String> listRemoved(ArrayList<String> removedList,
                                                Commit current) {
        System.out.println("=== Removed Files ===");
        for (Object name : current.getRms()) {
            removedList.add((String) name);
            System.out.println(name);
        }
        System.out.println();
        return removedList;
    }


    /** Merges GIVENBRANCH with the current branch. */
    public static void merge(String givenBranch) {
        mergeFailures(givenBranch);
        Commit split = findSplit(givenBranch);
        boolean conflict = false;
        String currentSha1 = Utils.readObject(Utils.join(refs,
                Utils.readObject(headRef, String.class)), String.class);
        String currentBranch = Utils.readObject(headRef, String.class);
        Commit current = Utils.readObject(head, Commit.class);
        String givenSha1 = Utils.readObject(Utils.join(refs,
                givenBranch), String.class);
        Commit given = Utils.readObject(Utils.join(commits,
                givenSha1), Commit.class);

        HashMap<String, String> currentTracked = current.getBlobs();
        HashMap<String, String> givenTracked = given.getBlobs();
        HashMap<String, String> splitTracked = split.getBlobs();

        for (String name : splitTracked.keySet()) {
            if (!givenTracked.containsKey(name) && splitTracked.get(
                    name).equals(currentTracked.get(name))) {
                rm(name);
            } else if (!splitTracked.get(name).equals(givenTracked.get(name))
                    && splitTracked.get(
                    name).equals(currentTracked.get(name))) {
                checkoutFile(name, givenSha1);
                add(name);
            } else if (isConf(name, currentTracked,
                    givenTracked, splitTracked)) {
                conflict = true;
            }
        }

        for (String name : givenTracked.keySet()) {
            if (!splitTracked.containsKey(name)
                    && !currentTracked.containsKey(name)) {
                checkoutFile(name, givenSha1);
                add(name);
            } else if (!splitTracked.containsKey(name)
                    && currentTracked.containsKey(name) && !givenTracked.get(
                            name).equals(currentTracked.get(name))) {
                String cur = Utils.readContentsAsString(Utils.join(blobs,
                        currentTracked.get(name)));
                String giv = Utils.readContentsAsString(Utils.join(blobs,
                        givenTracked.get(name)));
                Utils.writeContents(Utils.join(Main.CWD, name),
                        "<<<<<<< HEAD" + cur + "=======" + giv + ">>>>>>>");
                add(name);
                conflict = true;
            }
        }

        commit("Merged " + givenBranch + " into " + currentBranch
                + ".", currentSha1, givenSha1);

        if (conflict) {
            Utils.message("Encountered a merge conflict.");
            System.exit(0);
        }

    }

    /** Yet another helper method for merge. Returns true if the file NAME
     * would result in a conflict with CURRENTTRACKED, GIVENTRACKED, and
     * SPLITTRACKED.*/
    static boolean isConf(String name, HashMap<String, String> currentTracked,
                          HashMap<String, String> givenTracked,
                          HashMap<String, String> splitTracked) {

        if ((!givenTracked.containsKey(name) && currentTracked.containsKey(
                name) && !splitTracked.get(name).equals(currentTracked.get(
                name))) || (givenTracked.containsKey(name)
                && !splitTracked.get(name).equals(givenTracked.get(name))
                && !currentTracked.containsKey(name))
                || (givenTracked.containsKey(name)
                && currentTracked.containsKey(name)
                && !splitTracked.get(name).equals(currentTracked.get(name))
                && !splitTracked.get(name).equals(givenTracked.get(name)))) {
            String cur = "";
            String giv = "";
            if (givenTracked.containsKey(name)) {
                giv = Utils.readContentsAsString(Utils.join(blobs,
                        givenTracked.get(name)));
            }
            if (currentTracked.containsKey(name)) {
                cur = Utils.readContentsAsString(Utils.join(blobs,
                        currentTracked.get(name)));
            }
            Utils.writeContents(Utils.join(Main.CWD, name),
                    "<<<<<<< HEAD\n" + cur + "=======\n" + giv + ">>>>>>>");
            add(name);
            return true;
        }
        return false;

    }

    /** Helper method to merge that returns TRUE if the commands should
     * exit before any execution. Merge is with the branch BRANCH that has
     * a split point at SPLIT. */
    public static void mergeFailures(String branch) {
        if (!Utils.join(refs, branch).exists()) {
            Utils.message("A branch with that name does not exist.");
            System.exit(0);
        }

        String currentBranch = Utils.readObject(headRef, String.class);
        Commit current = Utils.readObject(head, Commit.class);
        String sha1given = Utils.readObject(Utils.join(refs,
                branch), String.class);
        Commit given = Utils.readObject(Utils.join(commits,
                sha1given), Commit.class);

        if (currentBranch.equals(branch)) {
            Utils.message("Cannot merge a branch with itself.");
            System.exit(0);
        }

        if (stage.list().length + current.getRms().size()
                + given.getRms().size() > 0) {
            Utils.message("You have uncommitted changes.");
            System.exit(0);
        }

        HashMap currentTracked = current.getBlobs();
        HashMap givenTracked = given.getBlobs();
        for (String name : Main.CWD.list()) {
            if (!currentTracked.containsKey(name)) {
                if (givenTracked.containsKey(name) && !givenTracked.get(
                        name).equals(Utils.sha1(Utils.readContentsAsString(
                                Utils.join(Main.CWD, name))))) {
                    Utils.message("There is an untracked file in the way;"
                            + " delete it or add it first.");
                    System.exit(0);
                }
            }
        }
    }

    /** Helper function to merge that returns latest common
     * ancestor COMMIT between the current branch and GIVENBRANCH. */
    public static Commit findSplit(String givenBranch) {
        Commit current = Utils.readObject(head, Commit.class);
        String givenSha1 = Utils.readObject(Utils.join(refs,
                givenBranch), String.class);
        Commit given = Utils.readObject(Utils.join(commits,
                givenSha1), Commit.class);
        String currentSha1 = Utils.readObject(Utils.join(refs, Utils.readObject(
                headRef, String.class)), String.class);

        if (currentSha1.equals(givenSha1)) {
            Utils.message("Given branch is an ancestor of the current branch.");
            System.exit(0);
        }

        HashSet<String> givenTree = new HashSet<String>();
        givenTree.add(givenSha1);
        findAncestors(givenTree, givenSha1);
        Commit temp = current;
        String splitSha1 = null;
        HashMap<Integer, String> splits = new HashMap<Integer, String>();
        fS(splits, givenTree, currentSha1, 0);

        int min = Integer.MAX_VALUE;
        for (int x : splits.keySet()) {
            if (x < min) {
                min = x;
            }
        }
        splitSha1 = splits.get(min);

        if (splitSha1.equals(givenSha1)) {
            Utils.message("Given branch is an ancestor of the current branch.");
            System.exit(0);
        }
        if (splitSha1.equals(currentSha1)) {
            Utils.writeObject(head, temp);
            Utils.writeObject(Utils.join(refs, Utils.readObject(
                    headRef, String.class)), splitSha1);
            Utils.message("Current branch fast-forwarded.");
            System.exit(0);
        }

        return Utils.readObject(Utils.join(commits, splitSha1), Commit.class);
    }

    /** Finds all the ancestors of ID, add them to set,
     * and returns ANCESTORS. */
    public static HashSet<String> findAncestors(HashSet<String> ancestors,
                                                String id) {
        Commit temp = Utils.readObject(Utils.join(commits, id), Commit.class);

        while (temp.getParent() != null) {
            ancestors.add(temp.getParent());
            if (temp.getParent2() != null) {
                ancestors.add(temp.getParent2());
                findAncestors(ancestors, temp.getParent2());
            }
            temp = Utils.readObject(Utils.join(commits,
                    temp.getParent()), Commit.class);
        }
        return ancestors;
    }

    /** Finds all the possible splits from ID and returns SPLITS
     * which contains a DISTANCE. ANCESTORS is something. DIST
     * is something. */
    static HashMap<Integer, String> fS(HashMap<Integer, String> splits,
                                              HashSet<String> ancestors,
                                              String id,
                                              int dist) {

        Commit temp = Utils.readObject(head, Commit.class);
        String splitSha1 = id;
        while (temp.getParent() != null) {
            if (ancestors.contains(temp.getParent())) {
                splits.put(dist + 1, temp.getParent());
                break;
            } else if (ancestors.contains(temp.getParent2())) {
                splits.put(dist + 1, temp.getParent());
                break;
            } else {
                if (temp.getParent2() != null) {
                    fS(splits, ancestors, temp.getParent2(), dist + 1);
                }
                temp = Utils.readObject(Utils.join(commits,
                        temp.getParent()), Commit.class);
                dist++;
            }
        }

        return splits;
    }

    /** Helper function for status that sorts LIST of length N
     * to be in lexicographic order. */
    private static void lexicoSort(Object[] list, int n) {
        if (n < 2) {
            return;
        }
        int mid = n / 2;
        String[] l = new String[mid];
        String[] r = new String[n - mid];

        for (int i = 0; i < mid; i++) {
            l[i] = (String) list[i];
        }
        for (int i = mid; i < n; i++) {
            r[i - mid] = (String) list[i];
        }
        lexicoSort(l, mid);
        lexicoSort(r, n - mid);

        lexicoMerge(list, l, r, mid, n - mid);
    }

    /** Helper function for lexicoSort that merges L or length LEFT with
     *  R of length RIGHT. The merged list is written into LIST. */
    private static void lexicoMerge(Object[] list, String[] l,
                                    String[] r, int left, int right) {
        int i = 0, j = 0, k = 0;
        while (i < left && j < right) {
            if (l[i].compareTo(r[j]) <= 0) {
                list[k++] = l[i++];
            } else {
                list[k++] = r[j++];
            }
        }
        while (i < left) {
            list[k++] = l[i++];
        }
        while (j < right) {
            list[k++] = r[j++];
        }
    }

    /** A filter for listing only non-directory files. */
    public static class FileCheck implements FilenameFilter {

        /** Returns TRUE if a file with name NAME in directory
         *  FILE is not a directory. */
        public boolean accept(File file, String name) {
            return Utils.join(file, name).isFile();
        }
    }


}
