. /srv/virtualenvs/${VIRTUALENV_NAME}/bin/activate > /dev/null 2>&1

salt-jenkins-build --vm-source ${BUILD_VM_NAME} --workspace=${WORKSPACE} --pull-request=${PR} --output-columns=160 --echo-parseable-output
