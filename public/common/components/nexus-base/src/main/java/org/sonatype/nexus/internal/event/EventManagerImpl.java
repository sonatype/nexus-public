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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;

import org.sonatype.nexus.common.lifecycle.LifecycleSupport;
import org.sonatype.nexus.common.app.ManagedLifecycle;
import org.sonatype.nexus.common.event.EventAware.Asynchronous;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.common.event.HasAffinity;
import org.sonatype.nexus.common.property.SystemPropertiesHelper;
import org.sonatype.nexus.jmx.reflect.ManagedAttribute;
import org.sonatype.nexus.jmx.reflect.ManagedObject;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.eventbus.EventBus;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import static java.util.Objects.requireNonNull;
import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.EVENTS;
import static org.sonatype.nexus.common.event.EventBusFactory.reentrantAsyncEventBus;
import static org.sonatype.nexus.common.event.EventBusFactory.reentrantEventBus;

@Component
@ManagedLifecycle(phase = EVENTS)
// Start first within the EVENTS phase so the manager is available before NexusEventAwareRegistrar registers
// subscribers; via the reversed shutdown order this also stops it last, after subscribers are unregistered.
@Order(Ordered.HIGHEST_PRECEDENCE)
@ManagedObject(typeClass = EventManager.class)
public class EventManagerImpl
    extends LifecycleSupport
    implements EventManager
{
  static final int HOST_THREAD_POOL_SIZE =
      SystemPropertiesHelper.getInteger("org.sonatype.nexus.internal.event.EventManagerImpl.poolSize", 500);

  private final EventExecutor eventExecutor;

  private final EventBus eventBus;

  private final EventBus asyncBus;

  /**
   * Tally of events dropped because they were posted before the EVENTS phase started, keyed by event class
   * name. Accumulated by {@link #post} while the manager has not yet started and flushed as a single summary
   * log line by {@link #doStart}, so a full-fleet deploy no longer emits one WARN per dropped event.
   */
  private final Map<String, Long> preStartDropTally = new ConcurrentHashMap<>();

  /**
   * Set on the first {@link #doStart}. Distinguishes pre-start drops (accumulated into {@link #preStartDropTally}
   * and summarized at start) from post-shutdown drops (logged at DEBUG only, with no later flush point).
   * Intentionally never reset: this managed-lifecycle component starts once per JVM, so it gates both the one-time
   * summary flush and post-start tallying for the lifetime of the process.
   */
  private volatile boolean everStarted;

  @Autowired
  public EventManagerImpl(final EventExecutor eventExecutor) {
    this.eventExecutor = requireNonNull(eventExecutor);

    this.eventBus = reentrantEventBus("nexus");
    this.asyncBus = reentrantAsyncEventBus("nexus.async", eventExecutor);
  }

  @Override
  protected void doStart() throws Exception {
    // Flush the pre-EVENTS drop tally exactly once, on the first start. The everStarted guard makes this a no-op
    // on any later restart (so a stale tally is never re-logged), and it also gates post() so no further entries
    // accumulate afterwards. The tally is never cleared: a tally that races in just after this runs simply sits
    // harmlessly in the bounded map (one entry per event class) instead of surviving a clear. Every pre-EVENTS
    // publisher posts either on the boot thread or on worker threads its own earlier phase joins before the
    // lifecycle manager advances to EVENTS, so all pre-start posts happen-before this flush and the count is exact.
    if (!everStarted) {
      everStarted = true;
      if (!preStartDropTally.isEmpty()) {
        long total = preStartDropTally.values().stream().mapToLong(Long::longValue).sum();
        log.info("EventManager started; {} event(s) posted before the EVENTS phase were dropped "
            + "(expected for pre-EVENTS lifecycle publishers): {}", total, preStartDropTally);
      }
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
    // The EventManager is only usable once it has started (the EVENTS lifecycle phase). Posting earlier
    // (or after shutdown) is a lifecycle violation: the subscribers are not registered yet, so rather than
    // deliver to an incomplete bus we drop the event without failing the caller. These drops are benign --
    // every known pre-EVENTS publisher either re-reads authoritative state at its own phase or emits an
    // audit-only event -- so we log each at DEBUG and aggregate them into a single INFO summary when the
    // phase starts (see doStart), instead of emitting one WARN each (a full-fleet deploy logged hundreds).
    if (!isStarted()) {
      String type = event == null ? "null" : event.getClass().getName();
      log.debug("EventManager is not started; dropping event posted outside the EVENTS phase: {}", type);
      if (!everStarted) {
        preStartDropTally.merge(type, 1L, Long::sum);
      }
      return;
    }

    // notify synchronous subscribers before going asynchronous
    eventBus.post(event);

    if (isAffinityEnabled() && event instanceof HasAffinity) {
      String affinity = ((HasAffinity) event).getAffinity();
      if (affinity != null) {
        eventExecutor.executeWithAffinity(affinity, () -> asyncBus.post(event));
      }
      else {
        // unexpected state, fall back to previous behaviour
        log.warn("Event {} requested 'null' affinity", event);
        asyncBus.post(event);
      }
    }
    else {
      asyncBus.post(event);
    }
  }

  @Override
  @VisibleForTesting
  @ManagedAttribute
  public boolean isCalmPeriod() {
    return eventExecutor.isCalmPeriod();
  }

  @Override
  public boolean isAffinityEnabled() {
    return eventExecutor.isAffinityEnabled();
  }
}
