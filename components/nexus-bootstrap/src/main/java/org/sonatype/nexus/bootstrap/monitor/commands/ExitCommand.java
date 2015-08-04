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
package org.sonatype.nexus.bootstrap.monitor.commands;

import org.sonatype.nexus.bootstrap.ShutdownHelper;
import org.sonatype.nexus.bootstrap.monitor.CommandMonitorThread;

/**
 * Command to exit the JVM (via {@link ShutdownHelper#exit(int)}).
 *
 * @since 2.2
 */
public class ExitCommand
    implements CommandMonitorThread.Command
{

  public static final String NAME = "EXIT";

  @Override
  public String getId() {
    return NAME;
  }

  @Override
  public boolean execute() {
    ShutdownHelper.exit(666);

    throw new Error("Unreachable statement");
  }

}
