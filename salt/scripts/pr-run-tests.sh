# Random 1-10 seconds sleep to split the server load
sleep $(($RANDOM % 10))

salt-jenkins-build \
  --vm-source ${JENKINS_VM_SOURCE} \
  --cloud-deploy \
  --test-prep-sls=projects.salt.${BRANCH_NAME}.unit \
  --bootstrap-salt-commit ${SALT_MINION_BOOTSTRAP_RELEASE} \
  --test-git-url=${SALT_PR_GIT_URL} \
  --test-git-commit=${SALT_PR_GIT_COMMIT} \
  --test-pillar git_branch ${SALT_PR_GIT_BASE_BRANCH} \
  --test-default-command \
  --test-without-coverage
