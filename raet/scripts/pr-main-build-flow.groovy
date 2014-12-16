// Let's run Lint & Unit in parallel
parallel (
  {
    lint = build('raet/pr/lint')
  },
  {
    unit = build('raet/pr/unit')
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
