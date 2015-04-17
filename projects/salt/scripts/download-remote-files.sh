. /srv/virtualenvs/${VIRTUALENV_NAME}/bin/activate > /dev/null 2>&1

salt-jenkins-build \
  --output-columns=160 \
  --download-artifact /tmp/xml-unittests-output artifacts \
  --download-artifact /tmp/coverage.xml artifacts/coverage \
  --download-artifact /var/log/salt/minion artifacts/logs \
  --download-artifact /tmp/salt-runtests.log artifacts/logs ${JENKINS_SALTCLOUD_VM_NAME}

find artifacts/logs -type f -not -name '*.log' -exec mv {} {}.log \;
