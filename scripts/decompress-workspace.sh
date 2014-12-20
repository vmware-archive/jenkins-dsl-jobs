#!/bin/sh

# Wait for a running salt-call state.sls
if [ "$(pgrep -f 'salt-call state.sls')" != "" ]; then
    stdbuf -i0 -o0 -e0 printf "%s" "Waiting on running salt-call to finish "
    while [ "$(pgrep -f 'salt-call state.sls')" != "" ]; do
        sleep 1
        stdbuf -i0 -o0 -e0 printf "%s" "."
    done
    stdbuf -i0 -o0 -e0 printf "%s\n\n" "Done!"
fi

salt-call state.sls projects.clone
xz -cdk workspace.cpio.xz | cpio -div
