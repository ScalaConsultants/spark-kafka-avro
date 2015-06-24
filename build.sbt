val metaSettings = Seq(
  name := "spark-kafka-avro",
  description := "POC: Spark consumer for bottledwater-pg Kafka Avro topics",
  version := "1.0.0"
)

val scalaSettings = Seq(
  scalaVersion := "2.11.6",
  scalacOptions ++= Seq("-feature", "-unchecked", "-deprecation", "-encoding", "utf8")
)

val sbtSettings = Seq(
  connectInput in run := true,
  fork in run := true,
  javaOptions in run ++= Seq("-Xms512M", "-Xmx1G")
)

val dependencies = Seq(
  "org.apache.spark" % "spark-streaming_2.10" % "1.4.0",
  "org.apache.spark" % "spark-streaming-kafka_2.10" % "1.4.0"
)

lazy val root = (project in file(".")).
  settings(metaSettings: _*).
  settings(scalaSettings: _*).
  settings(sbtSettings: _*).
  settings(libraryDependencies ++= dependencies)
