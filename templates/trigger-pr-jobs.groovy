import jenkins.model.Jenkins

def triggered = []
new_prs = manager.build.getWorkspace().child('new-prs.txt').readToString().split(/(;|,|\n)/)
new_prs.each { pr_id ->
    if ( triggered.contains(pr_id) == false) {
        try {
            pr_job = Jenkins.instance.getJob('$project').getJob('pr').getJob(pr_id).getJob('main-build')
            manager.listener.logger.println("Triggering build for ${pr_job.getFullDisplayName()}")
            trigger = pr_job.triggers.iterator().next().value
            repo = trigger.getRepository()
            pr = repo.getPullRequest(pr_id.toInteger())
            repo.check(pr)
            pr = repo.pulls.get(pr_id.toInteger())
            repo.helper.builds.build(pr, pr.author, 'Job Created. Start Initial Build')
        } catch(e) {
            manager.listener.logger.println "Failed to get Job libnacl/pr/${pr_id}/main-build"
        }
        triggered.add(pr_id)
    }
}
