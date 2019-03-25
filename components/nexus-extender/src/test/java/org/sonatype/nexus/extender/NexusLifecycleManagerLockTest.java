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

import java.util.concurrent.CountDownLatch;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.lifecycle.Lifecycle;
import org.sonatype.goodies.lifecycle.LifecycleSupport;
import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.app.ManagedLifecycle;
import org.sonatype.nexus.common.app.ManagedLifecycleManager;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import org.eclipse.sisu.BeanEntry;
import org.eclipse.sisu.Mediator;
import org.eclipse.sisu.inject.BeanLocator;
import org.eclipse.sisu.inject.InjectorBindings;
import org.eclipse.sisu.inject.MutableBeanLocator;
import org.junit.Test;
import org.mockito.Mock;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import static com.google.inject.name.Names.named;
import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.SERVICES;

/**
 * UT capturing the potential deadlock scenario from https://bugs.eclipse.org/bugs/show_bug.cgi?id=539379
 *
 * This should no longer happen with the change to use the locator lock to also protect lifecycle activation.
 */
public class NexusLifecycleManagerLockTest
    extends TestSupport
{
  @Mock
  private BundleContext bundleContext;

  @Mock
  private Bundle systemBundle;

  @Test(timeout = 60_000) // if this test deadlocks then it will eventually timeout and fail
  public void addingComponentBundlesInParallelDuringLifecycleActivation() throws Exception {

    Injector injector = Guice.createInjector(binder -> {
      binder.bind(ManagedLifecycleManager.class).to(NexusLifecycleManager.class);
      binder.bind(Bundle.class).annotatedWith(named("system")).toInstance(systemBundle);
      binder.bind(Lifecycle.class).annotatedWith(named("trigger")).to(Trigger.class);
      binder.bind(CountDownLatch.class).toInstance(new CountDownLatch(1));
    });

    MutableBeanLocator locator = injector.getInstance(MutableBeanLocator.class);
    CountDownLatch latch = injector.getInstance(CountDownLatch.class);

    new Thread(() -> {
      try {
        // begin lifecycle activation in parallel
        injector.getInstance(ManagedLifecycleManager.class).to(SERVICES);
      }
      catch (Exception e) {
        throw new RuntimeException(e);
      }
    }).start();

    // wait for lifecycle activation to reach a critical point
    latch.await();

    // register new Bundle-backed injector - this eventually calls NexusLifecycleManager.sync()
    // which tries to acquire the lifecycle lock while we're still holding onto the locator lock
    locator.add(new InjectorBindings(
        Guice.createInjector(binder -> binder.bind(BundleContext.class).toInstance(bundleContext))));

    // if we reach here without encountering a deadlock then the test has passed
  }

  static class Watchee
  {
    // empty
  }

  static class Watcher
  {
    // empty
  }

  @Singleton
  @ManagedLifecycle(phase = SERVICES)
  static class Trigger
      extends LifecycleSupport
  {
    private final BeanLocator locator;

    private final CountDownLatch latch;

    @Inject
    public Trigger(BeanLocator locator, CountDownLatch latch) {
      this.locator = locator;
      this.latch = latch;
    }

    @Override
    protected void doStart() throws Exception {

      // let the main thread continue, give it enough time to begin to deadlock
      latch.countDown();
      Thread.sleep(1000);

      // register a new mediator - originally this would complete the deadlock
      // because this was asking for the locator lock while holding the lifecycle
      // lock, while the main test thread was asking for the lifecycle lock while
      // holding the locator lock (specifically via the BundleContext mediator)
      locator.watch(Key.get(Watchee.class), new ExampleMediator(), new Watcher());
    }
  }

  static class ExampleMediator
      implements Mediator<Named, Watchee, Watcher>
  {
    @Override
    public void add(BeanEntry<Named, Watchee> entry, Watcher watcher) throws Exception {
      // empty
    }

    @Override
    public void remove(BeanEntry<Named, Watchee> entry, Watcher watcher) throws Exception {
      // empty
    }
  }
}
