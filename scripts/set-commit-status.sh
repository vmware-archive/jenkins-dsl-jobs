echo '>>>>>>>>>>>>>> Set Commit State >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>'
[ "${VIRTUALENV_NAME}" != "" ] && . /srv/virtualenvs/${VIRTUALENV_NAME}/bin/activate > /dev/null 2>&1

github-commit-status \
    --auth-token=${GITHUB_AUTH_TOKEN} \
    --repo=${GITHUB_REPO} \
    --context=${COMMIT_STATUS_CONTEXT} \
    --target-url=${BUILD_URL} \
    ${SALT_PR_GIT_COMMIT:-${GIT_COMMIT}} || echo "Failed to set commit state"
echo '<<<<<<<<<<<<<< Set Commit State <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<'
