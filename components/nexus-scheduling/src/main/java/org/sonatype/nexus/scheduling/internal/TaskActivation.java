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
package org.sonatype.nexus.scheduling.internal;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.lifecycle.LifecycleSupport;
import org.sonatype.nexus.common.app.ManagedLifecycle;
import org.sonatype.nexus.scheduling.spi.SchedulerSPI;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.TASKS;

/**
 * Manages activation/passivation of the scheduler.
 *
 * @since 3.0
 */
@Named
@ManagedLifecycle(phase = TASKS)
@Singleton
public class TaskActivation
    extends LifecycleSupport
{
  private final SchedulerSPI scheduler;

  @Inject
  public TaskActivation(final SchedulerSPI scheduler) {
    this.scheduler = checkNotNull(scheduler);
  }

  @Override
  protected void doStart() throws Exception {
    scheduler.resume();
  }

  @Override
  protected void doStop() throws Exception {
    scheduler.pause();
  }
}
