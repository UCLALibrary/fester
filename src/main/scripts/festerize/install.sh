#!/bin/bash

# Determine which Pip 3 executable to use.
for PIP in pip3 pip; do
    if ! [ -x "$(command -v ${PIP})" ]; then
        echo "${PIP} not installed" >&2
        PIP=
    else
        break
    fi
done

if [ -z ${PIP} ]; then
    echo "A version of Pip compatible with Python 3 is required to install Festerize."
    exit 1
fi

LATEST_TAG=$(curl -sSL https://api.github.com/repos/uclalibrary/fester/releases/latest | grep '"tag_name": ".\+"' | grep -o '[0-9]\+\.[0-9]\+\.[0-9]\+' ) && \

echo -e "\nInstalling with ${PIP}\n" && \

${PIP} install -I "git+https://github.com/UCLALibrary/fester.git@${LATEST_TAG}#egg=festerize&subdirectory=src/main/scripts/festerize" && \

echo -e "\nTo uninstall, run '${PIP} uninstall festerize'.\n"
