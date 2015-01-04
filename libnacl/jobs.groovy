// libnacl Jenkins jobs seed script
@GrabResolver(name='jenkins-dsl-jobs', root='http://saltstack.github.io/jenkins-dsl-jobs/')
@Grab('com.saltstack:jenkins-dsl-jobs:1.2-SNAPSHOT')

import groovy.text.*
import com.saltstack.jenkins.PullRequestAdmins
import com.saltstack.jenkins.RandomString

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

def template_engine = new SimpleTemplateEngine()

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
        slack_notifications.appendNode('startNotification').setValue(false)
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
        readFileFromWorkspace('jenkins-seed', 'libnacl/groovy/master-main-build-flow.groovy')
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

        template_context = [
            commit_status_context: "default"
        ]
        script_template = template_engine.createTemplate(
            readFileFromWorkspace('jenkins-seed', 'templates/post-build-set-commit-status.groovy')
        )
        rendered_script_template = script_template.make(template_context.withDefault{ null })

        groovyPostBuild(rendered_script_template.toString())

        // Cleanup workspace
        wsCleanup()
    }
}

// Clone Master Job
def master_clone_job = job {
    name = 'libnacl/master/clone'
    displayName('Clone Repository')

    concurrentBuild(allowConcurrentBuild = true)
    description(project_description + ' - Clone Repository')
    label('worker')

    configure {
        job_properties = it.get('properties').get(0)
        job_properties.appendNode(
            'hudson.plugins.copyartifact.CopyArtifactPermissionProperty').appendNode(
                'projectNameList').appendNode(
                    'string').setValue('libnacl/master/*')
        github_project_property = job_properties.appendNode(
            'com.coravy.hudson.plugins.github.GithubProjectProperty')
        github_project_property.appendNode('projectUrl').setValue("https://github.com/${github_repo}")

    }

    wrappers {
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

    template_context = [
        commit_status_context: 'ci/clone',
        github_repo: github_repo,
        virtualenv_name: 'libnacl-master',
        virtualenv_setup_state_name: 'projects.clone'
    ]
    script_template = template_engine.createTemplate(
        readFileFromWorkspace('jenkins-seed', 'templates/branches-envvars-commit-status.groovy')
    )
    rendered_script_template = script_template.make(template_context.withDefault{ null })

    environmentVariables {
        groovy(rendered_script_template.toString())
    }

    // Job Steps
    steps {
        // Setup the required virtualenv
        shell(readFileFromWorkspace('jenkins-seed', 'scripts/prepare-virtualenv.sh'))

        // Compress the checked out workspace
        shell(readFileFromWorkspace('jenkins-seed', 'scripts/compress-workspace.sh'))
    }

    publishers {
        archiveArtifacts('workspace.cpio.xz')

        script_template = template_engine.createTemplate(
            readFileFromWorkspace('jenkins-seed', 'templates/post-build-set-commit-status.groovy')
        )
        rendered_script_template = script_template.make(template_context.withDefault{ null })

        groovyPostBuild(rendered_script_template.toString())
    }
}

// Lint Master Job
def master_lint_job = job {
    name = 'libnacl/master/lint'
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

    template_context = [
        commit_status_context: 'ci/lint',
        github_repo: github_repo,
        virtualenv_name: 'libnacl-master',
        virtualenv_setup_state_name: 'projects.libnacl.lint'
    ]
    script_template = template_engine.createTemplate(
        readFileFromWorkspace('jenkins-seed', 'templates/branches-envvars-commit-status.groovy')
    )
    rendered_script_template = script_template.make(template_context.withDefault{ null })

    environmentVariables {
        groovy(rendered_script_template.toString())
    }

    // Job Steps
    steps {
        // Setup the required virtualenv
        shell(readFileFromWorkspace('jenkins-seed', 'scripts/prepare-virtualenv.sh'))

        // Copy the workspace artifact
        copyArtifacts('libnacl/master/clone', 'workspace.cpio.xz') {
            buildNumber('${CLONE_BUILD_ID}')
        }
        shell(readFileFromWorkspace('jenkins-seed', 'scripts/decompress-workspace.sh'))

        // Run Lint Code
        shell(readFileFromWorkspace('jenkins-seed', 'libnacl/scripts/run-lint.sh'))
    }

    publishers {
        // Report Violations
        violations {
            pylint(10, 999, 999, 'pylint-report*.xml')
        }

        script_template = template_engine.createTemplate(
            readFileFromWorkspace('jenkins-seed', 'templates/post-build-set-commit-status.groovy')
        )
        rendered_script_template = script_template.make(template_context.withDefault{ null })

        groovyPostBuild(rendered_script_template.toString())
    }
}

// Master Unit Tests
def master_unit_job = job {
    name = 'libnacl/master/unit'
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

    template_context = [
        commit_status_context: 'ci/unit',
        github_repo: github_repo,
        virtualenv_name: 'libnacl-master',
        virtualenv_setup_state_name: 'projects.libnacl.unit'
    ]
    script_template = template_engine.createTemplate(
        readFileFromWorkspace('jenkins-seed', 'templates/branches-envvars-commit-status.groovy')
    )
    rendered_script_template = script_template.make(template_context.withDefault{ null })

    environmentVariables {
        groovy(rendered_script_template.toString())
    }

    // Job Steps
    steps {
        // Setup the required virtualenv
        shell(readFileFromWorkspace('jenkins-seed', 'scripts/prepare-virtualenv.sh'))

        // Copy the workspace artifact
        copyArtifacts('libnacl/master/clone', 'workspace.cpio.xz') {
            buildNumber('${CLONE_BUILD_ID}')
        }
        shell(readFileFromWorkspace('jenkins-seed', 'scripts/decompress-workspace.sh'))

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

        script_template = template_engine.createTemplate(
            readFileFromWorkspace('jenkins-seed', 'templates/post-build-set-commit-status.groovy')
        )
        rendered_script_template = script_template.make(template_context.withDefault{ null })

        groovyPostBuild(rendered_script_template.toString())
    }
}

dsl_job = job {
    name = 'libnacl/pr/jenkins-seed'
    displayName('PR Jenkins Seed')

    concurrentBuild(allowConcurrentBuild = false)

    description('PR Jenkins Seed')

    label('worker')

    configure {
        it.appendNode('authToken').setValue(RandomString.generate())
        job_properties = it.get('properties').get(0)
        github_project_property = job_properties.appendNode(
            'com.coravy.hudson.plugins.github.GithubProjectProperty')
        github_project_property.appendNode('projectUrl').setValue("https://github.com/${github_repo}")
    }

    wrappers {
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
        default_artifact_days_to_keep,
        default_artifact_nr_of_jobs_to_keep
    )

    environmentVariables {
        template_context = [
            include_open_prs: true
        ]
        script_template = template_engine.createTemplate(
            readFileFromWorkspace('jenkins-seed', 'templates/pr-job-seed-envvars.groovy')
        )
        rendered_script_template = script_template.make(template_context.withDefault{ null })
        groovy(rendered_script_template.toString())
    }

    // Job Steps
    steps {
        dsl {
            removeAction('DELETE')
            text(
                readFileFromWorkspace('jenkins-seed', 'libnacl/groovy/pr-dsl-job.groovy')
            )
        }
    }

    publishers {
        template_context = [
            project: 'libnacl'
        ]
        script_template = template_engine.createTemplate(
            readFileFromWorkspace('jenkins-seed', 'templates/pr-job-seed-post-build.groovy')
        )
        rendered_script_template = script_template.make(template_context.withDefault{ null })
        groovyPostBuild(rendered_script_template.toString())
    }
}

