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
package org.sonatype.nexus.rapture.internal.logging;

import org.sonatype.nexus.extdirect.DirectComponentSupport;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.softwarementors.extjs.djn.config.annotations.DirectAction;
import com.softwarementors.extjs.djn.config.annotations.DirectMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * LogEvent component.
 *
 * @since 3.0
 */
@Component
@DirectAction(action = "rapture_LogEvent")
public class LogEventComponent
    extends DirectComponentSupport
{
  private final boolean enabled;

  @Autowired
  public LogEventComponent(
      @Value("${nexus.log.extdirect.recording.enabled:false}") final boolean enabled)
  {
    this.enabled = enabled;
  }

  @DirectMethod
  @Timed
  @ExceptionMetered
  public void recordEvent(final LogEventXO event) {
    if (!enabled) {
      return;
    }

    checkNotNull(event);

    String level = event.getLevel();
    checkState(level != null, "Invalid level");

    Logger logger = LoggerFactory.getLogger(event.getLogger());
    switch (level) {
      case "trace":
        logger.trace(event.getMessage());
        break;
      case "debug":
        logger.debug(event.getMessage());
        break;
      case "info":
        logger.info(event.getMessage());
        break;
      case "warn":
        logger.warn(event.getMessage());
        break;
      case "error":
        logger.error(event.getMessage());
        break;
      default:
        checkState(false, "Invalid level: %s", level);
    }
  }
}
