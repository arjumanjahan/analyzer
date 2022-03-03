#!/bin/bash
#
# Spark submit command
#

set -e

app-init.sh $env || echo " error while starting the application"