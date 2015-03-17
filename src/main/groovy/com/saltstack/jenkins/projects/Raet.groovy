package com.saltstack.jenkins.projects

import com.saltstack.jenkins.Project


class Raet extends Project {

    Raet() {
        super()
        this.name = 'raet'
        this.display_name = 'RAET'
        this.repo = 'saltstack/raet'
    }
}
