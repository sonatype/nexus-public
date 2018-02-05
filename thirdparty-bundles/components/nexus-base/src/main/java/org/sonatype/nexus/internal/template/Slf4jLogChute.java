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
package org.sonatype.nexus.internal.template;

import org.sonatype.goodies.common.Loggers;

import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeServices;
import org.apache.velocity.runtime.log.LogChute;
import org.slf4j.Logger;

/**
 * Slf4j LogChute.
 */
class Slf4jLogChute
    implements LogChute
{
  /**
   * The SLF4J Logger instance to use for logging.
   */
  private final Logger logger;

  /**
   * A flag to redirect Velocity INFO level to DEBUG level
   *
   * Velocity is kinda chatty in INFO level, that is not always what we need.
   */
  private final boolean redirectVelocityInfoToDebug;

  public Slf4jLogChute() {
    this.logger = Loggers.getLogger(VelocityEngine.class);
    this.redirectVelocityInfoToDebug = true;
  }

  public void init(final RuntimeServices srv) throws Exception {
    // nothing
  }

  public boolean isLevelEnabled(final int level) {
    switch (level) {
      case TRACE_ID:
        return logger.isTraceEnabled();
      case DEBUG_ID:
        return logger.isDebugEnabled();
      case INFO_ID:
        return redirectVelocityInfoToDebug ? logger.isDebugEnabled() : logger.isInfoEnabled();
      case WARN_ID:
        return logger.isWarnEnabled();
      case ERROR_ID:
        return logger.isErrorEnabled();
      default:
        return level > INFO_ID;
    }
  }

  public void log(final int level, final String msg) {
    switch (level) {
      case TRACE_ID:
        logger.trace(msg);
        break;
      case DEBUG_ID:
        logger.debug(msg);
        break;
      case INFO_ID:
        if (redirectVelocityInfoToDebug) {
          logger.debug(msg);
        }
        else {
          logger.info(msg);
        }
        break;
      case WARN_ID:
        logger.warn(msg);
        break;
      case ERROR_ID:
        logger.error(msg);
        break;
      default:
        logger.info(msg);
    }
  }

  public void log(final int level, final String msg, final Throwable t) {
    switch (level) {
      case TRACE_ID:
        logger.trace(msg, t);
        break;
      case DEBUG_ID:
        logger.debug(msg, t);
        break;
      case INFO_ID:
        if (redirectVelocityInfoToDebug) {
          logger.debug(msg, t);
        }
        else {
          logger.info(msg, t);
        }
        break;
      case WARN_ID:
        logger.warn(msg, t);
        break;
      case ERROR_ID:
        logger.error(msg, t);
        break;
      default:
        logger.info(msg, t);
    }
  }
}
