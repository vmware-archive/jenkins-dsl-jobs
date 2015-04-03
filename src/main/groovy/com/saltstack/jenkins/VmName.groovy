package com.saltstack.jenkins;

import java.io.IOException;
import java.util.logging.Logger;

import hudson.EnvVars;


class VmName {

    /*
     * The replacements order is replace last item first
     */

    static MAX_SIZE;
    def replacements;

    VmName() {
        replacements = [
            ['slt', 's'],
            ['dev', 'd'],
            ['cld', 'c'],
            ['salt', 'slt'],
            ['s-cloud', 'cld'],
            ['2015.2', '15_2'],
            ['2014.7', '14_7'],
            ['2014.1', '14_1'],
            ['devel', 'dev'],
            ['bs', 'bootstrap'],
            ['develop', 'devel'],
            ['salt-cloud', 's-cloud'],
            ['nightly', 'ntly'],
            ['freebsd', 'fbsd'],
            ['centos-', 'cent'],
            ['ubuntu', 'ubtu'],
            ['debian', 'deb'],
            ['fedora-', 'f'],
            ['opensuse', 'osuse'],
            ['-cloud-', '-'],
        ]
    }

    def generate(build) {
        LOGGER.info("Injecting SALT_VM_NAME");
        EnvVars build_env_vars = new EnvVars();
        try {
            build_env_vars = build.getEnvironment();
            String vm_name_prefix = build_env_vars.get("JENKINS_VM_NAME_PREFIX", "Z");
            String vm_name_suffix = build_env_vars.get("JOB_NAME").replace("/", "-");
            String build_number = build_env_vars.get("BUILD_NUMBER").padLeft(4, '0');
            String salt_vm_name = "${vm_name_prefix}-${vm_name_suffix}-${build_number}";

            def local_replacements = this.replacements.collect()
            while ( true ) {
                if ( local_replacements.size() <= 0 ) {
                    break
                }
                def replacement = local_replacements.pop()
                salt_vm_name = salt_vm_name.replace(replacement[0], replacement[1])
                if ( salt_vm_name.size() <= this.MAX_SIZE ) {
                    break
                }
            }
            LOGGER.info("SALT_VM_NAME = ${salt_vm_name}");
            return salt_vm_name
        } catch (IOException e) {
            LOGGER.warning("Failed to inject SALT_VM_NAME: " + e.toString());
        } catch (InterruptedException e) {
            LOGGER.warning("Failed to inject SALT_VM_NAME: " + e.toString());
        }
    }

    private static final Logger LOGGER = Logger.getLogger(VmName.class.getName());
}
