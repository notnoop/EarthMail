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
import se.scalablesolutions.akka.actor.ActorRegistry.actorFor

import com.notnoop.apns._

@Path("/api/")
@Produces(Array(MediaType.APPLICATION_JSON))
class SimpleService {
  @POST
  @Path("/user/")
  def createUser() = {
    val a = Map("asdf" -> "asdf")
    Serializer.ScalaJSON.toBinary(a)
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
    "Broadcast Message: " + body.length
  }

  @PUT
  @Path("/user/alias/{alias}/")
  def modifyUserByAlias(@PathParam("alias") alias: Long, body: Array[Byte]) =
  {
    "Modifying by Alias: " + alias
  }

}

@Path("/api/user/{user_id: [0-9]+}")
@Produces(Array(MediaType.APPLICATION_JSON))
class User {

  @PathParam("user_id") var userId: Long = _

  @PUT
  @Path("/")
  def modifyUserById(body: Array[Byte]) = {
    "Modifying " + userId
  }

  @POST
  @Path("/creds/reset/")
  def resetCredentials() = {
    "Reseting " + userId
  }

  @DELETE
  @Path("/")
  def deleteUser() = {
    "Deleting " + userId
  }

  @GET
  @Path("/messages/")
  def retrieveMessages(@QueryParam("since") sinceMessage: Int) = {
    "Retrieve Messages: " + userId + " since " + sinceMessage
  }

  @Path("/messages/message/{message_id: [0-9]+}/")
  def retrieveMessage(@PathParam("message_id") messageId: Long) =
    new UserMessage(userId, messageId)
}

@Produces(Array(MediaType.APPLICATION_JSON))
class UserMessage(val userId: Long, val messageId: Long) {

  @GET
  @Path("/")
  def retrieveMessage() = {
    "Retrieve Message: " + messageId + " for " + userId
  }

  @POST
  @Path("/read/")
  def markAsRead() = {
    "Marking as read: Message: " + messageId + " for " + userId
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
    "Deleting Message: " + messageId + " for " + userId
  }

}

class SimpleServiceActor extends Actor {
  val apnsService = APNS.newService()
    .withCert("/tmp/SMS.p12", "123456")
    .withSandboxDestination()
    .build()
  val api = new EarthMailApi(Some(apnsService))

  def receive = {
    case _ =>
  }
}

object Commands {
  trait Command

  case class CreateUser(body: Array[Byte]) extends Command
  case class ModifyUserById(id: Long, body: Array[Byte]) extends Command
  case class ModifyUserByAlias(alias: String, body: Array[Byte]) extends Command
  case class ResetCredentials(id: Long, body: Array[Byte]) extends Command
  case class DeleteUser(id: Long) extends Command
  case class SendMessage(body: Array[Byte]) extends Command
  case class BroadcastMessage(body: Array[Byte]) extends Command
  case class GetMessages(id: Long, since: Option[Long]) extends Command
  case class GetMessage(userId: Long, messageId: Long) extends Command
  case class GetMessageBody(userId: Long, messageId: Long) extends Command
  case class MarkMessageAsRead(userId: Long, messageId: Long) extends Command
  case class DeleteMessage(userId: Long, messageId: Long) extends Command
}

