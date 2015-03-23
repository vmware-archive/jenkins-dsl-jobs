package com.saltstack.jenkins

import hudson.model.User
import hudson.model.Result;
import jenkins.model.Jenkins
import jenkins.security.ApiTokenProperty
import org.kohsuke.github.GHEvent
import org.kohsuke.github.GitHub
import org.kohsuke.github.GHRepository
import org.kohsuke.github.GHIssueState
import org.kohsuke.github.GHCommitState
import com.cloudbees.jenkins.GitHubRepositoryName


class Project {
    String name;
    String display_name;
    String repo;
    Boolean setup_push_hooks = false;
    Boolean create_branches_webhook = false;
    Boolean set_commit_status = false; 

    protected static String webhooks_user = 'salt-testing';

    private GitHub _github;
    private GHRepository _repo;
    private Boolean _authenticated;

    Project() {
        this.name;
        this.display_name;
        this.repo;
        this.setup_push_hooks = false;
        this.create_branches_webhook = false;
        this.set_commit_status = false;
    }

    def GHRepository getRepository() {
        if ( this._github == null ) {
            try {
                this._github = GitHub.connect()
                this._authenticated = true
            } catch (Throwable e2) {
                this._github = GitHub.connectAnonymously()
                this._authenticated = false
            }
        }
        if ( this._repo == null ) {
            this._repo = this._github.getRepository(this.repo)
        }
        return this._repo
    }

    def GHRepository getAuthenticatedRepository() {
        if ( this._repo == null ) {
            try {
                def github_repo_url = "https://github.com/${this.repo}"
                this._repo = GitHubRepositoryName.create(github_repo_url).resolve().iterator().next()
            } catch (Throwable e1) {
                return null
            }
        }
        return this._repo
    }

    def getRepositoryDescription() {
        if ( getAuthenticatedRepository() != null ) {
            return getAuthenticatedRepository().getDescription()
        }
        return null
    }

    def getRepositoryBranches() {
        def branches = []
        if ( getAuthenticatedRepository() != null ) {
            getAuthenticatedRepository().getBranches().each { branch_name, branch_data ->
                branches.add(branch_name)
            }
        }
        return branches
    }

    def getRepositoryWebHooks() {
        def hooks = []
        if ( this.getAuthenticatedRepository() != null ) {
            if ( this._authenticated == false ) {
                return hooks
            }
            try {
                this.getAuthenticatedRepository().getHooks().each { hook ->
                    if ( hook.getName() == 'web' ) {
                        hooks.add(hook)
                    }
                }
            } catch ( Throwable e ) {
                return hooks
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
            this.getAuthenticatedRepository().createWebHook(
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
            manager.listener.logger.println "This job's build.getProject() weirdly returns null. Not checking existing pull requests web hooks."
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
        def webhooks_apitoken = User.get(this.webhooks_user).getProperty(ApiTokenProperty.class).getApiToken()
        try {
            def webhook_url = pr_seed_job.getAbsoluteUrl() + 'build?token=' + pr_seed_job.getAuthToken().getToken()
            webhook_url = webhook_url.replace(
                'https://', "https://${this.webhooks_user}:${webhooks_apitoken}@").replace(
                    'http://', "http://${this.webhooks_user}:${webhooks_apitoken}@")
            this.getAuthenticatedRepository().createWebHook(
                webhook_url.toURL(),
                [GHEvent.PULL_REQUEST]
            )
        } catch (pr_jenkins_seed_error) {
            manager.listener.logger.println 'Failed to setup pull requests webhook: ' + pr_jenkins_seed_error.toString()
        }
    }

    def setCommitStatus(manager) {
        manager.listener.logger.println "Setting Commit Status to the current build result"

        def result = manager.build.getResult()

        def commit_status_context = manager.envVars.get('COMMIT_STATUS_CONTEXT', 'ci')
        manager.listener.logger.println "GitHub commit status context: ${commit_status_context}"

        def state = GHCommitState.ERROR;

        if (result == null) { // Build is ongoing
            state = GHCommitState.PENDING;
            manager.listener.logger.println 'GitHub commit status is PENDING'
        } else if (result.isBetterOrEqualTo(Result.SUCCESS)) {
            state = GHCommitState.SUCCESS;
            manager.listener.logger.println 'GitHub commit status is SUCCESS'
        } else if (result.isBetterOrEqualTo(Result.UNSTABLE)) {
            state = GHCommitState.FAILURE;
            manager.listener.logger.println 'GitHub commit status is FAILURE'
        } else {
            manager.listener.logger.println 'GitHub commit status is ERROR'
        }

        def git_commit = manager.envVars.get('GIT_COMMIT', manager.envVars.get('ghprbActualCommit'))
        if ( git_commit != null ) {
            def status_result = this.getAuthenticatedRepository().createCommitStatus(
                git_commit,
                state,
                manager.build.getAbsoluteUrl(),
                manager.build.getFullDisplayName(),
                commit_status_context
            )
            if ( ! status_result ) {
                msg = 'Failed to set commit status on GitHub'
                manager.addWarningBadge(msg)
                manager.listener.logger.println msg
            } else {
                msg = "GitHub commit status successfuly set"
                manager.addInfoBadge(msg)
                manager.listener.logger.println(msg)
            }
        } else {
            msg = 'No git commit SHA information could be found. Not setting final commit status information.'
            manager.listener.logger.println(msg)
        }
    }

    def getOpenPullRequests() {
        def prs = []
        if ( this.getAuthenticatedRepository() != null ) {
            this.getAuthenticatedRepository().getPullRequests(GHIssueState.OPEN).each { pr ->
                println '  * Processing PR #' + pr.number
                prs.add([
                    number: pr.number,
                    title: pr.title,
                    url: pr.getHtmlUrl(),
                    body: pr.body,
                    sha: pr.getHead().getSha(),
                    repo: this.repo
                ])
            }
        } else {
            println "Unable to load the authenticated repository instance for ${this.display_name}"
        }

        return prs
    }

}
