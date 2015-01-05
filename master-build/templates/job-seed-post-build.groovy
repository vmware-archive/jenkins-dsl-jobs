import groovy.json.*
import jenkins.model.Jenkins
import org.kohsuke.github.GHEvent
import com.cloudbees.jenkins.GitHubRepositoryName

new JsonSlurper().parseText(manager.envVars['GITHUB_JSON_DATA']).each { name, data ->
    def github_repo_url = "https://github.com/" + data.github_repo
    def repo = GitHubRepositoryName.create(github_repo_url).resolve().iterator().next()
    try {
        repo.getHooks().each { hook ->
            if ( hook.getName() == 'web' ) {
                if ( data.create_branches_webhook == true ) {
                    manager.listener.logger.println 'Setting up branches create/delete webhook for ' + data.display_name + ' ...'
                    def project = manager.build.getProject()
                    try {
                        hook_config = hook.getConfig()
                        if ( hook_config.url.startsWith(project.getAbsoluteUrl()) ) {
                            hook.delete()
                        }
                    } catch(e) {
                        manager.listener.logger.println 'Failed to delete existing webhook:' + e.toString()
                    }
                    def webhook_url = project.getAbsoluteUrl() + '?token=' + project.getAuthToken().getToken()
                    repo.createWebHook(
                        webhook_url.toURL()
                        [GHEvent.CREATE, GHEvent.DELETE]
                    )
                }
                // Let's setup the pull request webhooks if the jobs needing it are found
                try {
                    job = Jenkins.instance.getJob(name).getJob('pr').getJob('jenkins-seed')
                    manager.listener.logger.println 'Setting up pull requests webhook for ' + data.display_name + ' ...'
                    try {
                        hook_config = hook.getConfig()
                        if ( hook_config.url.startsWith(job.getAbsoluteUrl()) ) {
                            hook.delete()
                        }
                    } catch(e) {
                        manager.listener.logger.println 'Failed to delete existing webhook:' + e.toString()
                    }
                    def webhook_url = job.getAbsoluteUrl() + '?token=' + project.getAuthToken().getToken()
                    repo.createWebHook(
                        webhook_url.toURL()
                        [GHEvent.PULL_REQUEST]
                    )
                } catch (pr_jenkins_seed_error) {
                    manager.listener.logger.println 'Failed to setup pull requests webhook: ' + pr_jenkins_seed_error.toString()
                }
            }
        }
    } catch(hooks_error) {
        manager.listener.logger.println 'Unable to query existing hooks for ' + data.display_name + ': ' + hooks_error.toString()
    }
}
