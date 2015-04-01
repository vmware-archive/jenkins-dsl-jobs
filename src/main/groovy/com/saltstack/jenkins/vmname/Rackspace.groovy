package com.saltstack.jenkins.vmname;

import com.saltstack.jenkins.VmName


class Rackspace extends VmName {

    Rackspace() {
        super()
        replacements = this.replacements + [
            'rackspace', 'rs'
        ]
    }

}
