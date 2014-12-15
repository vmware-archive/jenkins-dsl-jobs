. /srv/virtualenvs/${VIRTUALENV_NAME}/bin/activate > /dev/null 2>&1

nosetests -V

if [ "${RUN_COVERAGE}" = "true" ]; then
    nosetests \
    --with-xunit \
    --xunit-file=${WORKSPACE}/nosetests.xml \
    --with-coverage \
    --cover-erase \
    --cover-package=sorbic \
    --cover-xml \
    --cover-xml-file=${WORKSPACE}/coverage.xml \
    -v tests
else
    nosetests \
    --with-xunit \
    --xunit-file=${WORKSPACE}/nosetests.xml
    -v tests
fi
