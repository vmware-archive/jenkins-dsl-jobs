import com.saltstack.jenkins.Projects

def projects = new Projects()
projects.triggerPullRequestJobs(manager)
