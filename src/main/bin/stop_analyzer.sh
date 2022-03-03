#!/bin/bash

job_name="analyzer"
owner="DSIFOSRVDHDP"
declare -a appplication_id=()
application_ids=$(yarn application -list | awk '($2 == "${job_name}") && ($4 == "${owner}") {print $1}')

for application_id in  "${application_ids[@]}"
do
  if [ -z "$application_id" ]; then
    echo "No spark job: ${job_name} for owner: ${owner}"
    else
      echo "application -kill ${application_id}"
      yarn application  -kill ${application_id}
      fi

done