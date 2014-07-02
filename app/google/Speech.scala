package google

import scala.concurrent.ExecutionContext.Implicits.global
import play.api.Play.current
import play.api.libs.ws.WS
import play.api.libs.json.JsValue
import play.api.libs.json.JsResult
import play.api.libs.json.Json

object Speech {

  private val apiKey = "AIzaSyBOti4mM-6x9WDnZIjIeyEU21OpBXqWBgw"

  def getText(data: Array[Byte]) = {

    val url = "https://www.google.com/" +
      "/speech-api/v2/recognize" +
      "?output=json" +
      "&lang=en-us&key=" + apiKey +
      "&results=6&pfilter=2"

    val futureResp = WS.url(url)
      .withHeaders(("Content-Type", "audio/x-flac; rate=16000"))
      .withHeaders(("User-Agent", "speech2text"))
      .withRequestTimeout(60000)
      .post(data)

    val futureText = for (resp <- futureResp) yield {
      val jsonString = resp.body.split("\n")(1)
      val json: JsValue = Json.parse(jsonString)
      val jsonTranscript = ((json \ "result")(0) \ "alternative")(0) \ "transcript"
      val jsonResult: JsResult[String] = jsonTranscript.validate[String]
      jsonResult.getOrElse("");
    }

    futureText
  }

}