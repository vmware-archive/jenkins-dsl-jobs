// libnacl Jenkins jobs seed script

// Common variable Definitions
def github_repo = 'saltstack/libnacl'
def repo_api = new URL("https://api.github.com/repos/${github_repo}")
def repo_data = new groovy.json.JsonSlurper().parse(repo_api.newReader())
def project_description = repo_data['description']

// Job rotation defaults
def default_days_to_keep = 90
def default_nr_of_jobs_to_keep = 180
def default_artifact_days_to_keep = default_days_to_keep
def default_artifact_nr_of_jobs_to_keep = default_nr_of_jobs_to_keep

// Job Timeout defauls
def default_timeout_percent = 150
def default_timeout_builds = 10
def default_timeout_minutes = 15


// Define the folder structure
folder {
    name('libnacl')
    displayName('libnacl')
    description = project_description
}
folder {
    name('libnacl/master')
    displayName('Master Branch')
    description = project_description
}
folder {
    name('libnacl/pr')
    displayName('Pull Requests')
    description = project_description
}

// Main master branch job
def master_main_job = job(type: BuildFlow) {
    name = 'libnacl/master-main-build'
    displayName('Master Branch Main Build')
    description(project_description)
    label('worker')
    concurrentBuild(allowConcurrentBuild = true)

    configure {
        it.appendNode('buildNeedsWorkspace').setValue(true)
        it.get('publishers').get(0).appendNode(
            'org.zeroturnaround.jenkins.flowbuildtestaggregator.FlowTestAggregator',
            [plugin: 'build-flow-test-aggregator@']
        )
    }

    wrappers {
        // Inject global defined passwords in the environment
        injectPasswords()

        // Add timestamps to console log
        timestamps()

        // Color Support to console log
        colorizeOutput('xterm')

        // Build Timeout
        timeout {
            elastic(
                percentage = default_timeout_percent,
                numberOfBuilds = default_timeout_builds,
                minutesDefault= default_timeout_minutes
            )
            writeDescription('Build failed due to timeout after {0} minutes')
        }
    }

    // Delete old jobs
    logRotator(
        default_days_to_keep,
        default_nr_of_jobs_to_keep,
        default_artifact_days_to_keep,
        default_artifact_nr_of_jobs_to_keep
    )

    // scm configuration
    scm {
        github(
            github_repo,
            branch = '*/master',
            protocol = 'https'
        )
    }
    checkoutRetryCount(3)

    // Job Triggers
    triggers {
        githubPush()
    }

    buildFlow(
        readFileFromWorkspace('jenkins-seed', 'libnacl/scripts/master-main-build-flow.groovy')
    )

    publishers {
        // Report Coverage
        cobertura('unit/coverage.xml') {
            failNoReports = false
        }
        // Report Violations
        violations {
            pylint(10, 999, 999, 'lint/pylint-report*.xml')
        }

        // Set commit status
        githubCommitNotifier()

        // Cleanup workspace
        wsCleanup()
    }
}

// Lint Master Job
def master_lint_job = job {
    name = 'libnacl/master/lint'
    displayName('Lint')
    concurrentBuild(allowConcurrentBuild = true)
    description(project_description + ' - Code Lint')
    label('worker')

    wrappers {
        // Inject global defined passwords in the environment
        injectPasswords()

        // Cleanup the workspace before starting
        preBuildCleanup()

        // Add timestamps to console log
        timestamps()

        // Color Support to console log
        colorizeOutput('xterm')

        // Build Timeout
        timeout {
            elastic(
                percentage = default_timeout_percent,
                numberOfBuilds = default_timeout_builds,
                minutesDefault= default_timeout_minutes
            )
            writeDescription('Build failed due to timeout after {0} minutes')
        }
    }

    // Delete old jobs
    logRotator(
        default_days_to_keep,
        default_nr_of_jobs_to_keep,
        default_artifact_days_to_keep,
        default_artifact_nr_of_jobs_to_keep
    )

    // scm configuration
    scm {
        github(
            github_repo,
            branch = '*/master',
            protocol = 'https'
        )
    }
    checkoutRetryCount(3)

    environmentVariables {
        env('GITHUB_REPO', github_repo)
        env('COMMIT_STATUS_CONTEXT', 'ci/lint')
        env('VIRTUALENV_NAME', 'libnacl-master')
        env('VIRTUALENV_SETUP_STATE_NAME', 'projects.libnacl.lint')
    }

    // Job Steps
    steps {
        // Setup the required virtualenv
        shell(readFileFromWorkspace('jenkins-seed', 'scripts/prepare-virtualenv.sh'))

        // Set initial commit status
        shell(readFileFromWorkspace('jenkins-seed', 'scripts/set-commit-status.sh'))

        // Run Lint Code
        shell(readFileFromWorkspace('jenkins-seed', 'libnacl/scripts/run-lint.sh'))
    }

    publishers {
        // Report Violations
        violations {
            pylint(10, 999, 999, 'pylint-report*.xml')
        }

        postBuildTask {
            // Set final commit status
            task('.', readFileFromWorkspace('jenkins-seed', 'scripts/set-commit-status.sh'))
        }
    }
}

