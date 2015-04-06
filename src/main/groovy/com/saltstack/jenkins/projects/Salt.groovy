package com.saltstack.jenkins.projects

import com.saltstack.jenkins.Project


class Salt extends Project {

    static private EOL_BRANCHES = [
        '2014.1'
    ]

    Salt() {
        super()
        this.name = 'salt'
        this.display_name = 'Salt'
        this.repo = 'saltstack/salt'
        this.create_branches_webhook = true
    }

    def getRepositoryBranches() {
        return super.getRepositoryBranches().grep(~/(develop|([\d]{4}.[\d]{1,2}))/).findAll { branch ->
            this.EOL_BRANCHES.contains(branch) == false
        }
    }
}
