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
package org.sonatype.nexus.common.log;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nullable;

import org.sonatype.goodies.lifecycle.Lifecycle;

/**
 * Log manager.
 */
public interface LogManager
  extends Lifecycle
{
  String DEFAULT_LOGGER = "logfile";

  Set<File> getLogFiles();

  @Nullable
  File getLogFile(String fileName);

  /**
   * If the named logger is a file based log, return the file name associated with it.
   *
   * @since 3.17
   * @param loggerName name of the logger whose log file's name, if applicable, should be returned
   * @return The file name to which the named logger appends
   */
  Optional<String> getLogFor(String loggerName);

  /**
   * If the named logger is a file based log, return the file associated with it.
   *
   * @since 3.17
   * @param loggerName name of the logger whose log file, if applicable, should be returned
   * @return The file to which the named logger appends
   */
  Optional<File> getLogFileForLogger(String loggerName);

  /**
   * Provides access to named log-file streams.
   *
   * @param fileName name of log file to fetch
   * @return Stream to log file or {@code null} if non-existent.
   */
  @Nullable
  InputStream getLogFileStream(String fileName, long fromByte, long bytesCount) throws IOException;

  /**
   * Return mapping of existing loggers which have explicit levels configured (never null).
   *
   * @since 2.7
   */
  Map<String, LoggerLevel> getLoggers();

  /**
   * Return mapping of existing loggers which have been overridden by the user.
   *
   * @since 3.2
   */
  Map<String, LoggerLevel> getOverriddenLoggers();

  /**
   * @since 2.7
   */
  void resetLoggers();

  /**
   * @since 2.7
   */
  void setLoggerLevel(String name, @Nullable LoggerLevel level);

  /**
   * @since 2.7
   */
  void unsetLoggerLevel(String name);

  /**
   * @since 2.7
   */
  @Nullable
  LoggerLevel getLoggerLevel(String name);

  /**
   * @since 2.7
   */
  LoggerLevel getLoggerEffectiveLevel(String name);
}
