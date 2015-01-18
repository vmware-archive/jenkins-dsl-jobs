// bootstrap Jenkins jobs seed script
@GrabResolver(name='jenkins-dsl-jobs', root='http://saltstack.github.io/jenkins-dsl-jobs/')
@Grab('com.saltstack:jenkins-dsl-jobs:1.2-SNAPSHOT')

import groovy.json.*
import groovy.text.*
import com.saltstack.jenkins.JenkinsPerms
import com.saltstack.jenkins.PullRequestAdmins
import com.saltstack.jenkins.RandomString

// get current thread / Executor
def thr = Thread.currentThread()
// get current build
def build = thr?.executable

// Common variable Definitions
def github_repo = 'saltstack/salt-bootstrap'
def github_json_data = new JsonSlurper().parseText(build.getEnvironment()['GITHUB_JSON_DATA'])
def project_description = github_json_data['bootstrap']['description']
def branches = ['stable', 'develop']

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
    name('bootstrap')
    displayName(github_json_data['bootstrap']['display_name'])
    description = project_description
}
branches.each { job_branch ->
    folder {
        name("bootstrap/${job_branch}")
        displayName("${job_branch.capitalize()} Branch")
        description = project_description
    }
}
folder {
    name('bootstrap/pr')
    displayName('Pull Requests')
    description = project_description
}

branches.each { job_branch ->
    // Branch Main Job
    job(type: BuildFlow) {
        name = "bootstrap/${job_branch}-main-build"
        displayName("${job_branch.capitalize()} Branch Main Build")
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
                [plugin: 'slack']
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
                branch = "*/${job_branch}",
                protocol = 'https'
            )
        }
        checkoutRetryCount(3)

        // Job Triggers
        triggers {
            githubPush()
        }

        template_context = [
            job_branch: job_branch,
        ]
        script_template = template_engine.createTemplate(
            readFileFromWorkspace('maintenance/jenkins-seed', 'bootstrap/templates/branches-main-build-flow.groovy')
        )
        rendered_script_template = script_template.make(template_context.withDefault{ null })
        buildFlow(
            rendered_script_template.toString()
        )

        publishers {
            // Report Violations
            violations {
                checkstyle(10, 999, 999, 'lint/checkstyle.xml')
            }

            template_context = [
                commit_status_context: "default"
            ]
            script_template = template_engine.createTemplate(
                readFileFromWorkspace('maintenance/jenkins-seed', 'templates/post-build-set-commit-status.groovy')
            )
            rendered_script_template = script_template.make(template_context.withDefault{ null })

            groovyPostBuild(rendered_script_template.toString())

            // Cleanup workspace
            wsCleanup()
        }
    }


    // Clone Job
    job {
        name = "bootstrap/${job_branch}/clone"
        displayName('Clone Repository')

        concurrentBuild(allowConcurrentBuild = true)
        description(project_description + ' - Clone Repository')
        label('worker')

        configure {
            job_properties = it.get('properties').get(0)
            job_properties.appendNode(
                'hudson.plugins.copyartifact.CopyArtifactPermissionProperty').appendNode(
                    'projectNameList').appendNode(
                        'string').setValue("bootstrap/${job_branch}/*")
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
                branch = "*/${job_branch}",
                protocol = 'https'
            )
        }
        checkoutRetryCount(3)

        template_context = [
            commit_status_context: 'ci/clone',
            github_repo: github_repo,
        ]
        script_template = template_engine.createTemplate(
            readFileFromWorkspace('maintenance/jenkins-seed', 'templates/branches-envvars-commit-status.groovy')
        )
        rendered_script_template = script_template.make(template_context.withDefault{ null })

        environmentVariables {
            groovy(rendered_script_template.toString())
        }

        // Job Steps
        steps {
            // Compress the checked out workspace
            shell(readFileFromWorkspace('maintenance/jenkins-seed', 'scripts/compress-workspace.sh'))
        }

        publishers {
            archiveArtifacts('workspace.cpio.xz')

            script_template = template_engine.createTemplate(
                readFileFromWorkspace('maintenance/jenkins-seed', 'templates/post-build-set-commit-status.groovy')
            )
            rendered_script_template = script_template.make(template_context.withDefault{ null })
            groovyPostBuild(rendered_script_template.toString())
        }
    }

    // Lint Job
    job {
        name = "bootstrap/${job_branch}/lint"
        displayName('Lint')
        concurrentBuild(allowConcurrentBuild = true)
        description(project_description + ' - Code Lint')
        label('container')

        // Parameters Definition
        parameters {
            stringParam('CLONE_BUILD_ID')
            stringParam('GIT_COMMIT')
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
            virtualenv_setup_state_name: 'projects.bootstrap.lint'
        ]
        script_template = template_engine.createTemplate(
            readFileFromWorkspace('maintenance/jenkins-seed', 'templates/branches-envvars-commit-status.groovy')
        )
        rendered_script_template = script_template.make(template_context.withDefault{ null })

        environmentVariables {
            groovy(rendered_script_template.toString())
        }

        // Job Steps
        steps {
            // Copy the workspace artifact
            copyArtifacts("bootstrap/${job_branch}/clone", 'workspace.cpio.xz') {
                buildNumber('${CLONE_BUILD_ID}')
            }
            shell(readFileFromWorkspace('maintenance/jenkins-seed', 'scripts/decompress-workspace.sh'))

            // Setup the required virtualenv
            shell(readFileFromWorkspace('maintenance/jenkins-seed', 'scripts/prepare-virtualenv.sh'))

            // Run Lint Code
            shell(readFileFromWorkspace('maintenance/jenkins-seed', 'bootstrap/scripts/run-lint.sh'))
        }

        publishers {
            // Report Violations
            violations {
                checkstyle(10, 999, 999, 'checkstyle.xml')
            }

            script_template = template_engine.createTemplate(
                readFileFromWorkspace('maintenance/jenkins-seed', 'templates/post-build-set-commit-status.groovy')
            )
            rendered_script_template = script_template.make(template_context.withDefault{ null })
            groovyPostBuild(rendered_script_template.toString())
        }
    }
}

dsl_job = job {
    name = 'bootstrap/pr/jenkins-seed'
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
        auth_matrix = job_properties.appendNode('hudson.security.AuthorizationMatrixProperty')
        auth_matrix.appendNode('blocksInheritance').setValue(true)
    }

    authorization {
        JenkinsPerms.usernames.each { username ->
            JenkinsPerms.permissions.each { permname ->
                permission("${permname}:${username}")
            }
        }
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

    environmentVariables {
        template_context = [
            include_open_prs: true
        ]
        script_template = template_engine.createTemplate(
            readFileFromWorkspace('maintenance/jenkins-seed', 'templates/pr-job-seed-envvars.groovy')
        )
        rendered_script_template = script_template.make(template_context.withDefault{ null })
        groovy(rendered_script_template.toString())
    }

    // Job Steps
    steps {
        dsl {
            removeAction('DELETE')
            text(
                readFileFromWorkspace('maintenance/jenkins-seed', 'bootstrap/groovy/pr-dsl-job.groovy')
            )
        }
    }

    publishers {
        template_context = [
            project: 'bootstrap'
        ]
        script_template = template_engine.createTemplate(
            readFileFromWorkspace('maintenance/jenkins-seed', 'templates/pr-job-seed-post-build.groovy')
        )
        rendered_script_template = script_template.make(template_context.withDefault{ null })
        groovyPostBuild(rendered_script_template.toString())
    }
}
