<h3>Software versions</h3>
<ul>
    <li>java version 1.8</li>
	<li>Scala version- 2.11.12</li>
	<li>Spark version- 2.4.5</li>
	<li>Intellij 2020.3</li>
   <li>maven</li>
</ul>


<h3># Assumptions</h3>
<ul>
<li>The code developed in a secure network to test it in PROD like environment</li>
<li>Spark-scala job is developed to run in cluster mode as it doesn't need a port to be exposed for the job to run the job</li>
<li>File ftp://ita.ee.lbl.gov/traces/NASA_access_log_Jul95.gz is downloaded using curl command in start script</li>
<li>In case the file cant be downloaded the same is available in /src/main/config folder</li>
<li>the input file is placed in hadoop directory and output files are created in output directory so the output can be analyzed in Dermio</li>
<li>The validation of both input and output files are done through Dermio for ease of validation in seconds</li>
</ul>



<h3>Introduction</h3>
<ul>
<li>Nasa Web Access Log Analyzer Application</li>
</ul>



<h3>Objective</h3>
<ul>
<li>- Fetch top N visitor</li>
<li>- Fetch top N urls</li>
</ul>

<h3>Code walkthrough</h3>
<ul>
<li>Input Download URL - ftp://ita.ee.lbl.gov/traces/NASA_access_log_Jul95.gz</li>	
<li> src/main/scala</li>
	   <li>com.nasa.analyzer.AnalyzerSpark* - This is main entry class. SparkSession object is build here.</li>
      <li>com.nasa.analyzer.AnalyzerTransformer* - The spark Dataset processing logic is present here.</li>
<li> src/main/config</li>
      <li>analyzer.conf* - all configurations are maintained in a Typesafe config file.</li>
<li> src/test/scala</li>
	  <li>com.nasa.analyzer.AnalyzerTransformerTest.scala* - This is the Unit test class for NasaWebAccessStatsProcessor.</li>
</ul>


<h3>Configurations</h3>
<ul>
<li>Configuration file name- *analyzer.conf*</li>
<li>Properties set in configuration file-</li>
<li>**valueOfN**- This property is to set the value of N in the topNVisitors and topNUrls. *REQUIRED INT value*</li>
<li>**writeCurruptLogEnteries**- Some of the log lines are corrupt. If writeCurruptLogEnteries is set to TRUE, then corrupt lines will be writen to filesystem, else not. *BOOLEAN value*</li>
<li>**filterResponseCodes**- When evaluating topNUrls, this property is used to filter the log lines with undesired response code. Eg- when set to value ["304","400"], all log lines with response code 304 and 400 will be filtered. *LIST value*</li>
<li>**downloadFileLoc**- This is the ftp location from where the input gz file will be downloaded. If set to blank, then file-download will not happen, assuming that the file is already on local filesystem. *STRING value*</li>
<li>**downloadFile** -this is set to true or false if we want to totaly get away with the download option within scala code</li>
<li>**gzFSLocation**- This is the local filesystem path where gz file is downloaded. If the property "downloadFileLoc" is not set, then the application assumes that the gz file is present at this location. *REQUIRED STRING value*</li>
	
Incase required configuration properties are not set, then the application exits with code 0.
</ul>


<h3>Assumptions</h3>
<ul>
<li> Data is structured in the following format- <li>
 `<visitor> - - [<date> <timezone>] "<method> <url> <protocol>" <resonseCode> <unknownvariable>`
 `E.g.- unicomp6.unicomp.net - - [01/Jul/1995:00:00:14 -0400] "GET /shuttle/countdown/count.gif HTTP/1.0" 200 40310`
  String split functions have been used to derive date and other attributes based on the assumption that the data log line will follow this format.

<li> When log-lines are tokenized using tokenizer **SPACE** (" "), there are entries with-<li>
* *10 tokens* - log-lines with 10 tokens have protocol value set as HTTP/1.0
* *9 tokens* - log-lines with 9 tokens don't have any protocol value set.
* *8 or less tokens* - these are considered as corrupt lines as it has incomplete information, such as missing method(GET,POST,HEAD etc) and/or missing other information.

<li>To evaluate topNurls, the application gives two options-<li>
* filterResponseCodes set with a value - when set with a list of responseCodes, the log-lines with the specified response codes will be filtered out. Eg. a 404 response request can be filtered out when processing top N urls.
* filterResponseCodes set BLANK - when set to blank, all log-lines will be considered valid to evaluate the top N urls. Only lines missed will be corrupt lines identified as tokens < 9.

<li> To evaluate topNvisitors, no special logic has been added to filter log-lines based on HTTP method. The current implementation considers GET, POST, HEAD etc methods as the visitors.
   </ul>
	

<h3>Steps to compile</h3>
<ul>
<li> click on maven on the right side of the window screen</li>
<li> Run cmd- `maven clean`. </li>
<li> Run cmd 'maven install' this will create the tar.gz file based on the details in the assembly</li>
<li>The jar is generated in the target directory. Check jar full path in the console-log.</li>
 </ul>

 <p align="center">
    <img src="ParametersforJob.JPG" width="350"/>
 </p>

<h2>Spark-Docker Application</h2>
<h3>Description</h3>
<li>This is a Spark application that downloads the dataset at ftp://ita.ee.lbl.gov/traces/NASA_access_log_Jul95.gz and uses Apache Spark to determine the top-n most frequent visitors and urls for each day of the trace. The java application is packaged with a "Dockerized Apache Spark" run in a containerized environment.</li>

<li>For the dockerized spark image I have used the bde2020/spark-base:2.4.3-hadoop2.7 as my base image. The detailed documentation is in the link below:</li>
<p>https://github.com/P7h/docker-spark</p>
 </ul>




