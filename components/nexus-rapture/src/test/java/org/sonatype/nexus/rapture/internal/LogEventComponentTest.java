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
package org.sonatype.nexus.rapture.internal;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.rapture.internal.logging.LogEventComponent;
import org.sonatype.nexus.rapture.internal.logging.LogEventXO;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

public class LogEventComponentTest
    extends TestSupport
{
  @Mock
  private MockedStatic<LoggerFactory> loggerFactory;

  @Mock
  private Logger log;

  private LogEventComponent underTest;

  @Before
  public void setup() {
    // Required for class setup
    loggerFactory.when(() -> LoggerFactory.getLogger(LogEventComponent.class))
        .thenReturn(mock(Logger.class));

    // The logger we care about
    loggerFactory.when(() -> LoggerFactory.getLogger("org.something"))
        .thenReturn(log);
  }

  @Test
  public void testEnabledLogging() {
    underTest = new LogEventComponent(true);

    underTest.recordEvent(createLogEvent());

    loggerFactory.verify(() -> LoggerFactory.getLogger("org.something"));
  }

  @Test
  public void testDisabledLogging() {
    underTest = new LogEventComponent(false);

    underTest.recordEvent(createLogEvent());

    loggerFactory.verify(() -> LoggerFactory.getLogger(any(String.class)), times(0));
  }

  private LogEventXO createLogEvent() {
    LogEventXO logEventXO = new LogEventXO();
    logEventXO.setLogger("org.something");
    logEventXO.setLevel("debug");
    return logEventXO;
  }
}
