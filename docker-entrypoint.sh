#!/bin/bash
#
# Spark submit command
#

set -e

/app/analyzer/current/bin/start_analyzer.sh dev || echo " error while starting the application"