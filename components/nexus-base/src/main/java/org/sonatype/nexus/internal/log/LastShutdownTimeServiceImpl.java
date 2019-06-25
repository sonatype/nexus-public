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
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.log.LastShutdownTimeService;
import org.sonatype.nexus.common.log.LogManager;

import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.io.input.ReversedLinesFileReader;

import static org.sonatype.nexus.common.log.LogManager.DEFAULT_LOGGER;

/**
 * {@link LastShutdownTimeService} implementation.
 *
 * @since 3.13
 */
@Named
@Singleton
public class LastShutdownTimeServiceImpl
    extends ComponentSupport
    implements LastShutdownTimeService
{
  private static final String NEXUS_LOG_PATTERN = "(?<time>\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2})";
  private static final String NEXUS_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
  private static final String GROUP_NAME = "time";
  private static final String START_INDICATOR = "org.sonatype.nexus.pax.logging.NexusLogActivator - start";
  private static final int DEFAULT_LINE_READING_LIMIT = 10_000;

  private final DateFormat nexusFormat = new SimpleDateFormat(NEXUS_DATE_FORMAT);

  private final Pattern nexusPattern = Pattern.compile(NEXUS_LOG_PATTERN);

  private final LogManager logManager;

  private Optional<Date> shutdownTimeGuess = null;

  @Inject
  public LastShutdownTimeServiceImpl(final LogManager logManager,
                                     @Named("${nexus.log.lastShutdownTime.enabled:-true}") final boolean enabled) {
    this.logManager = logManager;
    if (!enabled) {
      this.shutdownTimeGuess = Optional.empty();
    }
  }

  @Override
  public synchronized Optional<Date> estimateLastShutdownTime() {
    if (shutdownTimeGuess == null) {
      shutdownTimeGuess = findBestEstimate(logManager);
    }
    return shutdownTimeGuess;
  }

  private Optional<Date> findBestEstimate(final LogManager logManager) {
    File nexusFile = logManager.getLogFileForLogger(DEFAULT_LOGGER).orElse(null);
    Optional<Date> estimatedTime = Optional.empty();

    if (nexusFile == null) {
      log.warn("Missing log file for {} , so last shutdown time can't be estimated.", DEFAULT_LOGGER);
    } else if(nexusFile.length() == 0) {
      log.warn("Empty log file {} , so last shutdown time can't be estimated.", nexusFile);
    } else {
      try (ReversedLinesFileReader logReader = new ReversedLinesFileReader(nexusFile)) {
        estimatedTime = findShutdownTimeInLog(logReader, START_INDICATOR, nexusPattern, DEFAULT_LINE_READING_LIMIT, GROUP_NAME, nexusFormat);
      } catch (Exception e) {
        log.warn("Failed to process file {}.  Assuming no previous start time", nexusFile, e);
      }
    }

    return estimatedTime;
  }

  @VisibleForTesting
  Optional<Date> findShutdownTimeInLog(final ReversedLinesFileReader logReader,
                                       final String startIndicator,
                                       final Pattern timestampPattern,
                                       final int lineScanLimit,
                                       final String groupName,
                                       final DateFormat dateFormat) throws IOException, ParseException {

    int linesRead = advanceReaderToShutdownLine(logReader, startIndicator, lineScanLimit);
    return findTimeFromPreviousInstance(logReader, timestampPattern, lineScanLimit - linesRead, groupName, dateFormat);
  }

  /**
   * The main work of this method is done in moving the logReader to the point just before the last shutdown
   */
  private int advanceReaderToShutdownLine(final ReversedLinesFileReader logReader,
                                          final String startIndicator,
                                          final int lineScanLimit) throws IOException {
    int lineCount = 0;
    String line;
    while(lineCount++ < lineScanLimit && (line = logReader.readLine()) != null) {
      if (line.contains(startIndicator)) {
        break;
      }
    }

    return lineCount;
  }

  private Optional<Date> findTimeFromPreviousInstance(final ReversedLinesFileReader logReader,
                                                      final Pattern timestampPattern,
                                                      final int lineScanLimit,
                                                      final String groupName,
                                                      final DateFormat dateFormat) throws IOException, ParseException
  {
    int lineCount = 0;
    String line;
    while(lineCount++ < lineScanLimit && (line = logReader.readLine()) != null) {
      Matcher matcher = timestampPattern.matcher(line);
      if (matcher.find()) {
        return Optional.of(dateFormat.parse(matcher.group(groupName)));
      }
    }
    return Optional.empty();
  }
}
