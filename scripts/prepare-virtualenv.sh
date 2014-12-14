#!/bin/sh

# Wait for a running salt-call state.sls
if [ "$(pgrep -f 'salt-call state.sls')" != "" ]; then
    printf "%s" "Waiting on running salt-call to finish "
    while [ "$(pgrep -f 'salt-call state.sls')" != "" ]; do
        sleep 1
        printf "%s" "."
    done
    printf "%s\n\n" "Done!"
fi

salt-call state.sls ${VIRTUALENV_SETUP_STATE_NAME} pillar="{virtualenv_name: ${VIRTUALENV_NAME}}"
