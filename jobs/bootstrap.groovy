// Bootstrap Jenkins jobs seed script
import groovy.json.*
import groovy.text.*
import org.apache.commons.lang.RandomStringUtils

// get current thread / Executor
def thr = Thread.currentThread()
// get current build
def build = thr?.executable

// Common variable Definitions
def project = new JsonSlurper().parseText(build.getEnvironment().SEED_PROJECTS).bootstrap
def jenkins_perms = new JsonSlurper().parseText(build.getEnvironment().JENKINS_PERMS)

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
folder(project.name) {
    displayName(project.display_name)
    description = project.description
}
project.branches.each { job_branch ->
    folder("${project.name}/${job_branch}") {
        displayName("${job_branch.capitalize()} Branch")
        description = project.description
    }
}
folder("${project.name}/pr") {
    displayName('Pull Requests')
    description = project.description
}

project.branches.each { job_branch ->
    // Branch Main Job
    def build_flow_job = buildFlowJob("${project.name}/${job_branch}-main-build") {
        displayName("${job_branch.capitalize()} Branch Main Build")
        description(project.description)
        label('worker')
        concurrentBuild(allowConcurrentBuild = true)

        configure {
            it.appendNode('authToken').setValue(new RandomStringUtils().randomAlphanumeric(32))
            it.appendNode('buildNeedsWorkspace').setValue(true)
            job_properties = it.get('properties').get(0)
            job_publishers = it.get('publishers').get(0)
            github_project_property = job_properties.appendNode(
                'com.coravy.hudson.plugins.github.GithubProjectProperty')
            github_project_property.appendNode('projectUrl').setValue("https://github.com/${project.repo}")
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
                [plugin: 'slack@latest']
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
                project.repo,
                branch = "*/${job_branch}",
                protocol = 'https'
            )
        }
        checkoutRetryCount(3)

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
            groovyPostBuild(
                readFileFromWorkspace('maintenance/jenkins-seed', 'groovy/post-build-set-commit-status.groovy')
            )
        }
    }

    // Main Build Push Trigger
    if ( project.setup_push_hooks ) {
        cachefile = build.getWorkspace().child('push-hooks.cache')
        def data;
        try {
            data = new JsonSlurper().parseText(cachefile.readToString())
        } catch (Throwable e) {
            data = [:]
        }
        if ( ! data.containsKey(project.name) ) {
            data[project.name] = []
        }
        if ( ! data[project.name].contains(build_flow_job.name) ) {
            data[project.name].add(build_flow_job.name)
        }
        cachefile.write(new JsonBuilder(data).toString(), 'UTF-8')
    }

    // Clone Job
    freeStyleJob("bootstrap/${job_branch}/clone") {
        displayName('Clone Repository')

        concurrentBuild(allowConcurrentBuild = true)
        description(project.description + ' - Clone Repository')
        label('worker')

        configure {
            job_properties = it.get('properties').get(0)
            job_properties.appendNode(
                'hudson.plugins.copyartifact.CopyArtifactPermissionProperty').appendNode(
                    'projectNameList').appendNode(
                        'string').setValue("${project.name}/${job_branch}/*")
            github_project_property = job_properties.appendNode(
                'com.coravy.hudson.plugins.github.GithubProjectProperty')
            github_project_property.appendNode('projectUrl').setValue("https://github.com/${project.repo}")
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
                project.repo,
                branch = "*/${job_branch}",
                protocol = 'https'
            )
        }
        checkoutRetryCount(3)

        template_context = [
            commit_status_context: 'ci/clone',
            github_repo: project.repo,
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
                readFileFromWorkspace('maintenance/jenkins-seed', 'groovy/post-build-set-commit-status.groovy')
            )
            rendered_script_template = script_template.make(template_context.withDefault{ null })
            groovyPostBuild(rendered_script_template.toString())
        }
    }

    // Lint Job
    freeStyleJob("${project.name}/${job_branch}/lint") {
        displayName('Lint')
        concurrentBuild(allowConcurrentBuild = true)
        description(project.description + ' - Code Lint')
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
            github_project_property.appendNode('projectUrl').setValue("https://github.com/${project.repo}")
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
            github_repo: project.repo,
            virtualenv_setup_state_name: 'projects.bootstrap.lint',
            sudo_salt_call: true
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
            copyArtifacts("${project.name}/${job_branch}/clone", 'workspace.cpio.xz') {
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
                readFileFromWorkspace('maintenance/jenkins-seed', 'groovy/post-build-set-commit-status.groovy')
            )
            rendered_script_template = script_template.make(template_context.withDefault{ null })
            groovyPostBuild(rendered_script_template.toString())

            archiveArtifacts {
                pattern('*.log')
                allowEmpty(true)
            }
        }
    }
}

freeStyleJob("${project.name}/pr/jenkins-seed") {
    displayName('PR Jenkins Seed')

    concurrentBuild(allowConcurrentBuild = false)

    description('PR Jenkins Seed')

    label('worker')

    configure {
        it.appendNode('authToken').setValue(new RandomStringUtils().randomAlphanumeric(32))
        job_properties = it.get('properties').get(0)
        github_project_property = job_properties.appendNode(
            'com.coravy.hudson.plugins.github.GithubProjectProperty')
        github_project_property.appendNode('projectUrl').setValue("https://github.com/${project.repo}")
        auth_matrix = job_properties.appendNode('hudson.security.AuthorizationMatrixProperty')
        auth_matrix.appendNode('blocksInheritance').setValue(true)
    }

    authorization {
        for ( username in jenkins_perms.usernames ) {
            for ( permname in jenkins_perms.project ) {
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
        groovy('''
        import com.saltstack.jenkins.JenkinsPerms
        import com.saltstack.jenkins.PullRequestAdmins
        import com.saltstack.jenkins.projects.Bootstrap

        return [
            SEED_DATA: new Bootstrap().toJSON(include_branches = false, include_prs = true),
            JENKINS_PERMS: JenkinsPerms.toJSON(),
            PULL_REQUEST_ADMINS: PullRequestAdmins.toJSON()
        ]
        '''.stripIndent().trim())
    }


    // Job Steps
    steps {
        dsl {
            removeAction('DISABLE')
            text(
                readFileFromWorkspace('maintenance/jenkins-seed', 'bootstrap/groovy/pr-dsl-job.groovy')
            )
        }
    }

    publishers {
        groovyPostBuild(
            readFileFromWorkspace('maintenance/jenkins-seed', 'groovy/pr-job-seed-post-build.groovy')
        )
    }
}
