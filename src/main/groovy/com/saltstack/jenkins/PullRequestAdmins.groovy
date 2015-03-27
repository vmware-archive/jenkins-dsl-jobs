package com.saltstack.jenkins

import groovy.json.*
import com.saltstack.jenkins.JenkinsPerms


class PullRequestAdmins {

    /* Use this list to add additional Pull Request Administrators besides 
     * the Jenkins Administrators
     */

    static usernames = [
    ] + JenkinsPerms.usernames as Set

    def static toMap() {
        return [
            usernames: PullRequestAdmins.usernames
        ]
    }

    def static toJSON() {
        return new JsonBuilder(PullRequestAdmins.toMap()).toString()
    }

}
