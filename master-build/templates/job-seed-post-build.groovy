import groovy.json.*
import org.kohsuke.github.GHEvent
import com.cloudbees.jenkins.GitHubRepositoryName

def projects = '''
$projects
'''
new JsonSlurper().parseText(projects.trim()).each { data ->
    if ( data.add_branches_webhook == true ) {
        manager.listener.logger.println 'Setting up branches create/delete webhook for ' + data.display_name + ' ...'
        def github_repo_url = "https://github.com/" + data.github_repo
        def repo = GitHubRepositoryName.create(github_repo_url).resolve().iterator().next()
        repo.getHooks().each { hook ->
            if ( hook.getName() == 'web' ) {
                try {
                    hook_config = hook.getConfig()
                    if ( hook_config.url.startsWith(job.getAbsoluteUrl()) ) {
                        hook.delete()
                    }
                } catch(e) {
                    manager.listener.logger.println 'Failed to delete existing webhook:' + e.toString()
                }
            }
        }
        def project = manager.build.getProject()
        def webhook_url = project.getAbsoluteUrl() + '?token=' + project.getAuthToken().getToken()
        repo.createWebHook(
            webhook_url.toURL()
            [GHEvent.CREATE, GHEvent.DELETE]
        )
    }
}
