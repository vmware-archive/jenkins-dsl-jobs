package com.saltstack.jenkins.projects

import com.saltstack.jenkins.Project


class RAET extends Project {

    RAET() {
        super()
        this.name = 'raet'
        this.display_name = 'RAET'
        this.repo = 'saltstack/raet'
    }
}
