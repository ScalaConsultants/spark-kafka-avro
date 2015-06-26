package io.scalac.spark

import io.confluent.kafka.serializers.KafkaAvroDecoder
import org.apache.spark.SparkConf
import org.apache.spark.streaming.kafka.KafkaUtils
import org.apache.spark.streaming.{StreamingContext, Seconds}

case class Config(postgres: Map[String, String] = Map(), broker: String = "", schemaRegistry: String = "")

object AvroConsumer {

  def main(args: Array[String]) {
    val parser = new scopt.OptionParser[Config]("spark") {
      opt[Map[String, String]]("postgres") required() action { (x, c) =>
        c.copy(postgres = x)
      } keyValueName("<key>", "<value>") text ("are PostgreSQL connection params")

      opt[String]("broker") required() action { (x, c) =>
        c.copy(broker = x)
      } valueName ("<broker1-host:port>,<broker2-host:port>") text ("is a list of one or more Kafka brokers")

      opt[String]("schema-registry") required() action { (x, c) =>
        c.copy(schemaRegistry = x)
      } valueName ("<schema registry url>") text ("is the Avro schema-registry URL")
    }

    parser.parse(args, Config()) match {
      case Some(config) => process(config)
      case _ =>
    }
  }

  def process(config: Config) = {
    val conf = new SparkConf().setAppName("DirectKafkaAvroConsumer")
    val ssc = new StreamingContext(conf, Seconds(10))
    val kafkaParams = Map[String, String](
      "auto.offset.reset" -> "smallest",
      "metadata.broker.list" -> config.broker,
      "schema.registry.url" -> config.schemaRegistry)
    val topicSet = Set("categories")
    val messages = KafkaUtils.createDirectStream[Object, Object, KafkaAvroDecoder, KafkaAvroDecoder](ssc, kafkaParams, topicSet)

    // Process lines
     val lines = messages.map(AvroConverter.convert(_))
     lines.print()

    ssc.start()
    ssc.awaitTermination()
  }
}
