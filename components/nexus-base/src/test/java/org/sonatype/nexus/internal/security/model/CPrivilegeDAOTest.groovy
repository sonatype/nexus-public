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
import org.sonatype.nexus.testdb.DataSessionRule

import org.junit.Rule
import org.junit.experimental.categories.Category
import spock.lang.Specification

@Category(SQLTestGroup.class)
class CPrivilegeDAOTest
    extends Specification
{
  @Rule
  DataSessionRule sessionRule = new DataSessionRule().access(CPrivilegeDAO)

  DataSession session

  CPrivilegeDAO dao

  void setup() {
    session = sessionRule.openSession(DataStoreManager.CONFIG_DATASTORE_NAME)
    dao = session.access(CPrivilegeDAO)
  }

  void cleanup() {
    session.close()
  }

  def 'create read update delete'() {
    given: 'a privilege'
      def privilege = new CPrivilegeData(id: 'notes-read', name: 'app-notes-read', description: 'Notes Read',
          type: 'application', properties: ['domain': 'notes', 'actions': 'view'], readOnly: true)
      dao.create(privilege)
    when: 'it is read'
      def read = dao.read('notes-read').get()
    then:
      read != null
      read.id == 'notes-read'
      read.name == 'app-notes-read'
      read.description == 'Notes Read'
      read.type == 'application'
      read.properties == ['domain': 'notes', 'actions': 'view']
      read.readOnly
    when: 'it is updated'
      privilege.name = 'app-notes-read2'
      privilege.description = 'Notes Read2'
      privilege.type = 'application2'
      privilege.properties = ['domain': 'apps', 'actions': 'read,view']
      privilege.readOnly = false
      dao.update(privilege)
    and: 'it is read'
      read = dao.read('notes-read').get()
    then:
      read != null
      read.name == 'app-notes-read2'
      read.description == 'Notes Read2'
      read.type == 'application2'
      read.properties == ['domain': 'apps', 'actions': 'read,view']
      !read.readOnly
    when: 'it is deleted'
      dao.delete('notes-read')
    and: 'it is read'
      read = dao.read('notes-read')
    then: 'it will not be present'
      !read.isPresent()
  }

  def 'browse'() {
    given: 'a few privileges'
      def privilege1 = new CPrivilegeData(id: 'privilege1', name: 'Privilege1', description: 'Privilege 1',
          type: 'Application', properties: [:], readOnly: false)
      def privilege2 = new CPrivilegeData(id: 'privilege2', name: 'Privilege2', description: 'Privilege 2', readOnly: false,
          type: 'Application', properties: [:])
      def privilege3 = new CPrivilegeData(id: 'privilege3', name: 'Privilege3', description: 'Privilege 3', readOnly: false,
          type: 'Application', properties: [:])
    when: 'they are created'
      dao.create(privilege1)
      dao.create(privilege2)
      dao.create(privilege3)
    and: 'they are browsed'
      Iterable<CPrivilege> privileges = dao.browse()
    then: 'they are all there'
      privileges.size() == 3
  }

  def 'update returns status'() {
    given: 'a role not in the database'
      def privilege = new CPrivilegeData(id: 'privilege1', name: 'Privilege1', description: 'Privilege 1',
          type: 'Application', properties: [:], readOnly: false)
    when: 'it is updated'
      def status = dao.update(privilege)
    then: 'status is false'
      !status
    when: 'it is saved and then updated'
      dao.create(privilege)
      privilege.description = 'Privilege 2'
      status = dao.update(privilege)
    then: 'status is true'
      status
  }
}
