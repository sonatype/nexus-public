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
package org.sonatype.nexus.plugins.bcprov.internal;

import java.security.Security;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.proxy.events.NexusStoppedEvent;
import org.sonatype.sisu.goodies.common.ComponentSupport;
import org.sonatype.sisu.goodies.eventbus.EventBus;

import com.google.common.eventbus.Subscribe;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.eclipse.sisu.EagerSingleton;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Guava {@link EventBus} handler that listens for Nexus events and performs BC provider registration/removal. This
 * component is marked as {@code EagerSingleton} to be created (and hence to have registration happen) as early as
 * possible, even before any wiring happens in plugins.
 *
 * @author cstamas
 * @since 2.4
 */
@Named
@EagerSingleton
public class BCPluginEventHandler
  extends ComponentSupport
{
  private final boolean registered;

  /**
   * Default constructor.
   *
   * @param eventBus the {@link EventBus} to register with.
   */
  @Inject
  public BCPluginEventHandler(final EventBus eventBus) {
    checkNotNull(eventBus);

    // register BC and nag if already installed
    registered = Security.addProvider(new BouncyCastleProvider()) != -1;
    if (registered) {
      log.debug("BouncyCastle security provider registered");
    }
    else {
      log.warn("BouncyCastle security provider already registered; could become problematic");
    }

    eventBus.register(this);
  }

  /**
   * {@link NexusStoppedEvent} handler: unregister BC provider if needed (if it was registered by us, not by some 3rd
   * party).
   *
   * @param e the event (not used)
   */
  @Subscribe
  public void onNexusStoppedEvent(final NexusStoppedEvent e) {
    if (registered) {
      Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME);
      log.debug("BouncyCastle security provider unregistered");
    }
  }
}
