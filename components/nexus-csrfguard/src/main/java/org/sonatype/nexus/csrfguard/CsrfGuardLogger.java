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
package org.sonatype.nexus.csrfguard;

import org.owasp.csrfguard.CsrfGuard;
import org.owasp.csrfguard.log.ILogger;
import org.owasp.csrfguard.log.LogLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CSRF Guard Logging adapter.
 *
 * @since 2.9
 */
public class CsrfGuardLogger
    implements ILogger
{
  private static final Logger log = LoggerFactory.getLogger(CsrfGuard.class);

  @Override
  public void log(final String msg) {
    log.debug(msg);
  }

  @Override
  public void log(final LogLevel level, final String msg) {
    switch (level) {
      case Trace:
        log.trace(msg);
      case Debug:
        log.debug(msg);
      case Info:
        log.info(msg);
      case Warning:
        log.warn(msg);
      case Error:
      case Fatal:
        log.error(msg);
    }
  }

  @Override
  public void log(final Exception exception) {
    log.error(exception.getMessage(), exception);
  }

  @Override
  public void log(final LogLevel level, final Exception exception) {
    switch (level) {
      case Trace:
        log.trace(exception.getMessage(), exception);
      case Debug:
        log.debug(exception.getMessage(), exception);
      case Info:
        log.info(exception.getMessage(), exception);
      case Warning:
        log.warn(exception.getMessage(), exception);
      case Error:
      case Fatal:
        log.error(exception.getMessage(), exception);
    }
  }
}
