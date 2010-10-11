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

class ModelsSpec extends Spec with ShouldMatchers with SessionSetting {

  describe("User") {
    import Library.users

    it("(when added to DB) have a valid id") {
      val user = new User("1234", Some("123"), "udid", "asdf")
      users.insert(user)
      user.id should not be(0)
    }

    it("is searchable by alias") {
      users.insert(List(
        new User("123", Some("asdf"), "", ""),
        new User("321", Some("fdsa"), "", ""),
        new User("111", None, "", "")
      ))

      users.where(_.alias === Some("asdf")).single.password should be("123")
      users.where(_.alias === Some("fdsa")).single.password should be("321")
    }
  }

  describe("User Inbox") {
    import Library._

    it("contains messages of users") {
      val user = new User("123", Some("asdf"), "", "")
      users.insert(user)

      user.id should not be(0)

      val message = new Message("Test", "Message", user.id, List(), List(),
      List(), "", Map(),
      new java.sql.Timestamp(java.util.Calendar.getInstance().getTime().getTime()))
      messages.insert(message)

      user.messages.associate(message)

      user.messages.single should be(message)
    }
  }
}

