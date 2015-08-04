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
package org.sonatype.nexus.proxy;

import java.io.IOException;
import java.net.ServerSocket;

import org.sonatype.nexus.test.NexusTestSupport;

import org.codehaus.plexus.context.Context;

public abstract class NexusProxyTestSupport
    extends NexusTestSupport
{

  public static final String PROXY_SERVER_PORT = "proxy.server.port";

  @Override
  protected void customizeContext(Context ctx) {
    super.customizeContext(ctx);

    ctx.put(PROXY_SERVER_PORT, String.valueOf(allocatePort()));
  }

  private int allocatePort() {
    ServerSocket ss;
    try {
      ss = new ServerSocket(0);
    }
    catch (IOException e) {
      return 0;
    }
    int port = ss.getLocalPort();
    try {
      ss.close();
    }
    catch (IOException e) {
      // does it matter?
      fail("Error allocating port " + e.getMessage());
    }
    return port;
  }

}
