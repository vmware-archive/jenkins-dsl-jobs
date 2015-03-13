package com.saltstack.jenkins

import groovyx.net.http.HTTPBuilder
import groovyx.net.http.ContentType


class GitHubMarkup {

    def toHTML(String text, String context) {
        def http = new HTTPBuilder('https://api.github.com')
        http.request(POST) { req ->
            uri.path = '/markdown'
            requestContentType = ContentType.JSON
            body = [
                text: text,
                mode: 'gfm',
                context: context
            ]
            response.success = { resp ->
                return resp
            }
        }
    }

}
