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
            return new JsonSlurper().parseText(this.cachefile.readToString())
        } catch (Throwable e) {
            return [:]
        }
    }

    def record(project_name, job_name) {
        LOGGER.log(Level.INFO, "Recording job name '${job_name}' for project '${project_name}'")
        data = load()
        if ( ! data.contains(project_name) ) {
            data[project_name] = []
        }
        data[project_name].add(job_name)
        this.cachefile.write(new JsonBuilder(data).toString(), 'UTF-8')
    }

}
