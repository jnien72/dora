package com.eds.dora.web.controllers

import com.eds.dora.cluster.ClusterAdmin
import com.eds.dora.cluster.model.Topology
import com.eds.dora.cluster.util.{InstanceUtils, NamespaceInfoUtils, UserInfoUtils}
import com.eds.dora.util._
import com.eds.dora.web.annotations.SectionDiv
import com.eds.dora.web.model.SectionDesc
import com.eds.dora.web.views.html
import org.reflections.Reflections
import org.slf4j.LoggerFactory
import play.api.mvc.{Action, Controller}

import scala.sys.process._

object MainController extends Controller {

  val LOG = LoggerFactory.getLogger(getClass)

  def root = Action {
    TemporaryRedirect("/dora.html#dashboard")
  }

  def main = Action { request =>
    if(request.session.get("namespace").isDefined && request.session.get("username").isDefined) {
      val namespace = request.session.get("namespace").get
      val username = request.session.get("username").get
      val userInfo = UserInfoUtils.get(username)
      if (userInfo.isDefined) {
        val acl=userInfo.get.acl
          .filter(a => {
            val (namespace, map) = a
            val namespaceInfo = NamespaceInfoUtils.get(namespace)
            if(namespaceInfo.isDefined) {
              namespaceInfo.get.enabled
            } else {
              false
            }
          })

        val sectionsDesc=new Reflections(this.getClass.getPackage.getName)
          .getSubTypesOf(classOf[Controller]).toArray
          .map(x=>x.asInstanceOf[Class[_]].getDeclaredAnnotation(classOf[SectionDiv]))
          .filter(x=>(x!=null))
          .filter(x => {
            val namespaceOpt = acl.find(_._1.equals(namespace))
            if(namespaceOpt.isDefined) {
              namespaceOpt.get._2.find(_.equals(x.id())).isDefined
            } else {
              false
            }
          })
          .sortBy(x=>x.order())
          .map(x=> {
            new SectionDesc(x.id(), x.name(), x.icon(), true)
          })
        Ok(html.index(request, sectionsDesc, acl.map(_._1).filter(!_.equals(namespace)).toArray))
      } else {
        Redirect("/login.html").withNewSession
      }
    } else {
      Redirect("/login.html").withNewSession
    }
  }

  def login = Action { request =>
    if (request.session.get("username") == null || request.session.get("username").getOrElse(null) == null) {
      val loginFailed = (request.getQueryString("loginFailed").getOrElse(null)!=null)
      Ok(html.login(loginFailed))
    } else {
      TemporaryRedirect("/")
    }
  }

  def loginSubmit = Action(parse.tolerantFormUrlEncoded) { request =>
    if (request.session.get("username") == null || request.session.get("username").getOrElse(null) == null) {
      val username = request.body.get("username").map(_.head)
      val password = request.body.get("password").map(_.head)
      val state:String=request.getQueryString("state").getOrElse(null)
      val userInfo = UserInfoUtils.get(username.get)
      if(userInfo.isDefined && userInfo.get.password.equals(password.get)) {
        val namespaces = userInfo.get.acl.keySet.toList
        val availableNamespaces = namespaces.filter(namespace => {
          val namespaceInfo = NamespaceInfoUtils.get(namespace)
          if(namespaceInfo.isDefined) {
            namespaceInfo.get.enabled
          } else {
            false
          }
        })
        val namespace = if(availableNamespaces != null && !availableNamespaces.isEmpty) {
          namespaces(0)
        } else {
          throw new RuntimeException("No available namespaces")
        }
        LOG.info(username.get + " directly logged in with namespace [" + namespace + "]")
        if(state==null||state.trim.length==0){
          Redirect("/").withSession(request.session + ("username" -> username.get) + ("namespace" -> namespace))
        }else{
          Redirect("/dora.html#"+state).withSession(request.session + ("username" -> username.get) + ("namespace" -> namespace))
        }
      } else {
        Redirect("/login.html?loginFailed=1")
      }
    } else {
      TemporaryRedirect("/")
    }

  }

  def logout = Action { request =>
    Redirect("/login.html").withSession(request.session - "username")
  }

  def namespace = Action { request =>
    val user = request.session.get("username").getOrElse(null)
    Ok(JsonUtils.toJson(UserInfoUtils.get(user).get.acl.keySet, prettyPrint = true))
  }

