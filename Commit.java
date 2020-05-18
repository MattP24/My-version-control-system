package gitlet;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;

/** Commit class.
 * @author Super Stressed McSad
 * */
public class Commit implements Serializable {

    /** Creates an initial commit. */
    public Commit() {
        _msg = "initial commit";
        _branch = "master";
        _date = new Date(0);
        _author = "Matt Panec";
        _parent1 = null;
        _parent2 = null;
        _blobs = new HashMap<>();
        _rms = new HashSet<String>();
    }

    /** Creates a commit object with message MSG, branch BRANCH, date DATE,
     *  author AUTHOR, parent commit PARENT, possible PARENT2,
     *  list of tracked files BLOBS,
     *  list of files to ignore for next commit RMS.*/
    public Commit(String msg, String branch, Date date, String author,
                  String parent, String parent2, HashMap<String,
            String> blobs, HashSet<String> rms) {
        _msg = msg;
        _branch = branch;
        _date = date;
        _author = author;
        _parent1 = parent;
        _parent2 = parent2;
        _blobs = blobs;
        _rms = rms;

    }

    /** Returns branch of commit. */
    public String getBranch() {
        return _branch;
    }

    /** Returns Date of commit. */
    public Date getDate() {
        return _date;
    }

    /** Returns log of commit. */
    public String getMsg() {
        return _msg;
    }

    /** Returns author of commit. */
    public String getAuthor() {
        return _author;
    }

    /** Returns parent commit of commit. */
    public String getParent() {
        return _parent1;
    }

    /** Returns parent2 commit of commit. */
    public String getParent2() {
        return _parent2;
    }

    /** Returns Hashmap detailing all blobs associated with this commit. */
    public HashMap<String, String> getBlobs() {
        return _blobs;
    }

    /** Returns Hashset containing all the files to be removed. */
    public HashSet<String> getRms() {
        return _rms;
    }

    /** Name of the Branch that this commit is associated with. */
    private String _branch;

    /** Date that this commit was created. */
    private Date _date;

    /** Message that describes this commit. */
    private String _msg;

    /** Creator of this commit. */
    private String _author;

    /** Sha1 of the first parent commit. */
    private String _parent1;

    /** Sha1 of the second parent commit. Derived from a merge. */
    private String _parent2;

    /** List of all the files (blobs) that are associated with this commit. */
    private HashMap<String, String> _blobs;

    /** List of files to be removed. */
    private HashSet<String> _rms;

}
