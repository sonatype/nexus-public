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
package org.sonatype.nexus.internal.commands;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.orient.freeze.DatabaseFreezeService;

import com.google.common.annotations.VisibleForTesting;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.internal.commands.DatabaseFreezeAction.Mode.enable;

/**
 * An action to freeze and release the nexus database.
 *
 * @since 3.2
 */
@Named
@Command(name = "freezeDb", scope = "nexus", description = "Freeze or release Nexus database")
public class DatabaseFreezeAction
    implements Action
{
  private final DatabaseFreezeService databaseFreezeService;

  @Option(name = "-m", aliases = {"--mode"}, description = "Manage mode: enable or release (default enable)")
  Mode mode = enable;

  @VisibleForTesting
  enum Mode
  {
    enable, release //NOSONAR
  }

  @Inject
  public DatabaseFreezeAction(final DatabaseFreezeService databaseFreezeService) {
    this.databaseFreezeService = checkNotNull(databaseFreezeService);
  }

  @Override
  public Object execute() throws Exception {
    switch (mode) { //NOSONAR
      case enable:
        databaseFreezeService.freezeAllDatabases();
        break;
      case release:
        databaseFreezeService.releaseAllDatabases();
        break;
    }
    return null;
  }
}

