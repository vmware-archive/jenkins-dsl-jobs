// libnacl Jenkins jobs seed script

// Common variable Definitions
def github_project_url = 'https://github.com/saltstack/libnacl'
def git_url = 'https://github.com/saltstack/libnacl.git'
def project_description = 'Python ctypes wrapper for libsodium'

// Job rotation defaults
def default_days_to_keep = 90
def default_nr_of_jobs_to_keep = 180
def default_artifact_days_to_keep = default_days_to_keep
def default_artifact_nr_of_jobs_to_keep = default_nr_of_jobs_to_keep


def scm_configuration = 

folder {
  name = 'libnacl'
  displayName = 'libnacl'
  description = project_description

  // Main master branch job
  job {
    name = 'master-main-build'
    displayName = 'Master Branch Main Build'

    // scm configuration
    scm {
      github(
        'saltstack/libnacl',
        branch = 'master',
        protocol = 'https'
      )
    }
  }

  folder {
    name = 'master'
    displayName = 'Master Branch'

    job {
      name = 'lint'
      displayName = 'Lint'
      concurrentBuild = true
      description = project_description + ' - Code Lint'

      // Delete old jobs
      logRotator(
        default_days_to_keep,
        default_nr_of_jobs_to_keep,
        default_artifact_days_to_keep,
        default_artifact_nr_of_jobs_to_keep
      )

      // scm configuration
      scm {
        github(
          'saltstack/libnacl',
          branch = 'master',
          protocol = 'https'
        )
      }

    }

    job {
      name = 'unit'
      displayName = 'Unit'
      concurrentBuild = true
      description = project_description + ' - Unit Tests'

      // scm configuration
      scm {
        github(
          'saltstack/libnacl',
          branch = 'master',
          protocol = 'https'
        )
      }

    }
  }
}
