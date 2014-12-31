import hudson.model.Result;
import org.kohsuke.github.GHCommitState;
import com.cloudbees.jenkins.GitHubRepositoryName;

manager.listener.logger.println "Setting Commit Status to the current build result"

def result = manager.build.getResult()

def commit_status_context = 'default'
try {
    commit_status_context = manager.envVars['COMMIT_STATUS_CONTEXT']
} catch(e) {
    manager.listener.logger.println(
        'Defaulting to the "default" commit status context since "COMMIT_STATUS_CONTEXT" was ' + \
        'not found as an environment variable'
    )
}
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

def github_repo = ""
try {
    github_repo = manager.envVars['GITHUB_REPO']
} catch(e) {
    def github_repo_pattern = 'https://github.com/(.*)/pull/(.*?)'
    def regex_match = (manager.envVars['ghprbPullLink'] =~ ~github_repo_pattern)
    github_repo = regex_match[0][1]
}

if ( github_repo != null ) {
    def github_repo_url = 'https://github.com/' + github_repo + '.git'
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
                manager.listener.logger.println "GitHub commit status successfuly set"
            }
        }
    } else {
        msg = "Failed to resolve the github repo: " + 'https://github.com/' + github_repo + '.git'
        manager.createSummary('warning.gif').appendText(msg)
        manager.listener.logger.println msg
    }
} else {
    manager.listener.logger.println(
        'Unable to find the GitHub repo either by looing at "GITHUB_REPO" or ' + \
        '"ghprbPullLink" in the build environment'
    )
}
