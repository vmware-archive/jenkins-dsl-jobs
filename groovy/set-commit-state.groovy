import hudson.model.*
import org.eclipse.jgit.lib.ObjectId;
import org.kohsuke.github.GHCommitState;
import org.jenkinsci.plugins.github.util.BuildDataHelper;
import com.cloudbees.jenkins.GitHubRepositoryNameContributor;

def thr = Thread.currentThread()
def build = thr?.executable

def result = build.getResult();
if (result == null) { // Build is ongoing
  def state = GHCommitState.PENDING;
} else if (result.isBetterOrEqualTo(SUCCESS)) {
  def state = GHCommitState.SUCCESS;
} else if (result.isBetterOrEqualTo(UNSTABLE)) {
  def state = GHCommitState.FAILURE;
} else {
  def state = GHCommitState.ERROR;
}

def project = build.getProject()
def sha1 = ObjectId.toString(BuildDataHelper.getCommitSHA1(build));

GitHubRepositoryNameContributor.parseAssociatedNames(project).each {
  it.resolve().each {
    //println it
    //println "foo"
    //println(it.getUrl() + "/commit/" + sha1);
    // public GHCommitStatus createCommitStatus(String sha1, GHCommitState state, String targetUrl, String description, String context)
    def status_result = it.createCommitStatus(sha1, state, build.getAbsoluteUrl(), build.getFullDisplayName(), project.getFullName())
    println status_result
    //println "bar"
  }
}
