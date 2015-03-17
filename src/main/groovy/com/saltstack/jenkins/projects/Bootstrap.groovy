package com.saltstack.jenkins.projects

import com.saltstack.jenkins.Project

final class Bootstrap extends Project {

    Bootstrap() {
        super()
        this.name = 'bootstrap'
        this.display_name = 'Salt Bootstrap'
        this.repo = 'saltstack/salt-bootstrap'
    }

    def getRepositoryBranches() {
        return ['stable', 'develop']
    }

}
