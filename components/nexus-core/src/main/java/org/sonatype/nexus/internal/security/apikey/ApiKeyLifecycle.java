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
package org.sonatype.nexus.internal.security.apikey;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.sonatype.goodies.lifecycle.LifecycleManagerImpl;
import org.sonatype.nexus.common.app.NexusStartedEvent;
import org.sonatype.nexus.common.app.NexusStoppedEvent;
import org.sonatype.nexus.common.event.EventAware;
import org.sonatype.nexus.security.authc.apikey.ApiKeyStore;

import com.google.common.eventbus.Subscribe;
import org.eclipse.sisu.EagerSingleton;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Manages the lifecycle for the {@link ApiKeyStore}.
 *
 * @since 3.0
 */
@Named
@EagerSingleton
public class ApiKeyLifecycle
    extends LifecycleManagerImpl
    implements EventAware
{
  private final Provider<ApiKeyStore> keyStore;

  @Inject
  public ApiKeyLifecycle(final Provider<ApiKeyStore> keyStore) {
    this.keyStore = checkNotNull(keyStore);
  }

  @Subscribe
  public void on(final NexusStartedEvent event) throws Exception {
    add(keyStore.get());
    start();
  }

  @Subscribe
  public void on(final NexusStoppedEvent event) throws Exception {
    stop();
    clear();
  }
}
