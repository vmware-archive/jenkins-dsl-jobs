#!/bin/sh
echo '>>>>>>>>>>>>>> Compress Workspace >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>'
find . -not -name workspace.cpio.xz | cpio -o | xz > workspace.cpio.xz
echo '<<<<<<<<<<<<<< Compress Workspace <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<'
