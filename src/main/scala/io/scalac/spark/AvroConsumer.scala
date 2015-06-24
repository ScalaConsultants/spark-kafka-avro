package io.scalac.spark

import kafka.serializer.StringDecoder
import org.apache.spark.SparkConf
import org.apache.spark.streaming.kafka.KafkaUtils
import org.apache.spark.streaming.{StreamingContext, Seconds}

object AvroConsumer {

  def main(args: Array[String]) {
    val conf = new SparkConf().setAppName("DirectKafkaAvroConsumer")
    val ssc = new StreamingContext(conf, Seconds(10))
    val kafkaParams = Map[String, String]("metadata.broker.list" -> "tcp://localhost:1234")
    val topicSet = Set("categories")
    val messages = KafkaUtils.createDirectStream[String, String, StringDecoder, StringDecoder](ssc, kafkaParams, topicSet)

    // Process lines
    // val lines = SomeProcessor.process(messages.map(_._2))
    // lines.print()

    ssc.start()
    ssc.awaitTermination()
//    ssc.stop()
  }
}
