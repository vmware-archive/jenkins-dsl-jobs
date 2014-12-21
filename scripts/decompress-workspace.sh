#!/bin/sh
echo '>>>>>>>>>>>>>> Decompress Workspace >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>'
xz -cdk workspace.cpio.xz | cpio -div
echo '<<<<<<<<<<<<<< Decompress Workspace <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<'
