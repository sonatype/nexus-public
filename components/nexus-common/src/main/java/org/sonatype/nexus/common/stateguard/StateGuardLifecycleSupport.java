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
package org.sonatype.nexus.common.stateguard;

import javax.annotation.Nonnull;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.goodies.lifecycle.Lifecycle;

import static org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport.State.FAILED;
import static org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport.State.NEW;
import static org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport.State.STARTED;
import static org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport.State.STOPPED;

/**
 * {@link StateGuard} implementation of {@link Lifecycle}.
 *
 * @since 3.0
 */
public class StateGuardLifecycleSupport
    extends ComponentSupport
    implements Lifecycle, StateGuardAware
{
  /**
   * State constants.
   */
  public static final class State
  {
    private State() {
      // empty
    }

    public static final String NEW = "NEW";

    public static final String STARTED = "STARTED";

    public static final String STOPPED = "STOPPED";

    public static final String FAILED = "FAILED";
  }

  protected final StateGuard states = new StateGuard.Builder()
      .logger(createLogger())
      .initial(NEW)
      .failure(FAILED)
      .create();

  @Nonnull
  @Override
  public StateGuard getStateGuard() {
    return states;
  }

  @Override
  @Transitions(from = {NEW, STOPPED, FAILED}, to = STARTED)
  public void start() throws Exception {
    doStart();
  }

  protected void doStart() throws Exception {
    // nop
  }

  /**
   * @since 3.2.1
   */
  public boolean isStarted() {
    return getStateGuard().is(STARTED);
  }

  @Override
  @Transitions(from = STARTED, to = STOPPED)
  public void stop() throws Exception {
    doStop();
  }

  protected void doStop() throws Exception {
    // nop
  }
}
