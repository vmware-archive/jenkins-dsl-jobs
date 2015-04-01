package com.saltstack.jenkins.vmname;

import com.saltstack.jenkins.VmName


class Rackspace extends VmName {

    static private def replacements = VmName.replacements + [
        'rackspace', 'rs'
    ]

}
