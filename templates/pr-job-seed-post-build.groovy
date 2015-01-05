import groovy.json.*
import groovy.text.*
import jenkins.model.Jenkins
import org.kohsuke.github.GHCommitState
import com.cloudbees.jenkins.GitHubRepositoryName
import com.coravy.hudson.plugins.github.GithubProjectProperty

def triggered = []
def slurper = new JsonSlurper()
new_prs_file = manager.build.getWorkspace().child('new-prs.txt')
if ( new_prs_file.exists() ) {
    new_prs = slurper.parseText(new_prs_file.readToString())
    new_prs.each { pr_id, commit_sha ->
        if ( triggered.contains(pr_id) == false) {
            try {
                pr_job = Jenkins.instance.getJob('$project').getJob('pr').getJob(pr_id).getJob('main-build')
                def github_repo_url = manager.build.getProject().getProperty(GithubProjectProperty.class).getProjectUrl()
                repo = GitHubRepositoryName.create(github_repo_url.toString())
                manager.listener.logger.println("Triggering build for " + pr_job.getFullDisplayName() + " @ " + commit_sha)
                try {
                    repo.createCommitStatus(
                        commit_sha,
                        GHCommitState.SUCCESS,
                        pr_job.getAbsoluteUrl(),
                        pr_job.getFullDisplayName(),
                        'ci/create-jobs'
                    )
                } catch(create_commit_error) {
                    manager.listener.logger.println "Failed to set commit status: " + create_commit_error.toString()
                }
                pull_req = repo.getPullRequest(pr_id.toInteger())
                repo.check(pull_req)
                pull_req = repo.pulls.get(pr_id.toInteger())
                repo.helper.builds.build(pull_req, pull_req.author, 'Job Created. Start Initial Build')
            } catch(e) {
                manager.listener.logger.println "Failed to get Job " + '$project' + "/pr/" + pr_id + "/main-build: " + e.toString()
            }
            triggered.add(pr_id)
        }
    }
} else {
    manager.listener.logger.println "The 'new-prs.txt' file was not found"
}
