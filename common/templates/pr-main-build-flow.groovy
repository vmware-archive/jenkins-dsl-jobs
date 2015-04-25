import hudson.FilePath

guard {
    retry(3) {
        clone = build('${project}/pr/${pr_number}/clone',
                      GIT_COMMIT: params['ghprbActualCommit'])
    }

    // Let's run Lint & Unit in parallel
    parallel (
        {
            lint = build('${project}/pr/${pr_number}/lint',
                         GIT_COMMIT: params['ghprbActualCommit'],
                         CLONE_BUILD_ID: clone.build.number)
        },
        {
            unit = build('${project}/pr/${pr_number}/tests',
                         GIT_COMMIT: params['ghprbActualCommit'],
                         CLONE_BUILD_ID: clone.build.number)
        }
    )

}
