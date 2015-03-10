package com.saltstack.jenkins

import com.saltstack.jenkins.JenkinsPerms

class PullRequestAdmins {

    /* Use this list to add additional Pull Request Administrators besides 
     * the Jenkins Administrators
     */

    static usernames = [
    ] + JenkinsPerms.usernames as Set
}
