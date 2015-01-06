. /srv/virtualenvs/${VIRTUALENV_NAME}/bin/activate > /dev/null 2>&1

# Random 1-10 seconds sleep to split the server load
sleep $(($RANDOM % 10))

salt-jenkins-build \
  --output-columns=160 \
  --vm-source ${BUILD_VM_NAME} \
  --cloud-deploy \
  --test-prep-sls=projects.salt.${BRANCH_NAME}.unit \
  --test-git-commit ${GIT_COMMIT} \
  --bootstrap-salt-commit ${SALT_MINION_BOOTSTRAP_RELEASE} \
  --test-default-command \
  --build-packages
