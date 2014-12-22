salt-jenkins-build \
  --output-columns=160 \
  --download-artifact '/tmp/xml-unittests-output/*.xml' ${WORKSPACE}/artifacts/unittests \
  --download-artifact '/tmp/coverage.xml' ${WORKSPACE}/artifacts/coverage \
  --download-artifact /var/log/salt/minion ${WORKSPACE}/artifacts/logs \
  --download-artifact /tmp/salt-runtests.log ${WORKSPACE}/artifacts/logs ${JENKINS_SALTCLOUD_VM_NAME}

find ${WORKSPACE}/artifacts/logs -type f -not -name '*.log' -exec mv {} {}.log \;
