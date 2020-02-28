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
package org.sonatype.nexus.internal.script

import javax.script.ScriptEngine
import javax.script.ScriptEngineManager

import org.sonatype.nexus.common.app.GlobalComponentLookupHelper
import org.sonatype.nexus.common.script.ScriptCleanupHandler
import org.sonatype.nexus.common.script.ScriptService

import org.eclipse.sisu.inject.BeanLocator
import spock.lang.Specification

class ScriptServiceImplTest
    extends Specification
{
  ScriptEngineManager engineManager

  def setup() {
    engineManager = Mock(ScriptEngineManager)
  }

  def 'groovy script engine is returned correctly'() {
    given: 'a script service that will only return a groovy engine'
      def scriptService = createScriptService(true)
      engineManager.getEngineByName('groovy') >> Mock(ScriptEngine)
    when: 'a groovy engine is requested'
      def engine = scriptService.engineForLanguage('groovy')
    then: 'an engine is returned'
      engine != null
  }

  def 'exception is thrown when javascript engine is requested'() {
    given: 'a script service that will only return a groovy engine'
      def scriptService = createScriptService(true)
    when: 'a javascript engine is requested'
      scriptService.engineForLanguage('javascript')
    then: 'an IllegalScriptLanguageException exception is thrown'
      thrown(IllegalScriptLanguageException)
  }

  def 'javascript engine is returned when groovyOnly is set to false'() {
    given: 'a script service that is not limited to groovy engine'
      def scriptService = createScriptService(false)
      engineManager.getEngineByName('javascript') >> Mock(ScriptEngine)
    when: 'a javascript engine is requested'
      def engine = scriptService.engineForLanguage('javascript')
    then: 'an engine is returned'
      engine != null
  }

  private ScriptService createScriptService(boolean groovyOnly) {
    return new ScriptServiceImpl(
        engineManager,
        Mock(BeanLocator),
        Mock(GlobalComponentLookupHelper),
        Mock(List),
        Mock(ScriptCleanupHandler),
        groovyOnly)
  }
}
