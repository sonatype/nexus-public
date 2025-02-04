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
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;
import java.util.regex.Pattern;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.log.LogManager;

import org.apache.commons.io.input.ReversedLinesFileReader;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.common.log.LogManager.DEFAULT_LOGGER;

public class LastShutdownTimeServiceImplTest
    extends TestSupport
{

  private static final String START_MARKER = "startMarker";

  private static final String GROUP_NAME = "time";

  @Mock
  private LogManager logManager;

  @Mock
  private File nexusFile;

  @Mock
  private ReversedLinesFileReader reader;

  private Pattern pattern;

  private DateFormat dateFormat;

  private LastShutdownTimeServiceImpl lastShutdownTimeService;

  @Before
  public void setup() {
    initMocks();

    pattern = Pattern.compile("(?<time>\\d{4}-\\d{2}-\\d{2})");

    when(logManager.getLogFileForLogger(DEFAULT_LOGGER)).thenReturn(Optional.of(nexusFile));

    dateFormat = new SimpleDateFormat("yyyy-MM-dd");
  }

  @Test
  public void missingNexusYieldsEmpty() {
    when(logManager.getLogFor(DEFAULT_LOGGER)).thenReturn(Optional.of("nexus.log"));
    when(logManager.getLogFile("nexus.log")).thenReturn(null);
    lastShutdownTimeService = new LastShutdownTimeServiceImpl(logManager, true);
    assertThat(lastShutdownTimeService.estimateLastShutdownTime().isPresent(), equalTo(false));
  }

  @Test
  public void emptyNexusYieldsEmpty() {
    File emptyFile = mock(File.class);
    when(emptyFile.length()).thenReturn(0L);

    when(logManager.getLogFor(DEFAULT_LOGGER)).thenReturn(Optional.of("nexus.log"));
    when(logManager.getLogFile("nexus.log")).thenReturn(emptyFile);
    lastShutdownTimeService = new LastShutdownTimeServiceImpl(logManager, true);
    assertThat(lastShutdownTimeService.estimateLastShutdownTime().isPresent(), equalTo(false));
  }

  @Test
  public void parsesLineCorrectly() throws IOException, ParseException {
    when(reader.readLine())
        .thenReturn("Not a matching line")
        .thenReturn("2009-01-01 Part of new startup")
        .thenReturn(START_MARKER)
        .thenReturn("Not a matching line")
        .thenReturn("2008-01-01 Last line of old shutdown")
        .thenReturn(null);

    lastShutdownTimeService = new LastShutdownTimeServiceImpl(logManager, true);

    Optional<Date> result = lastShutdownTimeService.findShutdownTimeInLog(reader, START_MARKER, pattern, 1000, GROUP_NAME, dateFormat);

    assertThat(result.isPresent(), equalTo(true));
    assertThat(result.get(), equalTo(new SimpleDateFormat("yyyy-MM-dd").parse("2008-01-01")));
  }

  @Test
  public void noStartupMarkerIndicatesNoPrevShutdown() throws IOException, ParseException {
    when(reader.readLine())
        .thenReturn("Not a matching line")
        .thenReturn("2009-01-01 Part of new startup")
        .thenReturn("Not a matching line")
        .thenReturn("2008-01-01 Just some line")
        .thenReturn(null);

    lastShutdownTimeService = new LastShutdownTimeServiceImpl(logManager, true);

    Optional<Date> result = lastShutdownTimeService.findShutdownTimeInLog(reader, START_MARKER, pattern, 1000, GROUP_NAME, dateFormat);

    assertThat(result.isPresent(), equalTo(false));
  }

  @Test
  public void ensureObeysLineLimit() throws IOException, ParseException {
    when(reader.readLine())
        .thenReturn("Not a matching line")
        .thenReturn("2009-01-01 Part of new startup")
        .thenReturn(START_MARKER)
        .thenReturn("Not a matching line")
        .thenReturn("2008-01-01 Last line of old shutdown")
        .thenReturn(null);

    lastShutdownTimeService = new LastShutdownTimeServiceImpl(logManager, true);

    Optional<Date> result = lastShutdownTimeService.findShutdownTimeInLog(reader, START_MARKER, pattern, 4, GROUP_NAME, dateFormat);

    assertThat(result.isPresent(), equalTo(false));
    verify(reader, times(4)).readLine();
  }

  @Test
  public void disablingServiceAccessesNothing() throws IOException {
    lastShutdownTimeService = new LastShutdownTimeServiceImpl(logManager, false);

    Optional<Date> result = lastShutdownTimeService.estimateLastShutdownTime();

    assertThat(result.isPresent(), equalTo(false));
    verify(reader, never()).readLine();
  }

  @Test
  public void handleMissingStartMarker() throws Exception {
    ReversedLinesFileReader realReader = new ReversedLinesFileReader(
        Paths.get(getClass().getResource("no-start-marker.log").toURI()), Charset.defaultCharset());

    lastShutdownTimeService = new LastShutdownTimeServiceImpl(logManager, true);

    Optional<Date> result =
        lastShutdownTimeService.findShutdownTimeInLog(realReader, START_MARKER, pattern, 1000, GROUP_NAME, dateFormat);

    assertThat(result.isPresent(), equalTo(false));
  }
}
