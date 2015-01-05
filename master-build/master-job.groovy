@GrabResolver(name='jenkins-dsl-jobs', root='http://saltstack.github.io/jenkins-dsl-jobs/')
@Grab('com.saltstack:jenkins-dsl-jobs:1.2-SNAPSHOT')

import groovy.text.*
import com.saltstack.jenkins.JenkinsPerms
import com.saltstack.jenkins.RandomString

def github_repo = 'saltstack/jenkins-dsl-jobs'

// Job rotation defaults
def default_days_to_keep = 30
def default_nr_of_jobs_to_keep = 80
def default_artifact_days_to_keep = default_days_to_keep
def default_artifact_nr_of_jobs_to_keep = default_nr_of_jobs_to_keep

def template_engine = new SimpleTemplateEngine()

projects = [
    libnacl: [
        display_name: 'libnacl',
        githib_repo: 'saltstack/libnacl',
        create_branches_webhook: false
    ],
    raet: [
        display_name: 'RAET',
        github_repo: 'saltstack/raet',
        create_branches_webhook: false
    ],
    salt: [
        display_name: 'Salt',
        github_repo: 'saltstack/salt',
        create_branches_webhook: true
    ],
    sorbic: [
        display_name: 'Sorbic',
        github_repo: 'thatch45/sorbic',
        create_branches_webhook: false
    ],
]

job {
    name = 'jenkins-seed'
    displayName('Jenkins Jobs Seed')

    concurrentBuild(allowConcurrentBuild = true)

    configure {
        job_properties = it.get('properties').get(0)
        job_properties.appendNode('authToken').setValue(RandomString.generate())
        github_project_property = job_properties.appendNode(
            'com.coravy.hudson.plugins.github.GithubProjectProperty')
        github_project_property.appendNode('projectUrl').setValue("https://github.com/${github_repo}")
        auth_matrix = job_properties.appendNode('hudson.security.AuthorizationMatrixProperty')
        auth_matrix.appendNode('blocksInheritance').setValue(true)
    }

    authorization {
        JenkinsPerms.usernames.each { username ->
            JenkinsPerms.permissions.each { permname ->
                permission(permname, username)
            }
        }
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

    environmentVariables {
        template_context = [
            projects: projects.toString()
        ]
        script_template = template_engine.createTemplate(
            readFileFromWorkspace('jenkins-seed', 'master-build/templates/job-seed-envvars.groovy')
        )
        rendered_script_template = script_template.make(template_context.withDefault{ null })
        groovy(rendered_script_template.toString())
    }

    // Job Steps
    steps {
        dsl {
            removeAction('DELETE')
            external('jobs/*.groovy')
        }
    }

    publishers {
        template_context = [
            projects: projects.toString()
        ]
        script_template = template_engine.createTemplate(
            readFileFromWorkspace('jenkins-seed', 'master-build/templates/job-seed-post-build.groovy')
        )
        rendered_script_template = script_template.make(template_context.withDefault{ null })
        groovyPostBuild(rendered_script_template.toString())
    }
}
