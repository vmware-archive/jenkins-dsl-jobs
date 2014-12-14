#!/bin/sh

# Wait for a running salt-call state.sls
running_salt_call="$(pgrep -f 'salt-call state.sls')"
if [ "${running_salt_call}" != "" ]; then
    printf "%s" "Waiting on running salt-call to finish "
    while [ -e "/proc/${running_salt_call}" ]; do
        sleep 1
        printf "%s" "."
    done
    printf "%s" "Done!\n"
fi

salt-call state.sls ${VIRTUALENV_SETUP_STATE_NAME} pillar="{virtualenv_name: ${VIRTUALENV_NAME}}"
