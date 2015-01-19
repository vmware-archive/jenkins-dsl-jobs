import groovy.json.*
import hudson.model.User
import jenkins.model.Jenkins
import jenkins.security.ApiTokenProperty
import org.kohsuke.github.GHEvent
import com.cloudbees.jenkins.GitHubRepositoryName

def processed = []
def running_job = manager.build.getProject()
def webhooks_user = 'salt-testing'
def webhooks_apitoken = User.get(webhooks_user).getProperty(ApiTokenProperty.class).getApiToken()

new JsonSlurper().parseText(manager.envVars['GITHUB_JSON_DATA']).each { name, data ->
    if ( ! processed.contains(name) ) {
        processed.add(name)
        def github_repo_url = "https://github.com/" + data.github_repo
        def repo = GitHubRepositoryName.create(github_repo_url).resolve().iterator().next()
        try {
            repo.getHooks().each { hook ->
                if ( hook.getName() == 'web' ) {
                    if ( data.create_branches_webhook == true ) {
                        manager.listener.logger.println 'Setting up branches create/delete webhook for ' + data.display_name + ' ...'
                        if ( running_job != null ) {
                            try {
                                hook_config = hook.getConfig()
                                hook_url_regex = "https://(.*)@${running_job.getAbsoluteUrl().replace('https://', '').replace('http://', '')}(.*)"
                                hook_url_pattern = ~hook_url_regex
                                if ( hook_url_pattern.matcher(hook_config.url).getCount() > 0 ) {
                                    hook.delete()
                                }
                            } catch(e) {
                                manager.listener.logger.println 'Failed to delete existing webhook:' + e.toString()
                            }
                        } else {
                            manager.listener.logger.println 'This job\'s .getProject() weirdly returns null. Not checking existing branches web hooks.'
                        }
                    }
                    // Let's setup the pull request webhooks if the jobs needing it are found
                    def pr_seed_job = Jenkins.instance.getJob(name).getJob('pr').getJob('maintenance/jenkins-seed')
                    if ( pr_seed_job != null ) {
                        try {
                            hook_config = hook.getConfig()
                            hook_url_regex = "https://(.*)@${pr_seed_job.getAbsoluteUrl().replace('https://', '').replace('http://', '')}(.*)"
                            hook_url_pattern = ~hook_url_regex
                            if ( hook_url_pattern.matcher(hook_config.url).getCount() > 0 ) {
                                hook.delete()
                            }
                        } catch(e) {
                            manager.listener.logger.println 'Failed to delete existing webhook:' + e.toString()
                        }
                    }
                }
            }
            if ( data.create_branches_webhook == true ) {
                try {
                    manager.listener.logger.println 'Setting up branches create/delete webhook for ' + data.display_name + ' ...'
                    if ( running_job != null ) {
                        def webhook_url = running_job.getAbsoluteUrl() + 'build?token=' + running_job.getAuthToken().getToken()
                        webhook_url = webhook_url.replace(
                            'https://', 'https://' + webhooks_user + ':' + webhooks_apitoken + '@').replace(
                                'http://', 'http://' + webhooks_user + ':' + webhooks_apitoken + '@')
                        repo.createWebHook(
                            webhook_url.toURL(),
                            [GHEvent.CREATE, GHEvent.DELETE]
                        )
                    } else {
                        manager.listener.logger.println 'This job\'s .getProject() weirdly returns null. Not setting branches web hooks.'
                    }
                } catch(branches_webhook_error) {
                    manager.listener.logger.println 'Failed to setup create/delete webhook: ' + branches_webhook_error.toString()
                }
            }
            // Let's setup the pull request webhooks if the jobs needing it are found
            try {
                def pr_seed_job = Jenkins.instance.getJob(name).getJob('pr').getJob('jenkins-seed')
                if ( pr_seed_job != null ) {
                    manager.listener.logger.println 'Setting up pull requests webhook for ' + data.display_name + ' ...'
                    def webhook_url = pr_seed_job.getAbsoluteUrl() + 'build?token=' + pr_seed_job.getAuthToken().getToken()
                    webhook_url = webhook_url.replace(
                        'https://', 'https://' + webhooks_user + ':' + webhooks_apitoken + '@').replace(
                            'http://', 'http://' + webhooks_user + ':' + webhooks_apitoken + '@')
                    repo.createWebHook(
                        webhook_url.toURL(),
                        [GHEvent.PULL_REQUEST]
                    )
                }
            } catch (pr_jenkins_seed_error) {
                manager.listener.logger.println 'Failed to setup pull requests webhook: ' + pr_jenkins_seed_error.toString()
            }
        } catch(hooks_error) {
            manager.listener.logger.println 'Unable to query existing hooks for ' + data.display_name + ': ' + hooks_error.toString()
        }
    }
}
