package com.mediahub

import java.nio.file.Paths

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl._
import akka.stream.scaladsl.FileIO
import akka.util.ByteString

import scala.concurrent.Await
import scala.concurrent.duration._

import com.rabbitmq.client.ConnectionFactory


object Main extends App with MainConfig {

  // init akka stream system
  implicit val actorSystem = ActorSystem()
  import actorSystem.dispatcher
  implicit val flowMaterializer = ActorMaterializer()

  // init rabbit channel connection
  implicit val factory = new ConnectionFactory
  factory.setHost(serverHost)
  factory.setPort(serverPort)
  implicit val connection = factory.newConnection
  implicit val channel = connection.createChannel

  //Source
  val logFile = Paths.get(data)
  val source = FileIO.fromPath(logFile)

  //Flow
  val flow = Framing
    .delimiter(ByteString(System.lineSeparator()), maximumFrameLength = 512, allowTruncation = true)
    .map(_.utf8String)

  //Sink
  val sink = Sink.foreach(processLine)

  //Start hole process
  source
    .via(flow)
    .runWith(sink)
    .andThen {
      case _ =>
        actorSystem.terminate()
        Await.ready(actorSystem.whenTerminated, 1 minute)
        if (connection != null) connection.close()
    }

  case class Hub(tconst: String,	titleType: String,	primaryTitle: String, originalTitle: String,
                 isAdult: String,	startYear: String,	endYear: String,	runtimeMinutes: String, genres: String)

  def processLine(line: String): Unit = {
    val parsedLine = line.split("\t")
    val hub = Hub(parsedLine(0), parsedLine(1), parsedLine(2), parsedLine(3), parsedLine(4),
                    parsedLine(5), parsedLine(6), parsedLine(7), parsedLine(8))

    if(hub.genres.contains(genre) && hub.titleType == titleType) try {
      try {
        channel.queueDeclare(queueName, false, false, false, null)
        channel.basicPublish("", queueName, null, hub.originalTitle.getBytes)
        println(" [x] Sent '" + hub.originalTitle + "'")
      }
    }
  }

}


