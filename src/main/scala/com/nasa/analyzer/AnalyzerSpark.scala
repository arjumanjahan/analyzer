package com.nasa.analyzer

import com.typesafe.config.ConfigFactory

import java.io.{BufferedOutputStream,File,FileOutputStream,InputStream,OutputStream}
import java.net.URL
import org.apache.commons.logging.LogFactory
import org.apache.spark.sql.{SparkSession}
import org.apache.spark.{SparkConf}
import org.apache.spark.sql.expressions.Window
import scala.collection.JavaConverters



case class AccessLog(host: String, date: String, method: String, endpoint: String, responseCode: String)
case class LogAnalysisException(severity: String, message: String, functionName: String) extends Exception(message: String)

object AnalyzerSpark {
  private val logger = LogFactory.getLog(AnalyzerSpark.getClass)

  def main(args: Array[String]): Unit = {
    val runStatus = run(args)
    println("Run status is the following:" + runStatus)
    if (runStatus == 0) {
      return
    }  else  {
    System.exit(runStatus)
  }
}

  def run(args: Array[String]): Int = {
    var status = 0
    val env = args(0)
    val propertiesConf = ConfigFactory.load()
    val Master = propertiesConf.getString(s"$env.master")
    val jobName = propertiesConf.getString(s"$env.job_name")
    val logLevel = propertiesConf.getString(s"$env.log_level")
    val numOfResultsToFetch = propertiesConf.getInt(s"$env.valueOfN")
    val writeCorruptLines = propertiesConf.getBoolean(s"$env.writeCorruptLogEnteries")
    val filterResponseCodes = propertiesConf.getStringList(s"$env.filterResponseCodes")
    val FilterResponseCodeSeq = JavaConverters.asScalaIteratorConverter(filterResponseCodes.iterator()).asScala.toSeq
    val downloadURL = propertiesConf.getString(s"$env.downloadFileLoc")
    val downloadFile = propertiesConf.getBoolean(s"$env.downloadFile")
    val fileLocation = propertiesConf.getString(s"$env.gzFSLocation").replace("{env}", env)
    val resultFileLoc = propertiesConf.getString(s"$env.resultFileLoc").replace("{env}", env)
    if (numOfResultsToFetch <= 0) {
      logger.error("The number of results to fetch is not defined in property file")
      System.exit(0);
    }
    if (fileLocation.length() <= 0) {
      logger.error("The file system directory to keep downloaded file is not defined in property file")
      System.exit(0);
    }

    if (downloadFile || !downloadURL.equals("")) {
      if (getFile(downloadURL, fileLocation) == 0) {
        logger.error("Exception : Exiting execution to error while downloading file.")
        System.exit(0)
      }
      logger.info("Download complete at location- " + fileLocation)
    } else {
      logger.info("Download not required. File is already at location- " + fileLocation)
    }

    logger.info("Parsed command-line-arguments.")
    val conf = new SparkConf()
      .setMaster("local[4]")
      .setAppName(jobName)
      .set("spark.hadoop.mapred.output.compress","false")
      .set("spark.hadoop.validateOutputSpecs", "false")
      .set("spark.sql.autoBroadcastJoinThreshold", (1024*1024*700).toString)
      .set("spark.sql.autoBroadcastJoinThreshold.compressed", "true")
      //.set("spark.serializer", "org.apache.spark.serializer.kryoSerializer")
      //.set("spark.kryoSerializer.buffer.max", "2047")
      .set("spark.debug.maxToStringFields", "100")

    val sparkSession = SparkSession.builder().config(conf).getOrCreate()
    val sparkContext = sparkSession.sparkContext
    sparkContext.setLogLevel(logLevel)

    try {

    import sparkSession.implicits._
      println(s"Place the gz file in ${fileLocation}")
    val dataRdd = sparkContext.textFile(fileLocation)
    logger.info("Start Transformation... ")
    val logLines = dataRdd.map(_.split(" ")).map(attributes => (attributes.length, attributes)).toDS()
    logLines.cache()
    //Filter logs based on tokens.size, i.e. attribute.length.
    val filteredLogLinesWithHttp = logLines.filter(logLines("_1") === 10)
    val filteredLogLinesWithoutHttp = logLines.filter(logLines("_1") === 9)
    filteredLogLinesWithHttp.printSchema()
    filteredLogLinesWithHttp.show()
    filteredLogLinesWithoutHttp.printSchema()
    filteredLogLinesWithoutHttp.show()
    //Filter corrupt logLines entries with incomplete information for processing.
    if (writeCorruptLines) {
      val corruptLogLines = logLines.filter(logLines("_1") < 9)
      val corruptLogLinesFormatted = corruptLogLines.map(x => x._1 + " - " + (x._2).mkString(" "))
      corruptLogLinesFormatted.printSchema()
      corruptLogLinesFormatted.show()
      AnalyzerTransformer.writeCorruptLogLinesToFS(corruptLogLinesFormatted, resultFileLoc)
    }

    logLines.unpersist()

    //Create datasets - AccessLog with & without HTTP
    val accessLogLinesWithHttp = filteredLogLinesWithHttp.map(x =>
      AccessLog(
        x._2(0),
        x._2(3).substring(1, x._2(3).indexOf(":")),
        x._2(5).substring(1),
        x._2(6).trim,
        x._2(8).trim))
    val accessLogLinesWithoutHttp = filteredLogLinesWithoutHttp.map(x =>
      AccessLog(
        x._2(0),
        x._2(3).substring(1, x._2(3).indexOf(":")),
        x._2(5).substring(1),
        x._2(6).substring(0, x._2(6).length - 1),
        x._2(7).trim))
    val accessLogLines = accessLogLinesWithoutHttp.union(accessLogLinesWithHttp)
    accessLogLines.cache()
    logger.info("AccessLog lines prepared..")

    //Define window for ranking operation
    val window = Window.partitionBy($"date").orderBy($"count".desc)

    //Fetch topN visitors
    val accessLogLinesTopVisitors = AnalyzerTransformer.getTopNVisitors(accessLogLines, numOfResultsToFetch, window)
    logger.info("Top" + numOfResultsToFetch + " Visitors processed.")

    //Fetch topN urls
    val accessLogLinesTopUrls = AnalyzerTransformer.getTopNUrls(accessLogLines, numOfResultsToFetch, window, FilterResponseCodeSeq)
    logger.info("Top" + numOfResultsToFetch + " urls processed.")

    //Total Host Counts
    val accessLogLinesTotalHostCounts = AnalyzerTransformer.getHostCounts(accessLogLines,  window, FilterResponseCodeSeq)
    logger.info("Total Host Counts is.")

    accessLogLines.unpersist()

    //Write the output to FS
    AnalyzerTransformer.writeResultsToFS(accessLogLinesTopVisitors,accessLogLinesTotalHostCounts, accessLogLinesTopUrls, resultFileLoc, numOfResultsToFetch)
    logger.info("Data processing finished. Output location- " + resultFileLoc)
  }catch {
    case e: Exception =>
      e.printStackTrace()
      status = 1
  } finally {
    sparkSession.stop()
  }
    val summary = s"${jobName} executed ${if (status == 0) "Sucesssfully" else "failed :("}! :)"
      println(summary)
      status
    }



  //Function to download the file from ftp
  def getFile(downloadURL: String, filePath: String): Int = {
    val url = new URL(downloadURL)
    var return_code = 1
    var in: InputStream = null
    var out: OutputStream = null
    try {
      val connection = url.openConnection()
      connection.setConnectTimeout(5000)
      connection.setReadTimeout(5000)
      connection.connect()
      logger.info("Connection to FTP server successfull.")
      in = connection.getInputStream
     val fileToDownloadAs = new java.io.File(filePath)
     out = new BufferedOutputStream(new FileOutputStream(fileToDownloadAs))
      logger.info("Downloading file from location- " + downloadURL)
      logger.info("Downloading file to location- " + filePath)
      val byteArray = Stream.continually(in.read).takeWhile(-1 !=).map(_.toByte).toArray
      out.write(byteArray)
    } catch {
      case ex: Exception =>
      {
        logger.error(new LogAnalysisException("error", s"getFile from ftp failed. Msg: $ex", "getFile").toString());
        return_code = 0
      }

    } finally {
      try {
        in.close()
        out.close()
      } catch {
        case ex: Exception => logger.error(new LogAnalysisException("error", s"Closing input/output streams. Msg: $ex", "getFile").toString());
      }

    }
     return_code
  }

  }



