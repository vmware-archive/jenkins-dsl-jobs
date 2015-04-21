#!/bin/sh
. "/srv/virtualenvs/${VIRTUALENV_NAME}/bin/activate" > /dev/null 2>&1

cd doc/

make clean || true

build_formats="html latexpdf xetexpdf epub"

for format in $build_formats; do
    if [ "$format" = "latexpdf" ] || [ "$format" = "xetexpdf" ]; then
        # We're having issues building PDF's. Skip it for now.
        continue
    fi
    make "${format}" SPHINXOPTS='-q' LATEXOPTS='-interaction=nonstopmode'
done

cd ..
