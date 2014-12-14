github-commit-status --auth-token=${GITHUB_AUTH_TOKEN} --repo=${GITHUB_REPO} --context=${COMMIT_STATUS_CONTEXT} --target-url=${BUILD_URL} ${GIT_COMMIT}
