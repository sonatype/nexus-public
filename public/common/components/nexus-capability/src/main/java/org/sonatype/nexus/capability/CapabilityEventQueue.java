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
package org.sonatype.nexus.capability;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.annotation.Priority;

import org.sonatype.nexus.common.app.ManagedLifecycle;
import org.sonatype.nexus.common.app.ManagedLifecycle.Phase;
import org.sonatype.nexus.common.event.EventHelper;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.common.stateguard.Guarded;
import org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport;
import org.sonatype.nexus.thread.NexusThreadFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport.State.STARTED;

/**
 * An event broadcasting queue for Capability state change events. These are handled asynchronously to avoid the
 * capability registry from holding a lock while event handlers are performing other actions.
 */
@Priority(Integer.MAX_VALUE)
@ManagedLifecycle(phase = Phase.CAPABILITIES)
@Component
public class CapabilityEventQueue
    extends StateGuardLifecycleSupport
{
  private final EventManager eventManager;

  private ExecutorService eventQueue;

  @Autowired
  public CapabilityEventQueue(final EventManager eventManager) {
    this.eventManager = checkNotNull(eventManager);
  }

  @Override
  protected void doStart() {
    eventQueue = Executors.newSingleThreadExecutor(new NexusThreadFactory("capability-event", "capability-event"));
  }

  @Override
  protected void doStop() throws Exception {
    if (eventQueue != null) {
      eventQueue.shutdown();
      try {
        eventQueue.awaitTermination(1, TimeUnit.MINUTES);
      }
      catch (Exception e) {
        eventQueue.shutdownNow();
        if (e instanceof InterruptedException) {
          Thread.currentThread().interrupt();
        }
        throw e;
      }
    }
  }

  @Guarded(by = STARTED)
  public void post(final CapabilityEvent event) {
    if (EventHelper.isReplicating()) {
      eventQueue.submit(() -> EventHelper.asReplicating(() -> eventManager.post(event)));
    }
    else {
      eventQueue.submit(() -> eventManager.post(event));
    }
  }
}
