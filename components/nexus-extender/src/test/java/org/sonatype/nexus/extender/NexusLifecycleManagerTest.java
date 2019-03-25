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
package org.sonatype.nexus.extender;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Named;

import org.sonatype.goodies.lifecycle.Lifecycle;
import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.app.ManagedLifecycle;
import org.sonatype.nexus.common.app.ManagedLifecycle.Phase;

import com.google.inject.Key;
import org.eclipse.sisu.BeanEntry;
import org.eclipse.sisu.inject.BeanLocator;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.osgi.framework.Bundle;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.CAPABILITIES;
import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.EVENTS;
import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.KERNEL;
import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.OFF;
import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.RESTORE;
import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.SCHEMAS;
import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.SECURITY;
import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.SERVICES;
import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.STORAGE;
import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.TASKS;
import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.UPGRADE;

public class NexusLifecycleManagerTest
    extends TestSupport
{
  @Mock
  private BeanLocator locator;

  @Mock
  private Bundle systemBundle;

  @Mock
  private OffPhase offPhase;

  @Mock
  private KernelPhase kernelPhase;

  @Mock
  private StoragePhase storagePhase;

  @Mock
  private RestorePhase restorePhase;

  @Mock
  private UpgradePhase upgradePhase;

  @Mock
  private SchemasPhase schemasPhase;

  @Mock
  private EventsPhase eventsPhase;

  @Mock
  private SecurityPhase securityPhase;

  @Mock
  private ServicesPhase servicesPhase;

  @Mock
  private CapabilitiesPhase capabilitiesPhase;

  @Mock
  private TasksPhase tasksPhase;

  private List<Lifecycle> phases;

  private List<Lifecycle> randomPhases;

  private NexusLifecycleManager underTest;

  @Before
  public void setUp() throws Exception {
    phases = newArrayList(
        offPhase,
        kernelPhase,
        storagePhase,
        restorePhase,
        upgradePhase,
        schemasPhase,
        eventsPhase,
        securityPhase,
        servicesPhase,
        capabilitiesPhase,
        tasksPhase
    );

    assertThat("One or more phases is not mocked", phases.size(), is(Phase.values().length));

    // OFF phase should never get called
    doThrow(new Exception("testing")).when(offPhase).start();
    doThrow(new Exception("testing")).when(offPhase).stop();

    // randomize location results
    randomPhases = new ArrayList<>(phases);
    Collections.shuffle(randomPhases);
    Iterable<BeanEntry<Named, Lifecycle>> entries = randomPhases.stream().map(phase -> {
      BeanEntry<Named, Lifecycle> entry = mock(BeanEntry.class);
      doReturn(phase.getClass().getSuperclass()).when(entry).getImplementationClass();
      when(entry.getValue()).thenReturn(phase);
      return entry;
    }).collect(toList());

    when(locator.<Named, Lifecycle> locate(Key.get(Lifecycle.class, Named.class))).thenReturn(entries);

    underTest = new NexusLifecycleManager(locator, systemBundle);
  }

  public InOrder verifyPhases() {
    return inOrder(
        offPhase,
        kernelPhase,
        storagePhase,
        restorePhase,
        upgradePhase,
        schemasPhase,
        eventsPhase,
        securityPhase,
        servicesPhase,
        capabilitiesPhase,
        tasksPhase,
        systemBundle
    );
  }

  @Test
  public void simpleLifecycleOrdering() throws Exception {
    InOrder inOrder = verifyPhases();

    underTest.to(KERNEL);
    assertThat(underTest.getCurrentPhase(), is(KERNEL));
    inOrder.verify(kernelPhase).start();

    underTest.to(STORAGE);
    assertThat(underTest.getCurrentPhase(), is(STORAGE));
    inOrder.verify(storagePhase).start();

    underTest.to(RESTORE);
    assertThat(underTest.getCurrentPhase(), is(RESTORE));
    inOrder.verify(restorePhase).start();

    underTest.to(UPGRADE);
    assertThat(underTest.getCurrentPhase(), is(UPGRADE));
    inOrder.verify(upgradePhase).start();

    underTest.to(SCHEMAS);
    assertThat(underTest.getCurrentPhase(), is(SCHEMAS));
    inOrder.verify(schemasPhase).start();

    underTest.to(EVENTS);
    assertThat(underTest.getCurrentPhase(), is(EVENTS));
    inOrder.verify(eventsPhase).start();

    underTest.to(SECURITY);
    assertThat(underTest.getCurrentPhase(), is(SECURITY));
    inOrder.verify(securityPhase).start();

    underTest.to(SERVICES);
    assertThat(underTest.getCurrentPhase(), is(SERVICES));
    inOrder.verify(servicesPhase).start();

    underTest.to(CAPABILITIES);
    assertThat(underTest.getCurrentPhase(), is(CAPABILITIES));
    inOrder.verify(capabilitiesPhase).start();

    underTest.to(TASKS);
    assertThat(underTest.getCurrentPhase(), is(TASKS));
    inOrder.verify(tasksPhase).start();

    inOrder.verifyNoMoreInteractions();

    underTest.to(CAPABILITIES);
    assertThat(underTest.getCurrentPhase(), is(CAPABILITIES));
    inOrder.verify(tasksPhase).stop();

    underTest.to(SERVICES);
    assertThat(underTest.getCurrentPhase(), is(SERVICES));
    inOrder.verify(capabilitiesPhase).stop();

    underTest.to(SECURITY);
    assertThat(underTest.getCurrentPhase(), is(SECURITY));
    inOrder.verify(servicesPhase).stop();

    underTest.to(EVENTS);
    assertThat(underTest.getCurrentPhase(), is(EVENTS));
    inOrder.verify(securityPhase).stop();

    underTest.to(SCHEMAS);
    assertThat(underTest.getCurrentPhase(), is(SCHEMAS));
    inOrder.verify(eventsPhase).stop();

    underTest.to(UPGRADE);
    assertThat(underTest.getCurrentPhase(), is(UPGRADE));
    inOrder.verify(schemasPhase).stop();

    underTest.to(RESTORE);
    assertThat(underTest.getCurrentPhase(), is(RESTORE));
    inOrder.verify(upgradePhase).stop();

    underTest.to(STORAGE);
    assertThat(underTest.getCurrentPhase(), is(STORAGE));
    inOrder.verify(restorePhase).stop();

    underTest.to(KERNEL);
    assertThat(underTest.getCurrentPhase(), is(KERNEL));
    inOrder.verify(storagePhase).stop();

    underTest.to(OFF);
    assertThat(underTest.getCurrentPhase(), is(OFF));
    inOrder.verify(kernelPhase).stop();
    inOrder.verify(systemBundle).stop();

    inOrder.verifyNoMoreInteractions();
  }

  @Test
  public void nonTaskErrorsStopStartup() throws Exception {
    InOrder inOrder = verifyPhases();

    Lifecycle badPhase = randomPhases.stream()
        .filter(phase -> !(phase.equals(offPhase) || phase.equals(tasksPhase)))
        .findFirst().get();

    doThrow(new Exception("testing")).when(badPhase).start();

    try {
      // attempt to reach last phase in the defined lifecycle
      underTest.to(Phase.values()[Phase.values().length - 1]);
      fail("Expected startup error to propagate");
    }
    catch (Exception e) {
      // lifecycle should have settled at the phase just before the bad phase
      assertThat(underTest.getCurrentPhase(), is(Phase.values()[phases.indexOf(badPhase) - 1]));

      // verify phases after OFF up to including bad phase attempted to start
      for (Lifecycle phase : phases.subList(1, phases.indexOf(badPhase) + 1)) {
        inOrder.verify(phase).start();
      }
      inOrder.verifyNoMoreInteractions();
    }
  }

  @Test
  public void taskErrorsDontStopStartup() throws Exception {
    InOrder inOrder = verifyPhases();

    doThrow(new Exception("testing")).when(tasksPhase).start();

    underTest.to(TASKS);

    assertThat(underTest.getCurrentPhase(), is(TASKS));

    inOrder.verify(kernelPhase).start();
    inOrder.verify(storagePhase).start();
    inOrder.verify(restorePhase).start();
    inOrder.verify(upgradePhase).start();
    inOrder.verify(schemasPhase).start();
    inOrder.verify(eventsPhase).start();
    inOrder.verify(securityPhase).start();
    inOrder.verify(servicesPhase).start();
    inOrder.verify(capabilitiesPhase).start();
    inOrder.verify(tasksPhase).start();

    inOrder.verifyNoMoreInteractions();
  }

  @Test
  public void errorsDontStopShutdown() throws Exception {
    underTest.to(TASKS);

    assertThat(underTest.getCurrentPhase(), is(TASKS));

    doThrow(new Exception("testing")).when(tasksPhase).stop();
    doThrow(new Exception("testing")).when(capabilitiesPhase).stop();
    doThrow(new Exception("testing")).when(servicesPhase).stop();
    doThrow(new Exception("testing")).when(securityPhase).stop();
    doThrow(new Exception("testing")).when(eventsPhase).stop();
    doThrow(new Exception("testing")).when(schemasPhase).stop();
    doThrow(new Exception("testing")).when(upgradePhase).stop();
    doThrow(new Exception("testing")).when(restorePhase).stop();
    doThrow(new Exception("testing")).when(storagePhase).stop();
    doThrow(new Exception("testing")).when(kernelPhase).stop();

    InOrder inOrder = verifyPhases();

    underTest.to(OFF);

    assertThat(underTest.getCurrentPhase(), is(OFF));

    inOrder.verify(tasksPhase).stop();
    inOrder.verify(capabilitiesPhase).stop();
    inOrder.verify(servicesPhase).stop();
    inOrder.verify(securityPhase).stop();
    inOrder.verify(eventsPhase).stop();
    inOrder.verify(schemasPhase).stop();
    inOrder.verify(upgradePhase).stop();
    inOrder.verify(restorePhase).stop();
    inOrder.verify(storagePhase).stop();
    inOrder.verify(kernelPhase).stop();
    inOrder.verify(systemBundle).stop();

    inOrder.verifyNoMoreInteractions();
  }

  private static class TestLifecycle
      implements Lifecycle
  {
    @Override
    public void start() throws Exception {
      // no-op
    }

    @Override
    public void stop() throws Exception {
      // no-op
    }
  }

  @ManagedLifecycle(phase = OFF)
  private static class OffPhase
      extends TestLifecycle
  {
  }

  @ManagedLifecycle(phase = KERNEL)
  private static class KernelPhase
      extends TestLifecycle
  {
  }

  @ManagedLifecycle(phase = STORAGE)
  private static class StoragePhase
      extends TestLifecycle
  {
  }

  @ManagedLifecycle(phase = RESTORE)
  private static class RestorePhase
      extends TestLifecycle
  {
  }

  @ManagedLifecycle(phase = UPGRADE)
  private static class UpgradePhase
      extends TestLifecycle
  {
  }

  @ManagedLifecycle(phase = SCHEMAS)
  private static class SchemasPhase
      extends TestLifecycle
  {
  }

  @ManagedLifecycle(phase = EVENTS)
  private static class EventsPhase
      extends TestLifecycle
  {
  }

  @ManagedLifecycle(phase = SECURITY)
  private static class SecurityPhase
      extends TestLifecycle
  {
  }

  @ManagedLifecycle(phase = SERVICES)
  private static class ServicesPhase
      extends TestLifecycle
  {
  }

  @ManagedLifecycle(phase = CAPABILITIES)
  private static class CapabilitiesPhase
      extends TestLifecycle
  {
  }

  @ManagedLifecycle(phase = TASKS)
  private static class TasksPhase
      extends TestLifecycle
  {
  }
}
