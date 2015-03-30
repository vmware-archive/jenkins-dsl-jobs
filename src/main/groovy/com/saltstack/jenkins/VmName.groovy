package com.saltstack.jenkins;

import java.io.IOException;
import java.util.logging.Logger;

import hudson.EnvVars;


class VmName {

    static def generate(build) {
        LOGGER.info("Injecting SALT_VM_NAME");
        EnvVars build_env_vars = new EnvVars();
        try {
            build_env_vars = build.getEnvironment();
            String vm_name_prefix = build_env_vars.get("JENKINS_VM_NAME_PREFIX", "Z");
            String vm_name_suffix = build_env_vars.get("JOB_NAME").replace("/", "-").
                replace("salt", "slt").
                replace("salt-cloud", "s-cloud").
                replace("nightly", "ntly").
                replace("linode", "lin").
                replace("rackspace", "rs");
            String build_number = build_env_vars.get("BUILD_NUMBER").padLeft(4, '0');
            String salt_vm_name = "${vm_name_prefix}-${vm_name_suffix}-${build_number}"
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
