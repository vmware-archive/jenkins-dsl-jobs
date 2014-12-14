salt-call state.sls ${VIRTUALENV_SETUP_STATE_NAME} pillar="{virtualenv_name: ${VIRTUALENV_NAME}}"
