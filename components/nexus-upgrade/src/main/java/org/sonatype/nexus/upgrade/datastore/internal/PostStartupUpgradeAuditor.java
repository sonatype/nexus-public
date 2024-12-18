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
package org.sonatype.nexus.upgrade.datastore.internal;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.app.ManagedLifecycle;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport;
import org.sonatype.nexus.common.upgrade.events.UpgradeEventSupport;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.TASKS;

/**
 * Stores auditable db upgrade events until after startup when auditing is enabled
 */
@Named
@Singleton
@ManagedLifecycle(phase = TASKS)
public class PostStartupUpgradeAuditor
    extends StateGuardLifecycleSupport
{
  private final Queue<UpgradeEventSupport> events = new ConcurrentLinkedQueue<>();

  private final EventManager eventManager;

  @Inject
  public PostStartupUpgradeAuditor(final EventManager eventManager) {
    this.eventManager = checkNotNull(eventManager);
  }

  @Override
  protected void doStart() {
    events.forEach(eventManager::post);
    events.clear();
  }

  public void post(final UpgradeEventSupport event) {
    if (!isStarted()) {
      events.add(event);
    }
    else {
      // The service has started which means that the auditor should work if enabled.
      eventManager.post(event);
    }
  }
}
