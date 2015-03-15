package com.saltstack.jenkins

import hudson.model.User
import jenkins.model.Jenkins
import jenkins.security.ApiTokenProperty
import org.kohsuke.github.GHEvent
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

    protected static String webhooks_user = 'salt-testing';

    private GitHub _github;
    private GHRepository _repo;
    private Boolean _authenticated;

    def getRepository() {
        if ( this._repo == null ) {
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
        return this._repo
    }

    def getAuthenticatedRepository() {
        if ( this._authenticated == false || this._authenticated == null ) {
            try {
                def github_repo_url = "https://github.com/${this.repo}"
                this._repo = GitHubRepositoryName.create(github_repo_url).resolve().iterator().next()
                this._authenticated = true
            } catch (Throwable e1) {
                this._authenticated = false
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
        if ( getAuthenticatedRepository() != null ) {
            if ( this._authenticated == false ) {
                return hooks
            }
            getAuthenticatedRepository().getHooks().each { hook ->
                if ( hook.getName() == 'web' ) {
                    hooks.add(hook)
                }
            }
        }
        return hooks
    }

    def configureBranchesWebHooks(manager) {
        if ( this.create_branches_webhook != true ) {
            manager.listener.logger.println "Not setting up branches web hooks for ${this.display_name}"
            return
        }
        def running_job = manager.build.getProject()
        if ( running_job == null ) {
            manager.listener.logger.println "This job's build.getProject() weirdly returns null. Not checking existing branches web hooks."
            return
        }
        manager.listener.logger.println "Setting up branches create/delete webhook for ${this.display_name} ..."
        // Delete existing hooks
        def hook_url_regex = 'http(s?)://(.*)@' + running_job.getAbsoluteUrl().replace('https://', '').replace('http://', '') + 'build(.*)'
        def hook_url_pattern = ~hook_url_regex
        manager.listener.logger.println 'Existing webhook regex: ' + hook_url_regex

        this.getRepositoryWebHooks().each { hook ->
            try {
                def hook_config = hook.getConfig()
                if ( hook_url_pattern.matcher(hook_config.url).getCount() > 0 ) {
                    manager.listener.logger.println 'Deleting web hook: ' + hook_config.url
                    hook.delete()
                }
            } catch(Throwable e1) {
                manager.listener.logger.println 'Failed to delete existing webhook:' + e1.toString()
            }
        }
        // Create hooks
        def webhooks_apitoken = User.get(this.webhooks_user).getProperty(ApiTokenProperty.class).getApiToken()
        try {
            def webhook_url = running_job.getAbsoluteUrl() + 'build?token=' + running_job.getAuthToken().getToken()
            webhook_url = webhook_url.replace(
                'https://', "https://${this.webhooks_user}:${webhooks_apitoken}@").replace(
                    'http://', "http://${this.webhooks_user}:${webhooks_apitoken}@")
            repo.createWebHook(
                webhook_url.toURL(),
                [GHEvent.CREATE, GHEvent.DELETE]
            )
        } catch(Throwable e2) {
            manager.listener.logger.println 'Failed to setup create webhook: ' + e2.toString()
        }
    }

    def configurePullRequestsWebHooks(manager) {
        def running_job = manager.build.getProject()
        if ( running_job == null ) {
            manager.listener.logger.println "This job's build.getProject() weirdly returns null. Not checking existing branches web hooks."
            return
        }
        def pr_seed_job = null
        try {
            pr_seed_job = Jenkins.instance.getJob(this.name).getJob('pr').getJob('jenkins-seed')
        } catch (Throwable e1) {
            manager.listener.logger.println "No pull-requests seed job was found for ${this.display_name}"
            return
        }
        if ( pr_seed_job == null ) {
            manager.listener.logger.println "No pull-requests seed job was found for ${this.display_name}"
            return
        }

        manager.listener.logger.println "Setting up pull requests webhook for ${this.display_name} ..."

        // Delete existing hooks
        def hook_url_regex = 'https://(.*)@' + pr_seed_job.getAbsoluteUrl().replace('https://', '').replace('http://', '') + 'build(.*)'
        def hook_url_pattern = ~hook_url_regex
        manager.listener.logger.println 'Existing webhook regex: ' + hook_url_regex

        this.getRepositoryWebHooks().each { hook ->
            try {
                def hook_config = hook.getConfig()
                    if ( hook_url_pattern.matcher(hook_config.url).getCount() > 0 ) {
                    manager.listener.logger.println 'Deleting web hook: ' + hook_config.url
                    hook.delete()
                }
            } catch(e) {
                manager.listener.logger.println 'Failed to delete existing webhook:' + e.toString()
            }
        }
        // Create pull request web hooks
         try {
            def webhook_url = pr_seed_job.getAbsoluteUrl() + 'build?token=' + pr_seed_job.getAuthToken().getToken()
            webhook_url = webhook_url.replace(
                'https://', "https://${this.webhooks_user}:${webhooks_apitoken}@").replace(
                    'http://', "http://${this.webhooks_user}:${webhooks_apitoken}@")
            repo.createWebHook(
                webhook_url.toURL(),
                [GHEvent.PULL_REQUEST]
            )
        } catch (pr_jenkins_seed_error) {
            manager.listener.logger.println 'Failed to setup pull requests webhook: ' + pr_jenkins_seed_error.toString()
        }
    }

}
