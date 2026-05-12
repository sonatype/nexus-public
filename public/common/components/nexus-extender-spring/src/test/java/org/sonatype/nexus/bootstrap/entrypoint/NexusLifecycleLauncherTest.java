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
package org.sonatype.nexus.bootstrap.entrypoint;

import org.sonatype.nexus.bootstrap.entrypoint.edition.NexusEdition;
import org.sonatype.nexus.bootstrap.entrypoint.edition.NexusEditionSelector;
import org.sonatype.nexus.bootstrap.entrypoint.jvm.ShutdownDelegate;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Tests for {@link NexusLifecycleLauncher}
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class NexusLifecycleLauncherTest
{
  @Mock
  private NexusEditionSelector nexusEditionSelector;

  @Mock
  private ShutdownDelegate shutdownDelegate;

  @Mock
  private ApplicationVersion applicationVersion;

  @Mock
  private NexusEdition nexusEdition;

  @Mock
  private NexusLifecycleManager nexusLifecycleManager;

  private NexusLifecycleLauncher launcher;

  private Logger logger;

  private Logger formatterLogger;

  private ListAppender<ILoggingEvent> logAppender;

  @Before
  public void setUp() {
    // Set up log capture for NexusLifecycleLauncher
    logger = (Logger) LoggerFactory.getLogger(NexusLifecycleLauncher.class);
    logAppender = new ListAppender<>();
    logAppender.start();
    logger.addAppender(logAppender);

    // Set up log capture for EditionVersionFormatter (for warning messages)
    formatterLogger = (Logger) LoggerFactory.getLogger(EditionVersionFormatter.class);
    formatterLogger.addAppender(logAppender);

    launcher = new NexusLifecycleLauncher(nexusEditionSelector, shutdownDelegate, applicationVersion, null);
  }

  @After
  public void tearDown() {
    logger.detachAppender(logAppender);
    formatterLogger.detachAppender(logAppender);
  }

  @Test
  public void testStop_withEditionAndVersion() throws Exception {
    // Setup
    when(nexusEditionSelector.getCurrent()).thenReturn(nexusEdition);
    when(nexusEdition.getId()).thenReturn("PRO");
    when(applicationVersion.getVersion()).thenReturn("3.75.0-01");

    // Execute
    launcher.stop();

    // Verify - check log message contains edition and version
    assertThat(logAppender.list, hasItem(
        hasProperty("formattedMessage", containsString("PRO/3.75.0-01"))));
    assertThat(logAppender.list, hasItem(
        hasProperty("formattedMessage", containsString("Stopping Nexus"))));
    assertThat(logAppender.list, hasItem(
        hasProperty("level", is(Level.INFO))));

    // nexusLifecycleManager is null, so it should not be called
    verifyNoInteractions(nexusLifecycleManager);
  }

  @Test
  public void testStop_withEditionButNoVersion() throws Exception {
    // Setup - no application version
    launcher = new NexusLifecycleLauncher(nexusEditionSelector, shutdownDelegate, null, null);
    when(nexusEditionSelector.getCurrent()).thenReturn(nexusEdition);
    when(nexusEdition.getId()).thenReturn("PRO");

    // Execute
    launcher.stop();

    // Verify - check log message contains only edition (no version)
    assertThat(logAppender.list, hasItem(
        hasProperty("formattedMessage", containsString("PRO"))));
    assertThat(logAppender.list, hasItem(
        hasProperty("formattedMessage", containsString("Stopping Nexus"))));
    // Should NOT contain a slash followed by version
    for (ILoggingEvent event : logAppender.list) {
      if (event.getFormattedMessage().contains("Stopping Nexus")) {
        assertThat(event.getFormattedMessage().contains("PRO/"), is(false));
      }
    }
  }

  @Test
  public void testStop_withNullEditionAndVersion() throws Exception {
    // Setup - null edition but version present
    when(nexusEditionSelector.getCurrent()).thenReturn(null);
    when(applicationVersion.getVersion()).thenReturn("3.75.0-01");

    // Execute
    launcher.stop();

    // Verify - check log message contains "unknown" for edition with version
    assertThat(logAppender.list, hasItem(
        hasProperty("formattedMessage", containsString("unknown/3.75.0-01"))));
    assertThat(logAppender.list, hasItem(
        hasProperty("formattedMessage", containsString("Stopping Nexus"))));
    // Verify warning log about unable to determine edition
    assertThat(logAppender.list, hasItem(
        hasProperty("formattedMessage", containsString("Unable to determine edition ID"))));
    assertThat(logAppender.list, hasItem(
        hasProperty("level", is(Level.WARN))));
  }

  @Test
  public void testStop_withNullEditionAndNoVersion() throws Exception {
    // Setup - null edition and no version
    launcher = new NexusLifecycleLauncher(nexusEditionSelector, shutdownDelegate, null, null);
    when(nexusEditionSelector.getCurrent()).thenReturn(null);

    // Execute
    launcher.stop();

    // Verify - check log message contains only "unknown"
    assertThat(logAppender.list, hasItem(
        hasProperty("formattedMessage", containsString("unknown"))));
    assertThat(logAppender.list, hasItem(
        hasProperty("formattedMessage", containsString("Stopping Nexus"))));
    // Verify warning log
    assertThat(logAppender.list, hasItem(
        hasProperty("formattedMessage", containsString("Unable to determine edition ID"))));
    // Should NOT contain a slash
    for (ILoggingEvent event : logAppender.list) {
      if (event.getFormattedMessage().contains("Stopping Nexus")) {
        assertThat(event.getFormattedMessage().contains("unknown/"), is(false));
      }
    }
  }

  @Test
  public void testStop_withNexusLifecycleManager() throws Exception {
    // Setup
    when(nexusEditionSelector.getCurrent()).thenReturn(nexusEdition);
    when(nexusEdition.getId()).thenReturn("OSS");
    when(applicationVersion.getVersion()).thenReturn("3.75.0-01");

    // Use reflection to set the nexusLifecycleManager (normally set via onNexusScanComplete)
    java.lang.reflect.Field field = NexusLifecycleLauncher.class.getDeclaredField("nexusLifecycleManager");
    field.setAccessible(true);
    field.set(launcher, nexusLifecycleManager);

    // Execute
    launcher.stop();

    // Verify - nexusLifecycleManager should be called to move to OFF phase
    verify(nexusLifecycleManager).to(Phase.OFF);

    // Verify log message
    assertThat(logAppender.list, hasItem(
        hasProperty("formattedMessage", containsString("OSS/3.75.0-01"))));
    assertThat(logAppender.list, hasItem(
        hasProperty("formattedMessage", containsString("Stopping Nexus"))));
  }

  @Test
  public void testFormatEditionAndVersion_differentEditionIds() throws Exception {
    // Test with different edition IDs to ensure formatting works correctly
    String[] editionIds = {"PRO", "OSS", "STARTER", "COMMUNITY"};

    for (String editionId : editionIds) {
      logAppender.list.clear(); // Clear logs between tests

      // Setup
      when(nexusEditionSelector.getCurrent()).thenReturn(nexusEdition);
      when(nexusEdition.getId()).thenReturn(editionId);
      when(applicationVersion.getVersion()).thenReturn("3.75.0-01");

      // Execute
      launcher.stop();

      // Verify - check each edition ID is properly formatted
      assertThat(logAppender.list, hasItem(
          hasProperty("formattedMessage", containsString(editionId + "/3.75.0-01"))));
    }
  }

  @Test
  public void testConstructor_withApplicationVersion() {
    // Test constructor accepts ApplicationVersion
    NexusLifecycleLauncher launcherWithVersion = new NexusLifecycleLauncher(
        nexusEditionSelector,
        shutdownDelegate,
        applicationVersion,
        null);

    // Verify no exceptions thrown - constructor handles nullable ApplicationVersion
    assertThat(launcherWithVersion != null, is(true));
  }

  @Test
  public void testConstructor_withoutApplicationVersion() {
    // Test constructor accepts null ApplicationVersion
    NexusLifecycleLauncher launcherWithoutVersion = new NexusLifecycleLauncher(
        nexusEditionSelector,
        shutdownDelegate,
        null,
        null);

    // Verify no exceptions thrown - constructor handles nullable ApplicationVersion
    assertThat(launcherWithoutVersion != null, is(true));
  }
}
