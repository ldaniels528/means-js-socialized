package com.socialized.javascript.routes

import com.github.ldaniels528.meansjs.nodejs.request.Request
import com.github.ldaniels528.meansjs.nodejs.splitargs.SplitArgs
import com.github.ldaniels528.meansjs.nodejs.{Require, console}
import com.github.ldaniels528.meansjs.util.ScalaJsHelper._
import com.socialized.javascript.StringHelper._

import scala.concurrent.{ExecutionContext, Future}

/**
  * Shared Content Parser
  * @author lawrence.daniels@gmail.com
  */
class SharedContentParser(require: Require) {
  val splitArgs = require[SplitArgs]("splitargs")
  val request = require[Request]("request")

  def parse(url: String)(implicit ec: ExecutionContext): Future[Map[String, String]] = {
    for {
      (response, body) <- request.getAsync(url) //if response.statusCode == 200

      dataSet = body.findIndices("<head", "</head>") map {
        case (start, end) => body.substring(start, end - start)
      } match {
        case Some(text) => text.extractAll("<meta", ">")
        case None => Nil
      }

    } yield (dataSet map mapify).foldLeft(Map[String, String]()) { (dict, map) => dict ++ map }
  }

  private def mapify(line: String): Map[String, String] = {
    val mapping = Map(splitArgs(line).toSeq flatMap (_.split("[=]", 2).toSeq match {
      case Seq(key, value) => Some(key -> value.unquote)
      case values =>
        console.error("missed: %s", values.mkString(", "))
        None
    }): _*)

    (for {
      name <- mapping.get("name") ?? mapping.get("property")
      content <- mapping.get("content")
    } yield (name, content)) match {
      case Some((key, value)) => Map(key -> value)
      case None => Map.empty
    }
  }

}