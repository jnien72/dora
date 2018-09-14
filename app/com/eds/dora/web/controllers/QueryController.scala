package com.eds.dora.web.controllers

import java.io._
import java.nio.charset.Charset
import java.util.regex.Pattern
import java.util.zip.{ZipEntry, ZipOutputStream}

import com.eds.dora.query.model.QueryResponse
import com.eds.dora.query.service.QueryClient
import com.eds.dora.util._
import com.eds.dora.web.annotations.SectionDiv
import com.eds.dora.web.views.html.query
import com.eds.sql.udf.DoraUDF
import org.slf4j.LoggerFactory
import play.api.libs.iteratee.Enumerator
import play.api.mvc._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.io.Source

@SectionDiv(id = "query", name = "Query", icon = "fa-search", order = 3, instance = "QueryInstance")
object QueryController extends Controller {

  val LOG = LoggerFactory.getLogger(getClass)

  def main = Action { request =>
    val doraUdfs=HiveUtils.getDefinedUdfs.map(x=>x._2.asInstanceOf[DoraUDF])
    Ok(query(doraUdfs))
  }

  val commentPattern: Pattern = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL)

  def queryResult = Action { request =>
    val isDownload = !request.headers.get("Content-Type").get.contains("json")
    val user: String = request.session.get("username").orNull
    val namespace: String = request.session.get("namespace").orNull

    val fileName = "dora-" + user + "-" + System.currentTimeMillis() + "-" + System.nanoTime()

    var t: Throwable = null
    var writer: Writer = null

    if(isDownload) {
      val enumerator = Enumerator.outputStream { os =>
        try {
          val zipOut = new ZipOutputStream(os)
          zipOut.putNextEntry(new ZipEntry(fileName + ".tsv"))
          writer = new OutputStreamWriter(zipOut, Charset.forName("UTF-8"))
          val body = request.body.asFormUrlEncoded.get
          val sql: String = commentPattern.matcher(body.get("sql").get.head).replaceAll("")
            .split("\n").map(x => x.trim).filter(x => !x.startsWith("--")).mkString("\n").trim
          val txDate: String = body.get("txDate").get.head
          var inputStream: InputStream = null
          try {
            val jobId: String = body.get("jobId").get.head
            val result = QueryClient.get(namespace).queryAsStream(user, jobId, sql, -1, txDate)
            val columns = result._1
            inputStream = result._2
            if (columns.isDefined) {
              val headers = columns.get.map(column => column.replace("\t", " ")).mkString("\t")
              writer.write(headers + "\n")
              Source.fromInputStream(inputStream).getLines().foreach(line => {
                val modifiedLine = JsonUtils.convertJsonToArray(line, columns.get).map(field => {
                  field.replaceFirst("^\"", "").replaceFirst("\"$", "").replace("\t", " ")
                }).mkString("\t")
                writer.write(modifiedLine + "\n")
              })
            } else {
              Source.fromInputStream(inputStream).getLines().foreach(line => {
                writer.write(line + "\n")
              })
            }
          } catch {
            case t: Throwable => writer.write(ExceptionUtils.toString(t))
          } finally {
            if (inputStream != null) {
              inputStream.close()
            }
          }
        } catch {
          case ex: Exception => {
            ex.printStackTrace()
            t = ex
          }
        } finally {
          try {
            writer.close()
          } catch {
            case t: Throwable =>
          }
        }
      }
      Ok.chunked(enumerator >>> Enumerator.eof).as("application/force-download")
        .withHeaders(("Content-Disposition", "attachment;filename=" + fileName + ".zip"))
    } else {
      val buffer = new CircularByteBuffer(1025*1024, true)
      val out = buffer.getOutputStream
      writer = new OutputStreamWriter(out)
      try {
        val body: Map[String, String] = JsonUtils.fromJson[Map[String, String]](request.body.asJson.get.toString())
        val sql: String = commentPattern.matcher(body.get("sql").get).replaceAll("")
          .split("\n").filter(x => !x.startsWith("--")).mkString("\n").trim
        val limit: Integer = body.get("limit").get.toInt
        val txDate: String = body.get("txDate").get
        var queryResponse:QueryResponse=null
        try{
          val jobId: String = body.get("jobId").get
          queryResponse=QueryClient.get(namespace).query(user, jobId, sql, limit, txDate)
        }catch{
          case t:Throwable=>queryResponse=QueryResponse("",null,null,ExceptionUtils.toString(t))
        }
        writer.write(JsonUtils.toJson(queryResponse))
      } catch {
        case ex: Exception => {
          ex.printStackTrace()
          t = ex
        }
      } finally {
        try {
          writer.close()
        } catch {
          case t: Throwable =>
        }
      }
      Ok.chunked(Enumerator.fromStream(buffer.getInputStream).andThen(Enumerator.eof)).as("text/html")
    }
  }

  def template = Action { request =>
    val username = request.session.get("username").orNull
    val namespace: String = request.session.get("namespace").orNull
    if (username != null) {
      val post = request.getQueryString("post").orNull
      val delete = request.getQueryString("delete").orNull

      if (post != null) {
        val json = JsonUtils.fromJson[Map[String,String]](request.body.asJson.get.toString())
        QueryClient.get(namespace).setTemplate(username, json.get("name").get,json.get("sql").get)
        Ok("Ok")
      } else if (delete != null) {
        QueryClient.get(namespace).deleteTemplate(username,delete)
        Ok("Ok")
      } else {
        Ok(JsonUtils.toJson(QueryClient.get(namespace).listTemplates(username)))
      }
    } else {
      Forbidden("Error: Not logged in")
    }
  }

  def schema = Action { request =>
    val namespace: String = request.session.get("namespace").orNull
    val username = request.session.get("username").orNull
    if (username != null) {
      Ok(JsonUtils.toJson(Array[String](namespace), prettyPrint = true))
    } else {
      Forbidden("Error: Not logged in")
    }
  }

  def table = Action { request =>
    val namespace: String = request.session.get("namespace").orNull
    val username = request.session.get("username").orNull
    if (username != null) {
      val schema: String = request.getQueryString("schema").getOrElse("")
      Ok(JsonUtils.toJson(QueryClient.get(namespace).meta(username, "table", schema), prettyPrint = true))
    } else {
      Forbidden("Error: Not logged in")
    }
  }

  def field = Action { request =>
    val schema: String = request.getQueryString("schema").getOrElse("")
    val table: String = request.getQueryString("table").getOrElse("")
    val namespace: String = request.session.get("namespace").orNull
    val username = request.session.get("username").orNull
    if (username != null) {
      Ok(JsonUtils.toJson(QueryClient.get(namespace).meta(username, "field", schema + "." + table), prettyPrint = true))
    } else {
      Forbidden("Error: Not logged in")
    }
  }

  private def stopJob(namespace: String, user:String, jobId: String): Unit = {
    GlobalSync.get(jobId).synchronized {
      try {
        QueryClient.get(namespace).stop(user, jobId)
      } catch {
        case ex: Exception => //ignore errors
      }
    }
  }


}