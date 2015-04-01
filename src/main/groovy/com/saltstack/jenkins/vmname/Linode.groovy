package com.saltstack.jenkins.vmname;

import com.saltstack.jenkins.VmName


class Linode extends VmName {
    static MAX_SIZE = 32

    def Linode() {
        super()
        replacements = [
            ['lin', 'l']
        ] + this.replacements + [
            ['.', '_'],
            ['linode', 'lin']
        ]
    }

}
