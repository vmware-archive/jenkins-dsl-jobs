shellcheck -s sh -f checkstyle ${WORKSPACE}/bootstrap-salt.sh | tee checkstyle.xml
exit ${PIPESTATUS[0]}
