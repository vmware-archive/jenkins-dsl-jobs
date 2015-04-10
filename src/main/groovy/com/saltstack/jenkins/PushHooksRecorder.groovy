package com.saltstack.jenkins

import groovy.json.*

class PushHooksRecorder {

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
        data = load()
        if ( ! data.contains(project_name) ) {
            data[project_name] = new HashSet()
        }
        data[project_name].add(job_name)
        this.cachefile.write(new JsonBuilder(data).toString(), 'UTF-8')
    }

}
