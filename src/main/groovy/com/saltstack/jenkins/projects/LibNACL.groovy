package com.saltstack.jenkins.projects

import com.saltstack.jenkins.Project


class LibNACL extends Project {

    LibNACL() {
        super()
        this.name = 'libnacl'
        this.display_name = 'libnacl'
        this.repo = 'saltstack/libnacl'
    }
}
