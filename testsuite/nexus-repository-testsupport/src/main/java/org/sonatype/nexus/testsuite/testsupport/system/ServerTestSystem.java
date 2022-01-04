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
package org.sonatype.nexus.testsuite.testsupport.system;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.httpfixture.server.api.Behaviour;
import org.sonatype.goodies.httpfixture.server.fluent.Server;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.common.net.PortAllocator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.commons.lang3.StringUtils.prependIfMissing;

@Named
@Singleton
public class ServerTestSystem
    extends TestSystemSupport
{
  private static final Logger log = LoggerFactory.getLogger(ServerTestSystem.class);

  List<Server> servers;

  @Inject
  public ServerTestSystem(final EventManager eventManager) {
    super(eventManager);
    servers = new ArrayList<>();
  }

  @Override
  protected void doAfter() {
    for (Server server : servers) {
      try {
        server.stop();
      }
      catch (Exception e) {
        log.error("Failed to stop server", e);
      }
    }
  }

  public Server createServer(final Map<String, Behaviour> behaviors) throws Exception {
    Server server = Server.withPort(PortAllocator.nextFreePort());
    for (Entry<String, Behaviour> entry : behaviors.entrySet()) {
      server = server.serve(prependIfMissing(entry.getKey(), "/")).withBehaviours(entry.getValue());
    }
    servers.add(server);
    server.start();
    return server;
  }
}
