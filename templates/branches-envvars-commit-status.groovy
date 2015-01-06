import hudson.model.Result;
import org.kohsuke.github.GHCommitState;
import com.cloudbees.jenkins.GitHubRepositoryName;
import com.coravy.hudson.plugins.github.GithubProjectProperty;

def build_env_vars = currentBuild.getEnvironment()
def result = currentBuild.getResult()

def state = GHCommitState.ERROR;

if (result == null) { // Build is ongoing
    state = GHCommitState.PENDING;
    out.println 'GitHub commit status is PENDING'
} else if (result.isBetterOrEqualTo(Result.SUCCESS)) {
    state = GHCommitState.SUCCESS;
    out.println 'GitHub commit status is SUCCESS'
} else if (result.isBetterOrEqualTo(Result.UNSTABLE)) {
    state = GHCommitState.FAILURE;
    out.println 'GitHub commit status is FAILURE'
} else {
    out.println 'GitHub commit status is ERROR'
}

def github_repo_url = currentJob.getProperty(GithubProjectProperty.class).getProjectUrl()

if ( github_repo_url != null ) {
    out.println 'GitHub Repository URL: ' + github_repo_url
    repo = GitHubRepositoryName.create(github_repo_url.toString())
    if ( repo != null ) {
        def git_commit = build_env_vars.get('GIT_COMMIT', build_env_vars.get('ghprbActualCommit'))
        repo.resolve().each {
            def status_result = it.createCommitStatus(
                git_commit,
                state,
                currentBuild.getAbsoluteUrl(),
                currentBuild.getFullDisplayName(),
                '$commit_status_context'
            )
            if ( ! status_result ) {
                msg = 'Failed to set commit status on GitHub'
                out.println msg
            } else {
                msg = "GitHub commit status successfuly set"
                out.println(msg)
            }
        }
    } else {
        msg = "Failed to resolve the github GIT repo URL from " + github_repo_url
        out.println msg
    }
} else {
    msg = "Unable to find the GitHub project URL from the build's properties"
    out.println msg
}

<% if ( vm_name_nodots != null ) { %>
def build_number = build_env_vars['BUILD_NUMBER'].padLeft(4, '0')
<% } %>

return [<%
    if ( github_repo != null ) { %>
    GITHUB_REPO: '$github_repo',<% } %><%
    if ( virtualenv_name != null ) { %>
    VIRTUALENV_NAME: '$virtualenv_name',<% } %><%
    if ( virtualenv_setup_state_name != null ) { %>
    VIRTUALENV_SETUP_STATE_NAME: '$virtualenv_setup_state_name',<% } %><%
    if ( branch_name != null ) { %>
    BRANCH_NAME: '$branch_name',<% } %><%
    if (build_vm_name != null) { %>
    BUILD_VM_NAME: '$build_vm_name',<% } %><%
    if (vm_name_nodots != null) { %>
    JENKINS_VM_NAME: build_env_vars['JENKINS_VM_NAME_PREFIX'] + '_' + '$vm_name_nodots' + '_' + build_number,<% } %>
    COMMIT_STATUS_CONTEXT: '$commit_status_context'
]