  def switchNamespace = Action { request =>
    if(request.body.asFormUrlEncoded.get("namespace") != null) {
      val namespace = request.body.asFormUrlEncoded.get("namespace")(0)
      val user = request.session.get("username").getOrElse(null)
      val userInfo = UserInfoUtils.get(user)
      if(userInfo.isDefined && userInfo.get.acl.find(_._1.equals(namespace)).isDefined) {
        request.session + ("namespace" -> namespace)
        val state:String = request.body.asFormUrlEncoded.get("state")(0)
        if(state==null||state.trim.length==0){
          Redirect("/").withSession(request.session + ("username" -> user) + ("namespace" -> namespace))
        }else {
          Redirect("/dora.html#" + state).withSession(request.session + ("username" -> user) + ("namespace" -> namespace))
        }
      } else {
        throw new RuntimeException("No permission for user [" + user+ "] to view namespace [" + namespace + "]")
      }
    } else {
      throw new RuntimeException("No namespace is found")
    }
  }

  def instancesStatus = Action { request =>
    val user = request.session.get("username").getOrElse(null)
    val namespace = request.session.get("namespace").getOrElse(null)
    val userInfo = UserInfoUtils.get(user)
    if(userInfo.isDefined) {
      val aclInstances = userInfo.get.acl.flatMap(acl => {
        if(acl._1.equals(namespace)) {
          acl._2.map(instanceType => {
            acl._1 + "-" + instanceType
          })
        } else {
          None
        }
      })
      val instances = InstanceUtils.retrieveAllInstanceDetails()
        .filter(instance => {
          val (instanceInfo, instanceStatus ) = instance._2
          aclInstances.find(instanceName => instanceName.equals(instanceInfo.getInstanceName())).isDefined
        })
        .map(instance => {
          val (instanceInfo, instanceStatus ) = instance._2
          if(instanceStatus != null) {
            Map[String, String](
              ("name"-> instanceInfo.getInstanceName()),
              ("heapSize"-> instanceInfo.heapSize),
              ("status"-> "running"),
              ("servicePort"-> instanceStatus.servicePort.toString),
              ("sparkUI"-> (instanceStatus.hostname + ":" + instanceStatus.sparkUiPort)),
              ("startTime"-> DateTimeUtils.getDateTimeExp(instanceStatus.startTime, "yyyy-MM-dd HH:mm:ss"))
            )
          } else {
            Map[String, String](
              ("name"-> instanceInfo.getInstanceName()),
              ("heapSize"-> instanceInfo.heapSize),
              ("status"-> "stopped"),
              ("servicePort"-> "--"),
              ("sparkUI"-> "--"),
              ("startTime"-> "--")
            )
          }
        })
      Ok(JsonUtils.toJson(instances))
    } else {
      throw new RuntimeException("User does not exist")
    }
  }

  def restartInstance(instanceName: String) = Action { request =>
    val user = request.session.get("username").getOrElse(null)
    val namespace = request.session.get("namespace").getOrElse(null)
    LOG.info("Restarting instance [" + namespace+ "][" + instanceName + "] by user [" + user+ "]")
    ClusterAdmin.restartInstance(instanceName)
    Ok("")
  }

  def getTopology = Action { request =>
    val user = request.session.get("username").getOrElse(null)
    if(user.equals(EnvProperty.get(EnvConstants.TOPOLOGY_EDITOR))) {
      val topologyStr = ZkClient.get().readData[String](ZkConstants.ZK_TOPOLOGY_HOME.toString)
      Ok(topologyStr)
    } else {
      throw new RuntimeException("You don't have permission to edit topology")
    }
  }

  def updateTopology = Action { request =>
    val user = request.session.get("username").getOrElse(null)
    if(user.equals(EnvProperty.get(EnvConstants.TOPOLOGY_EDITOR))) {
      val topologyStr = request.body.asJson.get.toString()
      LOG.info("Refreshing topology...")
      val topology = JsonUtils.fromJson[Topology](topologyStr)
      topology.namespaceList.foreach(namespace => {
        namespace.instances.foreach(instance => {
          instance.configuration = scala.collection.immutable.SortedMap(instance.configuration.toSeq.sortWith(_._1 > _._1):_*)
        })
      })
      ClusterAdmin.updateTopology(topology)
      Ok("")
    } else {
      throw new RuntimeException("You don't have permission to edit topology")
    }
  }

}