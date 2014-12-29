import org.eclipse.jgit.lib.ObjectId;
import org.kohsuke.github.GHCommitState;
import org.jenkinsci.plugins.github.util.BuildDataHelper;
import com.cloudbees.jenkins.GitHubRepositoryNameContributor;

def build_env_vars = build.getEnvVars()
def result = build.getResult()

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

try {
    def sha1 = ObjectId.toString(BuildDataHelper.getCommitSHA1(build));
} catch(IOException e) {
    def sha1 = build_env_vars['GIT_COMMIT']
}

GitHubRepositoryNameContributor.parseAssociatedNames(project).each {
    it.resolve().each {
        def status_result = it.createCommitStatus(
            sha1,
            state,
            build.getAbsoluteUrl(),
            build.getFullDisplayName(),
            '$commit_status_context'
        )
    }
}
