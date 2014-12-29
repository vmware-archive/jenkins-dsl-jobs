import hudson.model.Result.*;
import org.eclipse.jgit.lib.ObjectId;
import org.kohsuke.github.GHCommitState;
import org.jenkinsci.plugins.github.util.BuildDataHelper;
import com.cloudbees.jenkins.GitHubRepositoryNameContributor;


def build_env_vars = currentBuild.getEnvVars()
def result = currentBuild.getResult()

if (result == null) { // Build is ongoing
    def state = GHCommitState.PENDING;
} else if (result.isBetterOrEqualTo(SUCCESS)) {
    def state = GHCommitState.SUCCESS;
} else if (result.isBetterOrEqualTo(UNSTABLE)) {
    def state = GHCommitState.FAILURE;
} else {
    def state = GHCommitState.ERROR;
}

def project = currentBuild.getProject()

try {
    def sha1 = ObjectId.toString(BuildDataHelper.getCommitSHA1(currentBuild));
} catch(IOException e) {
    def sha1 = build_env_vars['GIT_COMMIT']
}

try {
    def commit_status_context = '$commit_status_context'
} catch(e) {
    try {
        def commit_status_context = 'ci/' + project.getFullName()
    } catch(e) {
        def commit_status_context = "default"
    }
}

GitHubRepositoryNameContributor.parseAssociatedNames(project).each {
    it.resolve().each {
        def status_result = it.createCommitStatus(
            sha1,
            state,
            currentBuild.getAbsoluteUrl(),
            currentBuild.getFullDisplayName(),
            commit_status_context
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
]