// Master Unit Tests
def master_unit_job = job {
    name = 'libnacl/master/unit'
    displayName('Unit')
    concurrentBuild(allowConcurrentBuild = true)
    description(project_description + ' - Unit Tests')
    label('worker')

    wrappers {
        // Inject global defined passwords in the environment
        injectPasswords()

        // Cleanup the workspace before starting
        preBuildCleanup()

        // Add timestamps to console log
        timestamps()

        // Color Support to console log
        colorizeOutput('xterm')

        // Build Timeout
        timeout {
            elastic(
                percentage = default_timeout_percent,
                numberOfBuilds = default_timeout_builds,
                minutesDefault= default_timeout_minutes
            )
            writeDescription('Build failed due to timeout after {0} minutes')
        }

    }

    // scm configuration
    scm {
        github(
            github_repo,
            branch = '*/master',
            protocol = 'https'
        )
    }
    checkoutRetryCount(3)

    environmentVariables {
        env('GITHUB_REPO', github_repo)
        env('COMMIT_STATUS_CONTEXT', 'ci/unit')
        env('VIRTUALENV_NAME', 'libnacl-master')
        env('VIRTUALENV_SETUP_STATE_NAME', 'projects.libnacl.unit')
    }

    // Job Steps
    steps {
        // Setup the required virtualenv
        shell(readFileFromWorkspace('jenkins-seed', 'scripts/prepare-virtualenv.sh'))

        // Set initial commit status
        shell(readFileFromWorkspace('jenkins-seed', 'scripts/set-commit-status.sh'))

        // Run Unit Tests
        shell(readFileFromWorkspace('jenkins-seed', 'libnacl/scripts/run-unit.sh'))
    }

    publishers {
        // Report Coverage
        cobertura('coverage.xml') {
            failNoReports = false
        }

        // Junit Reports
        archiveJunit('nosetests.xml') {
            retainLongStdout(true)
            testDataPublishers {
                publishTestStabilityData()
            }
        }

        postBuildTask {
            // Set final commit status
            task('.', readFileFromWorkspace('jenkins-seed', 'scripts/set-commit-status.sh'))
        }
    }
}

// PR Main Job
job(type: BuildFlow) {
    name = 'libnacl/pr-main-build'
    displayName('Pull Requests Main Build')
    description(project_description)
    label('worker')
    concurrentBuild(allowConcurrentBuild = true)

    configure {
        it.appendNode('buildNeedsWorkspace').setValue(true)
        it.get('publishers').get(0).appendNode(
            'org.zeroturnaround.jenkins.flowbuildtestaggregator.FlowTestAggregator',
            [plugin: 'build-flow-test-aggregator@']
        )
    }

    wrappers {
        // Inject global defined passwords in the environment
        injectPasswords()

        // Add timestamps to console log
        timestamps()

        // Color Support to console log
        colorizeOutput('xterm')

        // Build Timeout
        timeout {
            elastic(
                percentage = default_timeout_percent,
                numberOfBuilds = default_timeout_builds,
                minutesDefault= default_timeout_minutes
            )
            writeDescription('Build failed due to timeout after {0} minutes')
        }
    }

    // Delete old jobs
    logRotator(
        default_days_to_keep,
        default_nr_of_jobs_to_keep,
        default_artifact_days_to_keep,
        default_artifact_nr_of_jobs_to_keep
    )

    // scm configuration
    scm {
        git {
            remote {
                github(github_repo, protocol='https')
                refspec('+refs/pull/*:refs/remotes/origin/pr/*')
            }
            branch('${sha1}')
        }
    }
    checkoutRetryCount(3)

    // Job Triggers
    triggers {
        pullRequest {
            //orgWhiteList('saltstack')
            useGitHubHooks()
            permitAll()
        }
    }

    buildFlow(
        readFileFromWorkspace('jenkins-seed', 'libnacl/scripts/pr-main-build-flow.groovy')
    )

    publishers {
        // Report Coverage
        cobertura('unit/coverage.xml') {
            failNoReports = false
        }
        // Report Violations
        violations {
            pylint(10, 999, 999, 'lint/pylint-report*.xml')
        }

        // Set commit status
        githubCommitNotifier()

        // Cleanup workspace
        wsCleanup()
    }
}

