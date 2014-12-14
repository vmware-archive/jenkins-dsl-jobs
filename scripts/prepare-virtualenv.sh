# Wait for a running salt-call state.sls
running_salt_call=$(pgrep -f 'salt-call state.sls')
if [ "${running_salt_call}" != ""]; then
    echo -n "Waiting on running salt-call to finish "
    while [ -e /proc/${running_salt_call} ]; do
        sleep 1
        echo -n "."
    done
    echo "Done!"
fi
salt-call state.sls ${VIRTUALENV_SETUP_STATE_NAME} pillar="{virtualenv_name: ${VIRTUALENV_NAME}}"
