. /srv/virtualenvs/${VIRTUALENV_NAME}/bin/activate > /dev/null 2>&1

# Random 1-10 seconds sleep to split the server load
sleep $(($RANDOM % 10))

if [ "${SUDO_SALT_CALL_REQUIRED:-0}" -eq 1 ] && [ "$(id -u)" -ne 0 ]; then
    # Non root users must sudo
    SUDO="sudo -E"
else
    SUDO=""
fi

${SUDO} $(which salt-jenkins-build) \
  --vm-source ${JENKINS_VM_SOURCE} \
  --cloud-deploy \
  --test-prep-sls=projects.salt.${BRANCH_NAME}.unit \
  --bootstrap-salt-commit ${SALT_MINION_BOOTSTRAP_RELEASE} \
  --test-git-url=${SALT_PR_GIT_URL} \
  --test-git-commit=${SALT_PR_GIT_COMMIT} \
  --test-pillar git_branch ${SALT_PR_GIT_BASE_BRANCH} \
  --test-default-command \
  --test-without-coverage
