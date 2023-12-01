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
package org.sonatype.nexus.coreui.internal.log;

import java.util.Map;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import org.sonatype.nexus.common.log.LoggerLevel;

/**
 * Logger exchange object.
 */
public class LoggerXO
{
  @NotEmpty
  private String name;

  @NotNull
  private LoggerLevel level;

  private boolean override;

  public String getName() {
    return name;
  }

  public void setName(final String name) {
    this.name = name;
  }

  public LoggerLevel getLevel() {
    return level;
  }

  public void setLevel(final LoggerLevel level) {
    this.level = level;
  }

  public boolean isOverride() {
    return override;
  }

  public void setOverride(final boolean override) {
    this.override = override;
  }

  @Override
  public String toString() {
    return "LoggerXO(" +
        "name:" + name +
        ", level:" + level +
        ", override:" + override +
        ")";
  }

  public static LoggerXO fromEntry(Map.Entry<String, LoggerLevel> entry) {
    LoggerXO loggerXO = new LoggerXO();
    loggerXO.setName(entry.getKey());
    loggerXO.setLevel(entry.getValue());
    return loggerXO;
  }
}
