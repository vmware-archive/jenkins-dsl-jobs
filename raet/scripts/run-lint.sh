. /srv/virtualenvs/${VIRTUALENV_NAME}/bin/activate > /dev/null 2>&1

pylint --rcfile=${WORKSPACE}/.testing.pylintrc --disable=I ${WORKSPACE}/raet/ | tee ${WORKSPACE}/pylint-report.xml; EC1=${PIPESTATUS[0]}
exit $EC1
