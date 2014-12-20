#!/bin/sh

find . -not -name workspace.cpio.xz | cpio -ov | xz > workspace.cpio.xz
