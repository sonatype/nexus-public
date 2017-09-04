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
package org.sonatype.nexus.orient.internal.freeze;

import java.util.Map;
import java.util.Optional;
import javax.inject.Provider;

import org.sonatype.nexus.orient.freeze.DatabaseFreezeService;
import org.sonatype.nexus.orient.freeze.FreezeRequest;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricSet;
import com.google.common.collect.ImmutableMap;
import org.joda.time.DateTime;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * {@link Metric} providing information about the database readonly (frozen) status.
 *
 * @since 3.6
 */
public class ReadOnlyMetricSet
  implements MetricSet
{
  private final Map<String,Metric> metrics;

  public ReadOnlyMetricSet(final Provider<DatabaseFreezeService> databaseFreezeServiceProvider) {
    checkNotNull(databaseFreezeServiceProvider);
    this.metrics = ImmutableMap.of(
        "enabled", enabled(databaseFreezeServiceProvider),
        "pending", pending(databaseFreezeServiceProvider),
        "freezeTime", freezeTime(databaseFreezeServiceProvider));
  }

  @Override
  public Map<String,Metric> getMetrics() {
    return metrics;
  }

  private Metric enabled(final Provider<DatabaseFreezeService> databaseFreezeServiceProvider) {
    return (Gauge<Boolean>) () -> Optional.ofNullable(databaseFreezeServiceProvider.get())
        .map(DatabaseFreezeService::isFrozen)
        .orElse(false);
  }

  private Metric pending(final Provider<DatabaseFreezeService> databaseFreezeServiceProvider) {
    return (Gauge<Integer>) () -> Optional.ofNullable(databaseFreezeServiceProvider.get())
        .map(databaseFreezeService -> databaseFreezeService.getState().size())
        .orElse(0);
  }

  private Metric freezeTime(final Provider<DatabaseFreezeService> databaseFreezeServiceProvider) {
    return (Gauge<Long>) () -> Optional.ofNullable(databaseFreezeServiceProvider.get())
        .map(databaseFreezeService -> {
            Long val = databaseFreezeService.getState().stream()
                .map(FreezeRequest::getTimestamp)
                .min(DateTime::compareTo)
                .map(DateTime::getMillis)
                .orElse(0L);
            return val;
        })
        .orElse(0L);
  }
}
