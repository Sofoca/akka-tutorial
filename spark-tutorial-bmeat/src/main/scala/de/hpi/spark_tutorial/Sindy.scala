package de.hpi.spark_tutorial

import org.apache.spark.sql._
import org.apache.spark.sql.functions._

object Sindy {


  def discoverINDs(inputs: List[String], spark: SparkSession): Unit = {
    import spark.implicits._

    inputs.map(input => spark.read
      .option("inferSchema", "true")
      .option("header", "true")
      .option("delimiter", ";")
      .csv(input))
      .map(
        df =>
          df.columns.foldLeft(df) { (acc, col) => acc.withColumn(col, df(col).cast("string")) }
            .flatMap(row => row.getValuesMap[String](row.schema.fieldNames))
            .groupBy("_2")
            .agg(collect_set("_1"))
      )
      .reduce(_.union(_))
      .groupBy("_2")
      .agg(array_distinct(flatten(collect_set("collect_set(_1)"))))
      .select("array_distinct(flatten(collect_set(collect_set(_1))))")
      .flatMap(row => {
        val list = row.getAs[Seq[String]](0)
        list.map(item => {
          (item, list.filter(item2 => item2.toString != item.toString))
        })
      })
      .rdd
      .reduceByKey((first, second) => first.intersect(second))
      .toDF()
      .filter(row => row.getAs[Seq[String]](1).nonEmpty)
      .sort($"_1".asc)
      .collect()
      .foreach(row => println(row.getAs[String](0) + " < " + row.getAs[Seq[String]](1).sorted.mkString(",")))
  }
}
