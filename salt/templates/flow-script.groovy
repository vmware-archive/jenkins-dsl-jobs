import hudson.FilePath

guard {
    retry(3) {
        clone = build("salt/${branch_name}/clone")
    }

    // Let's run Lint & Unit in parallel
    parallel (
        {
            lint = build(
                "salt/${branch_name}/lint",
                CLONE_BUILD_ID: clone.build.number
            )
        },
        <% vm_names.each { name, job_name -> %>
        {
            ${name} = build(
                "salt/${branch_name}/${build_type_l}/<%
                    if ( build_type_l == 'cloud') { %>params["PROVIDER"].toLowerCase()/<% }
                %>${job_name}",
                GIT_COMMIT: params["GIT_COMMIT"]
            )
        },<% } %>
    )
} rescue {
    // Let's instantiate the build flow toolbox
    def toolbox = extension.'build-flow-toolbox'

    local_lint_workspace_copy = build.workspace.child('lint')
    local_lint_workspace_copy.mkdirs()
    toolbox.copyFiles(lint.workspace, local_lint_workspace_copy)

    <% vm_names.each { name, job_name -> %>
    local_${name}_workspace_copy = build.workspace.child('${job_name}')
    local_${name}_workspace_copy.mkdirs()
    toolbox.copyFiles(${name}.workspace, local_${name}_workspace_copy)
    <% } %>
   /*
    *  Copy the clone build changelog.xml into this jobs root for proper changelog report
    *  This does not currently work but is here for future reference
    */
    def clone_changelog = new FilePath(clone.getRootDir()).child('changelog.xml')
    def build_changelog = new FilePath(build.getRootDir()).child('changelog.xml')
    build_changelog.copyFrom(clone_changelog)

    // Delete the child workspaces directory
    lint.workspace.deleteRecursive()

    <% vm_names.each { name, job_name -> %>
    ${name}.workspace.deleteRecursive()<% } %>
}
