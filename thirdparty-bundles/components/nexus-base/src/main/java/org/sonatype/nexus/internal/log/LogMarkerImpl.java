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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.common.log.LogManager;
import org.sonatype.nexus.common.log.LogMarkInsertedEvent;
import org.sonatype.nexus.common.log.LogMarker;
import org.sonatype.nexus.common.log.LoggerLevel;

import com.google.common.base.Strings;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Default implementation of {@link LogMarker}.
 * 
 * @since 3.1
 */
@Named
@Singleton
public class LogMarkerImpl
    extends ComponentSupport
    implements LogMarker
{
  private final LogManager logManager;

  private final EventManager eventManager;

  @Inject
  public LogMarkerImpl(final LogManager logManager, final EventManager eventManager) {
    this.logManager = checkNotNull(logManager);
    this.eventManager = checkNotNull(eventManager);
  }

  @Override
  public void markLog(final String message) {
    // ensure that level for marking logger is enabled
    LoggerLevel loggerLevel = logManager.getLoggerEffectiveLevel(log.getName());
    if (LoggerLevel.INFO.compareTo(loggerLevel) < 0) {
      logManager.setLoggerLevel(log.getName(), LoggerLevel.INFO);
    }

    String asterixes = Strings.repeat("*", message.length() + 4);
    log.info("\n{}\n* {} *\n{}", asterixes, message, asterixes);

    eventManager.post(new LogMarkInsertedEvent(message));
  }
}
