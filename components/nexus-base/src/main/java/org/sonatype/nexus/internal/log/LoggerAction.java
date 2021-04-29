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

import org.sonatype.nexus.common.log.LogManager;
import org.sonatype.nexus.common.log.LoggerLevel;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.Option;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.Boolean.TRUE;

/**
 * Action to set or display logger level.
 *
 * @since 3.0
 */
@Named
@Command(name = "logger", scope = "nexus", description = "Set or display logger level")
public class LoggerAction
    implements Action
{
  private final LogManager logManager;

  @Option(name = "-d", aliases = {"--delete"}, description = "Delete logger")
  private Boolean delete;

  @Option(name = "-e", aliases = {"--effective"}, description = "Return effective logger level")
  private Boolean effective;

  // FIXME: Presently a strict flag is set on some Karaf stuff related to completion, and
  // FIXME: ... unless you use the logger-name completer, then the level completion will not get picked up
  // FIXME: ... unsure where, but looks like if a completer fails, all completers after it are ignored

  @Argument(name = "name", index = 0, required = true, description = "Logger name")
  @Completion(LoggerNameCompleter.class)
  private String name;

  @Argument(name = "level", index = 1, description = "Logger level")
  private LoggerLevel level;

  @Inject
  public LoggerAction(final LogManager logManager) {
    this.logManager = checkNotNull(logManager);
  }

  @Override
  public Object execute() throws Exception {
    if (TRUE.equals(delete)) {
      logManager.unsetLoggerLevel(name);
    }
    else if (level != null) {
      logManager.setLoggerLevel(name, level);
    }
    else {
      if (TRUE.equals(effective)) {
        level = logManager.getLoggerEffectiveLevel(name);
      }
      else {
        level = logManager.getLoggerLevel(name);
      }

      if (level != null) {
        System.out.println(name + " = " + level.toString());
      }
      else {
        System.out.println(name + " is not set");
      }
    }

    return null;
  }
}
