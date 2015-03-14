package com.saltstack.jenkins

import org.kohsuke.github.GitHub
import org.kohsuke.github.GHRepository
import com.cloudbees.jenkins.GitHubRepositoryName


class Project {
    protected static String name;
    protected static String display_name;
    protected static String repo;
    protected static Boolean setup_push_hooks = false;
    protected static Boolean create_branches_webhook = false;
    protected static Boolean set_commit_status = false;

    private GitHub _github;
    private GHRepository _repo;

    def getRepository() {
        if ( this._repo == null && this._github == null ) {
            try {
                def github_repo_url = "https://github.com/${this.repo}"
                this._repo = GitHubRepositoryName.create(github_repo_url).resolve().iterator().next()
            } catch (Throwable e1) {
                if ( this._github == null ) {
                    try {
                        this._github = GitHub.connect()
                    } catch (Throwable e2) {
                        this._github = GitHub.connectAnonymously()
                    }
                }
                this._repo = this._github.getRepository(this.repo)
            }
        }
        return this._repo
    }

    def getRepositoryDescription() {
        if ( getRepository() != null ) {
            return getRepository().getDescription()
        }
        return null
    }

    def getRepositoryBranches() {
        def branches = []
        if ( getRepository() != null ) {
            getRepository().getBranches().each { branch_name, branch_data ->
                branches.add(branch_name)
            }
        }
        return branches
    }

}
