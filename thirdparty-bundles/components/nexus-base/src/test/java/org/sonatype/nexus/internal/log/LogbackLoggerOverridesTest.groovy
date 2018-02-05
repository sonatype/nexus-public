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
package org.sonatype.nexus.internal.log

import org.sonatype.goodies.testsupport.TestSupport
import org.sonatype.nexus.common.log.LoggerLevel

import ch.qos.logback.classic.Logger
import groovy.xml.MarkupBuilder
import org.junit.Before
import org.junit.Test

/**
 * Tests for {@link LogbackLoggerOverrides}.
 */
public class LogbackLoggerOverridesTest
    extends TestSupport
{
  private File file

  private LogbackLoggerOverrides underTest

  @Before
  void setUp() {
    file = util.createTempFile('logback-overrides')
    underTest = new LogbackLoggerOverrides(file)
  }

  @Test
  void 'loggers are written in an expected logback xml-format'() {
    underTest.set(Logger.ROOT_LOGGER_NAME, LoggerLevel.WARN)
    underTest.set('foo', LoggerLevel.ERROR)
    underTest.set('bar', LoggerLevel.INFO)
    underTest.save()

    assert file.exists()

    log('XML:\n{}', file.text)

    def doc = new XmlSlurper().parse(file)
    assert doc.property.size() == 1
    assert doc.logger.size() == 2

    // root is stored as property name=root.level
    assert doc.property.breadthFirst().find {it.@name == 'root.level'}.@value == 'WARN'

    assert doc.logger.breadthFirst().find {it.@name == 'foo'}.@level == 'ERROR'
    assert doc.logger.breadthFirst().find {it.@name == 'bar'}.@level == 'INFO'
  }

  @Test
  void 'loggers are read from logback xml-format'() {
    def xml = new MarkupBuilder(file.newWriter())
    xml.included {
      logger(name: 'foo', level: 'ERROR')
      logger(name: 'bar', level: 'INFO')
    }

    log('XML:\n{}', file.text)

    underTest.load()

    assert underTest.get('foo') == LoggerLevel.ERROR
    assert underTest.get('bar') == LoggerLevel.INFO
  }

  @Test
  void 'logger elements are removed when reset'() {
    underTest.set('foo', LoggerLevel.ERROR)
    underTest.set('bar', LoggerLevel.INFO)
    underTest.save()
    assert file.exists()

    underTest.reset()
    assert file.exists()

    log('XML:\n{}', file.text)

    def doc = new XmlSlurper().parse(file)
    assert doc.logger.size() == 0
  }
}
