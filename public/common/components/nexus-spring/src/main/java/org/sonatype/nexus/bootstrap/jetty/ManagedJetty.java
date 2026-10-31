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
package org.sonatype.nexus.bootstrap.jetty;

import org.sonatype.nexus.bootstrap.entrypoint.jetty.JettyServer;

import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import static java.util.Objects.requireNonNull;

/**
 * Ties into Springs lifecycle phase to manage Nexus startup & shutdown
 */
@Component
public class ManagedJetty
    implements SmartLifecycle
{
  public static final String SYSTEM_USERID = "*SYSTEM";

  private final JettyServer jetty;

  private final ApplicationContext context;

  private boolean started;

  @Autowired
  public ManagedJetty(final ApplicationContext context, final JettyServer jetty) {
    this.context = requireNonNull(context);
    this.jetty = requireNonNull(jetty);
  }

  @Override
  public void start() {
    try {
      MDC.put("userId", SYSTEM_USERID);
      jetty.start(context, true);
    }
    catch (Exception e) {
      if (e instanceof RuntimeException re) {
        throw re;
      }
      throw new RuntimeException(e);
    }
    started = true;
  }

  @Override
  public void stop() {
    try {
      jetty.stop();
    }
    catch (Exception e) {
      if (e instanceof RuntimeException re) {
        throw re;
      }
      throw new RuntimeException(e);
    }
    finally {
      MDC.remove("userId");
    }
    started = false;
  }

  @Override
  public int getPhase() {
    // give Nexus a low precedence to ensure any Spring core is already complete
    return Integer.MAX_VALUE;
  }

  @Override
  public boolean isRunning() {
    return started;
  }

}
