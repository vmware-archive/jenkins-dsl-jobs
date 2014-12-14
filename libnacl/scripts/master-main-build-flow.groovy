// output values
/*
out.println '>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>'
out.println 'Triggered Parameters Map:'
out.println params
out.println 'Build Object Properties:'
build.properties.each { out.println "$it.key -> $it.value" }
out.println '<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<'
*/

// Set commit status
def shellOut = new StringBuffer()
def shellErr = new StringBuffer()
command = """github-commit-status \
    --auth-token=${build.environment.get('GITHUB_AUTH_TOKEN')} \
    --repo=${build.environment.get('GITHUB_REPO')} \
    --context=${build.environment.get('COMMIT_STATUS_CONTEXT')} \
    --target-url=${build.environment.get('BUILD_URL')} \
    ${build.environment.get('GIT_COMMIT')}
""".execute()
command.consumeProcessOutput(shellOut, shellErr)
command.waitForOrKill(1000)
if (shellOut) {
    println 'Commit Status Process STDOUT:'
    println $shellOut
}
if (shellErr) {
    println 'Commit Status Process STDERR:'
    println $shellErr
}

// Let's run Lint & Unit in parallel
parallel (
  {
    lint = build('libnacl/master/lint')
  },
  {
    unit = build('libnacl/master/unit')
  }
)

// Let's instantiate the build flow toolbox
def toolbox = extension.'build-flow-toolbox'

local_lint_workspace_copy = build.workspace.child('lint')
local_lint_workspace_copy.mkdirs()
toolbox.copyFiles(lint.workspace, local_lint_workspace_copy)

local_unit_workspace_copy = build.workspace.child('unit')
local_unit_workspace_copy.mkdirs()
toolbox.copyFiles(unit.workspace, local_unit_workspace_copy)

// Slurp artifacts
toolbox.slurpArtifacts(lint)
toolbox.slurpArtifacts(unit)

// Delete the child workspaces directory
lint.workspace.deleteRecursive()
unit.workspace.deleteRecursive()
