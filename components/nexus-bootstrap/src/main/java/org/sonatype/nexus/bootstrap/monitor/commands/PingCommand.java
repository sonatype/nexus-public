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

import org.sonatype.nexus.bootstrap.log.LogProxy;
import org.sonatype.nexus.bootstrap.monitor.CommandMonitorThread;

/**
 * Responds to pings (by doing nothing).
 *
 * @since 2.2
 */
public class PingCommand
    implements CommandMonitorThread.Command
{

  private static final LogProxy log = LogProxy.getLogger(PingCommand.class);

  public static final String NAME = "PING";

  @Override
  public String getId() {
    return NAME;
  }

  @Override
  public boolean execute() {
    log.debug("Pinged");
    return false;
  }

}
