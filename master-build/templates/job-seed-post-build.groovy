import com.saltstack.jenkins.Projects

def projects = new Projects()
projects.setup_projects_webhooks(manager)
projects.trigger_projects_pull_request_seed_jobs(manager)
