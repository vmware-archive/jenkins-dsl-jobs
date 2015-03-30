// Salt Jenkins jobs seed script
import groovy.json.*
import groovy.text.*
import org.apache.commons.lang.RandomStringUtils

// get current thread / Executor
def thr = Thread.currentThread()
// get current build
def build = thr?.executable

// Common variable Definitions
def project = new JsonSlurper().parseText(build.getEnvironment().SEED_PROJECTS).salt
def jenkins_perms = new JsonSlurper().parseText(build.getEnvironment().JENKINS_PERMS)


// Job rotation defaults
def default_days_to_keep = 90
def default_nr_of_jobs_to_keep = 180
def default_artifact_days_to_keep = default_days_to_keep
def default_artifact_nr_of_jobs_to_keep = default_nr_of_jobs_to_keep

// Job Timeout defauls
def default_timeout_percent = 150
def default_timeout_builds = 10
def default_timeout_minutes = 90

def salt_build_types = [
    'Cloud': [
        'Arch',
        'CentOS 5',
        'CentOS 6',
        'CentOS 7',
        'Debian 7',
        'Fedora 20',
        'openSUSE 13',
        'Ubuntu 10.04',
        'Ubuntu 12.04',
        'Ubuntu 14.04'
    ],
    'KVM': [
    ]
]

def salt_cloud_providers = [
    'Linode',
    'Rackspace'
]

def template_engine = new SimpleTemplateEngine()

// Define the folder structure
folder(project.name) {
    displayName(project.display_name)
    description = project.description
}

project.branches.each { branch_name ->
    def branch_folder_name = "salt/${branch_name.toLowerCase()}"
    folder(branch_folder_name) {
        displayName("${branch_name.capitalize()} Branch")
        description = project.description
    }

    salt_build_types.each { build_type, vm_names ->

        def build_type_l = build_type.toLowerCase()

        if ( vm_names != [] ) {
            def build_type_folder_name = "${branch_folder_name}/${build_type_l}"
            folder(build_type_folder_name) {
                displayName("${build_type} Builds")
                description = project.description
            }

            if (build_type_l == 'cloud') {
                salt_cloud_providers.each { provider_name ->

                    def provider_name_l = provider_name.toLowerCase()

                    cloud_provider_folder_name = "${build_type_folder_name}/${provider_name_l}"
                    folder(cloud_provider_folder_name) {
                        displayName(provider_name)
                        description = project.description
                    }
                }
            }
        }
    }
}


