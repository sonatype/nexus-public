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
import org.sonatype.nexus.security.config.CRole
import org.sonatype.nexus.testdb.DataSessionRule

import org.junit.Rule
import org.junit.experimental.categories.Category
import spock.lang.Specification

@Category(SQLTestGroup.class)
class CRoleDAOTest
    extends Specification
{
  @Rule
  DataSessionRule sessionRule = new DataSessionRule().access(CRoleDAO)

  DataSession session

  CRoleDAO dao

  void setup() {
    session = sessionRule.openSession(DataStoreManager.CONFIG_DATASTORE_NAME)
    dao = session.access(CRoleDAO)
  }

  void cleanup() {
    session.close()
  }

  def 'create read update delete'() {
    given: 'a role'
      def role = new CRoleData(id: 'admin', name: 'administrator', description: 'Admin User',
          privileges: ['priv1', 'priv2'], roles: ['role1', 'role2'], readOnly: true)
      dao.create(role)
    when: 'it is read'
      def read = dao.read('admin').get()
    then:
      read != null
      read.id == 'admin'
      read.name == 'administrator'
      read.description == 'Admin User'
      read.privileges == ['priv1', 'priv2'] as Set
      read.roles == ['role1', 'role2'] as Set
      read.readOnly
    when: 'it is updated'
      role.name = 'Administrator'
      role.description = 'Admin 2'
      role.privileges = ['priv2', 'priv3']
      role.roles = ['role3']
      role.readOnly = false
      dao.update(role)
    and: 'it is read'
      read = dao.read('admin').get()
    then:
      read != null
      read.name == 'Administrator'
      read.description == 'Admin 2'
      read.privileges == ['priv2', 'priv3'] as Set
      read.roles == ['role3'] as Set
      !read.readOnly
    when: 'it is deleted'
      dao.delete('admin')
    and: 'it is read'
      read = dao.read('admin')
    then: 'it will not be present'
      !read.isPresent()
  }

  def 'browse'() {
    given: 'a few roles'
      def role1 = new CRoleData(id: 'role1', name: 'Role1', description: 'Role 1', privileges: [], roles: [],
          readOnly: false)
      def role2 = new CRoleData(id: 'role2', name: 'Role2', description: 'Role 2', privileges: [], roles: [],
          readOnly: false)
      def role3 = new CRoleData(id: 'role3', name: 'Role3', description: 'Role 3', privileges: [], roles: [],
          readOnly: false)
    when: 'they are created'
      dao.create(role1)
      dao.create(role2)
      dao.create(role3)
    and: 'they are browsed'
      Iterable<CRole> roles = dao.browse()
    then: 'they are all there'
      roles.size() == 3
  }

  def 'update returns status'() {
    given: 'a role not in the database'
      def role = new CRoleData(id: 'role1', name: 'Role1', description: 'Role 1', privileges: [], roles: [],
          readOnly: false)
    when: 'it is updated'
      def status = dao.update(role)
    then: 'status is false'
      !status
    when: 'it is saved and then updated'
      dao.create(role)
      role.description = 'Role 2'
      status = dao.update(role)
    then: 'status is true'
      status
  }
}
