package com.saltstack.jenkins

import com.saltstack.jenkins.projects.*
import com.coravy.hudson.plugins.github.GithubProjectProperty

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

    def setCommitStatus(manager) {
        this.get_projects().each() { project ->
            def github_repo_url = manager.build.getProject().getProperty(GithubProjectProperty.class).getProjectUrl()
            if ( github_repo_url[-1] == '/' ) {
                github_repo_url = github_repo_url[0..-1]
            }
            if ( github_repo_url == "https://github.com/${project.repo}" ) {
                project.setCommitStatus(manager)
            }
        }
    }
}
