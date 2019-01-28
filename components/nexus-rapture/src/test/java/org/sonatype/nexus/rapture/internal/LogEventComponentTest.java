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

import org.sonatype.nexus.rapture.internal.logging.LogEventComponent;
import org.sonatype.nexus.rapture.internal.logging.LogEventXO;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest(LoggerFactory.class)
public class LogEventComponentTest
{
  private LogEventComponent underTest;

  @Test
  public void testEnabledLogging() {
    underTest = new LogEventComponent(true);

    mockStatic(LoggerFactory.class);
    when(LoggerFactory.getLogger("org.something")).thenReturn(mock(Logger.class));

    underTest.recordEvent(createLogEvent());

    verifyStatic();
    LoggerFactory.getLogger("org.something");
  }

  @Test
  public void testDisabledLogging() {
    underTest = new LogEventComponent(false);

    mockStatic(LoggerFactory.class);

    underTest.recordEvent(createLogEvent());

    PowerMockito.verifyNoMoreInteractions(LoggerFactory.class);
  }

  private LogEventXO createLogEvent() {
    LogEventXO logEventXO = new LogEventXO();
    logEventXO.setLogger("org.something");
    logEventXO.setLevel("debug");
    return logEventXO;
  }
}
