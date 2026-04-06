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
package org.sonatype.nexus.internal.metrics;

import java.util.Map;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricSet;
import com.google.common.collect.ImmutableMap;

/**
 * {@link Metric} providing information about the database readonly (frozen) status.
 * Since freeze functionality has been removed, these metrics always return default values.
 *
 * @since 3.6
 */
public class ReadOnlyMetricSet
    implements MetricSet
{
  private final Map<String, Metric> metrics;

  public ReadOnlyMetricSet() {
    this.metrics = ImmutableMap.of(
        "enabled", (Gauge<Boolean>) () -> false,
        "pending", (Gauge<Integer>) () -> 0,
        "freezeTime", (Gauge<Long>) () -> 0L);
  }

  @Override
  public Map<String, Metric> getMetrics() {
    return metrics;
  }
}
