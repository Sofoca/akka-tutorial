package de.hpi.spark_tutorial

import java.io.File

import org.apache.spark.sql.SparkSession
import org.apache.log4j.Logger
import org.apache.log4j.Level

object SimpleSpark extends App {

  // https://stackoverflow.com/questions/2315912/best-way-to-parse-command-line-parameters
  def parseArgs(args: Array[String]) = {

    //val usage = """
    //Usage: java -jar sindy.jar --path TPCH --cores 4 must work.
    //"""
    //if (args.length == 0) println(usage)
    val arglist = args.toList
    type OptionMap = Map[Symbol, String]

    @scala.annotation.tailrec
    def nextOption(map : OptionMap, list: List[String]) : OptionMap = {
      list match {
        case Nil => map
        case "--path" :: value :: tail =>
          nextOption(map ++ Map('path -> value.toString), tail)
        case "--cores" :: value :: tail =>
          nextOption(map ++ Map('cores -> value.toString), tail)
        case option :: tail => println("Unknown option "+option)
          sys.exit(1)
      }
    }
    nextOption(Map(),arglist)
  }

  override def main(args: Array[String]): Unit = {

    // Turn off logging
    Logger.getLogger("org").setLevel(Level.OFF)
    Logger.getLogger("akka").setLevel(Level.OFF)

    val parsed_args = parseArgs(args)

    var core_count = 4
    if (parsed_args.contains('cores)) {
      core_count = parsed_args('cores).toInt
    }

    var data_set_path = "./TPCH"
    if (parsed_args.contains('path)) {
      data_set_path = parsed_args('path).toString
    }


    // Create a SparkSession to work with Spark
    val sparkBuilder = SparkSession
      .builder()
      .appName("BMeat")
      .master("local[" + core_count + "]") // local, with 4 worker cores
    val spark = sparkBuilder.getOrCreate()

    // Set the default number of shuffle partitions (default is 200, which is too high for local deployment)
    spark.conf.set("spark.sql.shuffle.partitions", (core_count).toString) //


    def time[R](block: => R): R = {
      val t0 = System.currentTimeMillis()
      val result = block
      val t1 = System.currentTimeMillis()
      println(s"Execution: ${t1 - t0} ms")
      result
    }

    def getListOfFiles(dir: String):List[String] = {
      val d = new File(dir)
      if (d.exists && d.isDirectory) {
        d.listFiles.filter(_.isFile).map(_.toString).toList
      } else {
        List[String]()
      }
    }

    time {Sindy.discoverINDs(getListOfFiles(data_set_path), spark)}
    //Sindy.discoverINDs(getListOfFiles(data_set_path), spark)
  }
}
