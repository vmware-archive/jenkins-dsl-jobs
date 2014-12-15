// output values
/*
out.println '>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>'
out.println 'Triggered Parameters Map:'
out.println params
out.println 'Build Object Properties:'
build.properties.each { out.println "$it.key -> $it.value" }
out.println '<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<'
*/

// Let's run Lint & Unit in parallel
parallel (
  {
    lint = build('sorbic/pr/lint')
  },
  {
    unit = build('sorbic/pr/unit')
  }
)

// Let's instantiate the build flow toolbox
def toolbox = extension.'build-flow-toolbox'

local_lint_workspace_copy = build.workspace.child('lint')
local_lint_workspace_copy.mkdirs()
toolbox.copyFiles(lint.workspace, local_lint_workspace_copy)

#local_unit_workspace_copy = build.workspace.child('unit')
#local_unit_workspace_copy.mkdirs()
#toolbox.copyFiles(unit.workspace, local_unit_workspace_copy)

// Slurp artifacts
toolbox.slurpArtifacts(lint)
toolbox.slurpArtifacts(unit)

// Delete the child workspaces directory
lint.workspace.deleteRecursive()
unit.workspace.deleteRecursive()
