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

import org.squeryl._
import PrimitiveTypeMode._

import java.sql.Timestamp

class User(
  var password: String,
  var alias: Option[String],
//  var tags: List[String],
  val udid: String,
  var deviceTokens: String
) extends KeyedEntity[Long] {
  val id: Long = 0

  def this() = this("", Some(""), "", "")

  lazy val messages = Library.messagesToUsers.left(this)
}

class Message(
  val title: String,
  val message: String,
  val sender: Long,

  val tags: List[String],
  val users: List[Long],
  val aliases: List[String],

  val push: String,
  val extra: Map[String, String],

  val sentAt: Timestamp
) extends KeyedEntity[Long] {
  val id: Long = 0
}

class UserInbox(
  val user_id: Long,
  val message_id: Long,

  var read: Boolean
) extends KeyedEntity[dsl.CompositeKey2[Long,Long]] {
  def id = compositeKey(user_id, message_id)
}

object Library extends Schema {
  val users = table[User]
  val messages = table[Message]

  val messagesToUsers =
    manyToManyRelation(users, messages).
  via[UserInbox]((u, m, ui) => (u.id === ui.user_id, m.id === ui.message_id))

  override def drop = super.drop
}
