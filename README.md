<h3>Software versions</h3>
<ul>
    - java version 1.8
	- Scala version- 2.11.12
	- Spark version- 2.4.5
	- Intellij 2020.3
    - maven
</ul>


<h3># Assumptions</h3>
<ul>
*The code developed in a secure network to test it in PROD like environment
*Spark-scala job is developed to run in cluster mode as it doesn't need a port to be exposed for the job to run the job
*File ftp://ita.ee.lbl.gov/traces/NASA_access_log_Jul95.gz is downloaded using curl command in start script
*In case the file cant be downloaded the same is available in /src/main/config folder
*the input file is placed in hadoop directory and output files are created in output directory so the output can be analyzed in Dermio
*The validation of both input and output files are done through Dermio for ease of validation in seconds
</ul>



<h3>Introduction</h3>
<ul>
Nasa Web Access Log Analyzer Application
</ul>



<h3>Objective</h3>
<ul>
- Fetch top N visitor
- Fetch top N urls
</ul>

<h3>Code walkthrough</h3>
<ul>
Input Download URL - ftp://ita.ee.lbl.gov/traces/NASA_access_log_Jul95.gz	
1. src/main/scala
	* *com.nasa.analyzer.AnalyzerSpark* - This is main entry class. SparkSession object is build here.
	* *com.nasa.analyzer.AnalyzerTransformer* - The spark Dataset processing logic is present here.
2. src/main/config
	* *analyzer.conf* - all configurations are maintained in a Typesafe config file.
3. src/test/scala
	* *com.nasa.analyzer.AnalyzerTransformerTest.scala* - This is the Unit test class for NasaWebAccessStatsProcessor.
</ul>


<h3>Configurations</h3>
<ul>
Configuration file name- *analyzer.conf*
Properties set in configuration file-
- **valueOfN**- This property is to set the value of N in the topNVisitors and topNUrls. *REQUIRED INT value*
- **writeCurruptLogEnteries**- Some of the log lines are corrupt. If writeCurruptLogEnteries is set to TRUE, then corrupt lines will be writen to filesystem, else not. *BOOLEAN value*
- **filterResponseCodes**- When evaluating topNUrls, this property is used to filter the log lines with undesired response code. Eg- when set to value ["304","400"], all log lines with response code 304 and 400 will be filtered. *LIST value*
- **downloadFileLoc**- This is the ftp location from where the input gz file will be downloaded. If set to blank, then file-download will not happen, assuming that the file is already on local filesystem. *STRING value*
- **downloadFile** -this is set to true or false if we want to totaly get away with the download option within scala code
- **gzFSLocation**- This is the local filesystem path where gz file is downloaded. If the property "downloadFileLoc" is not set, then the application assumes that the gz file is present at this location. *REQUIRED STRING value*
	
Incase required configuration properties are not set, then the application exits with code 0.
</ul>


<h3>Assumptions</h3>
<ul>
1. Data is structured in the following format- 
	* `<visitor> - - [<date> <timezone>] "<method> <url> <protocol>" <resonseCode> <unknownvariable>`
  	  `E.g.- unicomp6.unicomp.net - - [01/Jul/1995:00:00:14 -0400] "GET /shuttle/countdown/count.gif HTTP/1.0" 200 40310`
  	  String split functions have been used to derive date and other attributes based on the assumption that the data log line will follow this format.

2. When log-lines are tokenized using tokenizer **SPACE** (" "), there are entries with-
	* *10 tokens* - log-lines with 10 tokens have protocol value set as HTTP/1.0
	* *9 tokens* - log-lines with 9 tokens don't have any protocol value set.
	* *8 or less tokens* - these are considered as corrupt lines as it has incomplete information, such as missing method(GET,POST,HEAD etc) and/or missing other information.
	
3. To evaluate topNurls, the application gives two options-
	* filterResponseCodes set with a value - when set with a list of responseCodes, the log-lines with the specified response codes will be filtered out. Eg. a 404 response request can be filtered out when processing top N urls.
	* filterResponseCodes set BLANK - when set to blank, all log-lines will be considered valid to evaluate the top N urls. Only lines missed will be corrupt lines identified as tokens < 9.
   
4. To evaluate topNvisitors, no special logic has been added to filter log-lines based on HTTP method. The current implementation considers GET, POST, HEAD etc methods as the visitors. 
   </ul>
	

<h3>Steps to compile</h3>
<ul>
1. click on maven on the right side of the window screen
2. Run cmd- `maven clean`. 
3. Run cmd 'maven install' this will create the tar.gz file based on the details in the assembly
4. The jar is generated in the target directory. Check jar full path in the console-log.
 </ul>

<h2>Spark-Docker Application</h2>
<h3>Description</h3>
This is a Spark application that downloads the dataset at ftp://ita.ee.lbl.gov/traces/NASA_access_log_Jul95.gz and uses Apache Spark to determine the top-n most frequent visitors and urls for each day of the trace. The java application is packaged with a "Dockerized Apache Spark" run in a containerized environment.

For the dockerized spark image I have used the bde2020/spark-base:2.4.3-hadoop2.7 as my base image. The detailed documentation is in the link below:
<p>https://github.com/P7h/docker-spark</p>
 </ul>




