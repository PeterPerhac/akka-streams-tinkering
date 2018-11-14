package com.perhac.experiments.akkastreams

import java.nio.file.Paths

import akka.Done
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.OverflowStrategy.backpressure
import akka.stream.scaladsl._
import akka.util.ByteString
import com.perhac.experiments.akkastreams.Tweet.parseTweet

import scala.PartialFunction.condOpt
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.util.matching.Regex

final case class Author(handle: String)
final case class Hashtag(name: String)
final case class Tweet(author: Author, timestamp: Long, body: String) {
  val hashtags: Set[Hashtag] = Hashtag.HashtagRegex.findAllIn(body).map(Hashtag.apply).toSet
}

object Hashtag {
  val HashtagRegex = """(#[\w-]+)""".r
}
object Tweet {
  val TweetRegex: Regex = """^(\w+); (.+)$""".r
  def parseTweet(s: String): Option[Tweet] = condOpt(s) {
    case TweetRegex(author, body) => Tweet(Author(author), System.currentTimeMillis(), body)
  }
}

object TweetsDemo extends App {

  implicit val system: ActorSystem = ActorSystem("reactive-tweets")
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  val utf8: ByteString => String = _.utf8String
  def combineSets[T]: (Set[T], Set[T]) => Set[T] = (s1, s2) => {
    println(s"combining sets $s1 and $s2")
    s1 ++ s2
  }

  val res: Future[Done] = FileIO
    .fromPath(Paths.get("tweets"))
    .buffer(10, backpressure)
    .via(Framing.delimiter(ByteString("\n"), 1024))
    .map(utf8.andThen(parseTweet))
    .map(_.map(_.hashtags).toSet)
    .reduce(combineSets[Hashtag])
    .mapConcat(identity)
    .take(5)
    .runWith(Sink.foreach(println))

  res.onComplete(_ => system.terminate())

  Await.ready(res, Duration.Inf)
}