// PR Lint Job
job {
    name = 'libnacl/pr/lint'
    displayName('Lint')
    concurrentBuild(allowConcurrentBuild = true)
    description(project_description + ' - Code Lint')
    label('worker')

    wrappers {
        // Inject global defined passwords in the environment
        injectPasswords()

        // Cleanup the workspace before starting
        preBuildCleanup()

        // Add timestamps to console log
        timestamps()

        // Color Support to console log
        colorizeOutput('xterm')

        // Build Timeout
        timeout {
            elastic(
                percentage = default_timeout_percent,
                numberOfBuilds = default_timeout_builds,
                minutesDefault= default_timeout_minutes
            )
            writeDescription('Build failed due to timeout after {0} minutes')
        }
    }

    // Delete old jobs
    logRotator(
        default_days_to_keep,
        default_nr_of_jobs_to_keep,
        default_artifact_days_to_keep,
        default_artifact_nr_of_jobs_to_keep
    )

    // scm configuration
    scm {
        git {
            remote {
                github(github_repo, protocol='https')
                refspec('+refs/pull/*:refs/remotes/origin/pr/*')
            }
            branch('${sha1}')
        }
    }
    checkoutRetryCount(3)

    environmentVariables {
        env('GITHUB_REPO', github_repo)
        env('COMMIT_STATUS_CONTEXT', 'ci/lint')
        env('VIRTUALENV_NAME', 'libnacl-pr')
        env('VIRTUALENV_SETUP_STATE_NAME', 'projects.libnacl.lint')
    }

    // Job Steps
    steps {
        // Setup the required virtualenv
        shell(readFileFromWorkspace('jenkins-seed', 'scripts/prepare-virtualenv.sh'))

        // Set initial commit status
        shell(readFileFromWorkspace('jenkins-seed', 'scripts/set-commit-status.sh'))

        // Run Lint Code
        shell(readFileFromWorkspace('jenkins-seed', 'libnacl/scripts/run-lint.sh'))
    }

    publishers {
        // Report Violations
        violations {
            pylint(10, 999, 999, 'pylint-report*.xml')
        }

        postBuildTask {
            // Set final commit status
            task('.', readFileFromWorkspace('jenkins-seed', 'scripts/set-commit-status.sh'))
        }
    }
}

// PR Unit Tests
job {
    name = 'libnacl/pr/unit'
    displayName('Unit')
    concurrentBuild(allowConcurrentBuild = true)
    description(project_description + ' - Unit Tests')
    label('worker')

    wrappers {
        // Inject global defined passwords in the environment
        injectPasswords()

        // Cleanup the workspace before starting
        preBuildCleanup()

        // Add timestamps to console log
        timestamps()

        // Color Support to console log
        colorizeOutput('xterm')

        // Build Timeout
        timeout {
            elastic(
                percentage = default_timeout_percent,
                numberOfBuilds = default_timeout_builds,
                minutesDefault= default_timeout_minutes
            )
            writeDescription('Build failed due to timeout after {0} minutes')
        }

    }

    // scm configuration
    scm {
        git {
            remote {
                github(github_repo, protocol='https')
                refspec('+refs/pull/*:refs/remotes/origin/pr/*')
            }
            branch('${sha1}')
        }
    }
    checkoutRetryCount(3)

    environmentVariables {
        env('GITHUB_REPO', github_repo)
        env('COMMIT_STATUS_CONTEXT', 'ci/unit')
        env('VIRTUALENV_NAME', 'libnacl-pr')
        env('VIRTUALENV_SETUP_STATE_NAME', 'projects.libnacl.unit')
    }

    // Job Steps
    steps {
        // Setup the required virtualenv
        shell(readFileFromWorkspace('jenkins-seed', 'scripts/prepare-virtualenv.sh'))

        // Set initial commit status
        shell(readFileFromWorkspace('jenkins-seed', 'scripts/set-commit-status.sh'))

        // Run Unit Tests
        shell(readFileFromWorkspace('jenkins-seed', 'libnacl/scripts/run-unit.sh'))
    }

    publishers {
        // Report Coverage
        cobertura('coverage.xml') {
            failNoReports = false
        }

        // Junit Reports
        archiveJunit('nosetests.xml') {
            retainLongStdout(true)
            testDataPublishers {
                publishTestStabilityData()
            }
        }

        postBuildTask {
            // Set final commit status
            task('.', readFileFromWorkspace('jenkins-seed', 'scripts/set-commit-status.sh'))
        }
    }
}
