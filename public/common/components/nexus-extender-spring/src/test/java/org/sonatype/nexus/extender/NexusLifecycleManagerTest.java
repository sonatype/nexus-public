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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.sonatype.nexus.common.lifecycle.Lifecycle;
import org.sonatype.nexus.common.app.ManagedLifecycle;
import org.sonatype.nexus.common.app.ManagedLifecycle.Phase;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.context.ApplicationContext;
import org.sonatype.nexus.common.PrecedenceConstants;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link NexusLifecycleManager}, focusing on @Order-based lifecycle ordering.
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class NexusLifecycleManagerTest
{
  /**
   * Records start/stop invocations from concrete test components so ordering tests can assert
   * call order without relying on Mockito spies (ByteBuddy proxies don't expose @Order).
   */
  static List<String> callLog = new ArrayList<>();

  @Mock
  private ApplicationContext applicationContext;

  private NexusLifecycleManager underTest;

  @Before
  public void setUp() {
    callLog.clear();
    underTest = new NexusLifecycleManager(0, applicationContext);
  }

  @Test
  public void componentsStartedInOrderOfAscendingOrderValue() throws Exception {
    // Register in deliberate non-sorted order: low, high, mid
    Map<String, Lifecycle> beans = new LinkedHashMap<>();
    beans.put("low", new LowPrecedenceComponent());
    beans.put("high", new HighPrecedenceComponent());
    beans.put("mid", new MidPrecedenceComponent());

    when(applicationContext.getBeansOfType(Lifecycle.class)).thenReturn(beans);

    underTest.to(Phase.KERNEL);

    // high (@Order(HIGHEST_PRECEDENCE)) -> mid (@Order(0)) -> low (@Order(LOWEST_PRECEDENCE))
    assertThat(callLog, is(List.of("start:high", "start:mid", "start:low")));
  }

  @Test
  public void componentsStoppedInReverseOrder() throws Exception {
    Map<String, Lifecycle> beans = new LinkedHashMap<>();
    beans.put("low", new LowPrecedenceComponent());
    beans.put("high", new HighPrecedenceComponent());
    beans.put("mid", new MidPrecedenceComponent());

    when(applicationContext.getBeansOfType(Lifecycle.class)).thenReturn(beans);

    underTest.to(Phase.KERNEL);

    // Discard start entries; only assert stop ordering
    callLog.clear();

    // Roll back to OFF — stops must be in reverse start order
    underTest.to(Phase.OFF);

    assertThat(callLog, is(List.of("stop:low", "stop:mid", "stop:high")));
  }

  @Test
  public void unannotatedComponentsGetDefaultPrecedence() throws Exception {
    Map<String, Lifecycle> beans = new LinkedHashMap<>();
    beans.put("noOrder", new NoOrderComponent());
    beans.put("high", new HighPrecedenceComponent());

    when(applicationContext.getBeansOfType(Lifecycle.class)).thenReturn(beans);

    underTest.to(Phase.KERNEL);

    // high (@Order(HIGHEST_PRECEDENCE)) must start before noOrder (defaults to DEFAULT_PRECEDENCE = 0)
    assertThat(callLog, is(List.of("start:high", "start:noOrder")));
  }

  @Test
  public void onlyComponentsInTargetPhaseAreStarted() throws Exception {
    Map<String, Lifecycle> beans = new LinkedHashMap<>();
    beans.put("kernel", new HighPrecedenceComponent());
    beans.put("storage", new StoragePhaseComponent());

    when(applicationContext.getBeansOfType(Lifecycle.class)).thenReturn(beans);

    // Only move to KERNEL, so STORAGE component should not start
    underTest.to(Phase.KERNEL);

    assertThat(callLog.contains("start:high"), is(true));
    assertThat(callLog.contains("start:storage"), is(false));
  }

  @Test
  public void getOrder_returnsAnnotatedValue() {
    assertThat(NexusLifecycleManager.getOrder(HighPrecedenceComponent.class), is(Ordered.HIGHEST_PRECEDENCE));
    assertThat(NexusLifecycleManager.getOrder(MidPrecedenceComponent.class), is(0));
  }

  @Test
  public void getOrder_returnsDefaultPrecedenceWhenNotAnnotated() {
    assertThat(NexusLifecycleManager.getOrder(NoOrderComponent.class), is(PrecedenceConstants.DEFAULT_PRECEDENCE));
  }

  @Test
  public void getOrder_returnsLowestPrecedenceWhenExplicitlyAnnotated() {
    assertThat(NexusLifecycleManager.getOrder(LowPrecedenceComponent.class), is(Ordered.LOWEST_PRECEDENCE));
  }

  @Test
  public void currentPhaseTracksProgress() throws Exception {
    when(applicationContext.getBeansOfType(Lifecycle.class)).thenReturn(Map.of());

    assertThat(underTest.getCurrentPhase(), is(Phase.OFF));

    underTest.to(Phase.KERNEL);
    assertThat(underTest.getCurrentPhase(), is(Phase.KERNEL));

    underTest.to(Phase.STORAGE);
    assertThat(underTest.getCurrentPhase(), is(Phase.STORAGE));
  }

  // ---------------------------------------------------------------------------
  // Concrete test components — annotations live on the real class, so
  // NexusLifecycleManager.getAnnotation() resolves them without unwrapping proxies.
  // start()/stop() append to callLog so ordering tests can assert call sequence.
  // ---------------------------------------------------------------------------

  @ManagedLifecycle(phase = Phase.KERNEL)
  @Order(Ordered.HIGHEST_PRECEDENCE)
  static class HighPrecedenceComponent
      implements Lifecycle
  {
    @Override
    public void start() throws Exception {
      callLog.add("start:high");
    }

    @Override
    public void stop() throws Exception {
      callLog.add("stop:high");
    }
  }

  @ManagedLifecycle(phase = Phase.KERNEL)
  @Order(0)
  static class MidPrecedenceComponent
      implements Lifecycle
  {
    @Override
    public void start() throws Exception {
      callLog.add("start:mid");
    }

    @Override
    public void stop() throws Exception {
      callLog.add("stop:mid");
    }
  }

  @ManagedLifecycle(phase = Phase.KERNEL)
  @Order(Ordered.LOWEST_PRECEDENCE)
  static class LowPrecedenceComponent
      implements Lifecycle
  {
    @Override
    public void start() throws Exception {
      callLog.add("start:low");
    }

    @Override
    public void stop() throws Exception {
      callLog.add("stop:low");
    }
  }

  @ManagedLifecycle(phase = Phase.KERNEL)
  static class NoOrderComponent
      implements Lifecycle
  {
    @Override
    public void start() throws Exception {
      callLog.add("start:noOrder");
    }

    @Override
    public void stop() throws Exception {
      callLog.add("stop:noOrder");
    }
  }

  @ManagedLifecycle(phase = Phase.STORAGE)
  @Order(Ordered.HIGHEST_PRECEDENCE)
  static class StoragePhaseComponent
      implements Lifecycle
  {
    @Override
    public void start() throws Exception {
      callLog.add("start:storage");
    }

    @Override
    public void stop() throws Exception {
      callLog.add("stop:storage");
    }
  }
}
