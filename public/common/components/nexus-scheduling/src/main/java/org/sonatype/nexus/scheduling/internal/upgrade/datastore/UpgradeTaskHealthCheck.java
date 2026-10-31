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
package org.sonatype.nexus.scheduling.internal.upgrade.datastore;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.sonatype.nexus.scheduling.TaskConfiguration;

import com.codahale.metrics.health.HealthCheck;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Iterables;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import static java.util.Objects.requireNonNull;

/**
 * A health check which fails when queued upgrade tasks have not been run in a timely fashion.
 */
@Qualifier("Pending Upgrade Tasks")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Component
public class UpgradeTaskHealthCheck
    extends HealthCheck
{
  private final LoadingCache<String, Result> cache;

  /**
   * @param age the maximum age of tasks before they should result in an unhealthy result
   * @param cacheDuration the duration a computed result should be cached before returning to the database
   */
  @Autowired
  public UpgradeTaskHealthCheck(
      final UpgradeTaskStore store,
      @Value("${nexus.upgrade.queue.stale.age:1d}") final Duration age,
      @Value("${nexus.upgrade.queue.stale.cache:5m}") final Duration cacheDuration)
  {
    requireNonNull(store);
    requireNonNull(age);
    requireNonNull(cacheDuration);

    cache = CacheBuilder.newBuilder()
        .expireAfterAccess(cacheDuration)
        .build(new CacheLoader<String, Result>()
        {
          @Override
          public Result load(final String key) throws Exception {
            Iterable<UpgradeTaskData> tasks = store.browse(OffsetDateTime.now().minus(age));
            if (Iterables.isEmpty(tasks)) {
              return Result.healthy("No upgrade tasks older than " + age);
            }
            String message = StreamSupport.stream(tasks.spliterator(), false)
                .map(UpgradeTaskData::getConfiguration)
                .map(UpgradeTaskHealthCheck::toConfiguration)
                .map(TaskConfiguration::getTypeId)
                .collect(Collectors.joining(", ",
                    "The following tasks have been queued by upgrades and have not completed: ",
                    ". Upgrade task queue is triggered after startup, look for stuck tasks or task failures after startup"));
            return Result.unhealthy(message);
          }
        });
  }

  @Override
  protected Result check() throws Exception {
    return cache.get("result");
  }

  private static TaskConfiguration toConfiguration(final Map<String, String> attributes) {
    TaskConfiguration config = new TaskConfiguration();
    config.addAll(attributes);
    return config;
  }

}
