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

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

import org.sonatype.nexus.common.app.ManagedComponentRegistrar;
import org.sonatype.nexus.common.app.ManagedLifecycle;
import org.sonatype.nexus.common.event.EventAware;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.common.lifecycle.LifecycleSupport;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.EVENTS;

/**
 * Registers singleton {@link EventAware} beans with the {@link EventManager}, replacing both the former
 * {@code EventAwareBeanPostProcessor} and the {@code List<EventAware>} constructor injection on
 * {@code EventManagerImpl} (which forced every {@link EventAware} bean to be constructed just to build the
 * {@code EventManager}).
 *
 * <p>
 * Registration is <em>phase-accurate</em>: a subscriber is placed on the bus exactly when its owning component
 * becomes active, so a handler can never receive an event before the component has started (and stops receiving
 * them before it shuts down) &mdash; without each subscriber having to guard its {@code @Subscribe} methods.
 * Two paths achieve this:
 * <ul>
 * <li><b>Bulk, in the {@link ManagedLifecycle.Phase#EVENTS EVENTS} phase</b> ({@link #doStart()}): beans that
 * are already active by the time the {@code EventManager} exists &mdash; those that are not lifecycle-managed
 * at all, plus those managed in a phase at or before EVENTS &mdash; are registered here.</li>
 * <li><b>Per-component, via {@link ManagedComponentRegistrar}</b>: beans managed in a phase <em>after</em> EVENTS
 * are deferred and registered by {@link #onStarted(Object)} when the lifecycle manager starts them, and
 * unregistered by {@link #onStopping(Object)} just before it stops them.</li>
 * </ul>
 *
 * <p>
 * {@code includeNonSingletons=false} excludes prototype-scoped subscribers (e.g. repository {@code Facet}s,
 * which register and unregister themselves per-repository via {@code FacetSupport}).
 */
@Component
@ManagedLifecycle(phase = EVENTS)
// Start last within the EVENTS phase so EventManagerImpl (and any other EVENTS-phase bean) is already started
// before subscribers are registered; via the reversed shutdown order this also unregisters them first, while
// the EventManager is still running.
@Order(Ordered.LOWEST_PRECEDENCE)
public class NexusEventAwareRegistrar
    extends LifecycleSupport
    implements ManagedComponentRegistrar
{
  private final ApplicationContext applicationContext;

  private final EventManager eventManager;

  private final Set<Object> registered =
      Collections.synchronizedSet(Collections.newSetFromMap(new IdentityHashMap<>()));

  @Autowired
  public NexusEventAwareRegistrar(
      final ApplicationContext applicationContext,
      final EventManager eventManager)
  {
    this.applicationContext = checkNotNull(applicationContext);
    this.eventManager = checkNotNull(eventManager);
  }

  @Override
  protected void doStart() {
    Map<String, EventAware> eventAwareBeans = applicationContext.getBeansOfType(EventAware.class, false, true);
    // Register only the beans that are already active by now; beans managed after EVENTS are deferred to
    // onStarted() so they join the bus exactly when their own phase starts.
    eventAwareBeans.values()
        .stream()
        .filter(bean -> !isManagedAfterEvents(bean))
        .forEach(this::doRegister);
    log.debug("Registered {} EventAware subscriber(s) during {} phase", registered.size(), EVENTS);
  }

  @Override
  public void onStarted(final Object component) {
    // Ignore callbacks that arrive before this registrar itself has started (i.e. components in phases at or before
    // EVENTS): those are handled by the bulk pass in doStart(), and the EventManager may not be available yet.
    if (isStarted() && component instanceof EventAware) {
      doRegister(component);
    }
  }

  @Override
  public void onStopping(final Object component) {
    // Withdraw a later-phase subscriber just before its component stops. Guarded by set membership so it is
    // idempotent and a no-op for beans already swept by doStop() (or never registered).
    if (component instanceof EventAware && registered.remove(component)) {
      safeUnregister(component);
    }
  }

  @Override
  protected void doStop() {
    // NexusLifecycleManager serializes doStart/doStop, so this never runs concurrently with doRegister; the
    // monitor is held only to make the iterate-then-clear over the synchronizedSet a single atomic operation.
    synchronized (registered) {
      registered.forEach(this::safeUnregister);
      registered.clear();
    }
  }

  private void doRegister(final Object bean) {
    if (registered.add(bean)) {
      try {
        eventManager.register(bean);
        log.trace("Registered EventAware subscriber: {}", bean.getClass().getName());
      }
      catch (Exception e) {
        registered.remove(bean);
        log.warn("Failed to register EventAware subscriber: {}", bean.getClass().getName(), e);
      }
    }
  }

  private void safeUnregister(final Object bean) {
    try {
      eventManager.unregister(bean);
    }
    catch (Exception e) {
      log.warn("Failed to unregister EventAware subscriber: {}", bean.getClass().getName(), e);
    }
  }

  /**
   * Is this bean lifecycle-managed in a phase strictly after {@link ManagedLifecycle.Phase#EVENTS}? Such beans are
   * deferred to {@link #onStarted(Object)}; everything else (non-lifecycle beans, and beans managed at or before
   * EVENTS) is registered in {@link #doStart()}.
   */
  private static boolean isManagedAfterEvents(final Object bean) {
    // Mirror NexusLifecycleManager: unwrap the Spring CGLIB subclass so the annotation is read from the user class.
    Class<?> clazz = bean.getClass();
    Class<?> impl = clazz.toString().contains("$$SpringCGLIB$$") ? clazz.getSuperclass() : clazz;
    ManagedLifecycle managed = impl.getAnnotation(ManagedLifecycle.class);
    return managed != null && managed.phase().ordinal() > EVENTS.ordinal();
  }
}
