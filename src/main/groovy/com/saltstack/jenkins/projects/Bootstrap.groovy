package com.saltstack.jenkins.projects

import com.saltstack.jenkins.Project

final class Bootstrap extends Project {
    {
        name = 'bootstrap'
        display_name = 'Salt Bootstrap'
        repo = 'saltstack/salt-bootstrap'
    }

    def getRepositoryBranches() {
        return ['stable', 'develop']
    }

}
