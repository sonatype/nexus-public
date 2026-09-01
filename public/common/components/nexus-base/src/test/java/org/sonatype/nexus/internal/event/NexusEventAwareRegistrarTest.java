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

import java.util.LinkedHashMap;
import java.util.Map;

import org.sonatype.nexus.common.app.ManagedLifecycle;
import org.sonatype.nexus.common.event.EventAware;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.security.internal.RealmManagerImpl;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.context.ApplicationContext;

import static java.util.Collections.emptyMap;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.EVENTS;
import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.TASKS;

@RunWith(MockitoJUnitRunner.Silent.class)
public class NexusEventAwareRegistrarTest
{
  @Mock
  private ApplicationContext applicationContext;

  @Mock
  private EventManager eventManager;

  private NexusEventAwareRegistrar underTest;

  @Before
  public void setUp() {
    underTest = new NexusEventAwareRegistrar(applicationContext, eventManager);
  }

  private void withSingletonEventAwareBeans(final Map<String, EventAware> beans) {
    // registrar restricts to singletons (includeNonSingletons=false) with allowEagerInit=true
    when(applicationContext.getBeansOfType(EventAware.class, false, true)).thenReturn(beans);
  }

  @Test
  public void registersNonLifecycleBeansOnStartButDefersLaterPhaseBeans() throws Exception {
    EventAware plain = new PlainSubscriber();
    EventAware laterPhase = new TasksPhaseSubscriber();
    Map<String, EventAware> beans = new LinkedHashMap<>();
    beans.put("plain", plain);
    beans.put("laterPhase", laterPhase);
    withSingletonEventAwareBeans(beans);

    underTest.start();

    // non-lifecycle beans are already active, so they register in the bulk EVENTS-phase pass...
    verify(eventManager).register(plain);
    // ...but a bean managed after EVENTS is deferred to onStarted() (its own phase), not registered here.
    verify(eventManager, never()).register(laterPhase);
  }

  @Test
  public void registersBeansManagedAtOrBeforeEventsDuringBulkStart() throws Exception {
    EventAware eventsPhase = new EventsPhaseSubscriber();
    Map<String, EventAware> beans = new LinkedHashMap<>();
    beans.put("eventsPhase", eventsPhase);
    withSingletonEventAwareBeans(beans);

    underTest.start();

    verify(eventManager).register(eventsPhase);
  }

  @Test
  public void onStartedRegistersLaterPhaseSubscriberOnceRegistrarHasStarted() throws Exception {
    withSingletonEventAwareBeans(emptyMap());
    underTest.start();

    EventAware laterPhase = new TasksPhaseSubscriber();
    underTest.onStarted(laterPhase);

    verify(eventManager).register(laterPhase);
  }

  @Test
  public void onStartedIsIgnoredBeforeRegistrarHasStarted() {
    // Components in phases at or before EVENTS may start before this registrar does; those are handled by the
    // bulk pass in doStart(), so an early onStarted() callback must be a no-op (the EventManager is not ready).
    underTest.onStarted(new TasksPhaseSubscriber());

    verifyNoInteractions(eventManager);
  }

  @Test
  public void onStartedIgnoresNonEventAwareComponents() throws Exception {
    withSingletonEventAwareBeans(emptyMap());
    underTest.start();

    underTest.onStarted(new Object());

    verifyNoInteractions(eventManager);
  }

  @Test
  public void onStoppingUnregistersALaterPhaseSubscriber() throws Exception {
    withSingletonEventAwareBeans(emptyMap());
    underTest.start();

    EventAware laterPhase = new TasksPhaseSubscriber();
    underTest.onStarted(laterPhase);
    underTest.onStopping(laterPhase);

    verify(eventManager).register(laterPhase);
    verify(eventManager).unregister(laterPhase);
  }

  @Test
  public void onStoppingIsANoOpForAComponentThatWasNeverRegistered() throws Exception {
    withSingletonEventAwareBeans(emptyMap());
    underTest.start();

    underTest.onStopping(new TasksPhaseSubscriber());

    verifyNoInteractions(eventManager);
  }

