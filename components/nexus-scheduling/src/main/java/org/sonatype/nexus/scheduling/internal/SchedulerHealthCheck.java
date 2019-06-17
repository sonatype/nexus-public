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
package org.sonatype.nexus.scheduling.internal;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.sonatype.nexus.scheduling.spi.SchedulerSPI;

import com.codahale.metrics.health.HealthCheck;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

/**
 * Scheduler health checks that reports a list of task descriptions that were recovered and had manual triggers created
 * automatically. This indicates to a user that those tasks should be reconfigured to their desired specification.
 *
 * @since 3.17
 */
@Named("Scheduler")
@Singleton
public class SchedulerHealthCheck
    extends HealthCheck
{
  private final Provider<SchedulerSPI> scheduler;

  @Inject
  public SchedulerHealthCheck(final Provider<SchedulerSPI> scheduler) {
    this.scheduler = checkNotNull(scheduler);
  }

  @Override
  protected Result check() {
    List<String> missingTaskDescriptions = scheduler.get().getMissingTriggerDescriptions();
    return missingTaskDescriptions.isEmpty() ? Result.healthy() : Result.unhealthy(reason(missingTaskDescriptions));
  }

  private String reason(final List<String> missingTaskDescriptions) {
    String taskDescriptions = String.join(", ", missingTaskDescriptions);
    return format("%s tasks require frequency updates: %s", missingTaskDescriptions.size(), taskDescriptions);
  }
}
