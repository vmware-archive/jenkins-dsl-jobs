package com.saltstack.jenkins.vmname;

import com.saltstack.jenkins.VmName


class Rackspace extends VmName {

    Rackspace() {
        super()
        MAX_SIZE = 256
        replacements = this.replacements + [
            'rackspace', 'rs'
        ]
    }

}
