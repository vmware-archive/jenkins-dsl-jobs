package com.saltstack.jenkins

class JenkinsPerms {
    static usernames = [
        'thatch45',
        's0undt3ch',
        'cachedout',
        'rallytime',
    ]

    static permissions = [
        'com.cloudbees.plugins.credentials.CredentialsProvider.Create',
        'com.cloudbees.plugins.credentials.CredentialsProvider.Delete',
        'com.cloudbees.plugins.credentials.CredentialsProvider.ManageDomains',
        'com.cloudbees.plugins.credentials.CredentialsProvider.Update'
        'com.cloudbees.plugins.credentials.CredentialsProvider.View',
        'hudson.model.Item.Build',
        'hudson.model.Item.Cancel',
        'hudson.model.Item.Configure'
        'hudson.model.Item.Delete',
        'hudson.model.Item.Discover',
        'hudson.model.Item.Move',
        'hudson.model.Item.Read',
        'hudson.model.Item.Update',
        'hudson.model.Item.ViewStatus',
        'hudson.model.Item.Workspace',
        'hudson.model.Run.Delete',
        'hudson.scm.SCM.Tag',
    ]
}
