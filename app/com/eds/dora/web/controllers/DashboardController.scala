package com.eds.dora.web.controllers


import com.eds.dora.cluster.util.UserInfoUtils
import com.eds.dora.query.service.QueryClient
import com.eds.dora.util.{DateTimeUtils, JsonUtils}
import com.eds.dora.web.annotations.SectionDiv
import com.eds.dora.web.views.html
import org.slf4j.LoggerFactory
import play.api.mvc._


@SectionDiv(id = "dashboard", name = "Dashboard", icon = "fa-dashboard", order = 1)
object DashboardController extends Controller {

  val LOG = LoggerFactory.getLogger(getClass)

  def main = Action { request =>
    if(request.session.get("namespace").isDefined && request.session.get("username").isDefined) {
      val namespace = request.session.get("namespace").get
      val userInfo = UserInfoUtils.get(request.session.get("username").get)
      if(userInfo.isDefined) {
        val namespaceAcl = userInfo.get.acl.find(_._1.equals(namespace))
        if(namespaceAcl.isDefined) {
          val components  = List(QueryController.getClass)
            .toArray
            .map(x => x.getDeclaredAnnotation(classOf[SectionDiv]))
            .filter(_ != null)
            .filter(x => {
              namespaceAcl.get._2.contains(x.id())
            })
            .map(_.id())
          val colSize = if(components.length == 0) 0 else 12/components.length
          Ok(html.dashboard(components, colSize))
        } else {
          Redirect("/login.html")
        }
      } else {
        Redirect("/login.html")
      }
    } else {
      Redirect("/login.html")
    }
  }

  def queryStatus = Action { request =>
    val namespace=request.session.get("namespace").get
    val user=request.session.get("username").get
    println(s"namespace $namespace")
    println(s"user $user")
    val jobStatusArr = QueryClient.get(namespace).status(user)
    val data: Array[Array[String]] = Array(Array("DateTime(local)", "TaskId", "Username", "Elapsed", "Status", "Query")) ++
      jobStatusArr.map(x => {
        if (x.elapsed.length == 0) {
          x.elapsed = DateTimeUtils.getTimeDiffExp(x.startMillis.toLong, System.currentTimeMillis())
        }
        val id=if(x.status.equalsIgnoreCase("running")){
          x.jobId +" [<a style='cursor:pointer;' onclick='sectionMap[\"dashboard\"].cancelQuery(\""+x.jobId+"\")'>cancel</a>]"
        }else{
          x.jobId
        }
        Array(x.startMillis, id,
          x.user, x.elapsed, x.status, x.sql)
      })
    Ok(JsonUtils.toJson(data))
  }

  def queryCancel = Action { request =>
    val id=request.getQueryString("id").getOrElse("")
    if(id.length>0){
      val namespace=request.session.get("namespace").get
      val user=request.session.get("username").get
      QueryClient.get(namespace).stop(user,id)
    }
    Ok("")
  }
}