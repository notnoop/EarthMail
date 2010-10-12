/*
 * Copyright 2010, Mahmood Ali.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *   * Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *   * Redistributions in binary form must reproduce the above
 *     copyright notice, this list of conditions and the following disclaimer
 *     in the documentation and/or other materials provided with the
 *     distribution.
 *   * Neither the name of Mahmood Ali. nor the names of its
 *     contributors may be used to endorse or promote products derived from
 *     this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.notnoop.earthmail.http

import com.notnoop.earthmail.EarthMailApi


import java.sql.Timestamp

import se.scalablesolutions.akka.actor.Actor
import se.scalablesolutions.akka.actor.Actor._
import se.scalablesolutions.akka.serialization.Serializer
import scala.xml.NodeSeq

import javax.ws.rs._
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

import se.scalablesolutions.akka.actor.ActorRegistry.actorFor

import com.notnoop.apns._

import Commands._
import ActorHelper._

object DateUtilities {
  val pattern = "yyyy-MM-dd HH:mm:ss"
  def renderTime(s: java.util.Date) = {
    val format = new java.text.SimpleDateFormat(pattern)
    format.setTimeZone(java.util.TimeZone.getTimeZone("GMT"));
    format.format(s)
  }
}

@Path("/api/")
@Produces(Array(MediaType.APPLICATION_JSON))
class SimpleService {

  @POST
  @Path("/user/")
  @Consumes(Array(MediaType.APPLICATION_JSON))
  def createUser(body: Array[Byte]) = {
    handle(CreateUser(body))
  }

  @POST
  @Path("/airmail/send/")
  @Consumes(Array(MediaType.APPLICATION_JSON))
  def sendMessage(body: Array[Byte]) = {
    "Sending Message: " + body.length
  }

  @POST
  @Path("/airmail/send/broadcast/")
  @Consumes(Array(MediaType.APPLICATION_JSON))
  def broadcastMessage(body: Array[Byte]) = {
    handle(BroadcastMessage(body))
  }

  @PUT
  @Path("/user/alias/{alias}/")
  @Consumes(Array(MediaType.APPLICATION_JSON))
  def modifyUserByAlias(@PathParam("alias") alias: String, body: Array[Byte]) =
  {
    handle(ModifyUserByAlias(alias, body))
  }

}

@Path("/api/user/{user_id: [0-9]+}")
@Produces(Array(MediaType.APPLICATION_JSON))
class User {
  @PathParam("user_id") var userId: Long = _

  @PUT
  @Path("/")
  def modifyUserById(body: Array[Byte]) = {
    handle(ModifyUserById(userId, body))
  }

  @POST
  @Path("/creds/reset/")
  def resetCredentials() = {
    handle(ResetCredentials(userId))
  }

  @DELETE
  @Path("/")
  def deleteUser() = {
    handle(DeleteUser(userId))
  }

  @GET
  @Path("/messages/")
  def retrieveMessages(@QueryParam("since") sinceMessage: Long) = {
    handle(GetMessages(userId, Some(sinceMessage)))
  }

  @Path("/messages/message/{message_id: [0-9]+}/")
  def retrieveMessage(@PathParam("message_id") messageId: Long) =
    new UserMessage(userId, messageId)

  @POST
  @Path("/messages/unread/")
  @Consumes(Array(MediaType.APPLICATION_JSON))
  def markMessagesReadBulk(body: Array[Byte]) = {
    handle(BulkMarkMessagesAsRead(userId, body))
  }

  @POST
  @Path("/messages/delete/")
  @Consumes(Array(MediaType.APPLICATION_JSON))
  def deleteMessagesBulk(body: Array[Byte]) = {
    handle(BulkDeleteMessages(userId, body))
  }
}

object ActorHelper {
  def sendReply( command: Command): Option[Any] =
    for{a <- actorFor[SimpleServiceActor]
        r <- (a !! command)} yield r

  def handle(cmd: Command) = sendReply(cmd) match {
    case None | Some(None) =>
        Response.notAcceptable(null).build
    case Some(data) =>
        Response.ok(data).build
  }
}

@Produces(Array(MediaType.APPLICATION_JSON))
class UserMessage(val userId: Long, val messageId: Long) {

  @GET
  @Path("/")
  def retrieveMessage() = {
    handle(GetMessage(userId, messageId))
  }

  @POST
  @Path("/read/")
  def markAsRead() = {
    handle(MarkMessageAsRead(userId, messageId))
  }

  @GET
  @Path("/body/")
  @Produces(Array(MediaType.TEXT_HTML))
  def retrieveMessageBody() = {
    <p>Body of Message { messageId } for { userId }</p>
  }

  @DELETE
  @Path("/")
  def deleteMessage() = {
    handle(DeleteMessage(userId, messageId))
  }

}

import org.squeryl._
import PrimitiveTypeMode._

import com.notnoop.earthmail.Library

object MySession {
  Class.forName("com.mysql.jdbc.Driver")
  SessionFactory.concreteFactory = Some(() => Session.create(
    java.sql.DriverManager.getConnection("jdbc:mysql:///mytest","root", ""),
    new adapters.MySQLAdapter()
  ))
  using(SessionFactory.concreteFactory.get()) {
//    Library.drop
//    Library.create
  }
  val session = SessionFactory.concreteFactory.get()
}

class SimpleServiceActor extends Actor {
  import net.liftweb.json.JsonAST
  import net.liftweb.json.JsonAST._
  import net.liftweb.json.JsonDSL._
  import net.liftweb.json.JsonParser._

  implicit val formats = net.liftweb.json.DefaultFormats

  override def shutdown = {
    super.shutdown
    MySession.session.close
  }
  val apnsService = APNS.newService()
    .withCert("/tmp/SMS.p12", "123456")
    .withSandboxDestination()
    .build()
  val api = new EarthMailApi(Some(apnsService))

  def parseAliasAndTags(js: JValue): (Option[String], List[String], List[String]) = {
    val alias = (js \ "alias") match {
        case JNothing => None
        case f => Some(f.extract[String])
    }

    val tags = (js \ "tags") match {
      case JNothing => List[String]()
      case f => f.extract[List[String]]
    }

    val deviceTokens = (js \ "device_tokens") match {
      case JNothing => List[String]()
      case f => f.extract[List[String]]
    }

    (alias, tags, deviceTokens)
  }

  import com.notnoop.earthmail.Message

  def messageToJson(e: Message, userId: Long, read: Boolean) = {
      val baseMsgUrl = "http://localhost:8080/api/user/" + userId.toString + "/messages/message/"
        (("message_id" -> e.id.toString) ~
         ("message_url" -> (baseMsgUrl + e.id.toString + "/")) ~
         ("message_body_url" -> (baseMsgUrl + e.id.toString + "/body/")) ~
         ("message_read_url" -> (baseMsgUrl + e.id.toString + "/read/")) ~
         ("unread" -> !read) ~
         ("message_sent" -> DateUtilities.renderTime(e.sentAt)) ~ // TODO
         ("title" -> e.title) ~
         ("extra" -> JNull) // TODO
       )
   }

  def extractMessageId(userId: Long, url: String) = {
    val last = if (url.endsWith("/")) (url.length - 1) else url.length
    val first = url.lastIndexOf('/', last - 1) + 1
    url.substring(first, last).toLong
  }

  import MySession.session

  def receive = {
  case CreateUser(body: Array[Byte]) =>
    val js = parse(new String(body))

    val udid = (js \ "udid") match {
      case JNothing => ""
      case f => f.extract[String]
    }

    val (alias, tags, deviceTokens) = parseAliasAndTags(js)
    using (session) {
    val user = api.createUser(alias, tags, udid, deviceTokens)

    self.reply(compact(render(
        (("user_url" -> ("http://localhost:8080/api/user/" + user.id.toString)) ~
        ("user_id" -> user.id.toString) ~
        ("password" -> user.password))
      )))
  }


  case ModifyUserById(id: Long, body: Array[Byte]) =>
    val js = parse(new String(body))

    val (alias, tags, deviceTokens) = parseAliasAndTags(js)
    using(session) {
      api.modifyUser(id, alias, tags, deviceTokens)
    }
    self.reply("""{"ok": true}""")
  case ModifyUserByAlias(alias: String, body: Array[Byte]) =>
    // TODO
    self.reply(None)
  case ResetCredentials(id: Long) =>
    using(session) {
      api.resetCredentials(id) match {
        case Some(user) =>
        self.reply(compact(render(
          ("user_url" -> ("http://localhost:8080/api/user/" + user.id.toString)) ~
          ("user_id" -> user.id.toString) ~
          ("password" -> user.password)
        )))
      case None => self.reply()
    }
    }

  case DeleteUser(id: Long) =>
    using(session) {
      api.deleteUser(id)
    }
    self.reply("""{"ok": true}""")

  case SendMessage(body) =>
    // TODO
    self.reply("Not handler yet")

  case BroadcastMessage(body) =>
    val js = parse(new String(body))

    val push = (js \ "push") match {
      case JNothing => ""
      case f => compact(render(f))
    }

    val title = (js \ "title") match {
      case JNothing => "Untitled"
      case f => f.extract[String]
    }

    val message = (js \ "message") match {
      case JNothing => "(No body)"
      case f => f.extract[String]
    }

    using(session) {
      api.massMessage(0,
        push, title, message, Map())
    }

    self.reply("""{"ok":true}""")



  case GetMessages(id: Long, since: Option[Long]) =>
    using(session) {
      val messages = api.retrieveMessages(id, since) get

      val items = messages.map(m => messageToJson(m._1, id, m._2.read)).toList

      self.reply(compact(render(
        ("badge" -> api.badgeOf(id)) ~
        ("messages" -> JArray(items))
      )))
    }

  case GetMessage(userId: Long, messageId: Long) =>
    using (session) {
      val messageAs = api.retrieveMessage(userId, messageId)
      messageAs match {
      case Some((message, association)) =>
        val item = messageToJson(message, userId, association.read)
        self.reply(compact(render(item)))
      case None =>
        self.reply(None)
      }
    }
  case MarkMessageAsRead(userId, messageId) =>
    using (session) {
      api.markAsRead(userId, messageId)
      self.reply("""{"ok":true}""")
    }

  case BulkMarkMessagesAsRead(userId, body) =>

    val js = parse(new String(body))
    val messages = ((js \ "mark_as_read") match {
      case JNothing => println("***" + js); List[String]()
      case JField(_, JArray(f)) => f.values.map(_.toString)
      case e => println("### " + e); List[String]()
    }).map(extractMessageId(userId, _))
    using (session) {
      api.bulkMarkAsRead(userId, messages)
    }
    self.reply("""{"ok":true}""")

  case DeleteMessage(userId, messageId) =>
    using (session) {
      if (api.deleteMessage(userId, messageId))
        self.reply("""{"ok:true}""")
      else
        self.reply(None)
    }
  case BulkDeleteMessages(userId, body) =>
    val js = parse(new String(body))
    val messages = ((js \ "delete") match {
      case JNothing => List[String]()
      case JField(_, JArray(f)) => f.values.map(_.toString)
      case _ => List[String]()
    }).map(extractMessageId(userId, _))
    using (session) {
      api.bulkDeleteMessages(userId, messages)
    }
    self.reply("""{"ok":true}""")
  }
}

object Commands {
  trait Command

  case class CreateUser(body: Array[Byte]) extends Command
  case class ModifyUserById(id: Long, body: Array[Byte]) extends Command
  case class ModifyUserByAlias(alias: String, body: Array[Byte]) extends Command
  case class ResetCredentials(id: Long) extends Command
  case class DeleteUser(id: Long) extends Command
  case class SendMessage(body: Array[Byte]) extends Command
  case class BroadcastMessage(body: Array[Byte]) extends Command
  case class GetMessages(id: Long, since: Option[Long]) extends Command
  case class GetMessage(userId: Long, messageId: Long) extends Command
  case class GetMessageBody(userId: Long, messageId: Long) extends Command
  case class MarkMessageAsRead(userId: Long, messageId: Long) extends Command
  case class DeleteMessage(userId: Long, messageId: Long) extends Command
  case class BulkMarkMessagesAsRead(userId: Long, body: Array[Byte]) extends Command
  case class BulkDeleteMessages(userId: Long, body: Array[Byte]) extends Command
}

