import hudson.model.Result;
import org.eclipse.jgit.lib.ObjectId;
import org.kohsuke.github.GHCommitState;
import org.jenkinsci.plugins.github.util.BuildDataHelper;
import com.cloudbees.jenkins.GitHubRepositoryNameContributor;

def result = manager.build.getResult()

if (result == null) { // Build is ongoing
    def state = GHCommitState.PENDING;
} else if (result.isBetterOrEqualTo(Result.SUCCESS)) {
    def state = GHCommitState.SUCCESS;
} else if (result.isBetterOrEqualTo(Result.UNSTABLE)) {
    def state = GHCommitState.FAILURE;
} else {
    def state = GHCommitState.ERROR;
}

def project = manager.build.getProject()

try {
    def sha1 = ObjectId.toString(BuildDataHelper.getCommitSHA1(manager.build));
} catch(IOException e) {
    def sha1 = manager.envVars['GIT_COMMIT']
}

GitHubRepositoryNameContributor.parseAssociatedNames(project).each {
    it.resolve().each {
        def status_result = it.createCommitStatus(
            sha1,
            state,
            manager.build.getAbsoluteUrl(),
            manager.build.getFullDisplayName(),
            '$commit_status_context'
        )
    }
}
