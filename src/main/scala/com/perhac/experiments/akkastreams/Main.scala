package com.perhac.experiments.akkastreams

import java.nio.file.Paths

import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl._
import akka.util.ByteString

import scala.concurrent._

object Main {

  implicit val system = ActorSystem("QuickStart")
  implicit val materializer = ActorMaterializer()
  implicit val ec: ExecutionContextExecutor = system.dispatcher

  val makeString: ByteString => String = _.utf8String
  val splitString: String => (String, Int, String) = _.split(",").toList match {
    case name :: age :: company :: Nil => (name, age.toInt, company)
  }

  def main(args: Array[String]): Unit = {
    FileIO
      .fromPath(Paths.get("input-data"))
      .via(Framing.delimiter(ByteString("\n"), Integer.MAX_VALUE))
      .map(makeString.andThen(splitString))
      .grouped(100)
      .to(Sink.foreach(println))
      .run()
      .onComplete(_ ⇒ system.terminate())
  }

}
