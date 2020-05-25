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
import org.sonatype.nexus.security.config.CPrivilege
import org.sonatype.nexus.security.config.CUserRoleMapping
import org.sonatype.nexus.testdb.DataSessionRule

import org.junit.Rule
import org.junit.experimental.categories.Category
import spock.lang.Specification

@Category(SQLTestGroup.class)
class CuserRoleMappingDAOTest
    extends Specification
{
  @Rule
  DataSessionRule sessionRule = new DataSessionRule().access(CUserRoleMappingDAO)

  DataSession session

  CUserRoleMappingDAO dao

  void setup() {
    session = sessionRule.openSession(DataStoreManager.CONFIG_DATASTORE_NAME)
    dao = session.access(CUserRoleMappingDAO)
  }

  void cleanup() {
    session.close()
  }

  def 'create read update delete (case-insensitive)'() {
    given: 'a user role mapping with a case-insensitive source'
      def roleMapping = new CUserRoleMappingData(userId: 'admin', source: 'default', roles: ['role1', 'role2'])
      dao.create(roleMapping)
    when: 'it is read with a different case'
      def read = dao.read('ADMIN', 'default').get()
    then:
      read != null
      read.userId == 'admin'
      read.source == 'default'
      read.roles == ['role1', 'role2'] as Set
    when: 'it is updated with a different case'
      roleMapping.roles = ['role3']
      roleMapping.userId = 'Admin'
      dao.update(roleMapping)
      read = dao.read('admin', 'default').get()
    then:
      read != null
      read.roles == ['role3'] as Set
    when: 'it is deleted with a different case'
      def deleted = dao.delete('ADMIN', 'default')
      read = dao.read('admin', 'default')
    then: 'it will not be present'
      !read.isPresent()
      deleted
  }

  def 'create read update delete (case-sensitive)'() {
    given: 'a user role mapping with a case-sensitive source'
      def roleMapping = new CUserRoleMappingData(userId: 'admin', source: 'other', roles: ['role1', 'role2'])
      dao.create(roleMapping)
    when: 'it is read with a different case'
      def read = dao.read('ADMIN', 'other')
    then: 'it is not found'
      !read.present
    when: 'it is read with the same case'
      read = dao.read('admin', 'other').get()
    then: 'it is found'
      read.userId == 'admin'
      read.source == 'other'
      read.roles == ['role1', 'role2'] as Set
    when: 'it is updated with a different case'
      roleMapping.roles = ['role3']
      roleMapping.userId = 'ADMIN'
      dao.update(roleMapping)
      read = dao.read('admin', 'other').get()
    then: 'nothing changes'
      read.userId == 'admin'
      read.source == 'other'
      read.roles == ['role1', 'role2'] as Set
    when: 'it is updated with the same case'
      roleMapping.userId = 'admin'
      dao.update(roleMapping)
      read = dao.read('admin', 'other').get()
    then: 'it is updated'
      read.roles == ['role3'] as Set
    when: 'it is deleted with different case'
      boolean deleted = dao.delete('Admin', 'other')
      read = dao.read('admin', 'other')
    then: 'it is still there'
      read.isPresent()
      !deleted
    when: 'it is deleted with the same case'
      deleted = dao.delete('admin', 'other')
      read = dao.read('admin', 'other')
    then: 'it is gone'
      !read.isPresent()
      deleted
  }

  def 'browse'() {
    given: 'a few user role mappings are created'
      def roleMapping1 = new CUserRoleMappingData(userId: 'user1', source: 'default', roles: [])
      def roleMapping2 = new CUserRoleMappingData(userId: 'user2', source: 'default', roles: [])
      def roleMapping3 = new CUserRoleMappingData(userId: 'user3', source: 'default', roles: [])
      dao.create(roleMapping1)
      dao.create(roleMapping2)
      dao.create(roleMapping3)
    when: 'they are browsed'
      Iterable<CUserRoleMapping> roleMappings = dao.browse()
    then: 'they are all there'
      roleMappings.size() == 3
  }

  def 'update returns status'() {
    given: 'a role not in the database'
      def roleMapping = new CUserRoleMappingData(userId: 'user1', source: 'default', roles: [])
    when: 'it is updated'
      def status = dao.update(roleMapping)
    then: 'status is false'
      !status
    when: 'it is saved and then updated'
      dao.create(roleMapping)
      roleMapping.roles = ['role1']
      status = dao.update(roleMapping)
    then: 'status is true'
      status
  }
}
