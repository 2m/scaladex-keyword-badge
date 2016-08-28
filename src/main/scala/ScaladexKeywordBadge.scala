package lt._2m.scaladex_keyword_badge

import akka.actor._
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.stream._
import akka.stream.scaladsl._
import com.drewhk.stream.xml.Xml
import com.drewhk.stream.xml.Xml._
import kantan.regex.implicits._
import org.htmlcleaner._
import scala.util._

object ScaladexKeywordBadge {

  type HttpFlow = Flow[(HttpRequest, Int), (Try[HttpResponse], Int), _]

  def main(args: Array[String]) = {
    implicit val system       = ActorSystem("ScaladexKeywordBadge")
    implicit val materializer = ActorMaterializer()
    implicit val ec           = system.dispatcher

    val poolClientFlow = Http().cachedHostConnectionPoolHttps[Int]("index.scala-lang.org")
    val bindingFuture  = Http().bindAndHandle(route(poolClientFlow), "localhost", 8080)

    scala.io.StdIn.readLine()
    bindingFuture.flatMap(_.unbind()).onComplete(_ => system.terminate())
  }

  def route(http: HttpFlow) =
    path("keyword" / Segment) { keyword =>
      get {
        parameters('style ? "flat", 'label ? keyword) { (style, label) =>
          extractMaterializer { implicit mat =>
            extractExecutionContext { implicit ec =>
              val numProjects = Source
                .single(HttpRequest(uri = s"/search?q=keywords:$keyword") -> 42)
                .via(http)
                .flatMapConcat {
                  case (Success(resp), _) => resp.entity.dataBytes
                }
                .via(Html.cleaner)
                .via(Xml.parser)
                .via(Xml.subslice(
                  "html" :: "body" :: "main" :: "div" :: "div" :: "div" :: "div" :: Nil))
                .map {
                  case Characters(text) => text
                }
                .mapConcat(_.evalRegex[Int](rx"(\d+) results", 1).to[collection.immutable.Seq])
                .map(_.toEither)
                .map {
                  case Right(projects) => projects
                }
                .runWith(Sink.headOption)(mat)
                .map(_.getOrElse(0))
              complete(numProjects.map(redirect(label, style, _)))
            }
          }
        }
      }
    }

  def redirect(label: String, style: String, projects: Int) =
    HttpResponse(status = StatusCodes.Found,
                 headers = headers.Location(
                     Uri(s"https://img.shields.io/badge/scaladex-$projects%20projects-blue.svg")
                       .withRawQueryString(s"style=$style&label=$label")) :: Nil,
                 entity = HttpEntity.Empty)
}

object Html {

  def cleaner = {
    val Charset    = "utf-8"
    val cleaner    = new HtmlCleaner()
    val serializer = new SimpleHtmlSerializer(cleaner.getProperties())

    val source = StreamConverters.asOutputStream()
    val sink   = StreamConverters.asInputStream()

    Flow.fromSinkAndSourceMat(sink, source) { (inputStream, outputStream) =>
      val tags = cleaner.clean(inputStream, Charset)
      serializer.writeToStream(tags, outputStream, Charset)
    }
  }
}
