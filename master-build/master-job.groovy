import groovy.json.*
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
        github_repo: 'saltstack/libnacl',
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
    bootstrap: [
        display_name: 'Salt Bootstrap',
        github_repo: 'saltstack/salt-bootstrap',
        create_branches_webhook: false
    ]
]

folder('maintenance') {
    displayName('Jenkins Maintenance Jobs')

    configure {
        folder_properties = it.get('properties').get(0)
        auth_matrix_prop = folder_properties.appendNode(
            'com.cloudbees.hudson.plugins.folder.properties.AuthorizationMatrixProperty'
        )
        JenkinsPerms.usernames.each { username ->
            JenkinsPerms.folder.each { permname ->
                auth_matrix_prop.appendNode('permission').setValue(
                    "${permname}:${username}"
                )
            }
        }
    }
}

freeStyleJob('maintenance/jenkins-seed') {
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
            JenkinsPerms.project.each { permname ->
                permission("${permname}:${username}")
            }
        }
    }

    wrappers {
        // Add timestamps to console log
        timestamps()

        // Color Support to console log
        colorizeOutput('xterm')
    }

    // Delete old jobs
    logRotator(
        default_days_to_keep,
        default_nr_of_jobs_to_keep,
        default_artifact_days_to_keep,
        default_artifact_nr_of_jobs_to_keep
    )

    scm {
        github (
            github_repo,
            branch = '*/master',
            protocol = 'https'
        )
    }
    checkoutRetryCount(3)

    environmentVariables {
        template_context = [
            projects: "'''${new JsonBuilder(projects).toString()}'''"
        ]
        script_template = template_engine.createTemplate(
            readFileFromWorkspace('maintenance/jenkins-master-seed', 'master-build/templates/job-seed-envvars.groovy')
        )
        rendered_script_template = script_template.make(template_context.withDefault{ null })
        groovy(rendered_script_template.toString())
    }

    // Job Steps
    steps {
        gradle {
            gradleName('gradle')
            useWrapper(false)
            description('Build the required dependencies')
        }

        dsl {
            removeAction('DELETE')
            external('jobs/*.groovy')
            additionalClasspath(
                '''lib/*
                src/main/groovy'''.stripIndent()
            )
        }
    }

    publishers {
        template_context = [
            projects: "'''${new JsonBuilder(projects).toString()}'''"
        ]
        script_template = template_engine.createTemplate(
            readFileFromWorkspace('maintenance/jenkins-master-seed', 'master-build/templates/job-seed-post-build.groovy')
        )
        rendered_script_template = script_template.make(template_context.withDefault{ null })
        groovyPostBuild(rendered_script_template.toString())
    }
}
