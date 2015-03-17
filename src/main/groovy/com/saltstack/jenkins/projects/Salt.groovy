package com.saltstack.jenkins.projects

import com.saltstack.jenkins.Project


class Salt extends Project {

    Salt() {
        this.name = 'salt'
        this.display_name = 'Salt'
        this.repo = 'saltstack/salt'
        this.create_branches_webhook = true
    }

    def getRepositoryBranches() {
        return super.getRepositoryBranches().grep(~/(develop|([\d]{4}.[\d]{1,2}))/)
    }
}
