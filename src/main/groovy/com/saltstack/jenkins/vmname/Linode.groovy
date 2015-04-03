package com.saltstack.jenkins.vmname;

import com.saltstack.jenkins.VmName


class Linode extends VmName {

    def Linode() {
        super()
        MAX_SIZE = 32
        replacements = [
            ['lin', 'l']
        ] + this.replacements + [
            ['.', '_'],
            ['linode', 'lin']
        ]
    }

}
