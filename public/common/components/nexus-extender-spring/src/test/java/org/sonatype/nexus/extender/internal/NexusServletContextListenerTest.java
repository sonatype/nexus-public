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
package org.sonatype.nexus.extender.internal;

import jakarta.servlet.ServletContextEvent;

import org.sonatype.nexus.bootstrap.entrypoint.EditionVersionFormatter;
import org.sonatype.nexus.bootstrap.entrypoint.edition.NexusEdition;
import org.sonatype.nexus.bootstrap.entrypoint.edition.NexusEditionSelector;
import org.sonatype.nexus.common.app.ApplicationVersion;
import org.sonatype.nexus.common.app.ManagedLifecycle.Phase;
import org.sonatype.nexus.extender.NexusLifecycleManager;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.slf4j.LoggerFactory;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Tests for {@link NexusServletContextListener}
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class NexusServletContextListenerTest
{
  @Mock
  private NexusEditionSelector nexusEditionSelector;

  @Mock
  private NexusLifecycleManager nexusLifecycleManager;

  @Mock
  private ApplicationVersion applicationVersion;

  @Mock
  private NexusEdition nexusEdition;

  @Mock
  private ServletContextEvent servletContextEvent;

  private NexusServletContextListener listener;

  private Logger logger;

  private Logger formatterLogger;

  private ListAppender<ILoggingEvent> logAppender;

  @Before
  public void setUp() {
    // Set up log capture for NexusServletContextListener
    logger = (Logger) LoggerFactory.getLogger(NexusServletContextListener.class);
    logAppender = new ListAppender<>();
    logAppender.start();
    logger.addAppender(logAppender);

    // Set up log capture for EditionVersionFormatter (for warning messages)
    formatterLogger = (Logger) LoggerFactory.getLogger(EditionVersionFormatter.class);
    formatterLogger.addAppender(logAppender);

    listener = new NexusServletContextListener(nexusEditionSelector, nexusLifecycleManager, applicationVersion, null);
  }

  @After
  public void tearDown() {
    logger.detachAppender(logAppender);
    formatterLogger.detachAppender(logAppender);
  }

  @Test
  public void testContextDestroyed_withEditionAndVersion() throws Exception {
    // Setup
    when(nexusEditionSelector.getCurrent()).thenReturn(nexusEdition);
    when(nexusEdition.getId()).thenReturn("PRO");
    when(applicationVersion.getVersion()).thenReturn("3.75.0-01");

    // Execute
    listener.contextDestroyed(servletContextEvent);

    // Verify - check log message contains edition and version
    assertThat(logAppender.list, hasItem(
        hasProperty("formattedMessage", containsString("PRO/3.75.0-01"))));
    assertThat(logAppender.list, hasItem(
        hasProperty("formattedMessage", containsString("Uptime:"))));
    assertThat(logAppender.list, hasItem(
        hasProperty("level", is(Level.INFO))));

    // Verify lifecycle manager moves to OFF phase
    verify(nexusLifecycleManager).to(Phase.OFF);
  }

  @Test
  public void testContextDestroyed_withEditionButNoVersion() throws Exception {
    // Setup - no application version
    listener = new NexusServletContextListener(nexusEditionSelector, nexusLifecycleManager, null, null);
    when(nexusEditionSelector.getCurrent()).thenReturn(nexusEdition);
    when(nexusEdition.getId()).thenReturn("PRO");

    // Execute
    listener.contextDestroyed(servletContextEvent);

    // Verify - check log message contains only edition (no version)
    assertThat(logAppender.list, hasItem(
        hasProperty("formattedMessage", containsString("PRO"))));
    assertThat(logAppender.list, hasItem(
        hasProperty("formattedMessage", containsString("Uptime:"))));
    // Should NOT contain a slash followed by version
    for (ILoggingEvent event : logAppender.list) {
      if (event.getFormattedMessage().contains("Uptime:")) {
        assertThat(event.getFormattedMessage().contains("PRO/"), is(false));
      }
    }

    // Verify lifecycle manager moves to OFF phase
    verify(nexusLifecycleManager).to(Phase.OFF);
  }

  @Test
  public void testContextDestroyed_withNullEditionAndVersion() throws Exception {
    // Setup - null edition but version present
    when(nexusEditionSelector.getCurrent()).thenReturn(null);
    when(applicationVersion.getVersion()).thenReturn("3.75.0-01");

    // Execute
    listener.contextDestroyed(servletContextEvent);

    // Verify - check log message contains "unknown" for edition with version
    assertThat(logAppender.list, hasItem(
        hasProperty("formattedMessage", containsString("unknown/3.75.0-01"))));
    assertThat(logAppender.list, hasItem(
        hasProperty("formattedMessage", containsString("Uptime:"))));
    // Verify warning log about unable to determine edition
    assertThat(logAppender.list, hasItem(
        hasProperty("formattedMessage", containsString("Unable to determine edition ID"))));
    assertThat(logAppender.list, hasItem(
        hasProperty("level", is(Level.WARN))));

    // Verify lifecycle manager moves to OFF phase
    verify(nexusLifecycleManager).to(Phase.OFF);
  }

  @Test
  public void testContextDestroyed_withNullEditionAndNoVersion() throws Exception {
    // Setup - null edition and no version
    listener = new NexusServletContextListener(nexusEditionSelector, nexusLifecycleManager, null, null);
    when(nexusEditionSelector.getCurrent()).thenReturn(null);

    // Execute
    listener.contextDestroyed(servletContextEvent);

    // Verify - check log message contains only "unknown"
    assertThat(logAppender.list, hasItem(
        hasProperty("formattedMessage", containsString("unknown"))));
    assertThat(logAppender.list, hasItem(
        hasProperty("formattedMessage", containsString("Uptime:"))));
    // Verify warning log
    assertThat(logAppender.list, hasItem(
        hasProperty("formattedMessage", containsString("Unable to determine edition ID"))));
    // Should NOT contain a slash
    for (ILoggingEvent event : logAppender.list) {
      if (event.getFormattedMessage().contains("Uptime:")) {
        assertThat(event.getFormattedMessage().contains("unknown/"), is(false));
      }
    }

    // Verify lifecycle manager moves to OFF phase
    verify(nexusLifecycleManager).to(Phase.OFF);
  }

  @Test
  public void testFormatEditionAndVersion_differentEditionIds() {
    // Test with different edition IDs to ensure formatting works correctly
    String[] editionIds = {"PRO", "OSS", "STARTER", "COMMUNITY"};

    for (String editionId : editionIds) {
      logAppender.list.clear(); // Clear logs between tests

      // Setup
      when(nexusEditionSelector.getCurrent()).thenReturn(nexusEdition);
      when(nexusEdition.getId()).thenReturn(editionId);
      when(applicationVersion.getVersion()).thenReturn("3.75.0-01");

      // Execute
      listener.contextDestroyed(servletContextEvent);

      // Verify - check each edition ID is properly formatted
      assertThat(logAppender.list, hasItem(
          hasProperty("formattedMessage", containsString(editionId + "/3.75.0-01"))));
    }
  }

  @Test
  public void testContextDestroyed_handlesException() throws Exception {
    // Setup - lifecycle manager throws exception
    when(nexusEditionSelector.getCurrent()).thenReturn(nexusEdition);
    when(nexusEdition.getId()).thenReturn("PRO");
    when(applicationVersion.getVersion()).thenReturn("3.75.0-01");
    doThrow(new RuntimeException("Test exception")).when(nexusLifecycleManager).to(Phase.OFF);

    // Execute - should not throw, exception should be caught and logged
    listener.contextDestroyed(servletContextEvent);

    // Verify - check error log
    assertThat(logAppender.list, hasItem(
        hasProperty("formattedMessage", containsString("Failed to stop nexus"))));
    assertThat(logAppender.list, hasItem(
        hasProperty("level", is(Level.ERROR))));

    // Verify lifecycle manager was still called
    verify(nexusLifecycleManager).to(Phase.OFF);
  }

  @Test
  public void testContextInitialized_withoutStartupPhase() throws Exception {
    // Execute
    listener.contextInitialized(servletContextEvent);

    // Verify - lifecycle manager moves to TASKS phase
    verify(nexusLifecycleManager).to(Phase.TASKS);

    // Verify log message about running all phases
    assertThat(logAppender.list, hasItem(
        hasProperty("formattedMessage", containsString("Running lifecycle phases"))));
  }

  @Test
  public void testContextInitialized_withStartupPhase() throws Exception {
    // Setup - with KERNEL as startup phase
    listener =
        new NexusServletContextListener(nexusEditionSelector, nexusLifecycleManager, applicationVersion, "KERNEL");

    // Execute
    listener.contextInitialized(servletContextEvent);

    // Verify - lifecycle manager moves to KERNEL phase (not TASKS)
    verify(nexusLifecycleManager).to(Phase.KERNEL);

    // Verify log message about running limited phases
    assertThat(logAppender.list, hasItem(
        hasProperty("formattedMessage", containsString("Running lifecycle phases"))));
  }

  @Test
  public void testConstructor_withApplicationVersion() {
    // Test constructor accepts ApplicationVersion
    NexusServletContextListener listenerWithVersion = new NexusServletContextListener(
        nexusEditionSelector,
        nexusLifecycleManager,
        applicationVersion,
        null);

    // Verify no exceptions thrown - constructor handles nullable ApplicationVersion
    assertThat(listenerWithVersion, is(notNullValue()));
  }

  @Test
  public void testConstructor_withoutApplicationVersion() {
    // Test constructor accepts null ApplicationVersion
    NexusServletContextListener listenerWithoutVersion = new NexusServletContextListener(
        nexusEditionSelector,
        nexusLifecycleManager,
        null,
        null);

    // Verify no exceptions thrown - constructor handles nullable ApplicationVersion
    assertThat(listenerWithoutVersion, is(notNullValue()));
  }
}
