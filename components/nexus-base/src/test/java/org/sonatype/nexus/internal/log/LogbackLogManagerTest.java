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

import java.io.File;
import java.util.Collections;
import java.util.Map;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.app.ApplicationDirectories;
import org.sonatype.nexus.common.entity.Continuation;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.common.log.LogConfigurationCustomizer;
import org.sonatype.nexus.common.log.LoggerLevel;
import org.sonatype.nexus.common.log.LoggerOverridesReloadEvent;
import org.sonatype.nexus.datastore.mybatis.ContinuationArrayList;
import org.sonatype.nexus.internal.log.overrides.datastore.DatastoreLoggerOverrides;
import org.sonatype.nexus.internal.log.overrides.datastore.LoggerOverridesEvent;
import org.sonatype.nexus.internal.log.overrides.datastore.LoggerOverridesEvent.Action;
import org.sonatype.nexus.internal.log.overrides.datastore.LoggingOverridesData;
import org.sonatype.nexus.internal.log.overrides.datastore.LoggingOverridesStore;
import org.sonatype.nexus.testcommon.event.SimpleEventManager;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.FileAppender;
import org.eclipse.sisu.inject.BeanLocator;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.testcontainers.shaded.com.google.common.collect.ImmutableSet;

import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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

  @Test
  public void testReloadOverridesFromDatastoreInvoked() throws Exception {
    EventManager eventManager = mock(EventManager.class);
    LoggingOverridesStore store = mock(LoggingOverridesStore.class);

    Continuation<LoggingOverridesData> data = new ContinuationArrayList<LoggingOverridesData>()
    {
      {
        add(new LoggingOverridesData("logger-name", LoggerLevel.DEBUG.toString()));
      }
    };
    when(store.readRecords()).thenReturn(data);

    DatastoreLoggerOverrides overrides = new DatastoreLoggerOverrides(
        mock(ApplicationDirectories.class), store, eventManager);

    LogbackLogManager underTest = new LogbackLogManager(eventManager, mock(BeanLocator.class), overrides);

    overrides.start();

    ArgumentCaptor<LoggerOverridesReloadEvent> eventCaptor = ArgumentCaptor.forClass(LoggerOverridesReloadEvent.class);
    verify(eventManager).post(eventCaptor.capture());

    assertThat(eventCaptor.getValue(), instanceOf(LoggerOverridesReloadEvent.class));

    Map<String, LoggerLevel> overriddenLoggers = underTest.getOverriddenLoggers();
    assertThat(overriddenLoggers.size(), is(1));
    assertThat(overriddenLoggers.get("logger-name").toString(), is(LoggerLevel.DEBUG.toString()));
  }

  @Test
  public void testLoggerOverridesEventProcessed() {
    EventManager eventManager = new SimpleEventManager();
    LogbackLogManager underTest = new LogbackLogManager(eventManager, mock(BeanLocator.class),
        mock(DatastoreLoggerOverrides.class));
    String testName = "test";
    String testLevel = "TRACE";

    eventManager.register(underTest);
    LoggerOverridesEvent changeEvent = new LoggerOverridesEvent(testName, testLevel, Action.CHANGE);
    changeEvent.setRemoteNodeId("nodeId");
    eventManager.post(changeEvent);
    LoggerContext context = LogbackLogManager.loggerContext();

    await().atMost(10, SECONDS)
        .untilAsserted(() -> assertThat(context.getLogger(testName).getLevel(), is(Level.toLevel(testLevel))));

    LoggerOverridesEvent resetEvent = new LoggerOverridesEvent(testName, null, Action.RESET);
    resetEvent.setRemoteNodeId("nodeId");
    eventManager.post(resetEvent);

    await().atMost(10, SECONDS)
        .untilAsserted(() -> assertThat(context.getLogger(testName).getLevel(), is(nullValue())));
  }

  @Test
  public void testResetRootLoggerEventProcessed() {
    EventManager eventManager = new SimpleEventManager();
    LoggerContext context = LogbackLogManager.loggerContext();
    LogbackLogManager underTest = new LogbackLogManager(eventManager, mock(BeanLocator.class),
        mock(DatastoreLoggerOverrides.class));
    eventManager.register(underTest);

    // change ROOT log level
    LoggerOverridesEvent changeEvent = new LoggerOverridesEvent("ROOT", "DEBUG", Action.CHANGE);
    changeEvent.setRemoteNodeId("nodeId");
    eventManager.post(changeEvent);

    await().atMost(10, SECONDS).untilAsserted(() -> assertThat(context.getLogger("ROOT").getLevel(), is(Level.DEBUG)));

    // reset ROOT log level back to INFO
    LoggerOverridesEvent resetEvent = new LoggerOverridesEvent("ROOT", null, Action.RESET);
    resetEvent.setRemoteNodeId("nodeId");
    eventManager.post(resetEvent);

    await().atMost(10, SECONDS).untilAsserted(() -> assertThat(context.getLogger("ROOT").getLevel(), is(Level.INFO)));
  }

  @Test
  public void testResetAllLoggersEventProcessed() {
    EventManager eventManager = new SimpleEventManager();
    LoggerContext context = LogbackLogManager.loggerContext();
    LoggerOverrides memoryLoggerOverrides = new MemoryLoggerOverrides();
    LogbackLogManager underTest = new LogbackLogManager(eventManager, mock(BeanLocator.class), memoryLoggerOverrides);
    eventManager.register(underTest);

    // send event with updating 2 loggers
    LoggerOverridesEvent changeEvent = new LoggerOverridesEvent("org.bar", "TRACE", Action.CHANGE);
    changeEvent.setRemoteNodeId("nodeId");
    eventManager.post(changeEvent);
    LoggerOverridesEvent changeEvent2 = new LoggerOverridesEvent("org.foo", "DEBUG", Action.CHANGE);
    changeEvent2.setRemoteNodeId("nodeId");
    eventManager.post(changeEvent2);

    // check that loggers were set.
    await().atMost(10, SECONDS)
        .untilAsserted(() -> assertThat(context.getLogger("org.bar").getLevel(), is(Level.TRACE)));
    await().atMost(10, SECONDS)
        .untilAsserted(() -> assertThat(context.getLogger("org.foo").getLevel(), is(Level.DEBUG)));

    // reset all loggers
    LoggerOverridesEvent resetEvent = new LoggerOverridesEvent(null, null, Action.RESET_ALL);
    resetEvent.setRemoteNodeId("nodeId");
    eventManager.post(resetEvent);

    await().atMost(10, SECONDS)
        .untilAsserted(() -> assertThat(context.getLogger("org.bar").getLevel(), is(nullValue())));
    await().atMost(10, SECONDS)
        .untilAsserted(() -> assertThat(context.getLogger("org.foo").getLevel(), is(nullValue())));
  }

  @Test
  public void testLoggerLevelWhenLogNotFound() {
    EventManager eventManager = new SimpleEventManager();
    LogbackLogManager underTest = spy(new LogbackLogManager(eventManager, mock(BeanLocator.class),
        mock(DatastoreLoggerOverrides.class)));
    String testFileName = "foo";

    when(underTest.getAllLogFiles(testFileName)).thenReturn(Collections.emptySet());

    underTest.getLogFile(testFileName);

    verify(underTest).logFileNotFound(testFileName);
  }

  @Test
  public void testNoLogWhenFileFound() {
    EventManager eventManager = new SimpleEventManager();
    LogbackLogManager underTest = spy(new LogbackLogManager(eventManager, mock(BeanLocator.class),
        mock(DatastoreLoggerOverrides.class)));
    String testFileName = "foo";

    when(underTest.getAllLogFiles(testFileName)).thenReturn(ImmutableSet.of(new File(testFileName)));

    File result = underTest.getLogFile(testFileName);

    verify(underTest, times(0)).logFileNotFound(any());

    assertEquals(testFileName, result.getName());
  }
}
