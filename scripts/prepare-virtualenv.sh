# Random 1-10 seconds sleep in order not to call state.sls at the same time
sleep $(($RANDOM % 10))

salt-call state.sls ${VIRTUALENV_SETUP_STATE_NAME} pillar="{virtualenv_name: ${VIRTUALENV_NAME}}" concurrent=true
