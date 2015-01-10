// Update the bootstrap script
@GrabResolver(name='jenkins-dsl-jobs', root='http://saltstack.github.io/jenkins-dsl-jobs/')
@Grab('com.saltstack:jenkins-dsl-jobs:1.2-SNAPSHOT')

import com.saltstack.jenkins.JenkinsPerms

folder {
    name('maintenance')
    displayName('Jenkins Maintenance Jobs')

    authorization {
        JenkinsPerms.usernames.each { username ->
            JenkinsPerms.permissions.each { permname ->
                permission("${permname}:${username}")
            }
        }
    }
}

job {
    name = 'maintenance/update-bootstrap'
    displayName('Update Bootstrap Script')
    description('Update the local copy of the bootstrap script')

    concurrentBuild(allowConcurrentBuild = false)

    // Run in on the master
    label('master')

    parameters {
        choiceParam('BRANCH', ['develop', 'stable'])
        choiceParam('REPOSITORY', ['saltstack', 's0undt3ch'])
    }

    authorization {
        JenkinsPerms.usernames.each { username ->
            JenkinsPerms.permissions.each { permname ->
                permission("${permname}:${username}")
            }
        }
    }

    // Job Steps
    steps {
        shell('wget -O /etc/salt/cloud.deploy.d/bootstrap-salt.sh https://raw.github.com/${REPOSITORY}/salt-bootstrap/${BRANCH}/bootstrap-salt.sh')
    }
}
