package com.saltstack.jenkins.projects

import com.saltstack.jenkins.Project


class Salt extends Project {
    {
        name = 'salt'
        display_name = 'Salt'
        repo = 'saltstack/salt'
        // In the production branch/environment, the value below should be true
        create_branches_webhook = false
    }
}
