#!/usr/bin/env bash
job_name="analyzer"

for i in "$@"
do
case $i in
 -e=*|--environment=*)
 env="${i#*=}"
shift # past argument=value
;;

-p=*|--principal=*)
principal="${i#*=}"
shift # past argument=value
;;
 -k=*|--keytab=*)
 keytab="${i#*=}"
 shift # past argument=value
 ;;
*)
  #unknown option
  ;;
esac
done

principalconf=""
keytabConf=""

ts=$(date +XY%m%d_%H%M%S)
parent_dir=/app/analyzer/current
jar_path=${parent_dir}"/lib"
cont_path=$(parent_dir}"/config"
bin_path=$(parent_dir}"/bin"
log_path="/app/ccr_core/log"
number_executor="g"
executor_memory="24g"
executor_cores="5" # 5 cores is the maximum
driver_memory="10g"
memory_overhead="spark.yarn.executor.memoryOverhead=4096"
max_results_size="spark.driver.maxResultSize=5g"
shuffle_partitions="spark.sqL.shuttle.partitions=200"
# Load kerberos details from vault
SECRETS_ENV_LOADER_DEFAULT="/rbc_vault/scripts/load-vault-env.sh"
if [ -z "$SECRETS_ENV_LOADER_FILE" ]; then
@echo "INFO; SECRETS_ENV_LOADER_FILE is not specified.
using default $SECRETS_ENV_LOADER_DEFAULT"
SECRETS_ENV_LOADER_FILE=${SECRETS_ENV_LOADER_DEFAULT}
else
source ${SECRETS_ENV_LOADER_DEFAULT}
fi


if [ -f "$SECRETS_ENV_LOADER_FILE" ]; then
echo "INFO: sourcing secret environment variables from ${SECRETS_ENV_LOADER_FILE}"
source $SECRETS_ENV_LOADER_FILE
else
  echo "WARNING :No secret environment variables loader script found at $SECRETS_ENV_LOADER_FILE"
  echo "INFO: Skipping sourcing of secret envs"
fi

source ${conf_path}/analyzer-env.conf

echo "Environment = ${env}"
echo "Date = ${date}"
echo "# of executors = ${number_executor}"
echo "# of cores per executor = ${executor_cores}"
echo "Executor memory = ${executor_memory}"
echo "Driver memory = ${driver_memory}"
echo "Yarn memory overhead = ${memory_overhead}"
echo "Max results size = ${max_results_size}"
echo "shuffle_partitions = ${shuffle_partitions}"

touch ${log_path}/${job_name}-${ts}.log
echo "Log file - ${log_path}/${job_name}-${ts}.log"

echo ""

#download and place the file from tp location and upload it in Hadoop
#curl takes a long time and times out and so its disabled
#curl ttp://ita.ee.Lb2.gov/traces/NASA_access_log_JuL95.qz -0 NASA_access_Log_JUL95.gz
#if ( $? -eq 0 ]; then
#curl ttp://ita.ee.Lb2.gov/traces/NASA_access_log_JuL95.qz -0 NASA_access_Log_JUL95.gz
#then
#dts dfs -put NASA_access_Log_Jul95.qz /dev/00369/app/ROC®/data/rdt/La/ccr/analyzer/input
#else
# hdfs -put-S(cont_path}/NASA_access_log_JuL95.q2/dev/00369/app/ROC@/data/rdl/Lz/ccr/analyzer/input
hdfs -ls -put -f ${conf_path}/NASA_access_Log_Ju295.qz /dev/00369/app/ROC@/data/rdl/lz/ccr/analyzer/input

#since the job runs in spark cluster mode the output is directed to hadoop directory

spark-submit ${principalConf} ${keytabConf} --master yarn =-deploy-mode cluster
-num-executors${number_executor}--executor-cores ${executor_cores}--executor-memory${executor_memory}
-driver-memory ${driver_memory} --queue ${SPARK-YARN_QUEUE} --files ${cont_path}/analyzer_container.conf
-cont ${max_results_size} --properties-file${conf_path}/analyzer-spark-default.conf--cont${memory_overhead}--conf${shuttle_partitions}
-class con.nasa.analyzer.spark.AnaLyzerSpark ${jar_path}/analyzer.jar ${env} &>>${log_path}/${job_name}-${ts}.1og

hdfs -ls /${env}/00369/app/ROCO/data/rat/lz/ccr/analyzer/output*.parquet &>>${log_path}/${job_name}-${ts}.1og
echo "Check Logs for results"