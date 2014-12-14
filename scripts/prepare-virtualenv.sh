# Wait for a running salt-call state.sls
wait $(pgrep -f 'salt-call state.sls')
salt-call state.sls ${VIRTUALENV_SETUP_STATE_NAME} pillar="{virtualenv_name: ${VIRTUALENV_NAME}}" concurrent=true
