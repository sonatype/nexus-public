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
package org.sonatype.nexus.script.plugin.internal

import org.sonatype.nexus.content.testsuite.groups.SQLTestGroup
import org.sonatype.nexus.datastore.api.DataSession
import org.sonatype.nexus.script.Script
import org.sonatype.nexus.testdb.DataSessionRule

import org.junit.Rule
import org.junit.experimental.categories.Category
import spock.lang.Specification

import static org.sonatype.nexus.datastore.api.DataStoreManager.CONFIG_DATASTORE_NAME

@Category(SQLTestGroup.class)
class ScriptDAOTest
    extends Specification
{
  @Rule
  DataSessionRule sessionRule = new DataSessionRule().access(ScriptDAO)

  DataSession session

  ScriptDAO dao

  void setup() {
    session = sessionRule.openSession(CONFIG_DATASTORE_NAME)
    dao = session.access(ScriptDAO)
  }

  void cleanup() {
    session.close()
  }

  def 'create read update delete'() {
    given: 'a Script'
      Script script = new ScriptData(name: 'foo', content: "log.info('hello')")
    when: 'the script is stored'
      dao.create(script)
    and: 'it is read back'
      Script read = dao.read(script.name).orElse(null)
    then: 'the read value matches the original'
      read.name == script.name
      read.type == script.type
      read.content == script.content
    when: 'it is updated'
      script.content = "log.info('world')"
      dao.update(script)
    and: 'it is read back'
      Script update = dao.read(script.name).orElse(null)
    then: 'the read value matches the update'
      update.name == script.name
      update.type == script.type
      update.content == script.content
    when: 'it is deleted'
      dao.delete(script.name)
    then: 'no script is found by that name'
      !dao.read(script.name).isPresent()
  }
}
