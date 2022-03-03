package com.nasa.analyzer

import org.apache.log4j.{BasicConfigurator, Logger}
import org.apache.spark.sql.{Dataset,Row}
import org.apache.spark.sql.expressions.WindowSpec
import org.apache.spark.sql.functions.{col,dense_rank,not}

object AnalyzerTransformer {


  BasicConfigurator.configure()
  val logger = Logger.getLogger(this.getClass.getName())



  //Function to get top N visitors
  def getTopNVisitors(
    accessLogLines: Dataset[AccessLog],
    numOfResultsToFetch: Int,
    window: WindowSpec): Dataset[Row] = {
    logger.debug("getTop " + numOfResultsToFetch + " Visitors started.")

    val accessLogLinesGroupByVisitor = accessLogLines.groupBy("host", "date")
      .count()
    val accessLogLinesRankedForVisitors = accessLogLinesGroupByVisitor.withColumn("dense_rank", dense_rank over window)
      .filter(col("dense_rank") <= numOfResultsToFetch)

    val accessLogLinesTopVisitors = accessLogLinesRankedForVisitors.select("*")
      .orderBy(col("date").asc, col("count").desc)
    logger.debug("getTop " + numOfResultsToFetch + " Visitors finished.")
    return accessLogLinesTopVisitors

  }

  //Function to get top N urls
  def getTopNUrls(
    accessLogLines: Dataset[AccessLog],
    numOfResultsToFetch: Int,
    window: WindowSpec,
    filterResponseCodesSeq: Seq[String]): Dataset[Row] = {

    logger.debug("getTop " + numOfResultsToFetch + " Urls started.")

    var accessLogLinesGroupByUrl: org.apache.spark.sql.DataFrame = null;

    if (filterResponseCodesSeq.size > 0) {
      logger.debug("responseCode filtering is enabled when retrieving top  " + numOfResultsToFetch + " urls. The resopnseCodes are- " + filterResponseCodesSeq)
      accessLogLinesGroupByUrl = accessLogLines.filter(not(col("responseCode") isin (filterResponseCodesSeq: _*)))
        .groupBy("endpoint", "date")
        .count()
    } else {
      logger.debug("responseCode filtering is disabled when retrieving top  " + numOfResultsToFetch + " urls.")
      accessLogLinesGroupByUrl = accessLogLines.groupBy("endpoint", "date")
        .count()
    }
    val accessLogLinesRankedForUrl = accessLogLinesGroupByUrl.withColumn("dense_rank", dense_rank over window)
      .filter(col("dense_rank") <= numOfResultsToFetch)
    val accessLogLinesTopUrls = accessLogLinesRankedForUrl.select("*")
      .orderBy(col("date").asc, col("count").desc)
    logger.debug("getTop " + numOfResultsToFetch + " Urls finished.")
    return accessLogLinesTopUrls

  }

  //Function to write the results on FileSystem
  def writeResultsToFS(
    accessLogLinesTopVisitors: Dataset[Row],
    accessLogLinesTopUrls: Dataset[Row],
    resultFileLoc: String,
    numOfResultsToFetch: Int) = {

    println(s"Printing Top${numOfResultsToFetch}Visitors ")
    accessLogLinesTopVisitors.toDF("visitor","dateVisited","numberOfTimesVisited","rated").show(false)

    logger.debug("Write results to filesystem started.")
    accessLogLinesTopVisitors.toDF("visitor","dateVisited","numberOfTimesVisited","rated").coalesce(1)
      .write.option("header", "true")
      .mode("overwrite")
      .parquet( resultFileLoc+"nasa_top_visitors.parquet")

    println(s"Printing Top${numOfResultsToFetch}Urls ")
    accessLogLinesTopUrls.toDF("endPoint","dateVisited","numberOfHits","rated").show(false)

    accessLogLinesTopUrls.toDF("endPoint","dateVisited","numberOfHits","rated").coalesce(1)
      .write
      .option("header", "true")
      .mode("overwrite")
      .parquet(resultFileLoc+"nasa_top_urls.parquet")
    logger.debug("Write results to filesystem finished.")
  }

  //Function to write the corrupt log lines on FileSystem
  def writeCorruptLogLinesToFS(
    corruptLogLinesFormatted: Dataset[String],
    resultFileLoc: String) = {
    println("Printing corrupt entries")
    corruptLogLinesFormatted.schema
    corruptLogLinesFormatted.show(false)
    logger.debug("Write corrupt entries to filesystem started.")

    corruptLogLinesFormatted.coalesce(1)
      .write.option("header", "true")
      .mode("overwrite")
      .parquet(resultFileLoc+"nasa_access_log_currupt_enteries.parquet" )
    logger.debug("Write corrupt entries to filesystem finished.")

  }

}