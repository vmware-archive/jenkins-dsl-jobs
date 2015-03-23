import com.saltstack.jenkins.Projects

def projects = new Projects()
projects.setCommitStatusPre(currentBuild, '$commit_status_context', out)

def build_env_vars = currentBuild.getEnvironment()

<% if ( vm_name_nodots != null ) { %>
def build_number = build_env_vars['BUILD_NUMBER'].padLeft(4, '0')
<% } %>

return [<%
    if ( sudo_salt_call != null ) { %>
    SUDO_SALT_CALL_REQUIRED: '1',<% } %><%
    if ( github_repo != null ) { %>
    GITHUB_REPO: '$github_repo',<% } %><%
    if ( virtualenv_name != null ) { %>
    VIRTUALENV_NAME: '$virtualenv_name',<% } %><%
    if ( virtualenv_setup_state_name != null ) { %>
    VIRTUALENV_SETUP_STATE_NAME: '$virtualenv_setup_state_name',<% } %><%
    if ( system_site_packages != null ) { %>
    SYSTEM_SITE_PACKAGES: true,<% } %><%
    if ( branch_name != null ) { %>
    BRANCH_NAME: '$branch_name',<% } %><%
    if (build_vm_name != null) { %>
    BUILD_VM_NAME: '$build_vm_name',<% } %><%
    if (vm_name_nodots != null) { %>
    JENKINS_VM_NAME: build_env_vars['JENKINS_VM_NAME_PREFIX'] + '_' + '$vm_name_nodots' + '_' + build_number,<% } %>
    COMMIT_STATUS_CONTEXT: '$commit_status_context'
]
