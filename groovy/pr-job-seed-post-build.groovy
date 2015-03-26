import com.saltstack.jenkins.Projects

def projects = new Projects()
projects.triggerPullRequestJobs(manager)
projects.cleanOldPullRequests(manager, 7)  // Delete disabled jobs older than 7 days
