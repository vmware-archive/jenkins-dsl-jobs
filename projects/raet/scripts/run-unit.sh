. /srv/virtualenvs/${VIRTUALENV_NAME}/bin/activate > /dev/null 2>&1

nosetests -V

nosetests \
  --with-xunit \
  --xunit-file=${WORKSPACE}/nosetests.xml \
  --with-coverage \
  --cover-erase \
  --cover-package=raet \
  --cover-xml \
  --cover-xml-file=${WORKSPACE}/coverage.xml \
  -v raet
