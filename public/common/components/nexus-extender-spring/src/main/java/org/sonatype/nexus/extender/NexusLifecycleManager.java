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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.annotation.Priority;

import org.sonatype.nexus.common.lifecycle.Lifecycle;
import org.sonatype.nexus.common.app.ManagedLifecycle;
import org.sonatype.nexus.common.app.ManagedLifecycle.Phase;
import org.sonatype.nexus.common.app.ManagedLifecycleManager;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.reverse;
import static java.lang.Math.max;
import static org.sonatype.nexus.common.app.FeatureFlags.STARTUP_TASKS_DELAY_SECONDS_VALUE;
import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.KERNEL;
import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.OFF;
import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.TASKS;

/**
 * Manages any {@link Lifecycle} components annotated with {@link ManagedLifecycle}.
 * <p>
 * Components are managed during their appropriate phase in order of their priority.
 */
@Singleton
@Component
public class NexusLifecycleManager
    extends ManagedLifecycleManager
{
  private static final Phase[] PHASES = Phase.values();

  private final Multimap<Phase, Lifecycle> components = HashMultimap.create();

  private volatile Phase currentPhase = OFF;

  private final int timeToDelay;

  private final ApplicationContext context;

  @Inject
  public NexusLifecycleManager(
      @Value(STARTUP_TASKS_DELAY_SECONDS_VALUE) final int timeToDelay,
      final ApplicationContext context)
  {
    this.timeToDelay = timeToDelay;
    this.context = checkNotNull(context);
  }

  @Override
  public Phase getCurrentPhase() {
    return currentPhase;
  }

  @Override
  public synchronized void to(final Phase targetPhase) throws Exception { // NOSONAR
    if (targetPhase == OFF) {
      declareShutdown();
    }
    else if (isShuttingDown()) {
      return; // cannot go back once shutdown has begun
    }

    final int target = targetPhase.ordinal();
    int current = currentPhase.ordinal();

    log.debug("Getting lifecycle managed components up to phase {}", current < target ? targetPhase : currentPhase);
    ListMultimap<Phase, Lifecycle> lifeCyclesInPhase =
        getLifecyclesInPhase(current < target ? targetPhase : currentPhase);

    // moving forwards to later phase, start components in priority order
    while (current < target) {
      Phase nextPhase = PHASES[++current];
      log.info("Start {}", nextPhase);
      boolean propagateNonTaskErrors = !TASKS.equals(nextPhase);
      for (Lifecycle entry : lifeCyclesInPhase.get(nextPhase)) {
        if (nextPhase.equals(TASKS) && timeToDelay > 0) {
          delayStartUpTask(nextPhase, entry, propagateNonTaskErrors);
        }
        else {
          startComponent(nextPhase, entry, propagateNonTaskErrors);
        }
      }
      currentPhase = nextPhase;
    }

    // rolling back to earlier phase, stop components in reverse priority order
    while (current > target) {
      Phase prevPhase = PHASES[--current];
      log.info("Stop {}", currentPhase);
      for (Lifecycle entry : reverse(lifeCyclesInPhase.get(currentPhase))) {
        stopComponent(currentPhase, entry, false);
      }
      currentPhase = prevPhase;
    }
  }

  @Override
  public void bounce(final Phase bouncePhase) throws Exception {
    Phase targetPhase = currentPhase;
    // re-run the given phase by moving to just before it before moving back
    if (bouncePhase.ordinal() <= targetPhase.ordinal()) {
      if (bouncePhase == KERNEL) {
        System.setProperty("karaf.restart", "true");
      }
      to(Phase.values()[max(0, bouncePhase.ordinal() - 1)]);
    }
    else {
      targetPhase = bouncePhase; // bounce phase is later, just move to it
    }
    to(targetPhase);
  }

  /**
   * Starts the given lifecycle component, propagating lifecycle errors only when requested.
   */
  private void startComponent(
      final Phase phase,
      final Lifecycle lifecycle,
      final boolean propagateErrors) throws Exception
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
  private void stopComponent(
      final Phase phase,
      final Lifecycle lifecycle,
      final boolean propagateErrors) throws Exception
  {
    try {
      if (components.remove(phase, lifecycle)) {
        log.debug("Stop {}: {}", phase, lifecycle);
        lifecycle.stop();
      }
    }
    catch (Exception | LinkageError e) {
      log.warn("Problem stopping {}: {}", phase, lifecycle, e);
      if (propagateErrors) {
        throw e;
      }
    }
  }

  private void delayStartUpTask(final Phase phase, final Lifecycle lifecycle, final boolean propagateErrors) {
    final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    scheduledExecutorService.schedule(() -> {
      try {
        startComponent(phase, lifecycle, propagateErrors);
      }
      catch (Exception e) {
        throw new RuntimeException(e);
      }
    }, timeToDelay, TimeUnit.SECONDS);
  }

  /**
   * Creates a multilevel index containing all managed lifecycles up to and including the target phase.
   */
  private ListMultimap<Phase, Lifecycle> getLifecyclesInPhase(final Phase targetPhase) {
    ListMultimap<Phase, Lifecycle> lifecyclesInPhase = ArrayListMultimap.create();

    final int target = targetPhase.ordinal();

    List<Lifecycle> lifecycles = new ArrayList<>(context.getBeansOfType(Lifecycle.class).values());
    // Make sure the lifecycles are processed in priority order, ideally could utilize spring's @Order annotation
    lifecycles.sort((o1, o2) -> {
      int priority1 = getPriority(o1.getClass());
      int priority2 = getPriority(o2.getClass());
      return -Integer.compare(priority1, priority2);
    });

    log.info("Indexing lifecycle managed components up to phase {}", targetPhase);

    for (Lifecycle entry : lifecycles) {
      ManagedLifecycle managedLifecycle = getAnnotation(ManagedLifecycle.class, entry.getClass());
      if (managedLifecycle != null && managedLifecycle.phase().ordinal() <= target) {
        lifecyclesInPhase.put(managedLifecycle.phase(), entry);
      }
    }

    return lifecyclesInPhase;
  }

  private static int getPriority(final Class<?> clazz) {
    Priority priorityAnnotation = getAnnotation(Priority.class, clazz);
    // 0 is lowest priority, default value
    return priorityAnnotation != null ? priorityAnnotation.value() : 0;
  }

  private static <A extends Annotation> A getAnnotation(final Class<A> annotation, final Class<?> clazz) {
    Class<?> impl = clazz.toString().contains("$$SpringCGLIB$$") ? clazz.getSuperclass() : clazz;
    return impl.getAnnotation(annotation);
  }
}