  @Test
  public void registersAsynchronousOnlySubscribers() throws Exception {
    // A bean that implements only EventAware.Asynchronous must still be discovered and registered: because
    // Asynchronous extends EventAware, getBeansOfType(EventAware.class, ...) sees it. This is the discovery-layer
    // guarantee behind async-only listeners such as DockerConfigBlobMetadataListener (NEXUS-52911).
    EventAware asyncOnly = new AsyncOnlySubscriber();
    Map<String, EventAware> beans = new LinkedHashMap<>();
    beans.put("asyncOnly", asyncOnly);
    withSingletonEventAwareBeans(beans);

    underTest.start();

    verify(eventManager).register(asyncOnly);
  }

  @Test
  public void continuesRegisteringAfterAnIndividualFailure() throws Exception {
    EventAware good1 = new PlainSubscriber();
    EventAware bad = new PlainSubscriber();
    EventAware good2 = new PlainSubscriber();
    Map<String, EventAware> beans = new LinkedHashMap<>();
    beans.put("good1", good1);
    beans.put("bad", bad);
    beans.put("good2", good2);
    withSingletonEventAwareBeans(beans);

    doThrow(new RuntimeException("boom")).when(eventManager).register(bad);

    underTest.start();

    verify(eventManager).register(good1);
    verify(eventManager).register(bad);
    verify(eventManager).register(good2);
  }

  @Test
  public void unregistersEverythingOnStop() throws Exception {
    EventAware plain = new PlainSubscriber();
    Map<String, EventAware> beans = new LinkedHashMap<>();
    beans.put("plain", plain);
    withSingletonEventAwareBeans(beans);

    underTest.start();
    underTest.stop();

    verify(eventManager).register(plain);
    verify(eventManager).unregister(plain);
  }

  @Test
  public void doesNothingWhenThereAreNoEventAwareBeans() throws Exception {
    withSingletonEventAwareBeans(emptyMap());

    underTest.start();

    verifyNoInteractions(eventManager);
  }

  @Test
  public void doesNotUnregisterABeanThatFailedToRegister() throws Exception {
    EventAware good = new PlainSubscriber();
    EventAware bad = new PlainSubscriber();
    Map<String, EventAware> beans = new LinkedHashMap<>();
    beans.put("good", good);
    beans.put("bad", bad);
    withSingletonEventAwareBeans(beans);

    doThrow(new RuntimeException("boom")).when(eventManager).register(bad);

    underTest.start();
    underTest.stop();

    // the good bean is unregistered on stop, but the bean that failed to register must not be unregistered
    verify(eventManager).unregister(good);
    verify(eventManager, never()).unregister(bad);
  }

  /**
   * Pins the singletons that self-register on the {@link EventManager} (via {@code eventManager.register(this)}) but
   * are intentionally <em>not</em> {@link EventAware}, so {@link NexusEventAwareRegistrar} does not also register them.
   * Guava's {@code EventBus.register()} is not idempotent: if one of these additionally implemented {@link EventAware},
   * the registrar would register it a second time and every {@code @Subscribe} handler would fire twice per event
   * (a silent double-invoke, no exception). If a future refactor makes one of these {@link EventAware}, this test
   * trips — the fix is to drop the manual {@code register(this)} and let the registrar own registration.
   */
  @Test
  public void selfRegisteringSingletonsMustNotAlsoBeEventAware() {
    assertFalse(
        "RealmManagerImpl self-registers on the EventManager; it must not also implement EventAware or the "
            + "registrar would double-register it. Drop the manual register(this) if it becomes EventAware.",
        EventAware.class.isAssignableFrom(RealmManagerImpl.class));
    assertFalse(
        "DebugEventInspector self-registers on the EventManager; it must not also implement EventAware or the "
            + "registrar would double-register it. Drop the manual register(this) if it becomes EventAware.",
        EventAware.class.isAssignableFrom(DebugEventInspector.class));
  }

  private static class PlainSubscriber
      implements EventAware
  {
  }

  private static class AsyncOnlySubscriber
      implements EventAware.Asynchronous
  {
  }

  @ManagedLifecycle(phase = TASKS)
  private static class TasksPhaseSubscriber
      implements EventAware
  {
  }

  @ManagedLifecycle(phase = EVENTS)
  private static class EventsPhaseSubscriber
      implements EventAware
  {
  }
}
