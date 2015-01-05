import groovy.json.*
import com.cloudbees.jenkins.GitHubRepositoryName

def projects = $projects
def environ_data = [:]
new JsonSlurper().parseText(projects).each { name, data ->
    out.println 'Grabbing data about ' + data.display_name + ' ...'
    def github_repo_url = "https://github.com/" + data.github_repo
    def repo = GitHubRepositoryName.create(github_repo_url).resolve().iterator().next()
    if ( repo != null ) {
        branches = []
        repo.getBranches().each { name, branch ->
            branches.add(name)
        }
        environ_data[name] = [
            display_name: data.display_name,
            github_repo: data.github_repo,
            description: repo.getDescription(),
            add_branches_webhook: data.add_branches_webhook,
            branches: branches
        ]
    } else {
        out.println 'Unable to grab data about ' + data.display_name
    }
}

return [
    GITHUB_JSON_DATA: new JsonBuilder(environ_data).toString()
]