project.branches.each { branch_name ->

    def branch_name_l = branch_name.toLowerCase()

    // Clone Job
    freeStyleJob("salt/${branch_name_l}/clone") {
        displayName('Clone Repository')

        concurrentBuild(allowConcurrentBuild = true)
        description(project.description + ' - Clone Repository')
        label('worker')

        configure {
            job_properties = it.get('properties').get(0)
            job_properties.appendNode(
                'hudson.plugins.copyartifact.CopyArtifactPermissionProperty').appendNode(
                    'projectNameList').appendNode(
                        'string').setValue("salt/${branch_name_l}/*")
            github_project_property = job_properties.appendNode(
                'com.coravy.hudson.plugins.github.GithubProjectProperty')
            github_project_property.appendNode('projectUrl').setValue("https://github.com/${project.repo}")
        }

        wrappers {
            // Cleanup the workspace before starting
            preBuildCleanup()

            // Add timestamps to console log
            timestamps()

            // Color Support to console log
            colorizeOutput('xterm')

            // Build Timeout
            timeout {
                elastic(
                    percentage = default_timeout_percent,
                    numberOfBuilds = default_timeout_builds,
                    minutesDefault= default_timeout_minutes
                )
                writeDescription('Build failed due to timeout after {0} minutes')
            }
        }

        // Delete old jobs
        /* Since we're just cloning the repository in order to make it an artifact to
        * user as workspace for all other jobs, we only need to keep the artifact for
        * a couple of minutes. Since one day is the minimum....
        */
        logRotator(
            default_days_to_keep,
            default_nr_of_jobs_to_keep,
            1,  //default_artifact_days_to_keep,
            default_artifact_nr_of_jobs_to_keep
        )

        // scm configuration
        scm {
            github(
                project.repo,
                branch = "*/${branch_name}",
                protocol = 'https'
            )
        }
        checkoutRetryCount(3)

        template_context = [
            commit_status_context: 'ci',
            github_repo: project.repo,
            sudo_salt_call: true,
            branch_name: branch_name,
            branch_name_l: branch_name_l,
            virtualenv_name: "salt-${branch_name_l}",
            virtualenv_setup_state_name: 'projects.clone'
        ]
        script_template = template_engine.createTemplate(
            readFileFromWorkspace('maintenance/jenkins-seed', 'templates/branches-envvars-commit-status.groovy')
        )
        rendered_script_template = script_template.make(template_context.withDefault{ null })

        environmentVariables {
            groovy(rendered_script_template.toString())
        }

        // Job Steps
        steps {
            // Setup the required virtualenv
            shell(readFileFromWorkspace('maintenance/jenkins-seed', 'scripts/prepare-virtualenv.sh'))

            // Compress the checked out workspace
            shell(readFileFromWorkspace('maintenance/jenkins-seed', 'scripts/compress-workspace.sh'))
        }

        publishers {
            archiveArtifacts {
                pattern('*.log')
                pattern('workspace.cpio.xz')
            }

            script_template = template_engine.createTemplate(
                readFileFromWorkspace('maintenance/jenkins-seed', 'groovy/post-build-set-commit-status.groovy')
            )
            rendered_script_template = script_template.make(template_context.withDefault{ null })

            groovyPostBuild(rendered_script_template.toString())
        }
    }

    // Lint Job
    freeStyleJob("salt/${branch_name_l}/lint") {
        displayName('Lint')
        concurrentBuild(allowConcurrentBuild = true)
        description(project.description + ' - Code Lint')
        label('worker')

        // Parameters Definition
        parameters {
            stringParam('CLONE_BUILD_ID')
        }

        configure {
            job_properties = it.get('properties').get(0)
            github_project_property = job_properties.appendNode(
                'com.coravy.hudson.plugins.github.GithubProjectProperty')
            github_project_property.appendNode('projectUrl').setValue("https://github.com/${project.repo}")
        }

        wrappers {
            // Cleanup the workspace before starting
            preBuildCleanup()

            // Add timestamps to console log
            timestamps()

            // Color Support to console log
            colorizeOutput('xterm')

            // Build Timeout
            timeout {
                elastic(
                    percentage = default_timeout_percent,
                    numberOfBuilds = default_timeout_builds,
                    minutesDefault= default_timeout_minutes
                )
                writeDescription('Build failed due to timeout after {0} minutes')
            }
        }

        // Delete old jobs
        logRotator(
            default_days_to_keep,
            default_nr_of_jobs_to_keep,
            default_artifact_days_to_keep,
            default_artifact_nr_of_jobs_to_keep
        )

        template_context = [
            commit_status_context: 'ci/lint',
            github_repo: project.repo,
            sudo_salt_call: true,
            branch_name: branch_name,
            branch_name_l: branch_name_l,
            virtualenv_name: "salt-${branch_name_l}",
            virtualenv_setup_state_name: "projects.salt.${branch_name_l}.lint"
        ]
        script_template = template_engine.createTemplate(
            readFileFromWorkspace('maintenance/jenkins-seed', 'templates/branches-envvars-commit-status.groovy')
        )
        rendered_script_template = script_template.make(template_context.withDefault{ null })

        environmentVariables {
            groovy(rendered_script_template.toString())
        }

        // Job Steps
        steps {
            // Setup the required virtualenv
            shell(readFileFromWorkspace('maintenance/jenkins-seed', 'scripts/prepare-virtualenv.sh'))

            // Copy the workspace artifact
            copyArtifacts("salt/${branch_name_l}/clone", 'workspace.cpio.xz') {
                buildNumber('${CLONE_BUILD_ID}')
            }
            shell(readFileFromWorkspace('maintenance/jenkins-seed', 'scripts/decompress-workspace.sh'))

            // Run Lint Code
            shell(readFileFromWorkspace('maintenance/jenkins-seed', 'salt/scripts/run-lint.sh'))
        }

        publishers {
            // Report Violations
            violations {
                pylint(10, 999, 999, 'pylint-report*.xml')
            }

            script_template = template_engine.createTemplate(
                readFileFromWorkspace('maintenance/jenkins-seed', 'groovy/post-build-set-commit-status.groovy')
            )
            rendered_script_template = script_template.make(template_context.withDefault{ null })

            groovyPostBuild(rendered_script_template.toString())

            archiveArtifacts {
                pattern('*.log')
                allowEmpty(true)
            }
       }
    }

    salt_build_types.each { build_type, vm_names ->

        def build_type_l = build_type.toLowerCase()

        if ( vm_names != [] ) {
            buildFlowJob("salt/${branch_name.toLowerCase()}-${build_type_l}-main-build") {
                displayName("${branch_name.capitalize()} Branch ${build_type} Main Build")
                description(project.description)
                label('worker')
                concurrentBuild(allowConcurrentBuild = true)

                parameters {
                    choiceParam('PROVIDER', salt_cloud_providers)
                }

                configure {
                    it.appendNode('buildNeedsWorkspace').setValue(true)
                    job_publishers = it.get('publishers').get(0)
                    job_publishers.appendNode(
                        'org.zeroturnaround.jenkins.flowbuildtestaggregator.FlowTestAggregator',
                        [plugin: 'build-flow-test-aggregator@latest']
                    )
                    job_properties = it.get('properties').get(0)
                    github_project_property = job_properties.appendNode(
                        'com.coravy.hudson.plugins.github.GithubProjectProperty')
                    github_project_property.appendNode('projectUrl').setValue("https://github.com/${project.repo}")
                    slack_notifications = job_properties.appendNode(
                        'jenkins.plugins.slack.SlackNotifier_-SlackJobProperty')
                    slack_notifications.appendNode('room').setValue('#jenkins')
                    slack_notifications.appendNode('startNotification').setValue(false)
                    slack_notifications.appendNode('notifySuccess').setValue(true)
                    slack_notifications.appendNode('notifyAborted').setValue(true)
                    slack_notifications.appendNode('notifyNotBuilt').setValue(true)
                    slack_notifications.appendNode('notifyFailure').setValue(true)
                    slack_notifications.appendNode('notifyBackToNormal').setValue(true)
                    job_publishers.appendNode(
                        'jenkins.plugins.slack.SlackNotifier',
                        [plugin: 'slack@latest']
                    )
                }

                wrappers {
                    // Add timestamps to console log
                    timestamps()

                    // Color Support to console log
                    colorizeOutput('xterm')

                    // Build Timeout
                    timeout {
                        elastic(
                            percentage = default_timeout_percent,
                            numberOfBuilds = default_timeout_builds,
                            minutesDefault= default_timeout_minutes
                        )
                        writeDescription('Build failed due to timeout after {0} minutes')
                    }
                }

                // Delete old jobs
                logRotator(
                    default_days_to_keep,
                    default_nr_of_jobs_to_keep,
                    default_artifact_days_to_keep,
                    default_artifact_nr_of_jobs_to_keep
                )

                // Job Triggers
                /* triggers disabled for now
                triggers {
                    githubPush()
                }
                */

                template_vm_data = []
                vm_names.each { vm_name ->
                    def vm_name_nospc = vm_name.toLowerCase().replace(' ', '-')
                    def vm_name_nodots = vm_name.toLowerCase().replace(' ', '_').replace('.', '_')
                    template_vm_data.add(
                        [vm_name_nodots, vm_name_nospc]
                    )
                }
                template_context = [
                    build_type_l: build_type_l,
                    branch_name: branch_name,
                    vm_names: template_vm_data
                ]
                flow_script_template = template_engine.createTemplate(
                    readFileFromWorkspace('maintenance/jenkins-seed', 'salt/templates/flow-script.groovy')
                )
                flow_script_template_text = flow_script_template.make(template_context.withDefault{ null })

                buildFlow(flow_script_template_text.toString())

                publishers {
                    template_context = [
                        commit_status_context: 'ci'
                    ]
                    script_template = template_engine.createTemplate(
                        readFileFromWorkspace('maintenance/jenkins-seed', 'groovy/post-build-set-commit-status.groovy')
                    )
                    rendered_script_template = script_template.make(template_context.withDefault{ null })

                    groovyPostBuild(rendered_script_template.toString())
                }
            }

            if (build_type_l == 'cloud') {
                salt_cloud_providers.each { provider_name ->

                    def provider_name_l = provider_name.toLowerCase()

                    vm_names.each { vm_name ->
                        def job_name = vm_name.toLowerCase().replace(' ', '-')
                        def vm_name_nodots = vm_name.replace(' ', '_').replace('.', '_').toLowerCase()
                        freeStyleJob("salt/${branch_name_l}/${build_type_l}/${provider_name_l}/${job_name}") {
                            displayName(vm_name)
                            concurrentBuild(allowConcurrentBuild = true)
                            description("${project.description} - ${build_type} - ${provider_name} - ${vm_name}")
                            label('cloud')

                            // Parameters Definition
                            parameters {
                                stringParam('GIT_COMMIT')
                            }

                            configure {
                                job_properties = it.get('properties').get(0)
                                github_project_property = job_properties.appendNode(
                                    'com.coravy.hudson.plugins.github.GithubProjectProperty')
                                    github_project_property.appendNode('projectUrl').setValue("https://github.com/${project.repo}")
                            }

                            wrappers {
                                // Cleanup the workspace before starting
                                preBuildCleanup()

                                // Add timestamps to console log
                                timestamps()

                                // Color Support to console log
                                colorizeOutput('xterm')

                                // Build Timeout
                                timeout {
                                    elastic(
                                        percentage = default_timeout_percent,
                                        numberOfBuilds = default_timeout_builds,
                                        minutesDefault= default_timeout_minutes
                                    )
                                    writeDescription('Build failed due to timeout after {0} minutes')
                                }
                            }
                            template_context = [
                                commit_status_context: "ci/${job_name}",
                                github_repo: project.repo,
                                sudo_salt_call: true,
                                branch_name: branch_name,
                                branch_name_l: branch_name_l,
                                build_vm_source: "${provider_name_l}_${vm_name_nodots}",
                                provider_name_upper: provider_name.toUpperCase(),
                                virtualenv_name: "salt-remote-testrun",
                                virtualenv_setup_state_name: "projects.salt.cloud-testrun",
                                system_site_packages: true
                            ]
                            script_template = template_engine.createTemplate(
                                readFileFromWorkspace('maintenance/jenkins-seed', 'templates/branches-envvars-commit-status.groovy')
                            )
                            rendered_script_template = script_template.make(template_context.withDefault{ null })

                            environmentVariables {
                                groovy(rendered_script_template.toString())
                            }

                            // Job Steps
                            steps {
                                // Setup the required virtualenv
                                shell(readFileFromWorkspace('maintenance/jenkins-seed', 'scripts/prepare-virtualenv.sh'))

                                // Run Unit Tests
                                shell(readFileFromWorkspace('maintenance/jenkins-seed', 'salt/scripts/branches-run-tests.sh'))
                            }

                            publishers {
                                postBuildTask {
                                    // Download remote files
                                    task('.', readFileFromWorkspace('maintenance/jenkins-seed', 'salt/scripts/download-remote-files.sh'))
                                    // Shutdown VM
                                    task('.', readFileFromWorkspace('maintenance/jenkins-seed', 'salt/scripts/shutdown-cloud-vm.sh'))
                                }

                                // Archive Artifacts
                                archiveArtifacts {
                                    pattern('*.log')
                                    pattern('artifacts/logs/*')
                                    pattern('artifacts/packages/*')
                                }

                                // Report Coverage
                                cobertura('artifacts/coverage/coverage.xml') {
                                    failNoReports = false
                                }

                                // Junit Reports
                                archiveJunit('artifacts/xml-unittests-output/*.xml') {
                                    retainLongStdout(true)
                                    testDataPublishers {
                                        publishTestStabilityData()
                                    }
                                }

                                script_template = template_engine.createTemplate(
                                    readFileFromWorkspace('maintenance/jenkins-seed', 'groovy/post-build-set-commit-status.groovy')
                                )
                                rendered_script_template = script_template.make(template_context.withDefault{ null })
                                groovyPostBuild(rendered_script_template.toString())
                            }
                        }
                    }
                }
            } else {
                vm_names.each { vm_name ->
                    def job_name = vm_name.toLowerCase().replace(' ', '-')
                    freeStyleJob("salt/${branch_name}/${build_type_l}/${job_name}") {
                        displayName(vm_name)
                        concurrentBuild(allowConcurrentBuild = true)
                        description("${project.description} - ${build_type} - ${vm_name}")
                        label('container')

                        configure {
                            job_properties = it.get('properties').get(0)
                            github_project_property = job_properties.appendNode(
                                'com.coravy.hudson.plugins.github.GithubProjectProperty')
                                github_project_property.appendNode('projectUrl').setValue("https://github.com/${project.repo}")
                        }

                        wrappers {
                            // Cleanup the workspace before starting
                            preBuildCleanup()

                            // Add timestamps to console log
                            timestamps()

                            // Color Support to console log
                            colorizeOutput('xterm')

                            // Build Timeout
                            timeout {
                                elastic(
                                    percentage = default_timeout_percent,
                                    numberOfBuilds = default_timeout_builds,
                                    minutesDefault= default_timeout_minutes
                                )
                                writeDescription('Build failed due to timeout after {0} minutes')
                            }
                        }

                        template_context = [
                            commit_status_context: "ci/${job_name}",
                            github_repo: project.repo,
                            sudo_salt_call: true,
                            branch_name: branch_name,
                            branch_name_l: branch_name_l,
                            build_vm_source: "${provider_name_l}_${vm_name_nodots}",
                            provider_name_upper: provider_name.toUpperCase(),
                            virtualenv_name: "salt-remote-testrun",
                            virtualenv_setup_state_name: "projects.salt.cloud-testrun",
                            sudo_salt_call: true,
                            system_site_packages: true
                        ]
                        script_template = template_engine.createTemplate(
                            readFileFromWorkspace('maintenance/jenkins-seed', 'templates/branches-envvars-commit-status.groovy')
                        )
                        rendered_script_template = script_template.make(template_context.withDefault{ null })

                        environmentVariables {
                            groovy(rendered_script_template.toString())
                        }

                        // Job Steps
                        steps {
                            // Setup the required virtualenv
                            shell(readFileFromWorkspace('maintenance/jenkins-seed', 'scripts/prepare-virtualenv.sh'))

                            // Run Unit Tests
                            shell(readFileFromWorkspace('maintenance/jenkins-seed', 'salt/scripts/branches-run-tests.sh'))
                        }

                        publishers {
                            // Archive Artifacts
                            archiveArtifacts {
                                pattern('*.log')
                                pattern('artifacts/logs/*')
                                pattern('artifacts/packages/*')
                            }

                            // Report Coverage
                            cobertura('artifacts/coverage/coverage.xml') {
                                failNoReports = false
                            }

                            // Junit Reports
                            archiveJunit('artifacts/xml-unittests-output/*.xml') {
                                retainLongStdout(true)
                                testDataPublishers {
                                    publishTestStabilityData()
                                }
                            }

                            postBuildTask {
                                // Download remote files
                                task('.', readFileFromWorkspace('maintenance/jenkins-seed', 'salt/scripts/download-remote-files.sh'))
                                // Shutdown VM
                                task('.', readFileFromWorkspace('maintenance/jenkins-seed', 'salt/scripts/shutdown-cloud-vm.sh'))
                            }

                            script_template = template_engine.createTemplate(
                                readFileFromWorkspace('maintenance/jenkins-seed', 'groovy/post-build-set-commit-status.groovy')
                            )
                            rendered_script_template = script_template.make(template_context.withDefault{ null })
                            groovyPostBuild(rendered_script_template.toString())
                        }
                    }
                }
            }
        }
    }
}
