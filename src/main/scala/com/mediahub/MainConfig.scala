package com.mediahub

import com.typesafe.config.ConfigFactory

trait MainConfig {

  val queueName = ConfigFactory.load().getString("rabbitmq.queueName")

  val serverHost = ConfigFactory.load().getString("rabbitmq.server.host")
  val serverPort = ConfigFactory.load().getInt("rabbitmq.server.port")

  val data = ConfigFactory.load().getString("data.dir")
  val genre = ConfigFactory.load().getString("data.genre")
  val titleType = ConfigFactory.load().getString("data.titleType")
}
