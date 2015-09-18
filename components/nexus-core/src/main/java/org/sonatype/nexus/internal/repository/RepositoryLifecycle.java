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
package org.sonatype.nexus.internal.repository;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.sonatype.goodies.lifecycle.LifecycleManagerImpl;
import org.sonatype.nexus.blobstore.api.BlobStoreManager;
import org.sonatype.nexus.common.app.NexusStartedEvent;
import org.sonatype.nexus.common.app.NexusStoppedEvent;
import org.sonatype.nexus.common.event.EventBus;
import org.sonatype.nexus.repository.config.ConfigurationStore;
import org.sonatype.nexus.repository.manager.RepositoryManager;

import com.google.common.eventbus.Subscribe;
import org.eclipse.sisu.EagerSingleton;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Repository lifecycle.
 *
 * @since 3.0
 */
@Named
@EagerSingleton
public class RepositoryLifecycle
    extends LifecycleManagerImpl
{
  private final EventBus eventBus;

  private final Provider<BlobStoreManager> blobStoreManager;

  private final Provider<ConfigurationStore> configurationStore;

  private final Provider<RepositoryManager> repositoryManager;

  @Inject
  public RepositoryLifecycle(final EventBus eventBus,
                             final Provider<BlobStoreManager> blobStoreManager,
                             final Provider<ConfigurationStore> configurationStore,
                             final Provider<RepositoryManager> repositoryManager)
  {
    this.eventBus = checkNotNull(eventBus);
    this.blobStoreManager = checkNotNull(blobStoreManager);
    this.configurationStore = checkNotNull(configurationStore);
    this.repositoryManager = checkNotNull(repositoryManager);

    eventBus.register(this);
  }

  @Subscribe
  public void on(final NexusStartedEvent event) throws Exception {
    add(blobStoreManager.get());
    add(configurationStore.get());
    add(repositoryManager.get());
    start();
  }

  @Subscribe
  public void on(final NexusStoppedEvent event) throws Exception {
    eventBus.unregister(this);

    stop();
    clear();
  }
}
