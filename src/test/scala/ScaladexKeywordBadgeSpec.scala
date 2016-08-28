package lt._2m.scaladex_keyword_badge

import akka.stream.scaladsl._
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest._
import scala.util._

class ScaladexKeywordBadgeSpec extends WordSpec with Matchers with ScalatestRouteTest {
  import ScaladexKeywordBadge.{route => badge}
  import ScaladexKeywordBadgeSpec._

  "scaladex keyword badge" should {

    "redirect to shields.io" in withResponse("<div>2 results</div>") { http =>
      Get("/keyword/alpakka") ~> badge(http) ~> check {
        header[Location] shouldBe redirect(projects = 2)
      }
    }

    "redirect to shields.io with 0 results" in withResponse("<p>no results</p>") { http =>
      Get("/keyword/alpakka") ~> badge(http) ~> check {
        header[Location] shouldBe redirect(projects = 0)
      }
    }

    "preserve style in redirect" in withResponse("<div>3 results</div>") { http =>
      Get("/keyword/alpakka?style=fancy") ~> badge(http) ~> check {
        header[Location] shouldBe redirect(style = "fancy", projects = 3)
      }
    }

    "preserve label in redirect" in withResponse("<div>3 results</div>") { http =>
      Get("/keyword/alpakka?label=Akka%20Persistence") ~> badge(http) ~> check {
        header[Location] shouldBe redirect(label = "Akka Persistence", projects = 3)
      }
    }

    "redirect default when connection fails" in withFailedConnection { http =>
      Get("/keyword/alpakka") ~> badge(http) ~> check {
        header[Location] shouldBe redirect()
      }
    }
  }

}

object ScaladexKeywordBadgeSpec {
  import ScaladexKeywordBadge._

  def withResponse(text: String)(test: HttpFlow => Any) = {
    val responseHtml =
      s"<html><body><main><div><div><div>$text"
    val response = HttpEntity(ContentTypes.`text/html(UTF-8)`, responseHtml)
    val http = Flow.fromSinkAndSource[(HttpRequest, Int), (Try[HttpResponse], Int)](
      Sink.ignore,
      Source.single((Success(HttpResponse(entity = response)), 42)))
    test(http)
  }

  def withFailedConnection(test: HttpFlow => Any) = {
    val http = Flow
      .fromSinkAndSource[(HttpRequest, Int), (Try[HttpResponse], Int)](Sink.ignore, Source.empty)
    test(http)
  }

  def redirect(label: String = "alpakka", style: String = "flat", projects: Int = 0) =
    Some(
      Location(
        Uri(s"https://img.shields.io/badge/scaladex-$projects%20projects-blue.svg")
          .withRawQueryString(s"style=$style&label=$label")))

}
