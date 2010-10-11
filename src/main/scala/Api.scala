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
package com.notnoop.earthmail

import java.sql.Timestamp

import org.squeryl.Session
import org.squeryl.PrimitiveTypeMode._

class EarthMailApi {
  val library = Library

  def generatePassword = "NewPassword"

  def createUser(alias: Option[String],
    tags: List[String],
    udid: String,
    deviceTokens: List[String]): User = {

    val user = new User(
      generatePassword,
      alias,
      udid,
      deviceTokens.head)

    library.users.insert(user)
    user
  }

  def modifyUser(userId: Long,
    newAlias: Option[String],
    tags: List[String],
    deviceTokens: List[String]) {

    update(Library.users)(s =>
      where(s.id === userId)

      // TODO: Handle tags and device tokens
      set(
        s.alias := newAlias
        //s.tags := tags,
        //s.deviceTokens := deviceTokens
      ))
  }

  def resetCredentials(userId: Long) = {

    update(Library.users)(s =>
      where(s.id === userId)
      set(s.password := generatePassword))
  }

  def deleteUser(userId: Long) = {
    Library.users.deleteWhere(s =>
      s.id === userId)
  }

  def sendMessage(sender: Long,
    payload: String,
    tags: List[String],
    users: List[Long],
    aliases: List[String],
    title: String,
    message: String,
    extras: Map[String, String]) {

    val recipients = Library.users.where(u =>
      u.id in(users) or
      (u.alias in(aliases))
    )

    val msg = new Message(title, message,
      sender, tags, users, aliases,
      "TODO", extras, Utilities.currentTimeStamp)
    Library.messages.insert(msg)

    for (r <- recipients)
      r.messages.associate(msg)
  }

  def massMessage(sender: Long,
    payload: String,
    title: String,
    message: String,
    extras: Map[String, String]) {

    val msg = new Message(title, message,
      sender, List(), List(), List(),
      "TODO", extras, Utilities.currentTimeStamp)
    Library.messages.insert(msg)

    for (r <- Library.users)
      r.messages.associate(msg)
  }

  def retrieveMessages(
    userId: Long,
    messageSince: Option[Long]) = {

    val user = Library.users.where(_.id === userId).single
    val messages = messageSince match {
      case Some(l) => user.messages.where(_.id.~ > l)
      case _ => user.messages
    }

    messages
  }


  def retrieveMessage(userId: Long, messageId: Long) = {

    val user = Library.users.where(_.id === userId).single
    val message = user.messages.where(_.id === messageId).single

    message
  }

  def deleteMessage(userId: Long, messageId: Long) = {
    val user = Library.users.where(_.id === userId).single
    val message = user.messages.where(_.id === messageId).single

    user.messages.dissociate(message)
  }

  def markAsRead(userId: Long, messageId: Long) = {
    update(Library.messagesToUsers)(a =>
      where(a.user_id === userId and a.message_id === messageId)
      set(a.read := true)
    )
  }

}
