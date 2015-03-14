package com.saltstack.jenkins

import org.kohsuke.github.GitHub
import org.kohsuke.github.GHRepository


class Project {
    protected static String name;
    protected static String display_name;
    protected static String repo;
    protected static Boolean setup_push_hooks = false;
    protected static Boolean create_branches_webhook = false;
    protected static Boolean set_commit_status = false;

    private GHRepository _repo;

    def getRepository() {
        if ( this._repo == null ) {
            def GitHub github;
            try {
                github = GitHub.connect()
            } catch (Throwable e) {
                github = GitHub.connectAnonymously()
            }
            this._repo = github.getRepository()
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
