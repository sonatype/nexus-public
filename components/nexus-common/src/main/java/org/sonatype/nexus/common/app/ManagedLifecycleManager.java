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
package org.sonatype.nexus.common.app;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.app.ManagedLifecycle.Phase;

/**
 * Manages {@link ManagedLifecycle} components.
 *
 * @since 3.3
 */
public abstract class ManagedLifecycleManager
    extends ComponentSupport
{
  /**
   * Returns the current phase.
   */
  public abstract Phase getCurrentPhase();

  /**
   * Attempts to move to the target phase by starting (or stopping) components phase-by-phase. If any components have
   * appeared since the last request which belong to the current phase or earlier then they are automatically started
   * before the current phase is changed. Similarly components that have disappeared are stopped.
   */
  public abstract void to(final Phase targetPhase) throws Exception;

  /**
   * Attempts to bounce the given phase by moving the lifecycle just before it then back towards the current phase,
   * re-running all the phases in between. If the bounce phase is after the current phase then it simply moves the
   * lifecycle forwards like {@link #to(Phase)}.
   *
   * @since 3.next
   */
  public abstract void bounce(final Phase bouncePhase) throws Exception;

  /**
   * Are we in the process of shutting down? (ie. moving to the {@code OFF} phase)
   *
   * @since 3.next
   */
  public static boolean isShuttingDown() {
    return shuttingDown;
  }

  private static volatile boolean shuttingDown;

  protected ManagedLifecycleManager() {
    shuttingDown = false;
  }

  /**
   * Flag that we are in the process of shutting down.
   *
   * @since 3.next
   */
  protected void declareShutdown() {
    log.info("Shutting down");
    shuttingDown = true;
  }
}
