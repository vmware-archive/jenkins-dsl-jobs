// Salt Jenkins jobs seed script
import lib.Admins

// Common variable Definitions
def github_repo = 'saltstack/salt'
def repo_api = new URL("https://api.github.com/repos/${github_repo}")
def repo_data = new groovy.json.JsonSlurper().parse(repo_api.newReader())
def project_description = repo_data['description']

// Job rotation defaults
def default_days_to_keep = 90
def default_nr_of_jobs_to_keep = 180
def default_artifact_days_to_keep = default_days_to_keep
def default_artifact_nr_of_jobs_to_keep = default_nr_of_jobs_to_keep

// Job Timeout defauls
def default_timeout_percent = 150
def default_timeout_builds = 10
def default_timeout_minutes = 60

// Get branches to build from GitHub. Only branches matching 'develop' and 'dddd.dd'
def branches_api = new URL("https://api.github.com/repos/${github_repo}/branches")
def salt_branches_data = new groovy.json.JsonSlurper().parse(branches_api.newReader())
def salt_branches = []
salt_branches_data.each {
  salt_branches.add(it.name)
}
salt_branches = salt_branches.grep(~/(develop|([\d]{4}.[\d]{1,2}))/)

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

// Define the folder structure
folder {
    name('salt')
    displayName('Salt')
    description = project_description
}

salt_branches.each { branch_name ->
    def branch_folder_name = "salt/${branch_name.toLowerCase()}"
    folder {
        name(branch_folder_name)
        displayName("${branch_folder_name.capitalize()} Branch")
        description = project_description
    }

    salt_build_types.each { salt_build_type, vm_names ->
        if ( vm_names != [] ) {
            def build_type_folder_name = "${branch_folder_name}/${salt_build_type.toLowerCase()}"
            folder {
                name(build_type_folder_name)
                displayName("${salt_build_type} Builds")
                description = project_description
            }

            if (salt_build_type.toLowerCase() == 'cloud') {
                salt_cloud_providers.each { provider_name ->
                    cloud_provider_folder_name = "${build_type_folder_name}/${provider_name.toLowerCase()}"
                    folder {
                        name(cloud_provider_folder_name)
                        displayName(provider_name)
                        description = project_description
                    }
                }
            }
        }
    }
}
