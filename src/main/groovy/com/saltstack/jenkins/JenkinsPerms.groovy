package com.saltstack.jenkins

import groovy.json.*
import hudson.security.Permission


class JenkinsPerms {
    static usernames = [
        'basepi',
        'cachedout',
        'cro',
        'dmurphy18',
        'goneballistic',
        'jfindlay',
        'jrporcaro',
        'pass-by-value',
        'pitatus',
        'rallytime',
        'rickh563',
        's0undt3ch',
        'salt-jenkins',
        'salt-testing',
        'SaltyBray',
        'shanedlee',
        'ssgward',
        'techhat',
        'terminalmage',
        'thatch45',
        'UtahDave',
        'whiteinge'
    ] as Set

    static available = hudson.security.Permission.getAll().collect { perm -> perm.getId() } as Set

    static folder = [
        'hudson.model.Item.Build',
        'hudson.model.Item.Cancel',
        'hudson.model.Item.Configure',
        'hudson.model.Item.Create',
        'hudson.model.Item.Delete',
        'hudson.model.Item.Discover',
        'hudson.model.Item.Move',
        'hudson.model.Item.Read',
        'hudson.model.Item.Workspace',
        'hudson.model.Run.Delete',
        'hudson.model.Run.Update',
    ].findAll { permission -> available.contains(permission) } as Set

    static project = [
        'com.cloudbees.plugins.credentials.CredentialsProvider.Create',
        'com.cloudbees.plugins.credentials.CredentialsProvider.Delete',
        'com.cloudbees.plugins.credentials.CredentialsProvider.ManageDomains',
        'com.cloudbees.plugins.credentials.CredentialsProvider.Update',
        'com.cloudbees.plugins.credentials.CredentialsProvider.View',
        'hudson.model.Item.Build',
        'hudson.model.Item.Cancel',
        'hudson.model.Item.Configure',
        'hudson.model.Item.Delete',
        'hudson.model.Item.Discover',
        'hudson.model.Item.Move',
        'hudson.model.Item.Read',
        'hudson.model.Item.Update',
        'hudson.model.Item.ViewStatus',
        'hudson.model.Item.Workspace',
        'hudson.model.Run.Delete',
        'hudson.model.Run.Update',
        'hudson.scm.SCM.Tag'
    ].findAll { permission -> available.contains(permission) } as Set

    def static toMap() {
        return [
            usernames: JenkinsPerms.usernames,
            available: JenkinsPerms.available,
            folder: JenkinsPerms.folder,
            project: JenkinsPerms.project
        ]
    }

    def static toJSON() {
        return new JsonBuilder(JenkinsPerms.toMap()).toString()
    }
}
