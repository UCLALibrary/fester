#!/usr/bin/env bash

# Define locations of our container's property values
PROPERTIES=/etc/fester/fester.properties
PROPERTIES_TMPL=/etc/fester/fester.properties.tmpl
PROPERTIES_DEFAULT=/etc/fester/fester.properties.default

# Find the python application on our system
PYTHON=$(which python3)

# Create properties file from defaults and environment
read -d '' SCRIPT <<- EOT
import os,string,configparser;
from io import StringIO;
template=string.Template(open('$PROPERTIES_TMPL').read());
config = StringIO()
config.write('[fester]\n')
config.write(open('$PROPERTIES_DEFAULT').read())
config.seek(0, os.SEEK_SET)
config_parser = configparser.ConfigParser()
config_parser.optionxform = str
config_parser.readfp(config)
properties = dict(config_parser.items('fester'))
properties.update(os.environ)
print(template.safe_substitute(properties))
EOT

# Write our merged properties file to /etc/fester directory
$PYTHON -c "$SCRIPT" >> $PROPERTIES

# If we have feature flags, grab the configuration
if [[ -v FEATURE_FLAGS && ! -z FEATURE_FLAGS ]]; then
  curl -s "${FEATURE_FLAGS}" > /etc/fester/fester-features.conf
  chown fester /etc/fester/fester-features.conf
fi

# Replaces parent process so signals are processed correctly
exec "$@"
