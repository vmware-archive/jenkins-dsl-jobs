#!/bin/sh
echo '>>>>>>>>>>>>>> Compress Workspace >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>'
find . -not -name workspace.cpio.xz | cpio -ov | xz > workspace.cpio.xz
echo '<<<<<<<<<<<<<< Compress Workspace <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<'
