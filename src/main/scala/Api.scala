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

import com.notnoop.apns.ApnsService

class EarthMailApi(apns: Option[ApnsService]) {
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
      deviceTokens.headOption getOrElse "")

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
        s.alias := newAlias,
        //s.tags := tags,
        s.deviceTokens := deviceTokens.headOption getOrElse ""
      ))
  }

  def resetCredentials(userId: Long) = {

    update(Library.users)(s =>
      where(s.id === userId)
      set(s.password := generatePassword))
    Library.users.lookup(userId)
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

    for (r <- recipients) {
      r.messages.associate(msg)
      r.badge += 1
      Library.users.update(r)
    }
  }

  def massMessage(sender: Long,
    payload: String,
    title: String,
    message: String,
    extras: Map[String, String]) {

    transaction {
    val msg = new Message(title, message,
      sender, List(), List(), List(),
      "TODO", extras, Utilities.currentTimeStamp)
    Library.messages.insert(msg)

    for (r <- Library.users)
      r.messages.associate(msg)

    update(Library.users)(a=> set(a.badge := a.badge.~ + 1))
    }

  }

  def retrieveMessages(
    userId: Long,
    messageSince: Option[Long]) = {

    val associations = Library.users.lookup(userId).map(u =>
      messageSince match {
      case Some(l) => u.messages.associationMap.where(_._1.id.~ > l)
      case _ => u.messages.associationMap
    })
    associations
  }


  def retrieveMessage(userId: Long, messageId: Long) = {
    Library.users.lookup(userId).map(u =>
      u.messages.associationMap.where(s => s._1.id === messageId).headOption
    ) getOrElse None
  }

  def deleteMessage(userId: Long, messageId: Long) : Boolean = {
    Library.users.lookup(userId).map(u => {
      val message = u.messages.where(s => s.id === messageId).headOption
      message match {
        case Some(m) => u.messages.dissociate(m)
        case _ => false
      }}) getOrElse false
  }

  def markAsRead(userId: Long, messageId: Long) = {
    transaction {
      update(Library.messagesToUsers)(a =>
        where(a.user_id === userId and a.message_id === messageId)
        set(a.read := true)
      )
      update(Library.users)(a =>
        where(a.id === userId)
        set(a.badge := a.badge.~ - 1)
      )
    }

  }

  def badgeOf(userId: Long) = {
    Library.users.lookup(userId).map(_.badge).getOrElse(0)
  }
}
