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
    private Boolean _authenticated;

    def getRepository() {
        if ( this._repo == null && this._github == null ) {
            try {
                def github_repo_url = "https://github.com/${this.repo}"
                this._repo = GitHubRepositoryName.create(github_repo_url).resolve().iterator().next()
                this._authenticated = true
            } catch (Throwable e1) {
                if ( this._github == null ) {
                    try {
                        this._github = GitHub.connect()
                        this._authenticated = true
                    } catch (Throwable e2) {
                        this._github = GitHub.connectAnonymously()
                        this._authenticated = false
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

    def getRepositoryWebHooks() {
        def hooks = []
        if ( getRepository() != null ) {
            if ( this._authenticated == false ) {
                return hooks
            }
            getRepository().getHooks().each { hook ->
                if ( hook.getName() == 'web' ) {
                    hooks.add(hook)
                }
            }
        }
        return hooks
    }

}
