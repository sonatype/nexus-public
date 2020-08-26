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
package org.sonatype.nexus.datastore.mybatis.internal;

import java.io.File;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.app.ApplicationDirectories;
import org.sonatype.nexus.common.app.ManagedLifecycle;
import org.sonatype.nexus.common.event.EventAware;
import org.sonatype.nexus.common.node.NodeAccess;
import org.sonatype.nexus.common.stateguard.Guarded;
import org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport;

import org.h2.tools.Server;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.TASKS;
import static org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport.State.STARTED;

/**
 * Enable H2 web console.
 *
 * @since 3.27
 */
@Named
@Singleton
@ManagedLifecycle(phase = TASKS)
public class H2WebConsole
    extends StateGuardLifecycleSupport
    implements EventAware, EventAware.Asynchronous
{
  private final File databasesDir;

  private final boolean httpListenerEnabled;

  private final int httpListenerPort;

  private Server h2Server;

  @Inject
  public H2WebConsole(
      final ApplicationDirectories applicationDirectories,
      @Named("${nexus.h2.httpListenerEnabled:-false}") final boolean httpListenerEnabled,
      @Named("${nexus.h2.httpListenerPort:-8082}") final int httpListenerPort,
      final NodeAccess nodeAccess)
  {
    checkNotNull(applicationDirectories);
    this.httpListenerEnabled = httpListenerEnabled;
    this.httpListenerPort = httpListenerPort;
    databasesDir = applicationDirectories.getWorkDirectory("db");
  }

  @Override
  protected void doStart() throws Exception {
    if (httpListenerEnabled) {
      h2Server = Server.createWebServer("-webPort", String.valueOf(httpListenerPort), "-webAllowOthers", "-ifExists",
          "-baseDir", databasesDir.toString()).start();
      log.info("Activated");
    }
  }

  @Override
  protected void doStop() throws Exception {
    if (h2Server != null) {
      // instance shutdown
      h2Server.shutdown();
      h2Server = null;

      log.info("Shutdown");
    }
  }

  @Guarded(by = STARTED)
  public Server getH2Server() {
    return h2Server;
  }
}
