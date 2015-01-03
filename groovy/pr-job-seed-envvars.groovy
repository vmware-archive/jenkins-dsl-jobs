import groovy.json.*
import org.kohsuke.github.GHIssueState
import com.cloudbees.jenkins.GitHubRepositoryName
import com.coravy.hudson.plugins.github.GithubProjectProperty


def github_repo_url = currentJob.getProperty(GithubProjectProperty.class).getProjectUrl()

if ( github_repo_url != null ) {
    out.println 'GitHub Repository URL: ' + github_repo_url
    def repo = GitHubRepositoryName.create(github_repo_url.toString()).resolve().iterator().next()
    if ( repo != null ) {
        def open_prs = []
        def closed_prs = []
        out.println 'Processing Open Pull Requests'
        repo.getPullRequests(GHIssueState.OPEN).each { pr ->
            out.println '  * Processing PR #' + pr.number
            open_prs.add([
                number: pr.number,
                title: pr.title,
                body:  pr.body.replace('\n', '<br/>\n'),
                sha: pr.getHead().getSha()
            ])
        }
        /*out.println 'Processing Closed Pull Requests'
        repo.getPullRequests(GHIssueState.CLOSED).each { pr ->
            out.println '  * Processing PR #' + pr.number
            closed_prs.add([
                number: pr.number,
                title: pr.title,
                body:  pr.body,
                sha: pr.getHead().getSha(),
                closed_at: pr.getClosedAt()
            ])
        }*/
        return [
            GITHUB_JSON_DATA: new JsonBuilder([
                open_prs: open_prs,
                //closed_prs: closed_prs,
                project_description: repo.getDescription(),
            ]).toString()
        ]
    }
} else {
    msg = "Unable to find the GitHub project URL from the build's properties"
    out.println msg
}
