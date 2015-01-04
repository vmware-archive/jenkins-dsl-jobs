import jenkins.model.Jenkins
import org.kohsuke.github.GHEvent
import com.cloudbees.jenkins.GitHubRepositoryName

[['libnacl', 'saltstack/libnacl'],
 ['raet', 'saltstack/raet'],
 ['salt', 'saltstack/salt'],
 ['sorbic', 'thatch45/sorbic']].each { project, github_repo ->
    job = Jenkins.instance.getJob(project).get('pr').get('jenkins-seed')
    if ( job != null ) {
        repo = GitHubRepositoryName.create("https://github.com/${github_repo}").resolve().iterator().next()
        repo.getHooks().each { hook ->
            if ( hook.getName() == 'web' ) {
                try {
                    hook_config = hook.getConfig()
                    if ( hook_config.url.startsWith(job.getAbsoluteUrl()) ) {
                        hook.delete()
                    }
                } catch(e) {
                }
            }
        }
        repo.createWebHook(
            "${job.getAbsoluteUrl()}?token=${job.getAuthToken().getToken()}",
            [GHEvent.PULL_REQUEST]
        )
    }
}
