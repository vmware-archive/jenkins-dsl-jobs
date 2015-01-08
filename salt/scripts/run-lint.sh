. /srv/virtualenvs/${VIRTUALENV_NAME}/bin/activate > /dev/null 2>&1

EC1=-1
EC2=-1

pylint --rcfile=${WORKSPACE}/.testing.pylintrc ${WORKSPACE}/salt/ | tee ${WORKSPACE}/pylint-report.xml; EC1=${PIPESTATUS[0]}
pylint --rcfile=${WORKSPACE}/.testing.pylintrc --disable=W0232,E1002 ${WORKSPACE}/tests/ | tee ${WORKSPACE}/pylint-report-tests.xml; EC2=${PIPESTATUS[0]}

if [ $EC1 -ne 0 ]; then
    exit $EC1
elif [ $EC2 -ne 0 ]; then
    exit $EC2
else
    exit 0
fi
