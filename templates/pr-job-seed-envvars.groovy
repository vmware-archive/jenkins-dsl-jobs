import groovy.json.*
import org.kohsuke.github.GHIssueState
import com.cloudbees.jenkins.GitHubRepositoryName
import com.coravy.hudson.plugins.github.GithubProjectProperty


def github_repo_url = currentJob.getProperty(GithubProjectProperty.class).getProjectUrl()

if ( github_repo_url != null ) {
    out.println 'GitHub Repository URL: ' + github_repo_url
    def repo = GitHubRepositoryName.create(github_repo_url.toString()).resolve().iterator().next()
    if ( repo != null ) {
        <% if ( include_open_prs != null ) { %>
        def open_prs = []
        out.println 'Processing Open Pull Requests'
        repo.getPullRequests(GHIssueState.OPEN).each { pr ->
            out.println '  * Processing PR #' + pr.number
            open_prs.add([
                number: pr.number,
                title: pr.title,
                body:  pr.body.replace('\\n', '<br/>\\n'),
                sha: pr.getHead().getSha()
            ])
        }
        <% } %>
        <% if ( include_closed_prs != null ) { %>
        def closed_prs = []
        out.println 'Processing Closed Pull PullRequestAdminsests'
        repo.getPullRequests(GHIssueState.CLOSED).each { pr ->
            out.println '  * Processing PR #' + pr.number
            closed_prs.add([
                number: pr.number,
                title: pr.title,
                body:  pr.body,
                sha: pr.getHead().getSha(),
                closed_at: pr.getClosedAt()
            ])
        }
        <% } %>
        <% if ( include_branches != null ) { %>
        branches = []
        repo.getBranches().each { name, branch ->
            branches.add(name)
        }
        <% } %>
        return [
            GITHUB_JSON_DATA: new JsonBuilder([
                <% if ( include_open_prs != null ) {
                %>open_prs: open_prs,<% } %>
                <% if ( include_closed_prs != null ) {
                %>closed_prs: closed_prs,<%
                } %><% if ( include_branches != null ) {
                %>branches: branches,<% } %>
                project_description: repo.getDescription(),
            ]).toString()
        ]
    }
} else {
    msg = "Unable to find the GitHub project URL from the build's properties"
    out.println msg
}
