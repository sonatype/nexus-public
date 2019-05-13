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
package org.sonatype.nexus.internal.script.groovy

import javax.script.ScriptException

import org.sonatype.nexus.common.app.ApplicationDirectories
import org.sonatype.nexus.common.script.ScriptCleanupHandler
import org.sonatype.nexus.internal.script.ScriptTask
import org.sonatype.nexus.scheduling.TaskSupport

import spock.lang.Specification

import static org.sonatype.nexus.internal.script.ScriptServiceImpl.SCRIPT_CLEANUP_HANDLER

class GroovyScriptEngineFactoryTest
    extends Specification
{

  def 'it will call the cleanup helper after executing the script'() {
    given:
      def factory = new GroovyScriptEngineFactory(getClass().getClassLoader(), Mock(ApplicationDirectories))
      def engine = factory.getScriptEngine()
      def binding = new Binding()
      def scriptCleanupHandler = Mock(ScriptCleanupHandler) {
        1 * cleanup(_)
      }
      binding.setVariable(SCRIPT_CLEANUP_HANDLER, scriptCleanupHandler)
    when:
      def result = engine.eval('return 1', binding)
    then:
      result == 1
  }

  def 'it will call the cleanup helper after executing the script even when an exception is thrown'() {
    given:
      def factory = new GroovyScriptEngineFactory(getClass().getClassLoader(), Mock(ApplicationDirectories))
      def engine = factory.getScriptEngine()
      def binding = new Binding()
      def scriptCleanupHandler = Mock(ScriptCleanupHandler) {
        1 * cleanup(_)
      }
      binding.setVariable(SCRIPT_CLEANUP_HANDLER, scriptCleanupHandler)
    when:
      engine.eval('throw new RuntimeException("bad")', binding)
    then:
      thrown(ScriptException)
  }

  def 'It will create a named context for the executor of the groovy script engine'() {
    given:
      def binding = new Binding()
      binding.setVariable('task', task)
      binding.setVariable('scriptName', scriptName)
    expect:
      GroovyScriptEngineFactory.getContext(binding) == expectedContext

    where:
      task                                           | scriptName   | expectedContext
      null                                           | 'testScript' | 'Script \'testScript\''
      Mock(ScriptTask) { getName() >> 'myTask' }     | null         | 'Task \'myTask\''
      null                                           | null         | 'An unknown script'
      Mock(TaskSupport) { getName() >> 'notScript' } | null         | 'An unknown script'
  }
}
