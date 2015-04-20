. /srv/virtualenvs/${VIRTUALENV_NAME}/bin/activate > /dev/null 2>&1

cd doc/

make clean

for format in $(echo "html latexpdf xetexpdf epud"); do
    make ${format} SPHINXOPTS='-q' LATEXOPTS='-interaction=nonstopmode'
done

cd ..
