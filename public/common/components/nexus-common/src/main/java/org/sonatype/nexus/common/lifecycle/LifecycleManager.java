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
package org.sonatype.nexus.common.lifecycle;

import java.util.concurrent.CopyOnWriteArrayList;

import org.sonatype.nexus.common.failure.MultipleFailures;
import org.sonatype.nexus.common.failure.MultipleFailures.MultipleFailuresException;

import com.google.common.collect.Lists;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Manages a set of {@link Lifecycle} components.
 */
public class LifecycleManager
    extends LifecycleSupport
{
  private final CopyOnWriteArrayList<Lifecycle> components = new CopyOnWriteArrayList<>();

  public void add(final Lifecycle component) {
    checkNotNull(component);
    boolean added = components.addIfAbsent(component);
    if (added) {
      log.trace("Added: {}", component);
    }
  }

  public void add(final Lifecycle... components) {
    checkNotNull(components);
    for (Lifecycle component : components) {
      add(component);
    }
  }

  public void remove(final Lifecycle component) {
    checkNotNull(component);
    boolean removed = components.remove(component);
    if (removed) {
      log.trace("Removed: {}", component);
    }
  }

  public void remove(final Lifecycle... components) {
    checkNotNull(components);
    for (Lifecycle component : components) {
      remove(component);
    }
  }

  public int size() {
    return components.size();
  }

  public void clear() {
    components.clear();
    log.trace("Cleared");
  }

  /**
   * Start all managed components.
   *
   * Components are started in the order added.
   *
   * @throws MultipleFailuresException
   */
  @Override
  protected void doStart() throws Exception {
    int count = components.size();
    log.debug("Starting {} components", count);

    MultipleFailures failures = new MultipleFailures(count);
    for (Lifecycle component : components) {
      try {
        component.start();
      }
      catch (Throwable failure) {
        logTransitionFailure("Failed to start component: " + component, failure);
        failures.add(failure);
      }
    }
    failures.maybePropagate("Failed to start " + failures.size() + " components");
  }

  /**
   * Stop all managed components.
   *
   * Stop order is reverse of start order.
   *
   * @throws MultipleFailuresException
   */
  @Override
  protected void doStop() throws Exception {
    int count = components.size();
    log.debug("Stopping {} components", count);

    MultipleFailures failures = new MultipleFailures(count);
    for (Lifecycle component : Lists.reverse(components)) {
      try {
        component.stop();
      }
      catch (Throwable failure) {
        logTransitionFailure("Failed to stop component: " + component, failure);
        failures.add(failure);
      }
    }
    failures.maybePropagate("Failed to stop " + failures.size() + " components");
  }
}
