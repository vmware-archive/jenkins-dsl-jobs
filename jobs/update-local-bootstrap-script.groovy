// Update the bootstrap script
@GrabResolver(name='jenkins-dsl-jobs', root='http://saltstack.github.io/jenkins-dsl-jobs/')
@GrabResolver(name='repo.jenkins-ci.org', root='http://repo.jenkins-ci.org/public')
@Grab('com.saltstack:jenkins-dsl-jobs:1.3-SNAPSHOT')

import com.saltstack.jenkins.JenkinsPerms

folder {
    name('maintenance')
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
                }
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
            JenkinsPerms.project.each { permname ->
                permission("${permname}:${username}")
            }
        }
    }

    // Job Steps
    steps {
        shell('wget -O /etc/salt/cloud.deploy.d/bootstrap-salt.sh https://raw.github.com/${REPOSITORY}/salt-bootstrap/${BRANCH}/bootstrap-salt.sh')
    }

    publishers {
        groovyPostBuild('''\
            import hudson.model.Result

            def result = manager.build.getResult()
            if (result.isBetterOrEqualTo(Result.SUCCESS)) {
                def process = "sh /etc/salt/cloud.deploy.d/bootstrap-salt.sh -v".execute()
                def bootstrap_version = process.text.split(' -- ')[-1]
                manager.addShortText(
                    "${bootstrap_version} from ${manager.envVars['BRANCH']}@${manager.envVars['REPOSITORY']}",
                    "grey", "white", "0px", "white"
                )
            }
        '''.stripIndent())
    }
}
