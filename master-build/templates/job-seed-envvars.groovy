import groovy.json.*
import com.cloudbees.jenkins.GitHubRepositoryName

def projects = new JsonSlurper().parseText('''$projects''')
def environ_data = [:]
projects.each { name, data ->
    out.println 'Grabbing data about ' + data.display_name + ' ...'
    def github_repo_url = "https://github.com/" + data.github_repo
    def repo = GitHubRepositoryName.create(github_repo_url).resolve().iterator().next()
    if ( repo != null ) {
        environ_data[name] = [
            display_name: display_name,
            github_repo: github_repo,
            description: repo.getDescription(),
            add_branches_webhook: add_branches_webhook,
        ]
    } else {
        out.println 'Unable to grab data about ' + display_name
    }
}

return [
    GITHUB_JSON_DATA: new JsonBuilder(environ_data).toString()
]
