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

import javax.annotation.Nullable;

/**
 * Emitted when a {@link LoggerLevel} has been changed.
 *
 * @since 3.1
 */
public class LoggerLevelChangedEvent
{
  private final String logger;

  @Nullable
  private final LoggerLevel level;

  public LoggerLevelChangedEvent(final String logger, @Nullable final LoggerLevel level) {
    this.logger = logger;
    this.level = level;
  }

  public String getLogger() {
    return logger;
  }

  /**
   * Level may be null if the logger level was unset.
   */
  @Nullable
  public LoggerLevel getLevel() {
    return level;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" +
        "logger='" + logger + '\'' +
        ", level=" + level +
        '}';
  }
}
