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

import java.util.Map;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.gossip.Level;
import org.sonatype.nexus.extdirect.DirectComponentSupport;

import com.google.common.collect.ImmutableMap;
import com.softwarementors.extjs.djn.config.annotations.DirectAction;
import com.softwarementors.extjs.djn.config.annotations.DirectMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * LogEvent component.
 *
 * @since 3.0
 */
@Named
@Singleton
@DirectAction(action = "rapture_LogEvent")
public class LogEventComponent
    extends DirectComponentSupport
{
  private static final Map<String, Level> levels = ImmutableMap.of(
      "trace", Level.TRACE,
      "debug", Level.DEBUG,
      "info", Level.INFO,
      "warn", Level.WARN,
      "error", Level.ERROR
  );

  @DirectMethod
  public void recordEvent(final LogEventXO event) {
    checkNotNull(event);

    Level level = levels.get(event.getLevel());
    checkState(level != null, "Invalid level: %s", event.getLevel());

    Logger logger = LoggerFactory.getLogger(event.getLogger());
    level.log(logger, event.getMessage());
  }
}
