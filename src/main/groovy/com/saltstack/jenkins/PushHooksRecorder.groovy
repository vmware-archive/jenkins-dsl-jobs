package com.saltstack.jenkins

import groovy.json.*
import java.util.logging.Level
import java.util.logging.Logger

class PushHooksRecorder {

    def cachefile;
    private static final Logger LOGGER = Logger.getLogger(Project.class.getName());

    PushHooksRecorder(build) {
        this.cachefile = build.getWorkspace().child('push-hooks.cache')
    }

    def load() {
        try {
            return new JsonSlurper().parseText(this.cachefile.readToString()) as Set
        catch (Throwable e) {
            return [] as Set
        }
    }

    def record(job_name) {
        data = load()
        data.add(job_name.split('/'))
        this.cachefile.write(new JsonBuilder(data).toString(), 'UTF-8')
    }

}
