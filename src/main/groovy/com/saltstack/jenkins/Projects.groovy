package com.saltstack.jenkins

import com.saltstack.jenkins.projects.*

class Projects {

    static def get_projects() {
        return [
            new Bootstrap(),
            new LibNACL(),
            new RAET(),
            new Salt(),
            new Sorbic()
        ]
    }

    def setup_projects_webhooks(manager) {
        this.get_projects().each() { project ->
            manager.listener.logger.println "Setting up webhooks for ${project.display_name}"
            project.configureBranchesWebHooks(manager)
            project.configurePullRequestsWebHooks(manager)
        }
    }
}
