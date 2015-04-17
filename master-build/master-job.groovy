import groovy.json.*
import groovy.text.*
import org.apache.commons.lang.RandomStringUtils

// get current thread / Executor
def thr = Thread.currentThread()

// get current build
def build = thr?.executable

def github_repo = 'saltstack/jenkins-dsl-jobs'

// Job rotation defaults
def default_days_to_keep = 30
def default_nr_of_jobs_to_keep = 80
def default_artifact_days_to_keep = default_days_to_keep
def default_artifact_nr_of_jobs_to_keep = default_nr_of_jobs_to_keep

def template_engine = new SimpleTemplateEngine()
def jenkins_perms = new JsonSlurper().parseText(build.getEnvironment().JENKINS_PERMS)

folder('maintenance') {
    displayName('Jenkins Maintenance Jobs')

    authorization {
        for ( username in jenkins_perms.usernames ) {
            println "Processing username ${username}"
            for ( permname in jenkins_perms.folder ) {
                println "Processing folder permission ${permname}"
                permission("${permname}:${username}")
            }
        }
    }
}


freeStyleJob('maintenance/jenkins-seed') {
    displayName('Jenkins Jobs Seed')

    concurrentBuild(allowConcurrentBuild = true)

    configure {
        job_properties = it.get('properties').get(0)
        job_properties.appendNode('authToken').setValue(new RandomStringUtils().randomAlphanumeric(16))
        github_project_property = job_properties.appendNode(
            'com.coravy.hudson.plugins.github.GithubProjectProperty')
        github_project_property.appendNode('projectUrl').setValue("https://github.com/${github_repo}")
        auth_matrix = job_properties.appendNode('hudson.security.AuthorizationMatrixProperty')
        auth_matrix.appendNode('blocksInheritance').setValue(true)
    }

    authorization {
        for ( username in jenkins_perms.usernames ) {
            println "Processing username ${username}"
            for ( permname in jenkins_perms.project ) {
                println "Processing project permission ${permname}"
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
            branch = '*/testing',
            protocol = 'https'
        )
    }
    checkoutRetryCount(3)

    environmentVariables {
        groovy('''
        import com.saltstack.jenkins.Projects
        import com.saltstack.jenkins.JenkinsPerms
        import com.saltstack.jenkins.PullRequestAdmins

        return [
            SEED_PROJECTS: Projects.toJSON(include_branches = true, include_prs = false),
            JENKINS_PERMS: JenkinsPerms.toJSON(),
            PULL_REQUEST_ADMINS: PullRequestAdmins.toJSON()
        ]
        '''.stripIndent().trim())
    }

    // Job Steps
    steps {
        dsl {
            removeAction('DELETE')
            external('jobs/*.groovy')
        }
    }

    publishers {
        groovyPostBuild(
            readFileFromWorkspace('maintenance/jenkins-master-seed', 'master-build/templates/job-seed-post-build.groovy')
        )
    }
}

freeStyleJob('maintenance/jenkins-salt-seed') {
    displayName('Jenkins Salt Branches Seed')

    concurrentBuild(allowConcurrentBuild = true)

    configure {
        job_properties = it.get('properties').get(0)
        job_properties.appendNode('authToken').setValue(new RandomStringUtils().randomAlphanumeric(16))
        github_project_property = job_properties.appendNode(
            'com.coravy.hudson.plugins.github.GithubProjectProperty')
        github_project_property.appendNode('projectUrl').setValue("https://github.com/${github_repo}")
        auth_matrix = job_properties.appendNode('hudson.security.AuthorizationMatrixProperty')
        auth_matrix.appendNode('blocksInheritance').setValue(true)
    }

    authorization {
        for ( username in jenkins_perms.usernames ) {
            println "Processing username ${username}"
            for ( permname in jenkins_perms.project ) {
                println "Processing project permission ${permname}"
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
            branch = '*/testing',
            protocol = 'https'
        )
    }
    checkoutRetryCount(3)

    environmentVariables {
        groovy('''
        import com.saltstack.jenkins.Projects
        import com.saltstack.jenkins.JenkinsPerms
        import com.saltstack.jenkins.PullRequestAdmins

        return [
            SEED_PROJECTS: Projects.toJSON(include_branches = true, include_prs = false),
            JENKINS_PERMS: JenkinsPerms.toJSON(),
            PULL_REQUEST_ADMINS: PullRequestAdmins.toJSON()
        ]
        '''.stripIndent().trim())
    }

    // Job Steps
    steps {
        dsl {
            removeAction('DELETE')
            external('jobs/salt.groovy')
        }
    }

    publishers {
        groovyPostBuild(
            readFileFromWorkspace('maintenance/jenkins-master-seed', 'master-build/templates/job-seed-post-build.groovy')
        )
    }
}
