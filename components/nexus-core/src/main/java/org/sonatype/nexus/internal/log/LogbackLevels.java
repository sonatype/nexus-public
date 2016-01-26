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

import org.sonatype.nexus.common.log.LoggerLevel;

import ch.qos.logback.classic.Level;

/**
 * Logback levels helper.
 *
 * @since 3.0
 */
class LogbackLevels
{
  private LogbackLevels() {
    // empty
  }

  /**
   * Convert a Logback {@link Level} into a {@link LoggerLevel}.
   */
  public static LoggerLevel convert(final Level level) {
    switch (level.toInt()) {
      case Level.ERROR_INT:
        return LoggerLevel.ERROR;

      case Level.WARN_INT:
        return LoggerLevel.WARN;

      case Level.INFO_INT:
        return LoggerLevel.INFO;

      case Level.DEBUG_INT:
        return LoggerLevel.DEBUG;

      case Level.OFF_INT:
        return LoggerLevel.OFF;

      case Level.TRACE_INT:
      default:
        return LoggerLevel.TRACE;
    }
  }

  /**
   * Convert a {@link LoggerLevel} into a Logback {@link Level}.
   */
  public static Level convert(final LoggerLevel level) {
    return Level.valueOf(level.name());
  }
}
