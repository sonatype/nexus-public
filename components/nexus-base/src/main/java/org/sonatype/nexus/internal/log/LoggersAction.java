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

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.support.table.ShellTable;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.Boolean.TRUE;

/**
 * Action to display configured loggers.
 *
 * @since 3.0
 */
@Named
@Command(name = "loggers", scope = "nexus", description = "Display loggers")
public class LoggersAction
    implements Action
{
  private final LogManager logManager;

  @Option(name = "-r", aliases = {"--reset"}, description = "Reset loggers")
  private Boolean reset;

  @Inject
  public LoggersAction(final LogManager logManager) {
    this.logManager = checkNotNull(logManager);
  }

  @Override
  public Object execute() throws Exception {
    if (TRUE.equals(reset)) {
      logManager.resetLoggers();
    }
    else {
      final ShellTable table = new ShellTable();
      table.column("Name");
      table.column("Level").alignRight();

      logManager.getLoggers()
          .keySet()
          .stream()
          .sorted()
          .forEach(key -> table.addRow().addContent(key, logManager.getLoggers().get(key)));

      table.print(System.out);
    }

    return null;
  }
}
