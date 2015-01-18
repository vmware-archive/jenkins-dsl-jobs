// $project Jenkins jobs seed script
@GrabResolver(name='jenkins-dsl-jobs', root='http://saltstack.github.io/jenkins-dsl-jobs/')
@Grab('com.saltstack:jenkins-dsl-jobs:1.2-SNAPSHOT')

import groovy.json.*
import groovy.text.*
import jenkins.model.Jenkins
import com.saltstack.jenkins.PullRequestAdmins

// get current thread / Executor
def thr = Thread.currentThread()
// get current build
def build = thr?.executable

def slurper = new groovy.json.JsonSlurper()

def github_json_data = slurper.parseText(build.getEnvironment().get('GITHUB_JSON_DATA', '""'))

// Common variable Definitions
def github_repo = 'saltstack/bootstrap'
def project_description = github_json_data['project_description']

// Job rotation defaults
def default_days_to_keep = 90
def default_nr_of_jobs_to_keep = 180
def default_artifact_days_to_keep = default_days_to_keep
def default_artifact_nr_of_jobs_to_keep = default_nr_of_jobs_to_keep

// Job Timeout defauls
def default_timeout_percent = 150
def default_timeout_builds = 10
def default_timeout_minutes = 15

def new_prs = [:]
def template_engine = new SimpleTemplateEngine()

// Define the folder structure
folder {
    name('bootstrap')
    displayName(github_json_data['bootstrap']['display_name'])
    description = project_description
}
folder {
    name('bootstrap/pr')
    displayName('Pull Requests')
    description = project_description
}

github_json_data['open_prs'].each { pr ->
    folder {
        name("bootstrap/pr/${pr.number}")
        displayName("PR #${pr.number}")
        description = "${pr.title}\n\n${pr.body}"
    }

    try {
        existing_job = Jenkins.instance.getJob('bootstrap').getJob('pr').getJob("${pr.number}").getJob('main-build')
        if ( existing_job == null ) {
            new_prs[pr.number] = pr.sha
        }
    } catch(e) {
        // no existing job
        new_prs[pr.number] = pr.sha
    }

    // PR Main Job
    job(type: BuildFlow) {
        name = "bootstrap/pr/${pr.number}/main-build"
        displayName("Main Build")
        description("${pr.title}\n\n${pr.body}")
        label('worker')
        concurrentBuild(allowConcurrentBuild = false)

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
                admins(PullRequestAdmins.usernames)
            }
        }

        template_context = [
            project: 'bootstrap',
            pr_number: pr.number
        ]
        script_template = template_engine.createTemplate(
            readFileFromWorkspace('maintenance/jenkins-seed', 'bootstrap/templates/pr-main-build-flow.groovy')
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
                        readFileFromWorkspace('maintenance/jenkins-seed', 'groovy/inject-submitter-email-address.groovy')
                    )
                }
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

    // PR Clone Job
    def pr_clone_job = job {
        name = "bootstrap/pr/${pr.number}/clone"
        displayName("Clone Repository")

        concurrentBuild(allowConcurrentBuild = true)
        description("${pr.title}\n\n${pr.body}")
        label('worker')

        parameters {
            stringParam('GIT_COMMIT')
        }

        configure {
            job_properties = it.get('properties').get(0)
            job_properties.appendNode(
                'hudson.plugins.copyartifact.CopyArtifactPermissionProperty').appendNode(
                    'projectNameList').appendNode(
                        'string').setValue("bootstrap/pr/${pr.number}/*")
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
            git {
                remote {
                    github(github_repo, protocol='https')
                    refspec('+refs/pull/*:refs/remotes/origin/pr/*')
                }
                branch("origin/pr/${pr.number}/merge")
                configure {
                    git_extensions = it.appendNode('extensions')
                    extension = git_extensions.appendNode('hudson.plugins.git.extensions.impl.ChangelogToBranch')
                    options = extension.appendNode('options')
                    options.appendNode('compareRemote').setValue('origin')
                    options.appendNode('compareTarget').setValue("pr/${pr.number}/head")
                }
            }
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

    // PR Lint Job
    job {
        name = "bootstrap/pr/${pr.number}/lint"
        displayName("Lint")
        concurrentBuild(allowConcurrentBuild = true)
        description("${pr.title}\n\n${pr.body}")
        label('worker')

        // Parameters Definition
        parameters {
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
            copyArtifacts("bootstrap/pr/${pr.number}/clone", 'workspace.cpio.xz') {
                buildNumber('${CLONE_BUILD_ID}')
            }
            shell(readFileFromWorkspace('maintenance/jenkins-seed', 'scripts/decompress-workspace.sh'))

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

// Write any new PR's to a file so we can trigger then in the post build step
new_prs_file = build.getWorkspace().child('new-prs.txt')
new_prs_file.deleteContents()
json_data = new JsonBuilder(new_prs)
new_prs_file.write(json_data.toString(), 'UTF-8')
