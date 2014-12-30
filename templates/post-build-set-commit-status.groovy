import hudson.model.Result;
import org.kohsuke.github.GHCommitState;
import com.cloudbees.jenkins.GitHubRepositoryName;

def result = manager.build.getResult()

def commit_status_context = 'default'
try {
    commit_status_context = manager.envVars['COMMIT_STATUS_CONTEXT']
} catch(e) {
    println 'Defaulting to the "default" commit status context'
}

def state = GHCommitState.ERROR;

if (result == null) { // Build is ongoing
    state = GHCommitState.PENDING;
} else if (result.isBetterOrEqualTo(Result.SUCCESS)) {
    state = GHCommitState.SUCCESS;
} else if (result.isBetterOrEqualTo(Result.UNSTABLE)) {
    state = GHCommitState.FAILURE;
}

def repo = GitHubRepositoryName.create('https://github.com/' + manager.envVars['GITHUB_REPO'] + '.git')
repo.resolve().each {
    def status_result = it.createCommitStatus(
        currentBuild.getBuildVariables()['GIT_COMMIT'],
        state,
        currentBuild.getAbsoluteUrl(),
        currentBuild.getFullDisplayName(),
        commit_status_context
    )
}
