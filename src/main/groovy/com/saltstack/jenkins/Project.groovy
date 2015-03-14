package com.saltstack.jenkins

import com.cloudbees.jenkins.GitHubRepositoryName

class Project {
    protected static String name;
    protected static String display_name;
    protected static String repo;
    protected static Boolean setup_push_hooks = false;
    protected static Boolean create_branches_webhook = false;
    protected static Boolean set_commit_status = false;

    private _repo = null;

    def getRepository() {
        if ( this._repo == null ) {
            this._repo = GitHubRepositoryName.create(
                "https://github.com/" + this.repo
            ).resolve().iterator().next()
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
