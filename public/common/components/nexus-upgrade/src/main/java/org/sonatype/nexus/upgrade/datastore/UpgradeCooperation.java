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

import org.sonatype.nexus.common.cooperation2.Cooperation2;

/**
 * Provides a {@link Cooperation2} instance for use by UPGRADE-phase migration steps.
 *
 * <p>
 * The default {@link LocalUpgradeCooperation} provides single-JVM cooperation. When clustering is
 * available, {@code DistributedUpgradeCooperation} (in {@code nexus-datastore-clustering}) overrides it
 * (via {@code @Primary}) so that cooperation is coordinated cluster-wide — important for migrations that
 * must run their work only once across the cluster.
 * </p>
 */
public interface UpgradeCooperation
{
  /**
   * Returns the {@link Cooperation2} for the given cooperation id/scope.
   *
   * <p>
   * Implementations memoize by id: repeated calls with the same id return the same {@link Cooperation2}
   * instance (and share its coordination state), so the "run only once" guarantee holds even if a caller
   * does not cache the result.
   * </p>
   */
  Cooperation2 get(String id);
}
