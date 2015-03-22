package com.saltstack.jenkins

import hudson.Functions
import com.saltstack.jenkins.GitHubMarkup

public class RenderUI {
    static def renderPullRequestDescription(pr) {
        return """
        <h3>
            <img src="${hudson.Functions.getResourcePath()}/plugin/github/logov3.png"/>
            <a href="${pr.url}" title="${pr.title}" alt="${pr.title}">#${pr.number}</a>
            &mdash;
            ${pr.title}
        <h3>
        <br/>
        ${GitHubMarkup.toHTML(pr.body, pr.owner.getFullName())}
        """.stripIndent()
    }
}
