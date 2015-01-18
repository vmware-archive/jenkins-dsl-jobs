shellcheck -s sh -f checkstyle bootstrap-salt.sh | tee checkstyle.xml
exit ${PIPESTATUS[0]}
