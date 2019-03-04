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

import java.lang.annotation.Annotation;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.lifecycle.Lifecycle;
import org.sonatype.nexus.common.app.ManagedLifecycle;
import org.sonatype.nexus.common.app.ManagedLifecycle.Phase;
import org.sonatype.nexus.common.app.ManagedLifecycleManager;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;
import com.google.inject.Key;
import org.eclipse.sisu.BeanEntry;
import org.eclipse.sisu.Mediator;
import org.eclipse.sisu.inject.BeanLocator;
import org.osgi.framework.BundleContext;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.reverse;
import static java.lang.Math.max;
import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.OFF;
import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.TASKS;

/**
 * Manages any {@link Lifecycle} components annotated with {@link ManagedLifecycle}.
 *
 * Components are managed during their appropriate phase in order of their priority.
 *
 * @since 3.0
 */
@Singleton
public class NexusLifecycleManager
    extends ManagedLifecycleManager
{
  private static final Phase[] PHASES = Phase.values();

  private final BeanLocator locator;

  private final Iterable<? extends BeanEntry<Named, Lifecycle>> lifecycles;

  private final Multimap<Phase, Lifecycle> components = HashMultimap.create();

  private ListMultimap<Phase, BeanEntry<Named, Lifecycle>> cachedIndex = ArrayListMultimap.create();

  private volatile Phase currentPhase = OFF;

  @Inject
  public NexusLifecycleManager(final BeanLocator locator) {
    this.locator = checkNotNull(locator);
    this.lifecycles = locator.locate(Key.get(Lifecycle.class, Named.class));

    locator.watch(Key.get(BundleContext.class), new BundleContextMediator(), this);
  }

  @Override
  public Phase getCurrentPhase() {
    return currentPhase;
  }

  @Override
  public void to(final Phase targetPhase) throws Exception {
    if (targetPhase == OFF) {
      registerShutdown();
    }
    else if (isShuttingDown()) {
      return; // cannot go back once shutdown has begun
    }

    synchronized (locator) {

      final int target = targetPhase.ordinal();
      int current = currentPhase.ordinal();

      // refresh index and start/stop components which appeared/disappeared since last index
      if (current < target) {
        reindex(targetPhase);
      }
      else {
        reindex(currentPhase);
      }

      // moving forwards to later phase, start components in priority order
      while (current < target) {
        Phase nextPhase = PHASES[++current];
        log.info("Start {}", nextPhase);
        boolean propagateNonTaskErrors = !TASKS.equals(nextPhase);
        for (BeanEntry<Named, Lifecycle> entry : cachedIndex.get(nextPhase)) {
          startComponent(nextPhase, entry.getValue(), propagateNonTaskErrors);
        }
        currentPhase = nextPhase;
      }

      // rolling back to earlier phase, stop components in reverse priority order
      while (current > target) {
        Phase prevPhase = PHASES[--current];
        log.info("Stop {}", currentPhase);
        for (BeanEntry<Named, Lifecycle> entry : reverse(cachedIndex.get(currentPhase))) {
          stopComponent(currentPhase, entry.getValue(), false);
        }
        currentPhase = prevPhase;
      }
    }
  }

  @Override
  public void bounce(final Phase bouncePhase) throws Exception {
    Phase targetPhase = currentPhase;
    // re-run the given phase by moving to just before it before moving back
    if (bouncePhase.ordinal() <= targetPhase.ordinal()) {
      to(Phase.values()[max(0, bouncePhase.ordinal() - 1)]);
    }
    else {
      targetPhase = bouncePhase; // bounce phase is later, just move to it
    }
    to(targetPhase);
  }

  /**
   * Starts/stops components that have appeared/disappeared since the last change.
   */
  public void sync() throws Exception {
    synchronized (locator) {
      reindex(currentPhase);
    }
  }

  /**
   * Refreshes the lifecycle index, starting/stopping any components belonging
   * to the current or earlier phases as they appear/disappear from the index.
   */
  private void reindex(final Phase targetPhase) throws Exception {
    ListMultimap<Phase, BeanEntry<Named, Lifecycle>> index = index(targetPhase);
    if (index.equals(cachedIndex)) {
      return; // nothing has changed
    }

    // remove entries from current index that also exist in new index, start any new entries
    for (int p = 1; p <= currentPhase.ordinal(); p++) {
      Phase phase = PHASES[p];
      for (BeanEntry<Named, Lifecycle> entry : index.get(phase)) {
        if (!cachedIndex.remove(phase, entry)) {
          startComponent(phase, entry.getValue(), false);
        }
      }
    }

    // any entries left in the current index have disappeared, so stop them in reverse order
    if (!cachedIndex.isEmpty()) {
      for (int p = currentPhase.ordinal(); p >= 1; p--) {
        Phase phase = PHASES[p];
        for (BeanEntry<Named, Lifecycle> entry : reverse(cachedIndex.get(phase))) {
          stopComponent(phase, entry.getValue(), false);
        }
      }
    }

    cachedIndex = index;
  }

  /**
   * Starts the given lifecycle component, propagating lifecycle errors only when requested.
   */
  private void startComponent(final Phase phase, final Lifecycle lifecycle, final boolean propagateErrors)
      throws Exception
  {
    try {
      if (components.put(phase, lifecycle)) {
        log.debug("Start {}: {}", phase, lifecycle);
        lifecycle.start();
      }
    }
    catch (Exception | LinkageError e) {
      if (propagateErrors) {
        throw e;
      }
      log.warn("Problem starting {}: {}", phase, lifecycle, e);
    }
  }

  /**
   * Stops the given lifecycle component, propagating lifecycle errors only when requested.
   */
  private void stopComponent(final Phase phase, final Lifecycle lifecycle, final boolean propagateErrors)
      throws Exception
  {
    try {
      if (components.remove(phase, lifecycle)) {
        log.debug("Stop {}: {}", phase, lifecycle);
        lifecycle.stop();
      }
    }
    catch (Exception | LinkageError e) {
      if (propagateErrors) {
        throw e;
      }
      log.warn("Problem stopping {}: {}", phase, lifecycle, e);
    }
  }

  /**
   * Creates a multilevel index containing all managed lifecycles up to and including the target phase.
   */
  private ListMultimap<Phase, BeanEntry<Named, Lifecycle>> index(final Phase targetPhase) {
    ListMultimap<Phase, BeanEntry<Named, Lifecycle>> index = ArrayListMultimap.create();

    final int target = targetPhase.ordinal();

    for (BeanEntry<Named, Lifecycle> entry : lifecycles) {
      ManagedLifecycle managedLifecycle = entry.getImplementationClass().getAnnotation(ManagedLifecycle.class);
      if (managedLifecycle != null && managedLifecycle.phase().ordinal() <= target) {
        index.put(managedLifecycle.phase(), entry);
      }
    }

    return index;
  }

  /**
   * Triggers {@link NexusLifecycleManager#sync} requests as {@link BundleContext}s come and go.
   */
  private static class BundleContextMediator
      implements Mediator<Annotation, BundleContext, NexusLifecycleManager>
  {
    @Override
    public void add(final BeanEntry<Annotation, BundleContext> entry, final NexusLifecycleManager manager)
        throws Exception
    {
      manager.sync();
    }

    @Override
    public void remove(final BeanEntry<Annotation, BundleContext> entry, final NexusLifecycleManager manager)
        throws Exception
    {
      manager.sync();
    }
  }
}
