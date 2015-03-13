package com.saltstack.jenkins

import com.cloudbees.jenkins.GitHubRepositoryName


class Project {
    public static String name;
    public static String display_name;
    public static String repo;
    public static Boolean setup_push_hooks = false;
    public static Boolean create_branches_webhook = false;
    public static Boolean set_commit_status = false;

    private _repo = null;

    def getRepository() {
        if ( this._repo == null ) {
            this.repo = GitHubRepositoryName.create(
                "https://github.com/" + this.repo
            ).resolve().iterator().next()
        }
        return this._repo
    }

    def getRepositoryDescription() {
        if ( this.getRepository() != null ) {
            return this.getRepository().getDescription()
        }
        return null
    }

    def getRepositoryBranches() {
        branches = []
        if ( this.getRepository() != null ) {
            this.getRepository().getBranches().each { branch_name, branch_data ->
                branches.add(branch_name)
            }
        }
        return branches
    }
}


class SaltBootstrap extends Project {
    public static String name = 'boostrap'
    public static String display_name = 'Salt Bootstrap'
    public static String github_repo = 'saltstack/salt-bootstrap'
}


class LibNACL extends Project {
    public static String name = 'libnacl'
    public static String display_name = 'libnacl'
    public static String github_repo = 'saltstack/libnacl'
}


class RAET extends Project {
    public static String name = 'raet'
    public static String display_name = 'RAET'
    public static String github_repo = 'saltstack/raet'
}


class Salt extends Project {
    public static String name = 'salt'
    public static String display_name = 'Salt'
    public static String github_repo = 'saltstack/salt'
    // In the production branch/environment, the value below should be true
    public static Boolean create_branches_webhook = false
}


class Sorbic extends Project {
    public static String name = 'sorbic'
    public static String display_name = 'Sorbic'
    public static String github_repo = 'thatch45/sorbic'
}


def projects = [
    'libnacl': LibNACL,
    'raet': RAET,
    'salt': Salt,
    'sorbic': Sorbic,
    'boostrap': SaltBootstrap
]
