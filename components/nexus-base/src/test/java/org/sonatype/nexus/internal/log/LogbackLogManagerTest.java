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
package org.sonatype.nexus.internal.log;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.common.log.LogConfigurationCustomizer;
import org.sonatype.nexus.common.log.LoggerLevel;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.FileAppender;
import org.eclipse.sisu.inject.BeanLocator;
import org.junit.Before;
import org.junit.Test;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.internal.log.LogbackLogManager.getLogFor;

/**
 * Tests for {@link LogbackLogManager}.
 */
public class LogbackLogManagerTest
    extends TestSupport
{
  private String fooLoggerName;

  private String barLoggerName;

  @Before
  public void setUp() {
    // generate some unique logger names to avoid having to mock up logback which is used by tests
    fooLoggerName = "foo" + System.currentTimeMillis();
    barLoggerName = "bar" + System.currentTimeMillis();
  }

  /**
   * Ensure if a customizer configures a level a logger is created with that level, and if a customizer configures a
   * default that it does not create a logger.
   */
  @Test
  public void testRegisterCustomization_createsLoggerAndIgnoresDefault() throws Exception {
    LogConfigurationCustomizer customizer = configuration -> {
      configuration.setLoggerLevel(fooLoggerName, LoggerLevel.DEBUG);
      configuration.setLoggerLevel(barLoggerName, LoggerLevel.DEFAULT);
    };

    LogbackLogManager underTest =
        new LogbackLogManager(mock(EventManager.class), mock(BeanLocator.class), new MemoryLoggerOverrides());

    LoggerContext context = LogbackLogManager.loggerContext();

    // verify default state
    assertThat(context.exists(fooLoggerName), nullValue());
    assertThat(context.exists(barLoggerName), nullValue());

    // start the manager
    underTest.start();

    // manually apply customizer (normally handled by mediator)
    underTest.registerCustomization(customizer);

    // verify customization was applied
    assertThat(context.getLogger(fooLoggerName).getLevel(), is(Level.DEBUG));

    // DEFAULT logger should not have been created
    assertThat(context.exists(barLoggerName), nullValue());
  }

  @Test
  public void testRegisterCustomization_overridesOverrideCustomizer() throws Exception {
    LogConfigurationCustomizer customizer =
        configuration -> configuration.setLoggerLevel(fooLoggerName, LoggerLevel.DEBUG);

    MemoryLoggerOverrides overrides = new MemoryLoggerOverrides();
    overrides.set(fooLoggerName, LoggerLevel.ERROR);
    LogbackLogManager underTest = new LogbackLogManager(mock(EventManager.class), mock(BeanLocator.class), overrides);

    LoggerContext context = LogbackLogManager.loggerContext();

    // verify default state
    assertThat(context.exists(fooLoggerName), nullValue());

    // start the manager
    underTest.start();

    // manually apply customizer (normally handled by mediator)
    underTest.registerCustomization(customizer);

    // verify customization was applied
    assertThat(context.getLogger(fooLoggerName).getLevel(), is(Level.ERROR));
  }

  @Test
  public void testGetLogFor() {
    Appender notFile = mock(Appender.class);
    when(notFile.getName()).thenReturn("nexus.log");

    FileAppender nexusLog = mock(FileAppender.class);
    when(nexusLog.getName()).thenReturn("nexus.log");
    when(nexusLog.getFile()).thenReturn("/var/log/nexus.log");

    FileAppender clusterLog = mock(FileAppender.class);
    when(clusterLog.getName()).thenReturn("clustered.log");
    when(clusterLog.getFile()).thenReturn("/var/log/clustered.log");

    assertThat(getLogFor("nexus.log", asList(notFile, nexusLog, clusterLog)).get(), is("nexus.log"));
    assertThat(getLogFor("clustered.log", asList(notFile, nexusLog, clusterLog)).get(), is("clustered.log"));
    assertThat(getLogFor("not_a_log.log", asList(notFile, nexusLog, clusterLog)).isPresent(), is(false));
  }
}
