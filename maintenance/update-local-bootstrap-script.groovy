// Update the bootstrap script
import groovy.json.*

// get current thread / Executor
def thr = Thread.currentThread()

// get current build
def build = thr?.executable

def jenkins_perms = new JsonSlurper().parseText(build.getEnvironment().JENKINS_PERMS)


freeStyleJob('maintenance/update-bootstrap') {
    displayName('Update Bootstrap Script')
    description('Update the local copy of the bootstrap script')

    concurrentBuild(allowConcurrentBuild = false)

    // Run in on the master
    label('master')

    parameters {
        choiceParam('BRANCH', ['develop', 'stable'])
        choiceParam('REPOSITORY', ['saltstack', 's0undt3ch'])
    }

    configure {
        job_properties = it.get('properties').get(0)
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

    // Job Steps
    steps {
        shell('sudo wget -O /etc/salt/cloud.deploy.d/bootstrap-salt.sh https://raw.github.com/${REPOSITORY}/salt-bootstrap/${BRANCH}/bootstrap-salt.sh')
    }

    publishers {
        groovyPostBuild('''\
            import hudson.model.Result

            def result = manager.build.getResult()
            if (result.isBetterOrEqualTo(Result.SUCCESS)) {
                def process = "sh /etc/salt/cloud.deploy.d/bootstrap-salt.sh -v".execute()
                def bootstrap_version = process.text.split(' -- ')[-1].split()[-1]
                manager.addShortText(
                    "v${bootstrap_version} from ${manager.envVars['BRANCH']}@${manager.envVars['REPOSITORY']}",
                    "grey", "white", "0px", "white"
                )
            }
        '''.stripIndent())
    }
}
