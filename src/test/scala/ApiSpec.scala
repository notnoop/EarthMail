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

import org.scalatest.Spec
import org.scalatest.matchers.ShouldMatchers

class ApiSpec extends Spec with ShouldMatchers with SessionSetting {
  import Library._

  val api = new EarthMailApi
  var user : User = _

  override def beforeEach = {
    super.beforeEach

    user = api.createUser(Some("asdf"), List(), "udid",
      List("DeviceToken"))
  }

  describe("API") {

    it("should create new users") {
      // creation in before each
      users.size should be(1)
      users.single.alias should be(Some("asdf"))
    }

    it("mass mail should reach all users") {
      val user1 = api.createUser(Some("fdsa"), List(), "udid1", List("DeviceToken1"))

      api.massMessage(user.id, "", "Title", "Message", Map())

      val msg0 = user.messages.single
      msg0.title should be("Title")
      msg0.message should be("Message")

      val msg1 = user1.messages.single
      msg0 should equal(msg1)
    }

    it("new messages are unread") {
      api.massMessage(user.id, "", "Title", "Message", Map())

      val msgRel = messagesToUsers.where(_.user_id === user.id).single
      msgRel.read should be(false)
    }

    it("new messages are read once marked as such") {
      api.massMessage(user.id, "", "Title", "Message", Map())

      val msgRel = messagesToUsers.where(_.user_id === user.id).single
      msgRel.read should be(false)

      api.markAsRead(user.id, msgRel.message_id)

      val msgRel1 = messagesToUsers.where(_.user_id === user.id).single
      msgRel1.read should be(true)
    }

  }
}
