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
package org.sonatype.nexus.internal.event;

import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.lifecycle.LifecycleSupport;
import org.sonatype.nexus.common.app.ManagedLifecycle;
import org.sonatype.nexus.common.event.EventAware;
import org.sonatype.nexus.common.event.EventAware.Asynchronous;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.common.property.SystemPropertiesHelper;
import org.sonatype.nexus.jmx.reflect.ManagedAttribute;
import org.sonatype.nexus.jmx.reflect.ManagedObject;
import org.sonatype.nexus.thread.NexusExecutorService;
import org.sonatype.nexus.thread.NexusThreadFactory;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.ReentrantAsyncEventBus;
import com.google.common.eventbus.ReentrantEventBus;
import com.google.inject.Key;
import org.eclipse.sisu.BeanEntry;
import org.eclipse.sisu.Mediator;
import org.eclipse.sisu.inject.BeanLocator;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.EVENTS;

/**
 * Default {@link EventManager}.
 */
@Named
@ManagedLifecycle(phase = EVENTS)
@ManagedObject(typeClass=EventManager.class)
@Singleton
public class EventManagerImpl
    extends LifecycleSupport
    implements EventManager
{
  private static final int HOST_THREAD_POOL_SIZE = SystemPropertiesHelper.getInteger(
      EventManagerImpl.class.getName() + ".poolSize", 500);

  private final BeanLocator beanLocator;

  private final EventBus eventBus;

  private final ThreadPoolExecutor threadPool;

  private final EventBus asyncBus;

  @Inject
  public EventManagerImpl(final BeanLocator beanLocator)
  {
    this.beanLocator = checkNotNull(beanLocator);
    this.eventBus = new ReentrantEventBus();

    // direct hand-off used! Host pool will use caller thread to execute async inspectors when pool full!
    this.threadPool = new ThreadPoolExecutor(
        0,
        HOST_THREAD_POOL_SIZE,
        60L,
        TimeUnit.SECONDS,
        new SynchronousQueue<>(),
        new NexusThreadFactory("event", "event-manager"),
        new CallerRunsPolicy()
    );

    this.asyncBus = new ReentrantAsyncEventBus(new EventExecutor(NexusExecutorService.forCurrentSubject(threadPool)));
  }

  /**
   * Mediator to register and unregister {@link EventAware} components.
   */
  private static class EventAwareMediator
    implements Mediator<Named, EventAware, EventManagerImpl>
  {
    @Override
    public void add(final BeanEntry<Named, EventAware> entry, final EventManagerImpl watcher) {
      watcher.register(entry.getValue());
    }

    @Override
    public void remove(final BeanEntry<Named, EventAware> entry, final EventManagerImpl watcher) {
      watcher.unregister(entry.getValue());
    }
  }

  @Override
  protected void doStart() throws Exception {
    // watch for EventSubscriber components and register/unregister them
    beanLocator.watch(Key.get(EventAware.class, Named.class), new EventAwareMediator(), this);
  }

  @Override
  protected void doStop() throws Exception {
    // we need clean shutdown, wait all background event inspectors to finish to have consistent state
    threadPool.shutdown();
    try {
      threadPool.awaitTermination(5L, TimeUnit.SECONDS);
    }
    catch (InterruptedException e) {
      log.debug("Interrupted while waiting for termination", e);
    }
  }

  @Override
  public void register(final Object object) {
    boolean async = object instanceof Asynchronous;

    if (async) {
      asyncBus.register(object);
    }
    else {
      eventBus.register(object);
    }

    log.trace("Registered {}{}", async ? "ASYNC " : "", object);
  }

  @Override
  public void unregister(final Object object) {
    boolean async = object instanceof Asynchronous;

    if (async) {
      asyncBus.unregister(object);
    }
    else {
      eventBus.unregister(object);
    }

    log.trace("Unregistered {}{}", async ? "ASYNC " : "", object);
  }

  @Override
  public void post(final Object event) {
    // notify synchronous subscribers before going asynchronous
    eventBus.post(event);
    asyncBus.post(event);
  }

  /**
   * Used by UTs and ITs only, to "wait for calm period", when all the async event inspectors finished.
   */
  @Override
  @VisibleForTesting
  @ManagedAttribute
  public boolean isCalmPeriod() {
    // "calm period" is when we have no queued nor active threads
    return threadPool.getQueue().isEmpty() && threadPool.getActiveCount() == 0;
  }
}
