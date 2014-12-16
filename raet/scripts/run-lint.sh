. /srv/virtualenvs/${VIRTUALENV_NAME}/bin/activate > /dev/null 2>&1

pylint --rcfile=.testing.pylintrc --disable=I raet/ | tee pylint-report.xml; EC1=${PIPESTATUS[0]}
exit $EC1
