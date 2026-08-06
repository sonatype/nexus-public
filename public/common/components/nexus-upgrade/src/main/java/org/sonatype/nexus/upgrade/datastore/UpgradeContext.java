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
package org.sonatype.nexus.upgrade.datastore;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Lifecycle-safe, generic registry for one-shot signals and context data raised by UPGRADE-phase
 * migration steps and consumed by later-phase components (e.g. {@code @ManagedLifecycle(phase = TASKS)}
 * managers) when they start.
 *
 * <p>
 * Migration steps run during the UPGRADE phase and must not inject later-phase components directly:
 * doing so forces those components (and their transitive dependencies) to be instantiated before their
 * phase, which is exactly the lifecycle hazard this bean avoids. {@code UpgradeContext} is a plain
 * singleton with no dependencies, so it is always available. A step records its intent here under a
 * caller-owned key, and the owning component reads it back at its own start.
 * </p>
 *
 * <p>
 * Entries are keyed by string constants owned by the calling modules (define the constant next to the
 * producer or consumer). This keeps this infrastructure class free of domain-specific concerns and lets
 * new signals be added without modifying {@code nexus-upgrade}. Backed by a {@link ConcurrentHashMap}
 * so a write made by an UPGRADE-phase step is visible to a later-phase reader, which may run on a
 * different thread.
 * </p>
 *
 * <p>
 * <strong>Cluster scope:</strong> this is a per-JVM, in-memory registry. In a clustered deployment the
 * migration body runs on only one node (leader election, or peer nodes skip an already-recorded Flyway
 * version), so a signal raised by a step is observed only by consumers on that same node — peer nodes
 * start with an empty context. Use this primitive only when the downstream action is acceptable to run
 * once cluster-wide (e.g. scheduling work via {@code UpgradeTaskScheduler}) or is otherwise
 * cluster-coordinated. For node-local actions that must run on every node, persist the signal instead
 * (e.g. via {@code UpgradeNexusKeyValueStore}).
 * </p>
 */
@Component
public class UpgradeContext
{
  private final Map<String, Object> attributes = new ConcurrentHashMap<>();

  /**
   * Raises a one-shot boolean signal under the given key.
   */
  public void setFlag(final String key) {
    attributes.put(checkNotNull(key), Boolean.TRUE);
  }

  /**
   * Returns {@code true} if the boolean signal under the given key has been raised.
   */
  public boolean isFlagSet(final String key) {
    return Boolean.TRUE.equals(attributes.get(checkNotNull(key)));
  }

  /**
   * Stores an arbitrary value under the given key.
   */
  public void put(final String key, final Object value) {
    attributes.put(checkNotNull(key), checkNotNull(value));
  }

  /**
   * Returns the value stored under the given key, if any.
   */
  public Optional<Object> get(final String key) {
    return Optional.ofNullable(attributes.get(checkNotNull(key)));
  }

  /**
   * Returns the value stored under the given key cast to {@code type}, if any. A value present under the
   * key but not assignable to {@code type} is treated as absent (returns {@link Optional#empty()}) rather
   * than throwing, so a key-namespace collision between modules cannot surface as a {@code
   * ClassCastException} at a consumer's start-up.
   */
  public <T> Optional<T> get(final String key, final Class<T> type) {
    return get(key).filter(type::isInstance).map(type::cast);
  }
}
