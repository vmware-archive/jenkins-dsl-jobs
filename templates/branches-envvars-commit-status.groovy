import com.saltstack.jenkins.Projects<% if ( generate_vm_name != null ) { %>
import com.saltstack.jenkins.VmName<% } %>

def projects = new Projects()
projects.setCommitStatusPre(currentBuild, '$commit_status_context', out)

def build_env_vars = currentBuild.getEnvironment()

return [<%
    if ( sudo_salt_call != null ) { %>
    SUDO_SALT_CALL_REQUIRED: '1',<% } %><%
    if ( virtualenv_name != null ) { %>
    VIRTUALENV_NAME: '$virtualenv_name',<% } %><%
    if ( virtualenv_setup_state_name != null ) { %>
    VIRTUALENV_SETUP_STATE_NAME: '$virtualenv_setup_state_name',<% } %><%
    if ( system_site_packages != null ) { %>
    SYSTEM_SITE_PACKAGES: true,<% } %><%
    if ( branch_name != null ) { %>
    BRANCH_NAME: '$branch_name',<% } %><%
    if (generate_vm_name != null) { %>
    JENKINS_VM_NAME: VmName.generate(currentBuild),<% } %>
    COMMIT_STATUS_CONTEXT: '$commit_status_context'
]
