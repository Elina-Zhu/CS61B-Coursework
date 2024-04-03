package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.List;

import static gitlet.Utils.join;
import static gitlet.Utils.plainFilenamesIn;

public class Remote implements Serializable {
    private String remoteName;
    private File remoteDir;

    public Remote(String remoteName, String dirPathString) {
        this.remoteName = remoteName;
        this.remoteDir = join(dirPathString);
    }

    public List<String> getBranchNames() {
        return plainFilenamesIn(join(remoteDir, "branch"));
    }

    public String getRemoteName() {
        return remoteName;
    }

    public File getRemoteDir() {
        return remoteDir;
    }
}
