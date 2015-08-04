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
package org.sonatype.nexus.proxy.repository.threads;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.proxy.events.NexusStoppedEvent;
import org.sonatype.nexus.proxy.repository.GroupRepository;
import org.sonatype.nexus.proxy.repository.ProxyRepository;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.threads.NexusExecutorService;
import org.sonatype.nexus.threads.NexusThreadFactory;
import org.sonatype.nexus.util.SystemPropertiesHelper;
import org.sonatype.sisu.goodies.eventbus.EventBus;

import com.google.common.eventbus.Subscribe;

import static com.google.common.base.Preconditions.checkNotNull;

@Singleton
@Named
public class DefaultThreadPoolManager
    implements ThreadPoolManager
{
  private static final int GROUP_REPOSITORY_THREAD_POOL_SIZE = SystemPropertiesHelper.getInteger(
      "nexus.groupRepositoryThreadPoolSize", 200);

  private static final int PROXY_REPOSITORY_THREAD_POOL_SIZE = SystemPropertiesHelper.getInteger(
      "nexus.proxyRepositoryThreadPoolSize", 50);
  
  private final EventBus eventBus;

  private final NexusExecutorService groupRepositoryThreadPool;

  private final NexusExecutorService proxyRepositoryThreadPool;

  @Inject
  public DefaultThreadPoolManager(final EventBus eventBus) {
    this.eventBus = checkNotNull(eventBus);
    // direct hand-off used! Group pool will use caller thread to execute the task when full!
    final ThreadPoolExecutor gTarget =
        new ThreadPoolExecutor(0, GROUP_REPOSITORY_THREAD_POOL_SIZE, 60L, TimeUnit.SECONDS,
            new SynchronousQueue<Runnable>(), new NexusThreadFactory("group", "Group TPool"),
            new CallerRunsPolicy());

    // direct hand-off used! Proxy pool will use caller thread to execute the task when full!
    final ThreadPoolExecutor pTarget =
        new ThreadPoolExecutor(0, PROXY_REPOSITORY_THREAD_POOL_SIZE, 60L, TimeUnit.SECONDS,
            new SynchronousQueue<Runnable>(), new NexusThreadFactory("proxy", "Proxy TPool"),
            new CallerRunsPolicy());

    this.groupRepositoryThreadPool = NexusExecutorService.forCurrentSubject(gTarget);
    this.proxyRepositoryThreadPool = NexusExecutorService.forCurrentSubject(pTarget);
    eventBus.register(this);
  }

  @Override
  public ExecutorService getRepositoryThreadPool(Repository repository) {
    if (repository.getRepositoryKind().isFacetAvailable(GroupRepository.class)) {
      return groupRepositoryThreadPool;
    }
    else if (repository.getRepositoryKind().isFacetAvailable(ProxyRepository.class)) {
      return proxyRepositoryThreadPool;
    }
    else {
      return null;
    }
  }

  @Override
  public synchronized void createPool(Repository repository) {
    // nop for now
  }

  @Override
  public synchronized void removePool(Repository repository) {
    // nop for now
  }

  public synchronized void shutdown() {
    eventBus.unregister(this);
    terminatePool(groupRepositoryThreadPool);
    terminatePool(proxyRepositoryThreadPool);
  }

  @Subscribe
  public void on(final NexusStoppedEvent e) {
    shutdown();
  }

  // ==

  protected void terminatePool(final ExecutorService executorService) {
    if (executorService != null) {
      executorService.shutdownNow();
    }
  }
}
