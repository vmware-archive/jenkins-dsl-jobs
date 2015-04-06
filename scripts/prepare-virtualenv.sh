#!/bin/sh
echo '>>>>>>>>>>>>>> Prepare Build Env >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>'
# Don't call 'salt-call' at the same time
sleep $(($RANDOM % 12))

if [ "${SUDO_SALT_CALL_REQUIRED:-0}" -eq 1 ] && [ "$(id -u)" -ne 0 ]; then
    # Non root users must sudo
    SUDO="sudo"
else
    SUDO=""
fi

if [ "${VIRTUALENV_NAME}" != "" ]; then
    ${SUDO} salt-call --force-color \
        --retcode-passthrough \
        --log-file="${WORKSPACE:-.}/salt-call.log" \
        --log-file-level=trace \
        state.sls "${VIRTUALENV_SETUP_STATE_NAME}" \
        queue=true \
        pillar="{virtualenv_name: ${VIRTUALENV_NAME}, system_site_packages: ${SYSTEM_SITE_PACKAGES:-false}}"
else
    ${SUDO} salt-call --force-color \
        --retcode-passthrough \
        --log-file="${WORKSPACE:-.}/salt-call.log" \
        --log-file-level=trace \
        state.sls "${VIRTUALENV_SETUP_STATE_NAME}" \
        queue=true
fi
echo '<<<<<<<<<<<<<< Prepare Build Env <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<'
