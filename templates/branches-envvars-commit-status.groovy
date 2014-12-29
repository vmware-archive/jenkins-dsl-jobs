import hudson.model.Result;
import org.kohsuke.github.GHCommitState;
import com.cloudbees.jenkins.GitHubRepositoryNameContributor;


def build_env_vars = currentBuild.getEnvVars()
def result = currentBuild.getResult()

def state = GHCommitState.ERROR;

if (result == null) { // Build is ongoing
    state = GHCommitState.PENDING;
} else if (result.isBetterOrEqualTo(Result.SUCCESS)) {
    state = GHCommitState.SUCCESS;
} else if (result.isBetterOrEqualTo(Result.UNSTABLE)) {
    state = GHCommitState.FAILURE;
}

def project = currentBuild.getProject()

GitHubRepositoryNameContributor.parseAssociatedNames(project).each {
    it.resolve().each {
        def status_result = it.createCommitStatus(
            currentBuild.getBuildVariables()['GIT_COMMIT'],
            state,
            currentBuild.getAbsoluteUrl(),
            currentBuild.getFullDisplayName(),
            '$commit_status_context'
        )
    }
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
    JENKINS_VM_NAME: build_env_vars['JENKINS_VM_NAME_PREFIX'] + '_' + '$vm_name_nodots' + '_' + build_number<% } %>
    COMMIT_STATUS_CONTEXT: '$commit_status_context'
]
