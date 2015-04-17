. /srv/virtualenvs/${VIRTUALENV_NAME}/bin/activate > /dev/null 2>&1

salt-jenkins-build --cloud-deploy --delete-vm ${JENKINS_VM_NAME}
