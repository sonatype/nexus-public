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
package org.sonatype.nexus.events;

import java.util.List;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.sonatype.nexus.threads.NexusExecutorService;
import org.sonatype.nexus.threads.NexusThreadFactory;
import org.sonatype.nexus.util.SystemPropertiesHelper;
import org.sonatype.sisu.goodies.common.ComponentSupport;
import org.sonatype.sisu.goodies.eventbus.EventBus;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A default host for {@link EventSubscriber}. This is an internal Nexus component and should not be used in
 * any plugin code, and hence, is subject of change without prior notice.
 *
 * @since 2.7.0
 */
@Named
@Singleton
public class EventSubscriberHost
    extends ComponentSupport
{
  private final int HOST_THREAD_POOL_SIZE = SystemPropertiesHelper.getInteger(
      EventSubscriberHost.class.getName() + ".poolSize", 500);

  private final EventBus eventBus;

  private final List<Provider<EventSubscriber>> eventSubscriberProviders;

  private final NexusExecutorService hostThreadPool;

  private final com.google.common.eventbus.AsyncEventBus asyncBus;

  @Inject
  public EventSubscriberHost(final EventBus eventBus, final List<Provider<EventSubscriber>> eventSubscriberProviders) {
    this.eventBus = checkNotNull(eventBus);
    this.eventSubscriberProviders = checkNotNull(eventSubscriberProviders);

    // direct hand-off used! Host pool will use caller thread to execute async inspectors when pool full!
    final ThreadPoolExecutor target =
        new ThreadPoolExecutor(0, HOST_THREAD_POOL_SIZE, 60L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>(),
            new NexusThreadFactory("esh", "Event Subscriber Host"), new CallerRunsPolicy());
    this.hostThreadPool = NexusExecutorService.forCurrentSubject(target);
    this.asyncBus = new com.google.common.eventbus.AsyncEventBus("esh-async", hostThreadPool);

    eventBus.register(this);
    log.info("Initialized");
  }

  public void startup() {
    log.info("Starting");
    for (Provider<EventSubscriber> eventSubscriberProvider : eventSubscriberProviders) {
      EventSubscriber es = null;
      try {
        es = eventSubscriberProvider.get();
        register(es);
      }
      catch (Exception e) {
        log.warn("Could not register {}", es, e);
      }
    }
  }

  public void shutdown() {
    eventBus.unregister(this);
    log.info("Stopping");

    for (Provider<EventSubscriber> eventSubscriberProvider : eventSubscriberProviders) {
      EventSubscriber es = null;
      try {
        es = eventSubscriberProvider.get();
        unregister(es);
      }
      catch (Exception e) {
        log.warn("Could not unregister {}", es, e);
      }
    }

    // we need clean shutdown, wait all background event inspectors to finish to have consistent state
    hostThreadPool.shutdown();
    try {
      hostThreadPool.awaitTermination(5L, TimeUnit.SECONDS);
    }
    catch (InterruptedException e) {
      log.debug("Interrupted while waiting for termination", e);
    }
  }

  public void register(final Object object) {
    if (object instanceof Asynchronous) {
      asyncBus.register(object);
    }
    else {
      eventBus.register(object);
    }
    log.trace("Registered {}", object);
  }

  public void unregister(final Object object) {
    if (object instanceof Asynchronous) {
      asyncBus.unregister(object);
    }
    else {
      eventBus.unregister(object);
    }
    log.trace("Unregistered {}", object);
  }

  /**
   * Used by UTs and ITs only, to "wait for calm period", when all the async event inspectors finished.
   */
  @VisibleForTesting
  public boolean isCalmPeriod() {
    // "calm period" is when we have no queued nor active threads
    return ((ThreadPoolExecutor) hostThreadPool.getTargetExecutorService()).getQueue().isEmpty()
        && ((ThreadPoolExecutor) hostThreadPool.getTargetExecutorService()).getActiveCount() == 0;
  }

  @Subscribe
  @AllowConcurrentEvents
  public void onEvent(final Object evt) {
    asyncBus.post(evt);
  }
}
