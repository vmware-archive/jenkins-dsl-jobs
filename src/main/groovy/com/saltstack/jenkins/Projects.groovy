package com.saltstack.jenkins

import groovy.json.*
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

    static def toMap(Boolean include_branches = false, Boolean include_prs = false) {
        def data = [:]
        this.get_projects().each() { project ->
            data[project.name] = project.toMap(include_branches, include_prs)
        }
        return data
    }

    static def toJSON(Boolean include_branches = false, Boolean include_prs = false) {
        return new JsonBuilder(this.toMap(include_branches, include_prs)).toString()
    }

    def setup_projects_webhooks(manager) {
        this.get_projects().each() { project ->
            manager.listener.logger.println "Setting up webhooks for ${project.display_name}"
            project.configureBranchesWebHooks(manager)
            project.configurePullRequestsWebHooks(manager)
        }
    }

    def filterOutProject(build) {
        def match = null
        this.get_projects().find { project ->
            def github_repo_url = build.getProject().getProperty(GithubProjectProperty.class).getProjectUrl().toString()
            if ( github_repo_url[-1] == '/' ) {
                github_repo_url = github_repo_url[0..-2]
            }
            if ( github_repo_url == "https://github.com/${project.repo}" ) {
                match = project
                return true
            }
            return false
        }
        return match
    }

    def setCommitStatusPre(currentBuild, commit_status_context, out) {
        def project = this.filterOutProject(currentBuild)
        if ( project.set_commit_status ) {
            project.setCommitStatusPre(currentBuild, commit_status_context, out)
        } else {
            out.println "Setting commit status for project ${project.display_name} is disabled. Skipping..."
        }
    }

    def setCommitStatusPost(manager) {
        def project = this.filterOutProject(manager.build)
        if ( project.set_commit_status ) {
            project.setCommitStatusPost(manager)
        } else {
            manager.listener.logger.println "Setting commit status for project ${project.display_name} is disabled. Skipping..."
        }
    }

    def triggerPullRequestJobs(manager) {
        this.filterOutProject(manager.build).triggerPullRequestJobs(manager)
    }
}
