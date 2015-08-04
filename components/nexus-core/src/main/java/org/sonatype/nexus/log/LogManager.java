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
package org.sonatype.nexus.log;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import org.sonatype.nexus.NexusStreamResponse;

/**
 * LogManager.
 *
 * @author cstamas
 * @author juven
 * @author adreghiciu@gmail.com
 */
public interface LogManager
{
  Set<File> getLogFiles();

  File getLogFile(String filename);

  NexusStreamResponse getApplicationLogAsStream(String logFile, long fromByte, long bytesCount)
      throws IOException;

  LogConfiguration getConfiguration()
      throws IOException;

  void setConfiguration(LogConfiguration configuration)
      throws IOException;

  void configure();

  void shutdown();

  /**
   * Return mapping of existing loggers which have explicit levels configured (never null).
   *
   * @since 2.7
   */
  Map<String, LoggerLevel> getLoggers();

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
  void resetLoggers();

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
