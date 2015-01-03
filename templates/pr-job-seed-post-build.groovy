import groovy.text.*
import jenkins.model.Jenkins
import org.kohsuke.github.GHCommitState

def triggered = []
def slurper = new groovy.json.JsonSlurper()
def github_json_data = slurper.parseText(manager.build.getEnvironment().get('GITHUB_JSON_DATA', '""'))
new_prs = slurper.parseText(manager.build.getWorkspace().child('new-prs.txt').readToString())
new_prs.each { pr ->
    if ( triggered.contains(pr.key) == false) {
        try {
            pr_job = Jenkins.instance.getJob('$project').getJob('pr').getJob(pr.key).getJob('main-build')
            manager.listener.logger.println("Triggering build for " + pr_job.getFullDisplayName())
            trigger = pr_job.triggers.iterator().next().value
            repo = trigger.getRepository()
            repo.createCommitStatus(
                pr.value,
                GHCommitState.PENDING,
                pr_job.getAbsoluteUrl(),
                pr_job.getFullDisplayName(),
                'ci/create-jobs'
            )
            pull_req = repo.getPullRequest(pr.key.toInteger())
            repo.check(pull_req)
            pull_req = repo.pulls.get(pr.key.toInteger())
            repo.helper.builds.build(pull_req, pull_req.author, 'Job Created. Start Initial Build')
        } catch(e) {
            manager.listener.logger.println "Failed to get Job " + '$project' + "/pr/${pr_id}/main-build"
        }
        triggered.add(pr_id)
    }
}
