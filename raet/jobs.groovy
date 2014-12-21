// raet Jenkins jobs seed script
import lib.Admins

// Common variable Definitions
def github_repo = 'saltstack/raet'
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
    name('raet')
    displayName('RAET')
    description = project_description
}
folder {
    name('raet/master')
    displayName('Master Branch')
    description = project_description
}
folder {
    name('raet/pr')
    displayName('Pull Requests')
    description = project_description
}

// Main master branch job
def master_main_job = job(type: BuildFlow) {
    name = 'raet/master-main-build'
    displayName('Master Branch Main Build')
    description(project_description)
    label('worker')
    concurrentBuild(allowConcurrentBuild = true)

    configure {
        it.appendNode('buildNeedsWorkspace').setValue(true)
        job_publishers = it.get('publishers').get(0)
        job_publishers.appendNode(
            'org.zeroturnaround.jenkins.flowbuildtestaggregator.FlowTestAggregator',
            [plugin: 'build-flow-test-aggregator']
        )
        job_properties = it.get('properties').get(0)
        github_project_property = job_properties.appendNode(
            'com.coravy.hudson.plugins.github.GithubProjectProperty')
        github_project_property.appendNode('projectUrl').setValue("https://github.com/${github_repo}")
        slack_notifications = job_properties.appendNode(
            'jenkins.plugins.slack.SlackNotifier_-SlackJobProperty')
        slack_notifications.appendNode('room').setValue('#jenkins')
        slack_notifications.appendNode('startNotifications').setValue(false)
        slack_notifications.appendNode('notifySuccess').setValue(true)
        slack_notifications.appendNode('notifyAborted').setValue(true)
        slack_notifications.appendNode('notifyNotBuilt').setValue(true)
        slack_notifications.appendNode('notifyFailure').setValue(true)
        slack_notifications.appendNode('notifyBackToNormal').setValue(true)
        job_publishers.appendNode(
            'jenkins.plugins.slack.SlackNotifier',
            [plugin: slack]
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
        readFileFromWorkspace('jenkins-seed', 'raet/groovy/master-main-build-flow.groovy')
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

// Clone Master Job
def master_clone_job = job {
    name = 'raet/master/clone'
    displayName('Clone Repository')

    concurrentBuild(allowConcurrentBuild = true)
    description(project_description + ' - Clone Repository')
    label('worker')

    configure {
        job_properties = it.get('properties').get(0)
        job_properties.appendNode(
            'hudson.plugins.copyartifact.CopyArtifactPermissionProperty').appendNode(
                'projectNameList').appendNode(
                    'string').setValue('raet/master/*')
        github_project_property = job_properties.appendNode(
            'com.coravy.hudson.plugins.github.GithubProjectProperty')
        github_project_property.appendNode('projectUrl').setValue("https://github.com/${github_repo}")

    }

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
    /* Since we're just cloning the repository in order to make it an artifact to
     * user as workspace for all other jobs, we only need to keep the artifact for
     * a couple of minutes. Since one day is the minimum....
     */
    logRotator(
        default_days_to_keep,
        default_nr_of_jobs_to_keep,
        1,  //default_artifact_days_to_keep,
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
        env('COMMIT_STATUS_CONTEXT', 'ci/clone')
        env('VIRTUALENV_NAME', 'raet-master')
        env('VIRTUALENV_SETUP_STATE_NAME', 'projects.clone')
    }

    // Job Steps
    steps {
        // Setup the required virtualenv
        shell(readFileFromWorkspace('jenkins-seed', 'scripts/prepare-virtualenv.sh'))

        // Set initial commit status
        shell(readFileFromWorkspace('jenkins-seed', 'scripts/set-commit-status.sh'))

        // Compress the checked out workspace
        shell(readFileFromWorkspace('jenkins-seed', 'scripts/compress-workspace.sh'))
    }

    publishers {
        archiveArtifacts('workspace.cpio.xz')

        postBuildTask {
            // Set final commit status
            task('.', readFileFromWorkspace('jenkins-seed', 'scripts/set-commit-status.sh'))
        }
    }
}

// Lint Master Job
def master_lint_job = job {
    name = 'raet/master/lint'
    displayName('Lint')
    concurrentBuild(allowConcurrentBuild = true)
    description(project_description + ' - Code Lint')
    label('worker')

    // Parameters Definition
    parameters {
        stringParam('CLONE_BUILD_ID')
    }

    configure {
        job_properties = it.get('properties').get(0)
        github_project_property = job_properties.appendNode(
            'com.coravy.hudson.plugins.github.GithubProjectProperty')
        github_project_property.appendNode('projectUrl').setValue("https://github.com/${github_repo}")
    }

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

    environmentVariables {
        env('GITHUB_REPO', github_repo)
        env('COMMIT_STATUS_CONTEXT', 'ci/lint')
        env('VIRTUALENV_NAME', 'raet-master')
        env('VIRTUALENV_SETUP_STATE_NAME', 'projects.raet.lint')
    }

    // Job Steps
    steps {
        // Setup the required virtualenv
        shell(readFileFromWorkspace('jenkins-seed', 'scripts/prepare-virtualenv.sh'))

        // Set initial commit status
        shell(readFileFromWorkspace('jenkins-seed', 'scripts/set-commit-status.sh'))

        // Copy the workspace artifact
        copyArtifacts('raet/master/clone', 'workspace.cpio.xz') {
            buildNumber('${CLONE_BUILD_ID}')
        }
        shell(readFileFromWorkspace('jenkins-seed', 'scripts/decompress-workspace.sh'))

        // Run Lint Code
        shell(readFileFromWorkspace('jenkins-seed', 'raet/scripts/run-lint.sh'))
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
    name = 'raet/master/unit'
    displayName('Unit')
    concurrentBuild(allowConcurrentBuild = true)
    description(project_description + ' - Unit Tests')
    label('worker')

    // Parameters Definition
    parameters {
        stringParam('CLONE_BUILD_ID')
    }

    configure {
        job_properties = it.get('properties').get(0)
        github_project_property = job_properties.appendNode(
            'com.coravy.hudson.plugins.github.GithubProjectProperty')
        github_project_property.appendNode('projectUrl').setValue("https://github.com/${github_repo}")
    }

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

    environmentVariables {
        env('GITHUB_REPO', github_repo)
        env('COMMIT_STATUS_CONTEXT', 'ci/unit')
        env('VIRTUALENV_NAME', 'raet-master')
        env('VIRTUALENV_SETUP_STATE_NAME', 'projects.raet.unit')
    }

    // Job Steps
    steps {
        // Setup the required virtualenv
        shell(readFileFromWorkspace('jenkins-seed', 'scripts/prepare-virtualenv.sh'))

        // Set initial commit status
        shell(readFileFromWorkspace('jenkins-seed', 'scripts/set-commit-status.sh'))

        // Copy the workspace artifact
        copyArtifacts('raet/master/clone', 'workspace.cpio.xz') {
            buildNumber('${CLONE_BUILD_ID}')
        }
        shell(readFileFromWorkspace('jenkins-seed', 'scripts/decompress-workspace.sh'))

        // Run Unit Tests
        shell(readFileFromWorkspace('jenkins-seed', 'raet/scripts/run-unit.sh'))
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
    name = 'raet/pr-main-build'
    displayName('Pull Requests Main Build')
    description(project_description)
    label('worker')
    concurrentBuild(allowConcurrentBuild = true)

    configure {
        it.appendNode('buildNeedsWorkspace').setValue(true)
        job_publishers = it.get('publishers').get(0)
        job_publishers.appendNode(
            'org.zeroturnaround.jenkins.flowbuildtestaggregator.FlowTestAggregator',
            [plugin: 'build-flow-test-aggregator@']
        )
        job_properties = it.get('properties').get(0)
        github_project_property = job_properties.appendNode(
            'com.coravy.hudson.plugins.github.GithubProjectProperty')
        github_project_property.appendNode('projectUrl').setValue("https://github.com/${github_repo}")
        slack_notifications = job_properties.appendNode(
            'jenkins.plugins.slack.SlackNotifier_-SlackJobProperty')
        slack_notifications.appendNode('room').setValue('#jenkins')
        slack_notifications.appendNode('startNotifications').setValue(false)
        slack_notifications.appendNode('notifySuccess').setValue(true)
        slack_notifications.appendNode('notifyAborted').setValue(true)
        slack_notifications.appendNode('notifyNotBuilt').setValue(true)
        slack_notifications.appendNode('notifyFailure').setValue(true)
        slack_notifications.appendNode('notifyBackToNormal').setValue(true)
        job_publishers.appendNode(
            'jenkins.plugins.slack.SlackNotifier',
            [plugin: slack]
        )
    }

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

    // Job Triggers
    triggers {
        pullRequest {
            useGitHubHooks()
            permitAll()
            triggerPhrase('Go Go Jenkins!')
            admins(Admins.usernames)
        }
    }

    buildFlow(
        readFileFromWorkspace('jenkins-seed', 'raet/groovy/pr-main-build-flow.groovy')
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

        extendedEmail('$DEFAULT_RECIPIENTS', '$DEFAULT_SUBJECT', '$DEFAULT_CONTENT') {
            trigger(
                triggerName: 'Failure',
                subject: '$DEFAULT_SUBJECT',
                body: '$DEFAULT_CONTENT',
                recipientList: '$DEFAULT_RECIPIENTS',
                sendToRecipientList: true
            )
            configure { node ->
                node.appendNode('presendScript').setValue(
                    readFileFromWorkspace('jenkins-seed', 'groovy/inject-submitter-email-address.groovy')
                )
            }
        }

        // Cleanup workspace
        wsCleanup()
    }
}

// PR Clone Job
def pr_clone_job = job {
    name = 'raet/pr/clone'
    displayName('Clone Repository')

    concurrentBuild(allowConcurrentBuild = true)
    description(project_description + ' - Clone Repository')
    label('worker')

    parameters {
        stringParam('PR')
    }

    configure {
        job_properties = it.get('properties').get(0)
        job_properties.appendNode(
            'hudson.plugins.copyartifact.CopyArtifactPermissionProperty').appendNode(
                'projectNameList').appendNode(
                    'string').setValue('raet/pr/*')
        github_project_property = job_properties.appendNode(
            'com.coravy.hudson.plugins.github.GithubProjectProperty')
        github_project_property.appendNode('projectUrl').setValue("https://github.com/${github_repo}")
    }

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
    /* Since we're just cloning the repository in order to make it an artifact to
     * user as workspace for all other jobs, we only need to keep the artifact for
     * a couple of minutes. Since one day is the minimum....
     */
    logRotator(
        default_days_to_keep,
        default_nr_of_jobs_to_keep,
        1,  //default_artifact_days_to_keep,
        default_artifact_nr_of_jobs_to_keep
    )

    // scm configuration
    scm {
        git {
            remote {
                github(github_repo, protocol='https')
                refspec('+refs/pull/*:refs/remotes/origin/pr/*')
            }
            branch('origin/pr/${PR}/merge')
            configure {
                git_extensions = it.appendNode('extensions')
                extension = git_extensions.appendNode('hudson.plugins.git.extensions.impl.ChangelogToBranch')
                options = extension.appendNode('options')
                options.appendNode('compareRemote').setValue('origin')
                options.appendNode('compareTarget').setValue('pr/${PR}/head')
            }
        }
    }
    checkoutRetryCount(3)

    environmentVariables {
        env('GITHUB_REPO', github_repo)
        env('COMMIT_STATUS_CONTEXT', 'ci/clone')
        env('VIRTUALENV_NAME', 'raet-pr')
        env('VIRTUALENV_SETUP_STATE_NAME', 'projects.clone')
    }

    // Job Steps
    steps {
        // Setup the required virtualenv
        shell(readFileFromWorkspace('jenkins-seed', 'scripts/prepare-virtualenv.sh'))

        // Set initial commit status
        shell(readFileFromWorkspace('jenkins-seed', 'scripts/set-commit-status.sh'))

        // Compress the checked out workspace
        shell(readFileFromWorkspace('jenkins-seed', 'scripts/compress-workspace.sh'))
    }

    publishers {
        archiveArtifacts('workspace.cpio.xz')

        postBuildTask {
            // Set final commit status
            task('.', readFileFromWorkspace('jenkins-seed', 'scripts/set-commit-status.sh'))
        }
    }
}

// PR Lint Job
job {
    name = 'raet/pr/lint'
    displayName('Lint')
    concurrentBuild(allowConcurrentBuild = true)
    description(project_description + ' - Code Lint')
    label('worker')

    // Parameters Definition
    parameters {
        stringParam('PR')
        stringParam('GIT_COMMIT')
        stringParam('CLONE_BUILD_ID')
    }

    configure {
        job_properties = it.get('properties').get(0)
        github_project_property = job_properties.appendNode(
            'com.coravy.hudson.plugins.github.GithubProjectProperty')
        github_project_property.appendNode('projectUrl').setValue("https://github.com/${github_repo}")
    }

    wrappers {
        // Inject global defined passwords in the environment
        injectPasswords()

        // Cleanup the workspace before starting
        preBuildCleanup()

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

    environmentVariables {
        env('GITHUB_REPO', github_repo)
        env('COMMIT_STATUS_CONTEXT', 'ci/lint')
        env('VIRTUALENV_NAME', 'raet-pr')
        env('VIRTUALENV_SETUP_STATE_NAME', 'projects.raet.lint')
    }

    // Job Steps
    steps {
        // Setup the required virtualenv
        shell(readFileFromWorkspace('jenkins-seed', 'scripts/prepare-virtualenv.sh'))

        // Set initial commit status
        shell(readFileFromWorkspace('jenkins-seed', 'scripts/set-commit-status.sh'))

        // Copy the workspace artifact
        copyArtifacts('raet/pr/clone', 'workspace.cpio.xz') {
            buildNumber('${CLONE_BUILD_ID}')
        }
        shell(readFileFromWorkspace('jenkins-seed', 'scripts/decompress-workspace.sh'))

        // Run Lint Code
        shell(readFileFromWorkspace('jenkins-seed', 'raet/scripts/run-lint.sh'))
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
    name = 'raet/pr/unit'
    displayName('Unit')
    concurrentBuild(allowConcurrentBuild = true)
    description(project_description + ' - Unit Tests')
    label('worker')

    // Parameters Definition
    parameters {
        stringParam('PR')
        stringParam('GIT_COMMIT')
        stringParam('CLONE_BUILD_ID')
    }

    configure {
        job_properties = it.get('properties').get(0)
        github_project_property = job_properties.appendNode(
            'com.coravy.hudson.plugins.github.GithubProjectProperty')
        github_project_property.appendNode('projectUrl').setValue("https://github.com/${github_repo}")
    }

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

    environmentVariables {
        env('GITHUB_REPO', github_repo)
        env('COMMIT_STATUS_CONTEXT', 'ci/unit')
        env('VIRTUALENV_NAME', 'raet-pr')
        env('VIRTUALENV_SETUP_STATE_NAME', 'projects.raet.unit')
    }

    // Job Steps
    steps {
        // Setup the required virtualenv
        shell(readFileFromWorkspace('jenkins-seed', 'scripts/prepare-virtualenv.sh'))

        // Set initial commit status
        shell(readFileFromWorkspace('jenkins-seed', 'scripts/set-commit-status.sh'))

        // Copy the workspace artifact
        copyArtifacts('raet/pr/clone', 'workspace.cpio.xz') {
            buildNumber('${CLONE_BUILD_ID}')
        }
        shell(readFileFromWorkspace('jenkins-seed', 'scripts/decompress-workspace.sh'))

        // Run Unit Tests
        shell(readFileFromWorkspace('jenkins-seed', 'raet/scripts/run-unit.sh'))
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
