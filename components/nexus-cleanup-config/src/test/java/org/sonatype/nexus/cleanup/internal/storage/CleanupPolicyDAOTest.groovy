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
package org.sonatype.nexus.cleanup.internal.storage

import org.sonatype.nexus.cleanup.storage.CleanupPolicy
import org.sonatype.nexus.content.testsuite.groups.SQLTestGroup
import org.sonatype.nexus.datastore.api.DataSession
import org.sonatype.nexus.testdb.DataSessionRule

import org.junit.Rule
import org.junit.experimental.categories.Category
import spock.lang.Specification

import static org.sonatype.nexus.datastore.api.DataStoreManager.DEFAULT_DATASTORE_NAME

@Category(SQLTestGroup.class)
class CleanupPolicyDAOTest
    extends Specification
{
  @Rule
  DataSessionRule sessionRule = new DataSessionRule().access(CleanupPolicyDAO)

  DataSession session

  CleanupPolicyDAO dao

  void setup() {
    session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)
    dao = session.access(CleanupPolicyDAO)
  }

  void cleanup() {
    session.close()
  }

  def 'create read update delete'() {
    given: 'a cleanup policy'
      CleanupPolicy policy = new CleanupPolicyData(
          name: 'foo',
          notes: 'some text',
          format: 'maven2',
          mode: 'deletion',
          criteria: [bar: 'one', baz: 'two']
      )
    when: 'it is created'
      dao.create(policy)
    and: 'it is read'
      CleanupPolicy read = dao.read(policy.name).orElse(null)
    then: 'it matches the original policy'
      read.name == policy.name
      read.notes == policy.notes
      read.format == policy.format
      read.mode == policy.mode
      read.criteria == [bar: 'one', baz: 'two']
    when: 'the policy is updated'
      policy.notes = 'some other text'
      policy.format = 'npm'
      policy.mode = 'other'
      policy.criteria = [one: 'baz', two: 'bar']
      dao.update(policy)
    and: 'it is read'
      CleanupPolicy update = dao.read(policy.name).orElse(null)
    then: 'it matches the updated policy'
      update.name == policy.name
      update.notes == policy.notes
      update.format == policy.format
      update.mode == policy.mode
      update.criteria == [one: 'baz', two: 'bar']
    when: 'the policy is delete'
      dao.delete(policy.name)
    then: 'it cannot be read anymore'
      !dao.read(policy.name).isPresent()
    when: 'several policies are created'
      (1..5).each {
        dao.create(new CleanupPolicyData(
            name: "foo${it}",
            notes: "some text ${it}",
            format: "maven${it}",
            mode: "mode${it}",
            criteria: [bar: "${it}" as String]
        ))
      }
    then: 'browsing finds them all'
      dao.count() == 5
      List<CleanupPolicy> items = dao.browse()
      items.size() == 5
    when: 'getting by format'
      List<CleanupPolicy> policies = dao.browseByFormat('maven5')
    then: 'it finds the correct policies'
      policies.size() == 1
      CleanupPolicy found = policies.first()
      found.name == 'foo5'
      found.notes == 'some text 5'
      found.format == 'maven5'
      found.mode == 'mode5'
      found.criteria == [bar: '5']
  }
}
