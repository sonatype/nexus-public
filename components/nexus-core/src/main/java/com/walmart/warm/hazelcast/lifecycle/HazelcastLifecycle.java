/**
 * Copyright (c) 2016-current Walmart, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package com.walmart.warm.hazelcast.lifecycle;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.events.EventSubscriber;
import org.sonatype.nexus.proxy.events.NexusStoppedEvent;

import com.google.common.eventbus.Subscribe;
import com.walmart.warm.hazelcast.HazelcastManager;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Lifecycle of {@link HazelcastManager} based on Nexus eventing.
 *
 * @since 1.2.14
 */
@Named
@Singleton
public class HazelcastLifecycle
    implements EventSubscriber
{
  private final HazelcastManager hazelcastManager;

  @Inject
  public HazelcastLifecycle(final HazelcastManager hazelcastManager) {
    this.hazelcastManager = checkNotNull(hazelcastManager);
  }

  @Subscribe
  public void on(final NexusStoppedEvent event) {
    hazelcastManager.shutdown();
  }
}
