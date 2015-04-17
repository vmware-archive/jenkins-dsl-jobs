. /srv/virtualenvs/${VIRTUALENV_NAME}/bin/activate > /dev/null 2>&1

salt-jenkins-build --vm-source ${BUILD_VM_SOURCE} --echo-parseable-output
