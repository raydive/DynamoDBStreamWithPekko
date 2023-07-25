import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.client.builder.AwsClientBuilder
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient
import com.amazonaws.services.dynamodbv2.streamsadapter.AmazonDynamoDBStreamsAdapterClient
import com.amazonaws.services.dynamodbv2.{AmazonDynamoDBClient, AmazonDynamoDBStreamsClient}
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.KinesisClientLibConfiguration
import com.github.j5ik2o.ak.kcl.dyanmodb.streams.KCLSourceOnDynamoDBStreams
import com.github.j5ik2o.ak.kcl.stage.CommittableRecord
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream._
import org.apache.pekko.stream.scaladsl._

import java.util.concurrent.Executors
import scala.concurrent.ExecutionContext

object Main extends App {
  implicit val system: ActorSystem = ActorSystem("Demo")
  val config = new KinesisClientLibConfiguration(
    "DynamoDBStreamWithPekko",
    "", // TODO テーブルからstreamarnの情報をとってくるようにする
    DefaultAWSCredentialsProviderChain.getInstance(),
    "worker"
  )
  val dynamoDBClient = AmazonDynamoDBClient.builder()
    .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(
      "http://localhost:8000", "us-west-2"
    )).build()
  val cloudwatchClient = AmazonCloudWatchClient.builder()
    .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(
      "http://localhost:4566", "us-west-2"
    )).build()
  val dynamoDBStreamsClient = AmazonDynamoDBStreamsClient.builder()
    .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(
      "http://localhost:8000", "us-west-2"
    )).build()
  val adapter = new AmazonDynamoDBStreamsAdapterClient(dynamoDBStreamsClient)

  val executorService = Executors.newCachedThreadPool()
  implicit val ctx = ExecutionContext.fromExecutorService(executorService)
  KCLSourceOnDynamoDBStreams
    .withoutCheckpoint(
      kinesisClientLibConfiguration = config,
      amazonDynamoDBStreamsAdapterClient = adapter,
      amazonDynamoDB = dynamoDBClient,
      amazonCloudWatchClientOpt = Some(cloudwatchClient),
      execService = executorService,
      metricsFactoryOpt = None
    )
    .viaMat(KillSwitches.single)(Keep.right)
    .via(Flow[CommittableRecord].map { r => println(r.sequenceNumber); r })
    .toMat(Sink.foreach(r => println(r.sequenceNumber)))(Keep.both)
    .run()
}