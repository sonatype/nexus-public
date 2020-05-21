/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
package org.sonatype.nexus.internal.security.model

import org.sonatype.nexus.content.testsuite.groups.SQLTestGroup
import org.sonatype.nexus.datastore.api.DataSession
import org.sonatype.nexus.datastore.api.DataStoreManager
import org.sonatype.nexus.security.config.CUser
import org.sonatype.nexus.testdb.DataSessionRule

import org.junit.Rule
import org.junit.experimental.categories.Category
import spock.lang.Specification

@Category(SQLTestGroup.class)
class CUserDAOTest
    extends Specification
{
  @Rule
  DataSessionRule sessionRule = new DataSessionRule().access(CUserDAO)

  DataSession session

  CUserDAO dao

  final String PASSWORD1 = '$shiro1$SHA-512$1024$NYQKemFvZqat9CepP2xO9A==$4m4dBi9f/EtJLpJSW6/7+IVxW3wHR4RNeGtbopiH+D5tlVDFqNKo667eMnqWUxFrRz4Y4IQvn5hv/BnWmEfN0Q=='

  final String PASSWORD2 = '$shiro1$SHA-512$1024$IDetfwWXaulpIe+XL7nOyQ==$ad70UxpgqaXRzaJ41mLnKMy1hzyu3+v7dQ44VHrrNVRpA11S17ZnQX22MZZhjih9DLDEWTe3hJmCfZ8s7/mRHQ=='

  void setup() {
    session = sessionRule.openSession(DataStoreManager.CONFIG_DATASTORE_NAME)
    dao = session.access(CUserDAO)
  }

  void cleanup() {
    session.close()
  }

  def 'create read update delete'() {
    given: 'a user'
      def user = new CUserData(id: 'jdoe', firstName: 'John', lastName: 'Doe', password: PASSWORD1, status: CUser.
          STATUS_ACTIVE,
          email: 'jdoe@example.com')
      dao.create(user)
    when: 'it is read'
      def read = dao.read('jdoe').get()
    then:
      read != null
      read.id == 'jdoe'
      read.firstName == 'John'
      read.lastName == 'Doe'
      read.password == PASSWORD1
      read.status == CUser.STATUS_ACTIVE
    when: 'it is updated'
      user.firstName = 'Jonathan'
      user.lastName = 'DoeDoe'
      user.password = PASSWORD2
      user.status = CUser.STATUS_DISABLED
      dao.update(user)
    and: 'it is read'
      read = dao.read('jdoe').get()
    then:
      read != null
      read.firstName == 'Jonathan'
      read.lastName == 'DoeDoe'
      read.password == PASSWORD2
      read.status == CUser.STATUS_DISABLED
    when: 'it is deleted'
      dao.delete('jdoe')
    and: 'it is read'
      read = dao.read('jdoe')
    then: 'it will not be present'
      !read.isPresent()
  }

  def 'browse'() {
    given: 'a few users are created'
      def user1 = new CUserData(id: 'jdoe', firstName: 'John', lastName: 'Doe', password: PASSWORD1, status: CUser.
          STATUS_ACTIVE,
          email: 'jdoe@example.com')
      def user2 = new CUserData(id: 'msmith', firstName: 'Mark', lastName: 'Smith', password: PASSWORD1,
          status: CUser.STATUS_ACTIVE,
          email: 'msmith@example.com')
      def user3 = new CUserData(id: 'ajones', firstName: 'April', lastName: 'Jones', password: PASSWORD1,
          status: CUser.STATUS_ACTIVE,
          email: 'ajones@example.com')
      dao.create(user1)
      dao.create(user2)
      dao.create(user3)
    when: 'they are browsed'
      Iterable<CUser> users = dao.browse()
    then: 'they are all there'
      users.size() == 3
  }

  def 'update returns status'() {
    given: 'a user not in the database'
      def user = new CUserData(id: 'jdoe', firstName: 'John', lastName: 'Doe', password: PASSWORD1, status: CUser.
          STATUS_ACTIVE, email: 'jdoe@example.com')
    when: 'it is updated'
      def status = dao.update(user)
    then: 'status is false'
      !status
    when: 'it is saved and then updated'
      dao.create(user)
      user.password = PASSWORD2
      status = dao.update(user)
    then: 'status is true'
      status
  }
}
