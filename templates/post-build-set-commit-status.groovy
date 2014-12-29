import hudson.model.Result;
import org.kohsuke.github.GHCommitState;
import com.cloudbees.jenkins.GitHubRepositoryNameContributor;

def result = manager.build.getResult()

try {
    def commit_status_context = manager.envVars['COMMIT_STATUS_CONTEXT']
} catch(e) {
    def commit_status_context = 'default'
}

def state = GHCommitState.ERROR;

if (result == null) { // Build is ongoing
    state = GHCommitState.PENDING;
} else if (result.isBetterOrEqualTo(Result.SUCCESS)) {
    state = GHCommitState.SUCCESS;
} else if (result.isBetterOrEqualTo(Result.UNSTABLE)) {
    state = GHCommitState.FAILURE;
}

def project = manager.build.getProject()

GitHubRepositoryNameContributor.parseAssociatedNames(project).each {
    it.resolve().each {
        def status_result = it.createCommitStatus(
            manager.build.getBuildVariables()['GIT_COMMIT'],
            state,
            manager.build.getAbsoluteUrl(),
            manager.build.getFullDisplayName(),
            commit_status_context
        )
    }
}
