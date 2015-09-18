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
package org.sonatype.nexus.internal.capability;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.capability.CapabilityRegistryEvent.Ready;
import org.sonatype.nexus.common.app.NexusInitializedEvent;
import org.sonatype.nexus.common.app.NexusStoppingEvent;
import org.sonatype.nexus.common.event.EventAware;
import org.sonatype.nexus.common.event.EventBus;
import org.sonatype.nexus.internal.capability.storage.OrientCapabilityStorage;

import com.google.common.base.Throwables;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Provider;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Loads configuration when Nexus is initialized.
 *
 * @since capabilities 2.0
 */
@Named
@Singleton
public class CapabilityRegistryBooter
  implements EventAware
{
  private final EventBus eventBus;

  private final Provider<DefaultCapabilityRegistry> capabilityRegistryProvider;

  private final Provider<OrientCapabilityStorage> capabilityStorageProvider;

  @Inject
  public CapabilityRegistryBooter(final EventBus eventBus,
                                  final Provider<DefaultCapabilityRegistry> capabilityRegistryProvider,
                                  final Provider<OrientCapabilityStorage> capabilityStorageProvider)
  {
    this.eventBus = checkNotNull(eventBus);
    this.capabilityRegistryProvider = checkNotNull(capabilityRegistryProvider);
    this.capabilityStorageProvider = checkNotNull(capabilityStorageProvider);
  }

  @Subscribe
  public void handle(final NexusInitializedEvent event) {
    try {
      capabilityStorageProvider.get().start();

      DefaultCapabilityRegistry registry = capabilityRegistryProvider.get();
      registry.load();

      // fire event when the registry is loaded and ready for use
      eventBus.post(new Ready(registry));
    }
    catch (final Exception e) {
      // fail hard with an error to stop further server activity
      throw new Error("Could not boot capabilities", e);
    }
  }

  @Subscribe
  public void handle(final NexusStoppingEvent event) {
    try {
      capabilityStorageProvider.get().stop();
    }
    catch (final Exception e) {
      throw Throwables.propagate(e);
    }
  }
}
