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
package org.sonatype.nexus.script.plugin.internal.orient

import org.sonatype.goodies.testsupport.TestSupport
import org.sonatype.nexus.orient.testsupport.DatabaseInstanceRule
import org.sonatype.nexus.script.Script
import org.sonatype.nexus.script.plugin.internal.ScriptStore

import org.junit.Before
import org.junit.Rule
import org.junit.Test

import static org.hamcrest.CoreMatchers.is
import static org.hamcrest.CoreMatchers.notNullValue
import static org.hamcrest.CoreMatchers.nullValue
import static org.junit.Assert.assertThat

/**
 * Tests for {@link ScriptStoreImpl}.
 * 
 * @since 3.0
 */
public class ScriptStoreImplTest
    extends TestSupport
{
  @Rule
  public DatabaseInstanceRule database = DatabaseInstanceRule.inMemory('test')

  private ScriptStore underTest

  @Before
  void setUp() throws Exception {
    underTest = new ScriptStoreImpl(database.getInstanceProvider(), new ScriptEntityAdapter())
    underTest.start()
  }

  @Test
  void 'Can create a new Script'() throws Exception {
    createScript()
  }

  @Test
  void 'Can list persisted Scripts'() throws Exception {
    createScript()
    def list = underTest.list()
    assertThat(list.size(), is(1))
    list[0].with {
      assertThat(name, is('test'))
      assertThat(content, is('println "hello"'))
      assertThat(type, is('groovy'))
    }
  }

  @Test
  void 'Can find a script by name'() throws Exception {
    Script script = createScript()
    Script loaded = underTest.get(script.name)
    assertThat(loaded, notNullValue())
    loaded.with {
      assertThat(name, is(script.name))
      assertThat(content, is(script.content))
      assertThat(type, is('groovy'))
    }
  }
  
  @Test
  void 'Finding a script by name returns null if not found'() throws Exception {
    assertThat(underTest.get('foo'), nullValue())
  }

  @Test
  void "Can update an existing Script"() throws Exception {
    Script script = createScript()
    script.content = 'println "foo"'
    underTest.update(script)
    def list = underTest.list()
    assertThat(list.size(), is(1))
    list[0].with {
      assertThat(name, is('test'))
      assertThat(content, is('println "foo"'))
      assertThat(type, is('groovy'))
    }
  }

  @Test
  void "Can delete an existing script"() throws Exception {
    Script script = createScript()
    assertThat(underTest.list().size(), is(1))
    underTest.delete(script)
    assertThat(underTest.list().size(), is(0))
  }
  
  @Test(expected = IllegalStateException)
  void "Cannot delete a Script that is not persisted"() {
    underTest.delete(new Script('not-persisted', 'content'))
  }

  @Test(expected = IllegalStateException)
  void "Cannot update a Script that is not persisted"() {
    underTest.update(new Script('not-persisted', 'content'))
  }

  private Script createScript() {
    Script script = new Script('test', 'println "hello"')
    underTest.create(script)
    return script
  }
}
