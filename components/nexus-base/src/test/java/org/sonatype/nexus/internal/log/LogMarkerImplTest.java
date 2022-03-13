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

import java.util.EnumSet;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.common.log.LogManager;
import org.sonatype.nexus.common.log.LogMarkInsertedEvent;
import org.sonatype.nexus.common.log.LoggerLevel;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.isNotNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests {@link LogMarkerImpl}.
 */
public class LogMarkerImplTest
    extends TestSupport
{
  private static final String LOG_NAME = LogMarkerImpl.class.getName();

  @Mock
  private LogManager logManager;

  @Mock
  private EventManager eventManager;

  private LogMarkerImpl logMarker;

  @Before
  public void setUp() {
    logMarker = new LogMarkerImpl(logManager, eventManager);
    when(logManager.getLoggerEffectiveLevel(LOG_NAME)).thenReturn(LoggerLevel.INFO);
  }

  @Test
  public void testMarkLog_EmitEvent() {
    logMarker.markLog("test");
    ArgumentCaptor<LogMarkInsertedEvent> argCaptor = ArgumentCaptor.forClass(LogMarkInsertedEvent.class);
    verify(eventManager).post(argCaptor.capture());
    assertThat(argCaptor.getValue().getMessage(), is("test"));
  }

  @Test
  public void testMarkLog_EnsureLogLevel() {
    for (LoggerLevel level : EnumSet.complementOf(EnumSet.of(LoggerLevel.DEFAULT))) {
      reset(logManager);
      when(logManager.getLoggerEffectiveLevel(LOG_NAME)).thenReturn(level);
      logMarker.markLog("test");
      try {
        if (EnumSet.of(LoggerLevel.OFF, LoggerLevel.ERROR, LoggerLevel.WARN).contains(level)) {
          verify(logManager).setLoggerLevel(LOG_NAME, LoggerLevel.INFO);
        }
        else {
          verify(logManager, never()).setLoggerLevel(isNotNull(), isNotNull());
        }
      }
      catch (AssertionError e) {
        throw new AssertionError("Mishandled log level " + level, e);
      }
    }
  }
}
