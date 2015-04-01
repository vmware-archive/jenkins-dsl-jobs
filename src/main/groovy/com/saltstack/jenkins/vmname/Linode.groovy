package com.saltstack.jenkins.vmname;

import com.saltstack.jenkins.VmName


class Linode extends VmName {
    static MAX_SIZE = 32

    static private def replacements = [
        ['lin', 'l']
    ] + VmName.replacements + [
        ['.', '_'],
        ['linode', 'lin']
    ]

}
