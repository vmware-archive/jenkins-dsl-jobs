package com.saltstack.jenkins

import groovyx.net.http.HTTPBuilder
import groovyx.net.http.ContentType
import static groovyx.net.http.Method.*


class GitHubMarkup {

    static def toHTML(text, context) {

        def http = new HTTPBuilder('https://api.github.com')
        http.request(POST, ContentType.TEXT) { req ->
            uri.path = '/markdown'
            headers.'User-Agent' = 'Mozilla/5.0'
            headers.'Accept' = 'application/json'
            requestContentType = ContentType.JSON
            body = [
                text: text,
                mode: 'gfm',
                context: context
            ]
            response.success = { resp, reader ->
                return reader.text
            }
        }

    }

}
