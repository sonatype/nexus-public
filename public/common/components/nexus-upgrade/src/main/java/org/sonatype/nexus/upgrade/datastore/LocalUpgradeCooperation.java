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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.sonatype.nexus.common.cooperation2.Cooperation2;
import org.sonatype.nexus.common.cooperation2.Cooperation2Factory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Default {@link UpgradeCooperation} providing single-JVM cooperation via the injected
 * {@link Cooperation2Factory}. Overridden by {@code DistributedUpgradeCooperation} (via {@code @Primary})
 * when clustering is available.
 */
@Component
public class LocalUpgradeCooperation
    implements UpgradeCooperation
{
  private final Cooperation2Factory cooperationFactory;

  private final ConcurrentMap<String, Cooperation2> cooperations = new ConcurrentHashMap<>();

  @Autowired
  public LocalUpgradeCooperation(final Cooperation2Factory cooperationFactory) {
    this.cooperationFactory = checkNotNull(cooperationFactory);
  }

  @Override
  public Cooperation2 get(final String id) {
    // Memoize per id so repeated calls for the same scope share one Cooperation2 (and its coordination
    // state); the local Cooperation2 keeps its coordinating map per-instance, so building a fresh one per
    // call would defeat the "run only once" guarantee.
    return cooperations.computeIfAbsent(id, key -> cooperationFactory.configure().build(key));
  }
}
