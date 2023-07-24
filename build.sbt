ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.11"

val PEKKO_KINESIS_VERSION = "1.0.5"

lazy val root = (project in file("."))
  .settings(
    name := "DynamoDBStreamWithPekko",
    libraryDependencies ++= Seq(
      "com.github.j5ik2o" %% "pekko-kinesis-kcl" % PEKKO_KINESIS_VERSION,
      "com.github.j5ik2o" %% "pekko-kinesis-kcl-dynamodb-streams" % PEKKO_KINESIS_VERSION
    )
  )
