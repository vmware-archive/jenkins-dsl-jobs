import hudson.model.Result;
import org.kohsuke.github.GHCommitState;
import com.cloudbees.jenkins.GitHubRepositoryName;
import com.coravy.hudson.plugins.github.GithubProjectProperty;

manager.listener.logger.println "Setting Commit Status to the current build result"

def result = manager.build.getResult()

def commit_status_context = manager.envVars.get('COMMIT_STATUS_CONTEXT', 'default')
manager.listener.logger.println "GitHub commit status context: " + commit_status_context

def state = GHCommitState.ERROR;

if (result == null) { // Build is ongoing
    state = GHCommitState.PENDING;
    manager.listener.logger.println 'GitHub commit status is PENDING'
} else if (result.isBetterOrEqualTo(Result.SUCCESS)) {
    state = GHCommitState.SUCCESS;
    manager.listener.logger.println 'GitHub commit status is SUCCESS'
} else if (result.isBetterOrEqualTo(Result.UNSTABLE)) {
    state = GHCommitState.FAILURE;
    manager.listener.logger.println 'GitHub commit status is FAILURE'
} else {
    manager.listener.logger.println 'GitHub commit status is ERROR'
}

def github_repo_url = manager.build.getProperty(GithubProjectProperty.class).getProjectUrl()

if ( github_repo_url != null ) {
    manager.listener.logger.println 'GitHub Repository URL: ' + github_repo_url
    repo = GitHubRepositoryName.create(github_repo_url)
    if ( repo != null ) {
        def git_commit = manager.build.getBuildVariables()['GIT_COMMIT']
        repo.resolve().each {
            def status_result = it.createCommitStatus(
                git_commit,
                state,
                manager.build.getAbsoluteUrl(),
                manager.build.getFullDisplayName(),
                commit_status_context
            )
            if ( ! status_result ) {
                msg = 'Failed to set commit status on GitHub'
                manager.createSummary('warning.gif').appendText(msg)
                manager.listener.logger.println msg
            } else {
                msg = "GitHub commit status successfuly set"
                manager.createSummary('info.gif').appendText(msg)
                manager.listener.logger.println(msg)
            }
        }
    } else {
        msg = "Failed to resolve the github GIT repo URL from " + github_repo_url
        manager.createSummary('warning.gif').appendText(msg)
        manager.listener.logger.println msg
    }
} else {
    msg = "Unable to find the GitHub project URL from the build's properties"
    manager.createSummary('warning.gif').appendText(msg)
    manager.listener.logger.println msg
}
