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
package org.sonatype.nexus.coreui.internal.log;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import org.sonatype.goodies.testsupport.Test5Support;
import org.sonatype.nexus.common.log.LogManager;
import org.sonatype.nexus.common.log.LoggerLevel;
import org.sonatype.nexus.repository.BadRequestException;
import org.sonatype.nexus.testcommon.extensions.AuthenticationExtension;
import org.sonatype.nexus.testcommon.extensions.AuthenticationExtension.WithUser;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(AuthenticationExtension.class)
@WithUser
class LoggingConfigurationResourceTest
    extends Test5Support
{
  @Mock
  private LogManager logManager;

  private LoggingConfigurationResource underTest;

  @BeforeEach
  void setup() {
    underTest = new LoggingConfigurationResource(logManager);
  }

  @Test
  void readAll_returnsLoggers() {
    Map<String, LoggerLevel> loggers = new LinkedHashMap<>();
    loggers.put("ROOT", LoggerLevel.INFO);
    loggers.put("org.sonatype.nexus", LoggerLevel.DEBUG);
    when(logManager.getEffectiveLoggersUpdatedByFetchedOverrides()).thenReturn(loggers);

    Collection<LoggerXO> result = underTest.readAll();

    assertThat(result, is(notNullValue()));
    assertThat(result, hasSize(2));
  }

  @Test
  void readAll_emptyLoggers() {
    when(logManager.getEffectiveLoggersUpdatedByFetchedOverrides()).thenReturn(Map.of());

    Collection<LoggerXO> result = underTest.readAll();

    assertThat(result, hasSize(0));
  }

  @Test
  void read_returnsLoggerWithOverride() {
    when(logManager.getLoggerEffectiveLevel("org.sonatype.nexus")).thenReturn(LoggerLevel.DEBUG);
    Map<String, LoggerLevel> loggers = Map.of("org.sonatype.nexus", LoggerLevel.DEBUG);
    when(logManager.getLoggers()).thenReturn(loggers);

    LoggerXO result = underTest.read("org.sonatype.nexus");

    assertThat(result, is(notNullValue()));
    assertThat(result.getName(), is("org.sonatype.nexus"));
    assertThat(result.getLevel(), is(LoggerLevel.DEBUG));
    assertThat(result.isOverride(), is(true));
  }

  @Test
  void read_returnsLoggerWithoutOverride() {
    when(logManager.getLoggerEffectiveLevel("org.sonatype.nexus")).thenReturn(LoggerLevel.INFO);
    when(logManager.getLoggers()).thenReturn(Map.of());

    LoggerXO result = underTest.read("org.sonatype.nexus");

    assertThat(result.getName(), is("org.sonatype.nexus"));
    assertThat(result.getLevel(), is(LoggerLevel.INFO));
    assertThat(result.isOverride(), is(false));
  }

  @Test
  void resetAll_delegatesToLogManager() {
    underTest.resetAll();

    verify(logManager).resetLoggers();
  }

  @Test
  void update_setsLoggerLevel() {
    UpdateLoggingConfigurationRequest request = new UpdateLoggingConfigurationRequest();
    request.setLevel(LoggerLevel.DEBUG);

    underTest.update("org.sonatype.nexus", request);

    verify(logManager).setLoggerLevel("org.sonatype.nexus", LoggerLevel.DEBUG);
  }

  @Test
  void update_unsetsLoggerLevel_whenDefault() {
    UpdateLoggingConfigurationRequest request = new UpdateLoggingConfigurationRequest();
    request.setLevel(LoggerLevel.DEFAULT);

    underTest.update("org.sonatype.nexus", request);

    verify(logManager).unsetLoggerLevel("org.sonatype.nexus");
  }

  @Test
  void update_throwsBadRequest_onIllegalArgument() {
    UpdateLoggingConfigurationRequest request = new UpdateLoggingConfigurationRequest();
    request.setLevel(LoggerLevel.TRACE);

    doThrow(new IllegalArgumentException("Invalid level"))
        .when(logManager)
        .setLoggerLevel("ROOT", LoggerLevel.TRACE);

    assertThrows(BadRequestException.class, () -> underTest.update("ROOT", request));
  }

  @Test
  void reset_unsetsLoggerLevel() {
    underTest.reset("org.sonatype.nexus");

    verify(logManager).unsetLoggerLevel("org.sonatype.nexus");
  }

  @Test
  void update_withInfoLevel() {
    UpdateLoggingConfigurationRequest request = new UpdateLoggingConfigurationRequest();
    request.setLevel(LoggerLevel.INFO);

    underTest.update("com.example", request);

    verify(logManager).setLoggerLevel("com.example", LoggerLevel.INFO);
  }

  @Test
  void update_withErrorLevel() {
    UpdateLoggingConfigurationRequest request = new UpdateLoggingConfigurationRequest();
    request.setLevel(LoggerLevel.ERROR);

    underTest.update("com.example", request);

    verify(logManager).setLoggerLevel("com.example", LoggerLevel.ERROR);
  }

  @Test
  void update_withWarnLevel() {
    UpdateLoggingConfigurationRequest request = new UpdateLoggingConfigurationRequest();
    request.setLevel(LoggerLevel.WARN);

    underTest.update("com.example", request);

    verify(logManager).setLoggerLevel("com.example", LoggerLevel.WARN);
  }

  @Test
  void update_withTraceLevel() {
    UpdateLoggingConfigurationRequest request = new UpdateLoggingConfigurationRequest();
    request.setLevel(LoggerLevel.TRACE);

    underTest.update("com.example", request);

    verify(logManager).setLoggerLevel("com.example", LoggerLevel.TRACE);
  }

  @Test
  void readAll_correctlyMapsLoggerLevels() {
    Map<String, LoggerLevel> loggers = new LinkedHashMap<>();
    loggers.put("ROOT", LoggerLevel.INFO);
    loggers.put("org.sonatype", LoggerLevel.DEBUG);
    loggers.put("com.example", LoggerLevel.WARN);
    when(logManager.getEffectiveLoggersUpdatedByFetchedOverrides()).thenReturn(loggers);

    Collection<LoggerXO> result = underTest.readAll();

    assertThat(result, hasSize(3));

    boolean foundRoot = false;
    boolean foundSonatype = false;
    boolean foundExample = false;
    for (LoggerXO logger : result) {
      if ("ROOT".equals(logger.getName())) {
        assertThat(logger.getLevel(), is(LoggerLevel.INFO));
        foundRoot = true;
      }
      else if ("org.sonatype".equals(logger.getName())) {
        assertThat(logger.getLevel(), is(LoggerLevel.DEBUG));
        foundSonatype = true;
      }
      else if ("com.example".equals(logger.getName())) {
        assertThat(logger.getLevel(), is(LoggerLevel.WARN));
        foundExample = true;
      }
    }
    assertThat(foundRoot, is(true));
    assertThat(foundSonatype, is(true));
    assertThat(foundExample, is(true));
  }
}
