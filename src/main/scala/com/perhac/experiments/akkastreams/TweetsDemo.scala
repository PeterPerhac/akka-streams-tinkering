package com.perhac.experiments.akkastreams

import java.nio.file.Paths

import akka.Done
import akka.actor.ActorSystem
import akka.stream.scaladsl._
import akka.stream.{ActorMaterializer, IOResult}
import akka.util.ByteString

import scala.PartialFunction.condOpt
import scala.concurrent.duration.{Duration, _}
import scala.concurrent.{Await, Future}
import scala.util.matching.Regex

object TweetsDemo extends App {

  final case class Author(handle: String)

  final case class Hashtag(name: String)

  final case class Tweet(author: Author, timestamp: Long, body: String) {
    def hashtags: Set[Hashtag] =
      body
        .split(" ")
        .collect {
          case t if t.startsWith("#") ⇒ Hashtag(t.replaceAll("[^#\\w]", ""))
        }
        .toSet
  }
  object Tweet {

    val TweetFormat: Regex = """^(\w+); (.+)$""".r

    def parseTweet(s: String): Option[Tweet] = condOpt(s) {
      case TweetFormat(author, body) => Tweet(Author(author), System.currentTimeMillis(), body)
    }
  }

  implicit val system: ActorSystem = ActorSystem("reactive-tweets")
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  val utf8: ByteString => String = _.utf8String
  def combineSets[T]: (Set[T], Set[T]) => Set[T] = (s1, s2) => {
    println(s"combining sets $s1 and $s2")
    s1 ++ s2
  }

  val fileSrc = FileIO.fromPath(Paths.get("tweets"))
  val tweets: Source[Option[Tweet], Future[IOResult]] =
    fileSrc.via(Framing.delimiter(ByteString("\n"), maximumFrameLength = 1024)).map(utf8.andThen(Tweet.parseTweet))
  val res: Future[Done] = tweets
    .mapConcat(_.map(_.hashtags).toList)
    .reduce(combineSets[Hashtag])
    .mapConcat(identity)
    .map(_.name.toUpperCase)
    .throttle(1, 250.millis)
    .runWith(Sink.foreach(println)) // Attach the Flow to a Sink that will finally print the hashtags

  import scala.concurrent.ExecutionContext.Implicits.global
  res.onComplete(_ => system.terminate())

  Await.ready(res, Duration.Inf)
}
